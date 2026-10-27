import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import messageService from '../services/messageService';
import Avatar from '../components/ui/Avatar';
import { messageTime, timeAgo } from '../lib/format';

function mergeMessages(previous, incoming) {
  const map = new Map(previous.map((m) => [m.id, m]));
  incoming.forEach((m) => map.set(m.id, m));
  return [...map.values()].sort((a, b) => a.id - b.id);
}

function dayLabel(value) {
  const date = new Date(value);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);

  const sameDay = (a, b) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate();

  if (sameDay(date, today)) return 'Today';
  if (sameDay(date, yesterday)) return 'Yesterday';

  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: date.getFullYear() === today.getFullYear() ? undefined : 'numeric',
  });
}

/**
 * Two-pane inbox. The left pane lists conversations, the right pane is the
 * active thread. The thread polls every 4 seconds and the inbox every 15, which
 * keeps the UI live without requiring a WebSocket server.
 */
export default function MessagesPage() {
  const { conversationId } = useParams();
  const activeId = conversationId ? Number(conversationId) : null;

  const { user, isAuthenticated, booting } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [conversations, setConversations] = useState([]);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [loadingList, setLoadingList] = useState(true);
  const [loadingThread, setLoadingThread] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [sending, setSending] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [error, setError] = useState('');

  const bottomRef = useRef(null);
  const threadRef = useRef(null);

  useEffect(() => {
    if (!booting && !isAuthenticated) {
      navigate('/login', {
        state: { from: activeId ? `/messages/${activeId}` : '/messages' },
        replace: true,
      });
    }
  }, [booting, isAuthenticated, navigate, activeId]);

  const loadConversations = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const list = await messageService.listConversations();
      setConversations(list);
    } catch (err) {
      setError(err?.message || 'Could not load your conversations.');
    } finally {
      setLoadingList(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    loadConversations();
    const interval = setInterval(loadConversations, 15000);
    return () => clearInterval(interval);
  }, [isAuthenticated, loadConversations]);

  const loadThread = useCallback(
    async (id, { silent = false } = {}) => {
      if (!id) return;
      if (!silent) setLoadingThread(true);

      try {
        const page = await messageService.getMessages(id, { size: 50 });

        setMessages((previous) => (silent ? mergeMessages(previous, page) : page));
        setHasMore(page.length === 50);

        if (page.some((m) => m.sender?.id !== user?.id && !m.read)) {
          await messageService.markRead(id);
          // Clear the navbar badge without waiting for its 45s poll.
          window.dispatchEvent(new CustomEvent('skillswap:messages-changed'));
        }

        // Keep the header and list row in sync, including deep links.
        if (!silent) {
          try {
            const conversation = await messageService.getConversation(id);
            setConversations((list) => {
              const exists = list.some((c) => c.id === id);
              const normalised = { ...conversation, unreadCount: 0 };
              return exists
                ? list.map((c) => (c.id === id ? { ...c, ...normalised } : c))
                : [normalised, ...list];
            });
          } catch {
            /* list will catch up on the next poll */
          }
        } else {
          setConversations((list) =>
            list.map((c) => (c.id === id ? { ...c, unreadCount: 0 } : c))
          );
        }

        if (!silent) {
          requestAnimationFrame(() => {
            bottomRef.current?.scrollIntoView({ block: 'end' });
          });
        }
      } catch (err) {
        if (!silent) {
          if (err?.status === 404) {
            showToast('That conversation is not available.');
            navigate('/messages', { replace: true });
          } else {
            setError(err?.message || 'Could not load this conversation.');
          }
        }
      } finally {
        if (!silent) setLoadingThread(false);
      }
    },
    [navigate, showToast, user?.id]
  );

  useEffect(() => {
    if (!activeId || !isAuthenticated) {
      setMessages([]);
      return;
    }
    setDraft('');
    setError('');
    loadThread(activeId);
  }, [activeId, isAuthenticated, loadThread]);

  // Poll the open thread for new messages.
  useEffect(() => {
    if (!activeId || !isAuthenticated) return undefined;
    const interval = setInterval(() => loadThread(activeId, { silent: true }), 4000);
    return () => clearInterval(interval);
  }, [activeId, isAuthenticated, loadThread]);

  const activeConversation = useMemo(
    () => conversations.find((c) => c.id === activeId) || null,
    [conversations, activeId]
  );

  const counterpart = activeConversation?.counterpart;

  const loadOlder = async () => {
    if (!activeId || !messages.length || loadingOlder) return;

    const element = threadRef.current;
    const previousHeight = element?.scrollHeight || 0;

    setLoadingOlder(true);
    try {
      const older = await messageService.getMessages(activeId, {
        before: messages[0].id,
        size: 50,
      });
      setMessages((previous) => mergeMessages(older, previous));
      setHasMore(older.length === 50);

      requestAnimationFrame(() => {
        if (element) element.scrollTop = element.scrollHeight - previousHeight;
      });
    } catch (err) {
      showToast(err?.message || 'Could not load earlier messages.');
    } finally {
      setLoadingOlder(false);
    }
  };

  const send = async (event) => {
    event?.preventDefault();
    const text = draft.trim();
    if (!text || !activeId || sending) return;

    setSending(true);
    try {
      const sent = await messageService.sendMessage(activeId, text);
      setMessages((previous) => mergeMessages(previous, [sent]));
      setDraft('');
      loadConversations();
      requestAnimationFrame(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
      });
    } catch (err) {
      showToast(err?.message || 'Could not send the message.');
    } finally {
      setSending(false);
    }
  };

  const onComposeKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send();
    }
  };

  if (booting) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading messages…</p>
        </div>
      </main>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Messages</h1>
            <p className="muted">
              Sort out details with your swap and session partners.
            </p>
          </div>
          <Link to="/explore" className="btn btn-outline">
            Find people
          </Link>
        </div>

        {error && (
          <div className="card" style={{ marginBottom: '14px', borderColor: 'var(--danger, #ef4444)' }}>
            <p className="muted">{error}</p>
          </div>
        )}

        <div className={`messages-layout${activeId ? ' has-active' : ''}`}>
          <aside className="conversation-list card">
            <div className="conversation-list-head">
              <strong>Conversations</strong>
              <span className="muted">{conversations.length}</span>
            </div>

            {loadingList ? (
              <div className="chat-placeholder muted">Loading…</div>
            ) : conversations.length === 0 ? (
              <div className="chat-placeholder">
                <div style={{ fontSize: '28px', marginBottom: '8px' }}>💬</div>
                <strong>No conversations yet</strong>
                <p className="muted" style={{ marginTop: '6px' }}>
                  Open someone&apos;s profile and press <strong>Message</strong> to start.
                </p>
                <Link to="/explore" className="btn btn-outline" style={{ marginTop: '12px' }}>
                  Browse people
                </Link>
              </div>
            ) : (
              conversations.map((conversation) => (
                <button
                  key={conversation.id}
                  type="button"
                  className={`conversation-item${conversation.id === activeId ? ' active' : ''}${
                    conversation.unreadCount > 0 ? ' unread' : ''
                  }`}
                  onClick={() => navigate(`/messages/${conversation.id}`)}
                >
                  <Avatar
                    name={conversation.counterpart?.fullName}
                    src={conversation.counterpart?.avatarUrl}
                    size={40}
                  />
                  <span className="conversation-main">
                    <span className="conversation-top">
                      <strong>{conversation.counterpart?.fullName || 'Student'}</strong>
                      <span className="muted">
                        {timeAgo(conversation.lastMessageAt || conversation.createdAt)}
                      </span>
                    </span>
                    <span className="conversation-preview">
                      {conversation.lastMessage ||
                        (conversation.lastMessageSenderId ? '' : 'Say hello 👋')}
                    </span>
                  </span>
                  {conversation.unreadCount > 0 && (
                    <span className="nav-badge">
                      {conversation.unreadCount > 9 ? '9+' : conversation.unreadCount}
                    </span>
                  )}
                </button>
              ))
            )}
          </aside>

          <section className="chat card">
            {!activeId ? (
              <div className="chat-placeholder">
                <div style={{ fontSize: '34px', marginBottom: '10px' }}>💬</div>
                <strong>Select a conversation</strong>
                <p className="muted" style={{ marginTop: '6px' }}>
                  Messages are private between you and the other person.
                </p>
              </div>
            ) : (
              <>
                <header className="chat-head">
                  <button
                    type="button"
                    className="btn-icon chat-back"
                    onClick={() => navigate('/messages')}
                    aria-label="Back to conversations"
                  >
                    ←
                  </button>
                  <Link to={counterpart ? `/profile/${counterpart.id}` : '/messages'} style={{ display: 'flex' }}>
                    <Avatar name={counterpart?.fullName} src={counterpart?.avatarUrl} size={40} />
                  </Link>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <strong style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {counterpart?.fullName || 'Conversation'}
                    </strong>
                    <div className="muted" style={{ fontSize: '12px' }}>
                      {counterpart?.username ? `@${counterpart.username}` : ''}
                    </div>
                  </div>
                  {counterpart && (
                    <Link to={`/profile/${counterpart.id}`} className="btn btn-outline btn-small">
                      View profile
                    </Link>
                  )}
                </header>

                <div className="chat-body" ref={threadRef}>
                  {loadingThread ? (
                    <div className="chat-placeholder muted">Loading messages…</div>
                  ) : messages.length === 0 ? (
                    <div className="chat-placeholder">
                      <div style={{ fontSize: '28px', marginBottom: '8px' }}>👋</div>
                      <strong>Start the conversation</strong>
                      <p className="muted" style={{ marginTop: '6px' }}>
                        Say hi and agree on a time, place or meeting link.
                      </p>
                    </div>
                  ) : (
                    <>
                      {hasMore && (
                        <button
                          type="button"
                          className="load-older"
                          onClick={loadOlder}
                          disabled={loadingOlder}
                        >
                          {loadingOlder ? 'Loading…' : 'Load earlier messages'}
                        </button>
                      )}

                      {messages.map((message, index) => {
                        const mine = message.sender?.id === user?.id;
                        const previous = messages[index - 1];
                        const showDay =
                          !previous ||
                          new Date(previous.createdAt).toDateString() !==
                            new Date(message.createdAt).toDateString();

                        return (
                          <div key={message.id}>
                            {showDay && <div className="chat-day">{dayLabel(message.createdAt)}</div>}
                            <div className={`bubble-row ${mine ? 'mine' : 'theirs'}`}>
                              {!mine && (
                                <Avatar
                                  name={message.sender?.fullName}
                                  src={message.sender?.avatarUrl}
                                  size={28}
                                />
                              )}
                              <div className={`bubble ${mine ? 'mine' : 'theirs'}`}>
                                <div className="bubble-text">{message.body}</div>
                                <div className="bubble-meta">
                                  {messageTime(message.createdAt)}
                                  {mine && <span title={message.read ? 'Read' : 'Sent'}>
                                    {message.read ? ' ✓✓' : ' ✓'}
                                  </span>}
                                </div>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                      <div ref={bottomRef} />
                    </>
                  )}
                </div>

                <form className="chat-compose" onSubmit={send}>
                  <textarea
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    onKeyDown={onComposeKeyDown}
                    placeholder="Write a message… (Enter to send, Shift+Enter for a new line)"
                    maxLength={2000}
                    rows={2}
                    aria-label="Message"
                  />
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={!draft.trim() || sending}
                  >
                    {sending ? 'Sending…' : 'Send'}
                  </button>
                </form>
              </>
            )}
          </section>
        </div>
      </div>
    </main>
  );
}
