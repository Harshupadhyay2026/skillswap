import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import swapService from '../services/swapService';
import learningService from '../services/learningService';
import Avatar from '../components/ui/Avatar';
import StatusBadge from '../components/swap/StatusBadge';
import SessionNotesPanel from '../components/session/SessionNotesPanel';

/** Splits `https://meet.jit.si/room-name` into the pieces the Jitsi API needs. */
function parseMeetingLink(link) {
  if (!link) return null;

  try {
    const url = new URL(link);
    const room = decodeURIComponent(url.pathname.replace(/^\/+/, ''));
    if (!url.hostname || !room) return null;
    return { domain: url.hostname, room };
  } catch {
    return null;
  }
}

function loadJitsiScript(domain) {
  if (window.JitsiMeetExternalAPI) return Promise.resolve();

  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-skillswap-jitsi]');
    if (existing) {
      existing.addEventListener('load', () => resolve(), { once: true });
      existing.addEventListener(
        'error',
        () => reject(new Error('Could not load the video service.')),
        { once: true }
      );
      return;
    }

    const script = document.createElement('script');
    script.src = `https://${domain}/external_api.js`;
    script.async = true;
    script.dataset.skillswapJitsi = 'true';
    script.onload = () => resolve();
    script.onerror = () =>
      reject(new Error('Could not load the video service. Check your connection and try again.'));
    document.head.appendChild(script);
  });
}

function formatDateTime(value) {
  if (!value) return 'Not scheduled';
  return new Date(value).toLocaleString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/**
 * In-app video room.
 *
 * Both swap and learning sessions store a Jitsi URL. This page loads the
 * session through a participants-only endpoint, then embeds Jitsi with the
 * External API — no new tab, no copy/pasting links. The "Open in new tab"
 * fallback stays available for browsers that block embedded media.
 */
export default function VideoRoomPage({ sessionType = 'swap' }) {
  const { id } = useParams();
  const { user, isAuthenticated, booting } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const isLearning = sessionType === 'learning';

  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [joined, setJoined] = useState(false);
  const [apiReady, setApiReady] = useState(false);
  const [embedError, setEmbedError] = useState('');

  const containerRef = useRef(null);
  const apiRef = useRef(null);

  useEffect(() => {
    if (!booting && !isAuthenticated) {
      navigate('/login', {
        state: { from: `/session/${sessionType}/${id}` },
        replace: true,
      });
    }
  }, [booting, isAuthenticated, navigate, sessionType, id]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;

    let cancelled = false;
    setLoading(true);
    setError('');

    const request = isLearning ? learningService.get(id) : swapService.getSwap(id);

    request
      .then((res) => {
        if (!cancelled) setSession(res.data);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err?.status === 404) {
          setError('This session is not available to your account.');
        } else if (err?.status !== 401) {
          setError(err?.message || 'Could not load this session.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id, isAuthenticated, isLearning]);

  const counterpart = useMemo(() => {
    if (!session) return null;
    if (isLearning) {
      return session.learner?.id === user?.id ? session.teacher : session.learner;
    }
    return session.sender?.id === user?.id ? session.receiver : session.sender;
  }, [session, isLearning, user?.id]);

  const meeting = useMemo(() => parseMeetingLink(session?.meetingLink), [session?.meetingLink]);
  const canJoin = session?.status === 'ACCEPTED' && Boolean(meeting);
  const canUseNotes = session?.status === 'ACCEPTED' || session?.status === 'COMPLETED';

  const title = session
    ? isLearning
      ? `${session.skill?.name || 'Learning'} session`
      : `${session.offeredSkill?.name || 'Skill'} ⇄ ${session.requestedSkill?.name || 'Skill'}`
    : 'Video room';

  const unavailable = useMemo(() => {
    if (!session) return '';
    if (session.status === 'PENDING') return 'The room opens after the request is accepted.';
    if (session.status === 'DECLINED') return 'This request was declined, so no room was created.';
    if (session.status === 'CANCELLED') return 'This request was cancelled, so the room is closed.';
    if (session.status === 'COMPLETED') {
      return 'This session is completed. The room is closed, but your shared notes are still below.';
    }
    if (session.status === 'ACCEPTED' && !meeting) {
      return 'The meeting link is missing. Open My Requests and check the session.';
    }
    return '';
  }, [session, meeting]);

  // Mount / dispose the Jitsi iframe only after the user presses Join.
  useEffect(() => {
    if (!joined || !meeting || !containerRef.current) return undefined;

    let disposed = false;
    let api = null;
    setEmbedError('');
    setApiReady(false);

    (async () => {
      try {
        await loadJitsiScript(meeting.domain);
        if (disposed || !containerRef.current) return;

        api = new window.JitsiMeetExternalAPI(meeting.domain, {
          roomName: meeting.room,
          parentNode: containerRef.current,
          width: '100%',
          height: '100%',
          userInfo: { displayName: user?.fullName || 'SkillSwap student' },
          configOverwrite: {
            prejoinPageEnabled: false,
            startWithAudioMuted: true,
            startWithVideoMuted: false,
            disableDeepLinking: true,
            enableWelcomePage: false,
            requireDisplayName: false,
          },
          interfaceConfigOverwrite: {
            MOBILE_APP_PROMO: false,
            SHOW_JITSI_WATERMARK: false,
            DISABLE_JOIN_LEAVE_NOTIFICATIONS: true,
          },
        });

        apiRef.current = api;
        setApiReady(true);

        api.addEventListener('videoConferenceLeft', () => {
          setJoined(false);
          setApiReady(false);
        });
        api.addEventListener('readyToClose', () => {
          setJoined(false);
          setApiReady(false);
        });
      } catch (err) {
        if (!disposed) setEmbedError(err?.message || 'Could not load the video room.');
      }
    })();

    return () => {
      disposed = true;
      try {
        api?.dispose();
      } catch {
        /* already gone */
      }
      if (apiRef.current === api) apiRef.current = null;
      setApiReady(false);
    };
  }, [joined, meeting, user?.fullName]);

  const joinRoom = () => {
    setEmbedError('');
    setJoined(true);
  };

  const leaveRoom = () => {
    try {
      apiRef.current?.dispose();
    } catch {
      /* ignore */
    }
    apiRef.current = null;
    setApiReady(false);
    setJoined(false);
    setEmbedError('');
  };

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(session.meetingLink);
      showToast('Meeting link copied.');
    } catch {
      showToast('Could not copy the link — select it and copy manually.');
    }
  };

  if (booting) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading session…</p>
        </div>
      </main>
    );
  }

  if (!isAuthenticated) return null;

  if (loading) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading session…</p>
        </div>
      </main>
    );
  }

  if (error || !session) {
    return (
      <main className="page">
        <div className="container narrow">
          <div className="card empty-state">
            <div style={{ fontSize: '36px', marginBottom: '10px' }}>🎥</div>
            <h2>Room unavailable</h2>
            <p className="muted" style={{ margin: '8px 0 18px' }}>
              {error || 'Could not load this session.'}
            </p>
            <Link to="/requests" className="btn btn-primary">
              Back to My Requests
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="page">
      <div className="container">
        <div className="video-page-head">
          <Link to="/requests" className="btn btn-outline">
            ← Back to requests
          </Link>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {session.meetingLink && (
              <a
                href={session.meetingLink}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-outline"
              >
                Open in new tab ↗
              </a>
            )}
            {joined && (
              <button type="button" className="btn btn-light" onClick={leaveRoom}>
                Leave room
              </button>
            )}
          </div>
        </div>

        <div className="video-intro card">
          <Avatar name={counterpart?.fullName} src={counterpart?.avatarUrl} size={64} />
          <div className="video-intro-main">
            <span className="tag">🎥 Video room</span>
            <h1>{title}</h1>
            <p className="muted" style={{ margin: 0 }}>
              with {counterpart?.fullName || 'your partner'}
              {counterpart?.college ? ` • ${counterpart.college}` : ''}
            </p>
          </div>
          <StatusBadge status={session.status} />
        </div>

        {joined ? (
          <div className="video-stage-wrap card">
            <div ref={containerRef} className="video-stage" />

            {!apiReady && (
              <div className="video-overlay">
                {embedError ? (
                  <>
                    <div style={{ fontSize: '32px' }}>📵</div>
                    <strong>Could not start the video room</strong>
                    <p className="muted" style={{ maxWidth: '440px', margin: 0 }}>
                      {embedError}
                    </p>
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', justifyContent: 'center' }}>
                      {session.meetingLink && (
                        <a
                          href={session.meetingLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="btn btn-primary"
                        >
                          Open in a new tab
                        </a>
                      )}
                      <button type="button" className="btn btn-outline" onClick={leaveRoom}>
                        Back to session
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <div className="video-spinner" aria-hidden="true" />
                    <strong>Connecting to the video room…</strong>
                    <p className="muted" style={{ margin: 0 }}>
                      Allow camera and microphone access when your browser asks.
                    </p>
                  </>
                )}
              </div>
            )}
          </div>
        ) : (
          <div className="video-grid">
            <section className="video-preview card">
              <div className="video-preview-art" aria-hidden="true">
                🎥
              </div>
              <h2>{canJoin ? 'Ready when you are' : 'Room not open yet'}</h2>
              <p className="muted">
                {unavailable ||
                  'Join the room right here — the video call opens inside SkillSwap, no link hunting needed.'}
              </p>

              <div className="video-actions">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={joinRoom}
                  disabled={!canJoin}
                >
                  🎥 Join video room
                </button>
                {session.meetingLink && (
                  <a
                    href={session.meetingLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-outline"
                  >
                    Open in new tab ↗
                  </a>
                )}
              </div>

              {canJoin && (
                <p className="muted" style={{ fontSize: '11.5px', marginTop: '14px' }}>
                  Your camera starts off, microphone muted. You can turn them on inside the room.
                </p>
              )}
            </section>

            <aside className="video-side card">
              <h3>Session details</h3>

              <div className="video-detail">
                <span>Type</span>
                <strong>{isLearning ? 'Learning session' : 'Skill swap'}</strong>
              </div>

              <div className="video-detail">
                <span>{isLearning ? 'Skill' : 'Skills'}</span>
                <strong>
                  {isLearning
                    ? `${session.skill?.icon || ''} ${session.skill?.name || '—'}`
                    : `${session.offeredSkill?.icon || ''} ${session.offeredSkill?.name || '—'} ⇄ ${
                        session.requestedSkill?.icon || ''
                      } ${session.requestedSkill?.name || '—'}`}
                </strong>
              </div>

              <div className="video-detail">
                <span>With</span>
                <strong>
                  {counterpart ? (
                    <Link to={`/profile/${counterpart.id}`}>{counterpart.fullName}</Link>
                  ) : (
                    '—'
                  )}
                </strong>
              </div>

              <div className="video-detail">
                <span>Scheduled</span>
                <strong>{formatDateTime(session.scheduledAt)}</strong>
              </div>

              {isLearning && (
                <div className="video-detail">
                  <span>Exchange</span>
                  <strong>Free skill swap</strong>
                </div>
              )}

              {session.meetingLink && (
                <>
                  <div className="video-detail">
                    <span>Room link</span>
                  </div>
                  <div className="video-link">
                    <input
                      readOnly
                      value={session.meetingLink}
                      onFocus={(event) => event.target.select()}
                      aria-label="Meeting link"
                    />
                    <button type="button" onClick={copyLink}>
                      Copy
                    </button>
                  </div>
                </>
              )}

              <p className="muted" style={{ fontSize: '11.5px', lineHeight: 1.55, marginTop: '14px' }}>
                The other participant joins from their own My Requests page. The room is private to
                the two of you — anyone with the link can enter, so only share it with your partner.
              </p>
            </aside>
          </div>
        )}

        <SessionNotesPanel
          enabled={canUseNotes}
          sessionType={sessionType}
          sessionId={session.id}
          currentUserId={user?.id}
        />
      </div>
    </main>
  );
}
