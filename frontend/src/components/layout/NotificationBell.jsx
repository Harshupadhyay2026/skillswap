import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import notificationService from '../../services/notificationService';
import { timeAgo } from '../../lib/format';

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

/**
 * Navbar bell.
 *
 * The badge polls every 30 seconds (and on tab focus). Opening the panel loads
 * the most recent eight notifications; "View all" goes to the full page.
 */
export default function NotificationBell() {
  const navigate = useNavigate();
  const wrapRef = useRef(null);

  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const refreshCount = useCallback(async () => {
    try {
      setCount(await notificationService.unreadCount());
    } catch {
      /* non-critical */
    }
  }, []);

  useEffect(() => {
    refreshCount();
    const interval = setInterval(refreshCount, 30000);
    window.addEventListener('focus', refreshCount);
    return () => {
      clearInterval(interval);
      window.removeEventListener('focus', refreshCount);
    };
  }, [refreshCount]);

  useEffect(() => {
    if (!open) return undefined;

    const onPointerDown = (event) => {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onPointerDown);

    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  const loadItems = async () => {
    setLoading(true);
    try {
      const page = await notificationService.list(0, 8);
      setItems(page?.items || []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) {
      loadItems();
      refreshCount();
    }
  };

  const openItem = async (item) => {
    setOpen(false);

    if (!item.read) {
      try {
        await notificationService.markRead(item.id);
        setCount((c) => Math.max(0, c - 1));
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
      setCount(0);
      setItems((list) => list.map((n) => ({ ...n, read: true })));
    } catch {
      /* ignore */
    }
  };

  return (
    <div className="notif-wrap" ref={wrapRef}>
      <button
        className="btn-icon notif-button"
        onClick={toggle}
        aria-label={count > 0 ? `Notifications, ${count} unread` : 'Notifications'}
        aria-expanded={open}
        title="Notifications"
        type="button"
      >
        🔔
        {count > 0 && <span className="notif-dot">{count > 9 ? '9+' : count}</span>}
      </button>

      {open && (
        <div className="notif-panel" role="dialog" aria-label="Notifications">
          <div className="notif-head">
            <strong>Notifications</strong>
            {count > 0 && (
              <button type="button" className="link-button" onClick={markAll}>
                Mark all read
              </button>
            )}
          </div>

          {loading ? (
            <div className="notif-empty muted">Loading…</div>
          ) : items.length === 0 ? (
            <div className="notif-empty">
              <div style={{ fontSize: '24px', marginBottom: '6px' }}>🔔</div>
              <div className="muted">You are all caught up.</div>
            </div>
          ) : (
            <div className="notif-list">
              {items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`notif-item${item.read ? '' : ' unread'}`}
                  onClick={() => openItem(item)}
                >
                  <span className="notif-icon">{TYPE_ICON[item.type] || '🔔'}</span>
                  <span style={{ minWidth: 0, textAlign: 'left' }}>
                    <strong>{item.title}</strong>
                    <span className="notif-body">{item.body}</span>
                    <span className="notif-time">{timeAgo(item.createdAt)}</span>
                  </span>
                </button>
              ))}
            </div>
          )}

          <button
            type="button"
            className="notif-viewall"
            onClick={() => {
              setOpen(false);
              navigate('/notifications');
            }}
          >
            View all notifications →
          </button>
        </div>
      )}
    </div>
  );
}
