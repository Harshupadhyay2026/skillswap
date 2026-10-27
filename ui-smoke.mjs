// Headless UI smoke test for the new Phase 3/4 pages.
// Starts Chrome with remote debugging, seeds a real JWT + conversation, then
// checks each route for rendered content and console errors.
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const APP = 'http://localhost:5173';
const API = 'http://localhost:9090/api';
const DEBUG_PORT = 9222;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function jsonFetch(url, options = {}) {
  const response = await fetch(url, options);
  const body = await response.text();
  let parsed;
  try { parsed = JSON.parse(body); } catch { parsed = body; }
  if (!response.ok) {
    throw new Error(`${options.method || 'GET'} ${url} -> ${response.status}: ${body.slice(0, 300)}`);
  }
  return parsed;
}

async function register(name, username) {
  const res = await jsonFetch(`${API}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name,
      username,
      email: `${username}@test.com`,
      password: 'secret123',
      college: 'UI Test College',
      branch: 'CSE',
      year: '2nd Year',
      mode: 'Both',
    }),
  });
  return { token: res.data.accessToken, user: res.data.user };
}

async function login(usernameOrEmail, password) {
  const res = await jsonFetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usernameOrEmail, password }),
  });
  return res.data;
}

function auth(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

const stamp = Date.now().toString().slice(-6);
const alice = await register('Alice UI', `alice_ui_${stamp}`);
const bob = await register('Bob UI', `bob_ui_${stamp}`);

// Conversation + two messages.
const conversation = await jsonFetch(`${API}/conversations`, {
  method: 'POST',
  headers: auth(alice.token),
  body: JSON.stringify({ userId: bob.user.id }),
});
const conversationId = conversation.data.id;

await jsonFetch(`${API}/conversations/${conversationId}/messages`, {
  method: 'POST',
  headers: auth(alice.token),
  body: JSON.stringify({ body: 'Hello from the UI check' }),
});
await jsonFetch(`${API}/conversations/${conversationId}/messages`, {
  method: 'POST',
  headers: auth(bob.token),
  body: JSON.stringify({ body: 'Hi Alice, good to see you' }),
});

// Q&A question -> answer -> accept, so Bob has reputation + a notification.
const problem = await jsonFetch(`${API}/problems`, {
  method: 'POST',
  headers: auth(alice.token),
  body: JSON.stringify({
    title: `UI smoke question ${stamp}`,
    description: 'Checking that the Q&A page renders.',
    category: 'Programming',
    level: 'BEGINNER',
  }),
});
const answer = await jsonFetch(`${API}/problems/${problem.data.id}/answers`, {
  method: 'POST',
  headers: auth(bob.token),
  body: JSON.stringify({ body: 'This is a UI smoke-test answer.' }),
});
await jsonFetch(`${API}/problems/${problem.data.id}/answers/${answer.data.id}/accept`, {
  method: 'PUT',
  headers: auth(alice.token),
  body: '{}',
});

// Avatar upload for Alice.
const png = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  'base64'
);
const form = new FormData();
form.append('file', new Blob([png], { type: 'image/png' }), 'avatar.png');
const avatarRes = await jsonFetch(`${API}/users/me/avatar`, {
  method: 'POST',
  headers: { Authorization: `Bearer ${alice.token}` },
  body: form,
});
if (!avatarRes.data?.avatarUrl) throw new Error('Avatar upload did not return a URL');

// Skills + accepted swap + accepted learning session for the embedded-room checks.
const pythonCatalog = await jsonFetch(`${API}/skills?query=Python`);
const pythonSkill = pythonCatalog.data.find((s) => s.name === 'Python');
const reactCatalog = await jsonFetch(`${API}/skills?query=React`);
const reactSkill = reactCatalog.data.find((s) => s.name === 'React');
if (!pythonSkill || !reactSkill) throw new Error('Seed skills are missing');

const addSkill = (token, skillId) =>
  jsonFetch(`${API}/users/skills`, {
    method: 'POST',
    headers: auth(token),
    body: JSON.stringify({
      skillId,
      skillType: 'TEACH',
      proficiency: 'INTERMEDIATE',
      pricePerSession: 20,
    }),
  });

await addSkill(alice.token, pythonSkill.id);
await addSkill(bob.token, reactSkill.id);

const swap = await jsonFetch(`${API}/swaps`, {
  method: 'POST',
  headers: auth(alice.token),
  body: JSON.stringify({
    receiverId: bob.user.id,
    offeredSkillId: pythonSkill.id,
    requestedSkillId: reactSkill.id,
    message: 'UI smoke video room',
  }),
});
await jsonFetch(`${API}/swaps/${swap.data.id}/accept`, {
  method: 'PUT',
  headers: auth(bob.token),
  body: '{}',
});

const learning = await jsonFetch(`${API}/learning`, {
  method: 'POST',
  headers: auth(bob.token),
  body: JSON.stringify({
    teacherId: alice.user.id,
    skillId: pythonSkill.id,
    message: 'UI smoke learning room',
  }),
});
await jsonFetch(`${API}/learning/${learning.data.id}/accept`, {
  method: 'PUT',
  headers: auth(alice.token),
  body: '{}',
});

// A note seeded through the API proves completed sessions keep their notes.
await jsonFetch(`${API}/sessions/learning/${learning.data.id}/notes`, {
  method: 'POST',
  headers: auth(bob.token),
  body: JSON.stringify({
    title: 'UI smoke seeded note',
    content: 'Notes should survive the video room and appear on the dashboard.',
    nextStep: 'Review Python loops before the next session.',
  }),
});

// ── Chrome + CDP ────────────────────────────────────────────────────────────
const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'skillswap-ui-'));
const chrome = spawn(CHROME, [
  '--headless=new',
  `--remote-debugging-port=${DEBUG_PORT}`,
  `--user-data-dir=${userDataDir}`,
  '--no-first-run',
  '--no-default-browser-check',
  '--disable-gpu',
  '--window-size=1360,1000',
  'about:blank',
], { stdio: 'ignore' });

let ws;
let nextId = 0;
const pending = new Map();
let errors = [];
let loadWaiters = [];

async function waitForDebugger() {
  for (let i = 0; i < 60; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/version`);
      if (res.ok) return;
    } catch { /* not up yet */ }
    await sleep(250);
  }
  throw new Error('Chrome debugger did not start');
}

function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', {
    expression,
    returnByValue: true,
    awaitPromise: true,
  });
  if (result.exceptionDetails) {
    throw new Error(result.exceptionDetails.text || 'Evaluation failed');
  }
  return result.result?.value;
}

async function goto(url) {
  errors = [];
  await send('Page.navigate', { url });
  await Promise.race([
    new Promise((resolve) => loadWaiters.push(resolve)),
    sleep(8000),
  ]);
  // Poll until the route finishes its first data fetch instead of guessing.
  let text = '';
  for (let i = 0; i < 40; i++) {
    await sleep(200);
    text = await evaluate('document.body.innerText');
    if (text.length > 80 && !text.includes('Loading')) {
      break;
    }
  }
  await sleep(300);
  return evaluate('document.body.innerText');
}

const checks = [];
function check(name, condition, detail = '') {
  checks.push({ name, ok: Boolean(condition), detail });
  console.log(`  [${condition ? 'PASS' : 'FAIL'}] ${name}${condition ? '' : ` — ${detail}`}`);
}

try {
  await waitForDebugger();

  const tabRes = await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/new?${encodeURIComponent(`${APP}/login`)}`, { method: 'PUT' });
  const tab = await tabRes.json();

  ws = new WebSocket(tab.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = reject;
  });

  ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    if (message.id && pending.has(message.id)) {
      const { resolve, reject } = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) reject(new Error(message.error.message));
      else resolve(message.result);
      return;
    }

    if (message.method === 'Page.loadEventFired') {
      const waiters = loadWaiters;
      loadWaiters = [];
      waiters.forEach((resolve) => resolve());
    }

    if (message.method === 'Runtime.exceptionThrown') {
      const details = message.params.exceptionDetails;
      errors.push(`exception: ${details?.exception?.description || details?.text || 'unknown'}`);
    }

    if (message.method === 'Runtime.consoleAPICalled' && message.params.type === 'error') {
      errors.push(
        `console.error: ${message.params.args.map((a) => a.value || a.description || a.type).join(' ')}`
      );
    }

    if (message.method === 'Log.entryAdded' && message.params.entry.level === 'error') {
      errors.push(`log.error: ${message.params.entry.text}`);
    }
  };

  await send('Page.enable');
  await send('Runtime.enable');
  await send('Log.enable');

  // Seed the session on the app's origin, then reload.
  await goto(`${APP}/login`);
  await evaluate(`
    localStorage.setItem('skillswap_access_token', ${JSON.stringify(bob.token)});
    localStorage.setItem('skillswapUser', ${JSON.stringify(JSON.stringify(bob.user))});
    true;
  `);

  const qa = await goto(`${APP}/qa`);
  check('Q&A page renders', qa.includes('Q&A') && qa.includes('Ask a question'), qa.slice(0, 160));
  check('Q&A page has no console errors', errors.length === 0, errors.join(' | '));

  const detail = await goto(`${APP}/qa/${problem.data.id}`);
  check('Question detail renders', detail.includes(`UI smoke question ${stamp}`), detail.slice(0, 160));
  check('Question detail has no console errors', errors.length === 0, errors.join(' | '));

  const community = await goto(`${APP}/community`);
  check('Community page renders', community.includes('Top contributors'), community.slice(0, 160));
  check('Community shows the accepted answerer', community.includes('Bob UI'), community.slice(0, 300));
  check('Community page has no console errors', errors.length === 0, errors.join(' | '));

  const messages = await goto(`${APP}/messages`);
  check('Messages page renders', messages.includes('Messages') && messages.includes('Conversations'), messages.slice(0, 200));
  check('Messages inbox lists the counterpart', messages.includes('Alice UI'), messages.slice(0, 300));
  check('Messages page has no console errors', errors.length === 0, errors.join(' | '));

  const thread = await goto(`${APP}/messages/${conversationId}`);
  check('Thread renders both messages', thread.includes('Hello from the UI check') && thread.includes('Hi Alice, good to see you'), thread.slice(0, 300));
  const composeBox = await evaluate('Boolean(document.querySelector(\'textarea[aria-label="Message"]\'))');
  check('Thread has a compose box', composeBox, `compose=${composeBox}`);
  check('Thread has no console errors', errors.length === 0, errors.join(' | '));

  const notifications = await goto(`${APP}/notifications`);
  check('Notifications page renders', notifications.includes('Notifications'), notifications.slice(0, 160));
  check('Notifications include the accepted answer', notifications.includes('Your answer was accepted'), notifications.slice(0, 300));
  check('Notifications show email delivery status', notifications.includes('Email notifications'), notifications.slice(0, 220));
  check('Notifications page has no console errors', errors.length === 0, errors.join(' | '));

  const requests = await goto(`${APP}/requests`);
  check('Requests page shows the accepted swap', requests.includes('Join Meeting'), requests.slice(0, 300));
  const swapRoomHref = await evaluate(
    `document.querySelector('a[href="/session/swap/${swap.data.id}"]')?.getAttribute('href') || ''`
  );
  check(
    'Join Meeting opens the in-app swap room',
    swapRoomHref === `/session/swap/${swap.data.id}`,
    swapRoomHref
  );
  check('Requests page has no console errors', errors.length === 0, errors.join(' | '));

  const room = await goto(`${APP}/session/swap/${swap.data.id}`);
  check(
    'Swap room page renders with session details',
    room.includes('Video room') && room.includes('Python') && room.includes('React') && room.includes('Alice UI'),
    room.slice(0, 300)
  );
  const joinEnabled = await evaluate(`(() => {
    const b = [...document.querySelectorAll('button')].find((x) => x.textContent.includes('Join video room'));
    return Boolean(b) && !b.disabled;
  })()`);
  check('Join video room button is enabled', joinEnabled);
  const notesPanelVisible = await evaluate(
    'Boolean(document.querySelector(".session-notes")) && document.body.innerText.includes("Session notes")'
  );
  check('Accepted swap room shows shared session notes', notesPanelVisible);

  const notesFilled = await evaluate(`(() => {
    const setValue = (el, value) => {
      const proto = el instanceof HTMLTextAreaElement
        ? HTMLTextAreaElement.prototype
        : HTMLInputElement.prototype;
      Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, value);
      el.dispatchEvent(new Event('input', { bubbles: true }));
    };
    const title = document.querySelector('#session-note-title');
    const content = document.querySelector('#session-note-content');
    const next = document.querySelector('#session-note-next');
    if (!title || !content || !next) return false;
    setValue(title, 'Swap recap');
    setValue(content, 'We traded Python loops for React hooks.');
    setValue(next, 'Build one small hooks example.');
    return true;
  })()`);
  check('Session note form accepts a recap', notesFilled);

  await sleep(250);
  const noteSubmitted = await evaluate(`(() => {
    const button = [...document.querySelectorAll('.session-note-form button')]
      .find((item) => item.textContent.includes('Add note'));
    button?.click();
    return Boolean(button);
  })()`);
  check('Session note can be saved from the room', noteSubmitted);

  let noteText = '';
  for (let i = 0; i < 30; i++) {
    await sleep(200);
    noteText = await evaluate('document.querySelector(".session-note-list")?.innerText || ""');
    if (noteText.includes('We traded Python loops')) break;
  }
  check(
    'Saved session note appears in the shared list',
    noteText.includes('Swap recap') &&
      noteText.includes('We traded Python loops for React hooks.') &&
      noteText.includes('Build one small hooks example.'),
    noteText.slice(0, 320)
  );
  check('Swap room notes have no console errors', errors.length === 0, errors.join(' | '));

  const learningRoom = await goto(`${APP}/session/learning/${learning.data.id}`);
  check(
    'Learning room page renders with session details',
    learningRoom.includes('Learning session') && learningRoom.includes('Python') && learningRoom.includes('Join video room'),
    learningRoom.slice(0, 300)
  );
  check(
    'Learning room shows the seeded shared note',
    learningRoom.includes('UI smoke seeded note') && learningRoom.includes('Notes should survive the video room'),
    learningRoom.slice(0, 400)
  );
  check('Learning room page has no console errors', errors.length === 0, errors.join(' | '));

  // Finally, press Join and wait for Jitsi to mount its iframe. Third-party
  // console noise after this point is expected, so no error assertion follows.
  await evaluate(`(() => {
    const b = [...document.querySelectorAll('button')].find((x) => x.textContent.includes('Join video room'));
    b?.click();
    return Boolean(b);
  })()`);
  let jitsiFrame = false;
  for (let i = 0; i < 60; i++) {
    await sleep(250);
    jitsiFrame = await evaluate('Boolean(document.querySelector(".video-stage iframe"))');
    if (jitsiFrame) break;
  }
  check('Jitsi video iframe is embedded in the room page', jitsiFrame);

  // Complete both accepted sessions through the API so the dashboard has
  // real learning history to summarize.
  await jsonFetch(`${API}/learning/${learning.data.id}/paid`, {
    method: 'PUT',
    headers: auth(bob.token),
    body: '{}',
  });
  await jsonFetch(`${API}/learning/${learning.data.id}/complete`, {
    method: 'PUT',
    headers: auth(bob.token),
    body: '{}',
  });
  await jsonFetch(`${API}/swaps/${swap.data.id}/complete`, {
    method: 'PUT',
    headers: auth(bob.token),
    body: '{}',
  });

  const completedRoom = await goto(`${APP}/session/learning/${learning.data.id}`);
  check(
    'Completed sessions keep their shared notes accessible',
    completedRoom.includes('This session is completed') &&
      completedRoom.includes('UI smoke seeded note') &&
      completedRoom.includes('Notes should survive the video room'),
    completedRoom.slice(0, 400)
  );

  const dashboard = await goto(`${APP}/dashboard`);
  check('Dashboard renders', dashboard.includes('My Profile') && dashboard.includes('Change photo'), dashboard.slice(0, 200));
  check(
    'Dashboard shows the learning progress section',
    dashboard.includes('Learning progress') && dashboard.includes('Small steps, visible progress'),
    dashboard.slice(0, 300)
  );
  check(
    'Progress summarizes completed Python sessions',
    dashboard.includes('Python') && dashboard.includes('2/5 sessions'),
    dashboard.slice(0, 500)
  );
  check('Progress counts captured notes', dashboard.includes('notes captured'), dashboard.slice(0, 400));
  const progressRows = await evaluate('document.querySelectorAll(".progress-row").length');
  check('Progress renders at least one skill row', progressRows > 0, `rows=${progressRows}`);
  check('Dashboard has no console errors', errors.length === 0, errors.join(' | '));

  const profile = await goto(`${APP}/profile/${alice.user.id}`);
  check('Profile renders the Message button', profile.includes('Message') && profile.includes('Alice UI'), profile.slice(0, 200));
  const profileImages = await evaluate(
    'document.querySelectorAll(\'img[src*="/api/files/avatars/"]\').length'
  );
  check('Profile renders the uploaded avatar image', profileImages > 0, `images=${profileImages}`);
  const avatarLoaded = await evaluate(`(() => {
    const img = document.querySelector('img[src*="/api/files/avatars/"]');
    return Boolean(img && img.complete && img.naturalWidth > 0);
  })()`);
  check('Uploaded avatar actually loads', avatarLoaded, `loaded=${avatarLoaded}`);
  check('Profile has no console errors', errors.length === 0, errors.join(' | '));

  const explore = await goto(`${APP}/explore`);
  check(
    'Explore page renders',
    explore.includes('Learn something new') && explore.includes('Meet your next mentor'),
    explore.slice(0, 220)
  );
  // Explore hydrates its teacher cards after the static hero renders, so wait
  // for a grouped card before opening the request modal.
  let groupedCardReady = false;
  for (let i = 0; i < 25; i += 1) {
    groupedCardReady = await evaluate(`[...document.querySelectorAll('.explore-card')].some(
      (item) => item.querySelectorAll('.explore-skill:not(.more)').length > 1
    )`);
    if (groupedCardReady) break;
    await sleep(200);
  }
  const openedGroupedRequest = await evaluate(`(() => {
    const card = [...document.querySelectorAll('.explore-card')].find(
      (item) => item.querySelectorAll('.explore-skill:not(.more)').length > 1
    );
    const button = card?.querySelector('.explore-connect');
    button?.click();
    return Boolean(button);
  })()`);
  for (let i = 0; i < 15; i += 1) {
    if (await evaluate(`Boolean(document.querySelector('.modal h2'))`)) break;
    await sleep(100);
  }
  const groupedRequest = await evaluate(`({
    title: document.querySelector('.modal h2')?.textContent || '',
    options: document.querySelectorAll('#request-skill option').length,
  })`);
  check(
    'Explore request modal offers the teacher’s skill list',
    groupedCardReady && openedGroupedRequest && groupedRequest.title.includes('Learn') && groupedRequest.options > 1,
    JSON.stringify(groupedRequest)
  );
  await evaluate(`document.querySelector('.modal .close')?.click()`);
  check('Explore has no console errors', errors.length === 0, errors.join(' | '));

  const dashboardAgain = await goto(`${APP}/dashboard`);
  const bellPresent = await evaluate(
    'Boolean(document.querySelector(\'.notif-button\')) && Boolean(document.querySelector(\'a[href="/messages"]\'))'
  );
  check('Navbar shows the bell and Messages link', bellPresent, dashboardAgain.slice(0, 120));
  const badgeGone = await evaluate(
    '!document.querySelector(\'a[href="/messages"] .nav-badge\')'
  );
  check('Reading a thread clears the navbar message badge', badgeGone);

  await evaluate('document.querySelector(".notif-button").click(); true;');
  await sleep(900);
  const panelText = await evaluate('document.querySelector(".notif-panel")?.innerText || ""');
  check('Notification bell opens a panel', panelText.includes('Notifications'), panelText.slice(0, 160));
  check(
    'Bell lists the accepted-answer notification',
    panelText.includes('Your answer was accepted'),
    panelText.slice(0, 300)
  );
  check('Bell panel has no console errors', errors.length === 0, errors.join(' | '));

  // ── Reporting + moderation UI ─────────────────────────────────────────────
  await goto(`${APP}/qa/${problem.data.id}`);

  const reportOpened = await evaluate(`(() => {
    const button = [...document.querySelectorAll('button')].find((b) => b.textContent.trim() === '⚑ Report');
    button?.click();
    return Boolean(button);
  })()`);
  check('Question detail exposes a Report action', reportOpened);

  await sleep(500);
  const reasonText = await evaluate('document.querySelector(".report-reasons")?.innerText || ""');
  check(
    'Report dialog explains the reason choices',
    reasonText.includes('Spam or advertising') && reasonText.includes('Harassment or bullying'),
    reasonText.slice(0, 240)
  );

  const reasonPicked = await evaluate(`(() => {
    const label = [...document.querySelectorAll('.report-reason')].find((l) => l.innerText.includes('Spam or advertising'));
    label?.querySelector('input')?.click();
    return Boolean(label);
  })()`);
  check('Report reason can be selected', reasonPicked);

  await evaluate(`(() => {
    const button = [...document.querySelectorAll('.report-modal-actions button')].find((b) => b.textContent.includes('Submit report'));
    button?.click();
    return Boolean(button);
  })()`);

  let reported = false;
  for (let i = 0; i < 30; i++) {
    await sleep(200);
    reported = await evaluate('Boolean(document.querySelector(".report-done"))');
    if (reported) break;
  }
  check('Submitting a report closes the dialog and marks it reported', reported);
  check('Reporting has no console errors', errors.length === 0, errors.join(' | '));

  const adminUsername = process.env.SMOKE_ADMIN_USERNAME;
  if (adminUsername) {
    const adminPassword = process.env.SMOKE_ADMIN_PASSWORD || 'secret123';
    const adminSession = await login(adminUsername, adminPassword);
    const adminMe = await jsonFetch(`${API}/users/me`, {
      headers: auth(adminSession.accessToken),
    });

    await goto(`${APP}/login`);
    await evaluate(`
      localStorage.setItem('skillswap_access_token', ${JSON.stringify(adminSession.accessToken)});
      localStorage.setItem('skillswapUser', ${JSON.stringify(JSON.stringify(adminMe.data))});
      true;
    `);

    let adminPage = await goto(`${APP}/admin/reports`);
    for (let i = 0; i < 30; i++) {
      if (adminPage.includes('Moderation queue') && adminPage.includes(`UI smoke question ${stamp}`)) break;
      await sleep(200);
      adminPage = await evaluate('document.body.innerText');
    }

    check(
      'Moderation queue renders for an admin',
      adminPage.includes('Moderation queue') &&
        adminPage.toLowerCase().includes('community safety'),
      adminPage.slice(0, 240)
    );
    check(
      'Queue lists the report filed through the UI',
      adminPage.includes(`UI smoke question ${stamp}`),
      adminPage.slice(0, 320)
    );

    const rowOpened = await evaluate(`(() => {
      const row = [...document.querySelectorAll('.report-row')].find((r) => r.innerText.includes(${JSON.stringify(`UI smoke question ${stamp}`)}));
      row?.click();
      return Boolean(row);
    })()`);
    check('Report queue row is selectable', rowOpened);

    await sleep(900);
    const detailText = await evaluate('document.querySelector(".admin-detail")?.innerText || ""');
    const detailLower = detailText.toLowerCase();
    check(
      'Report detail shows reporter context and decisions',
      detailLower.includes('reported by') &&
        detailLower.includes('claim for review') &&
        detailLower.includes('dismiss'),
      detailText.slice(0, 320)
    );

    const claimedClick = await evaluate(`(() => {
      const button = [...document.querySelectorAll('.admin-detail button')].find((b) => b.textContent.includes('Claim for review'));
      button?.click();
      return Boolean(button);
    })()`);
    check('Moderator can claim a report', claimedClick);

    let claimedText = '';
    for (let i = 0; i < 25; i++) {
      await sleep(200);
      claimedText = await evaluate('document.querySelector(".admin-detail")?.innerText || ""');
      if (claimedText.toUpperCase().includes('IN REVIEW')) break;
    }
    check(
      'Claimed report moves to IN REVIEW',
      claimedText.toUpperCase().includes('IN REVIEW'),
      claimedText.slice(0, 240)
    );

    const dismissOpened = await evaluate(`(() => {
      const button = [...document.querySelectorAll('.admin-actions button')].find((b) => b.textContent.trim() === 'Dismiss');
      button?.click();
      return Boolean(button);
    })()`);
    check('Dismiss opens a confirmation form', dismissOpened);

    await sleep(300);
    const confirmDismiss = await evaluate(`(() => {
      const button = [...document.querySelectorAll('.admin-action-form button')].find((b) => b.textContent.includes('Confirm dismiss'));
      button?.click();
      return Boolean(button);
    })()`);
    check('Dismiss confirmation is available', confirmDismiss);

    let dismissedText = '';
    for (let i = 0; i < 30; i++) {
      await sleep(200);
      dismissedText = await evaluate('document.querySelector(".admin-detail")?.innerText || ""');
      if (dismissedText.toUpperCase().includes('DISMISSED')) break;
    }
    check(
      'Dismissing records the decision in the audit trail',
      dismissedText.toUpperCase().includes('DISMISSED') &&
        dismissedText.toLowerCase().includes('decision history'),
      dismissedText.slice(0, 320)
    );
    check('Moderation page has no console errors', errors.length === 0, errors.join(' | '));

    // Back to Bob's session: the page itself must refuse non-moderators.
    await goto(`${APP}/login`);
    await evaluate(`
      localStorage.setItem('skillswap_access_token', ${JSON.stringify(bob.token)});
      localStorage.setItem('skillswapUser', ${JSON.stringify(JSON.stringify(bob.user))});
      true;
    `);
    const guard = await goto(`${APP}/admin/reports`);
    check('Non-moderators see the access guard', guard.includes('Moderator access only'), guard.slice(0, 200));
  } else {
    const guard = await goto(`${APP}/admin/reports`);
    check('Non-moderators see the access guard', guard.includes('Moderator access only'), guard.slice(0, 200));
  }
} finally {
  try { ws?.close(); } catch { /* ignore */ }
  chrome.kill();
  await sleep(300);
  try { fs.rmSync(userDataDir, { recursive: true, force: true }); } catch { /* ignore */ }
}

const passed = checks.filter((c) => c.ok).length;
console.log(`\n=== UI smoke: ${passed}/${checks.length} checks passed ===`);
if (passed !== checks.length) process.exit(1);
