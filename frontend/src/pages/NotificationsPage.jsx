import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import notificationService from '../services/notificationService';
import { timeAgo } from '../lib/format';

const TYPE_ICON = {
  SWAP_PROPOSED: '🤝',
  SWAP_ACCEPTED: '✅',
  SWAP_DECLINED: '🙅',
  SWAP_COMPLETED: '🎉',
  SWAP_CANCELLED: '↩️',
  LEARNING_REQUESTED: '📚',
  LEARNING_ACCEPTED: '✅',
  LEARNING_DECLINED: '🙅',
  LEARNING_COMPLETED: '🎉',
  LEARNING_CANCELLED: '↩️',
  ANSWER_ACCEPTED: '🏅',
  REPORT_RECEIVED: '🛡️',
  CONTENT_HIDDEN: '🙈',
  CONTENT_RESTORED: '♻️',
  ACCOUNT_SUSPENDED: '⛔',
  ACCOUNT_REACTIVATED: '✅',
};

/** Full notification history with "load more" and mark-all-read. */
export default function NotificationsPage() {
  const { isAuthenticated, booting } = useAuth();
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  const [emailStatus, setEmailStatus] = useState(null);
  const [emailNotice, setEmailNotice] = useState('');
  const [emailError, setEmailError] = useState('');
  const [sendingTest, setSendingTest] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) return;
    notificationService
      .emailStatus()
      .then(setEmailStatus)
      .catch(() => setEmailStatus(null));
  }, [isAuthenticated]);

  useEffect(() => {
    if (!booting && !isAuthenticated) {
      navigate('/login', { state: { from: '/notifications' }, replace: true });
    }
  }, [booting, isAuthenticated, navigate]);

  const load = useCallback(
    async (nextPage, replace = false) => {
      if (replace) setLoading(true);
      try {
        const data = await notificationService.list(nextPage, 20);
        setItems((previous) =>
          replace ? data?.items || [] : [...previous, ...(data?.items || [])]
        );
        setHasNext(Boolean(data?.hasNext));
        setPage(nextPage);
        setError('');
      } catch (err) {
        setError(err?.message || 'Could not load notifications.');
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    []
  );

  useEffect(() => {
    if (isAuthenticated) load(0, true);
  }, [isAuthenticated, load]);

  const openItem = async (item) => {
    if (!item.read) {
      try {
        await notificationService.markRead(item.id);
        setItems((list) =>
          list.map((n) => (n.id === item.id ? { ...n, read: true } : n))
        );
      } catch {
        /* still navigate */
      }
    }
    if (item.link) navigate(item.link);
  };

  const markAll = async () => {
    try {
      await notificationService.markAllRead();
      setItems((list) => list.map((n) => ({ ...n, read: true })));
    } catch (err) {
      setError(err?.message || 'Could not mark notifications as read.');
    }
  };

  const sendTestEmail = async () => {
    setSendingTest(true);
    setEmailNotice('');
    setEmailError('');
    try {
      const result = await notificationService.sendTestEmail();
      setEmailNotice(result?.message || 'Test email sent.');
    } catch (err) {
      setEmailError(err?.message || 'Could not send the test email.');
    } finally {
      setSendingTest(false);
    }
  };

  const emailCopy = () => {
    if (!emailStatus) return 'Checking email delivery…';
    if (!emailStatus.enabled) {
      return 'Email delivery is off. In-app notifications still work normally.';
    }
    if (!emailStatus.configured) {
      return 'SMTP is not configured yet. Add MAIL_HOST, MAIL_USERNAME and MAIL_PASSWORD to backend/.env.';
    }
    return `Every notification is also emailed to ${emailStatus.recipient}.`;
  };

  if (booting) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading notifications…</p>
        </div>
      </main>
    );
  }

  if (!isAuthenticated) return null;

  const unread = items.filter((n) => !n.read).length;

  return (
    <main className="page">
      <div className="container narrow">
        <div className="page-head">
          <div>
            <h1>Notifications</h1>
            <p className="muted">
              Swap updates, session changes and accepted answers.
            </p>
          </div>
          {unread > 0 && (
            <button type="button" className="btn btn-outline" onClick={markAll}>
              Mark all read
            </button>
          )}
        </div>

        <div className="card email-status-card">
          <div>
            <strong>Email notifications</strong>
            <p className="muted" style={{ marginTop: '4px' }}>{emailCopy()}</p>
            {emailNotice && (
              <p className="email-status-note success" role="status">{emailNotice}</p>
            )}
            {emailError && (
              <p className="email-status-note error" role="alert">{emailError}</p>
            )}
          </div>
          <button
            type="button"
            className="btn btn-outline"
            onClick={sendTestEmail}
            disabled={sendingTest || !emailStatus?.configured}
          >
            {sendingTest ? 'Sending…' : 'Send test email'}
          </button>
        </div>

        {error && (
          <div className="card" style={{ marginBottom: '14px' }}>
            <p className="muted">{error}</p>
          </div>
        )}

        {loading ? (
          <p className="muted">Loading…</p>
        ) : items.length === 0 ? (
          <div className="card empty-state">
            <div style={{ fontSize: '32px', marginBottom: '10px' }}>🔔</div>
            <h3>No notifications yet</h3>
            <p className="muted" style={{ marginTop: '6px' }}>
              When someone requests a swap, accepts a session, or your answer is accepted,
              it will show up here.
            </p>
          </div>
        ) : (
          <div className="notification-page-list">
            {items.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`notification-row card${item.read ? '' : ' unread'}`}
                onClick={() => openItem(item)}
              >
                <span className="notif-icon">{TYPE_ICON[item.type] || '🔔'}</span>
                <span className="notification-copy">
                  <strong>{item.title}</strong>
                  <span className="notif-body">{item.body}</span>
                  <span className="notif-time">{timeAgo(item.createdAt)}</span>
                </span>
                {!item.read && <span className="unread-pip" aria-label="Unread" />}
              </button>
            ))}

            {hasNext && (
              <button
                type="button"
                className="btn btn-outline"
                style={{ margin: '14px auto 0' }}
                onClick={() => {
                  setLoadingMore(true);
                  load(page + 1);
                }}
                disabled={loadingMore}
              >
                {loadingMore ? 'Loading…' : 'Load more'}
              </button>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
