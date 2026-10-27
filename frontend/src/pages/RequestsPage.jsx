import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import swapService from '../services/swapService';
import learningService from '../services/learningService';
import reviewService from '../services/reviewService';
import messageService from '../services/messageService';
import SwapCard from '../components/swap/SwapCard';
import LearningCard from '../components/learning/LearningCard';
import ReviewModal from '../components/review/ReviewModal';
import Avatar from '../components/ui/Avatar';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const TABS = [
  { key: 'incoming', label: 'Incoming' },
  { key: 'outgoing', label: 'Outgoing' },
  { key: 'history', label: 'History' },
  { key: 'reviews', label: 'To review' },
];

const FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'swaps', label: 'Skill Swaps' },
  { key: 'sessions', label: 'Direct Requests' },
];

const HISTORY_STATUSES = ['COMPLETED', 'DECLINED', 'CANCELLED'];

const ACTION_PAST_TENSE = {
  accept: 'accepted',
  decline: 'declined',
  cancel: 'cancelled',
  complete: 'completed',
};

/**
 * Unified inbox for skill exchange requests:
 *   - barter swaps (SkillSwapRequest)
 *   - direct free learning requests (LearningRequest)
 */
export default function RequestsPage() {
  const { user, isAuthenticated, booting } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [swaps, setSwaps] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [pendingReviews, setPendingReviews] = useState([]);
  const [reviewing, setReviewing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('incoming');
  const [filter, setFilter] = useState('all');
  const [busyKey, setBusyKey] = useState(null);

  const load = useCallback(async () => {
    if (!isAuthenticated) return;
    setLoading(true);
    setError('');
    try {
      const [swapsRes, sessionsRes, pendingRes] = await Promise.all([
        swapService.getAllMySwaps(),
        learningService.getAll(),
        reviewService.getPending().catch(() => ({ data: [] })),
      ]);
      setSwaps(swapsRes?.data || []);
      setSessions(sessionsRes?.data || []);
      setPendingReviews(pendingRes?.data || []);
    } catch (err) {
      setError(err?.message || 'Could not load your requests.');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (!booting && !isAuthenticated) {
      navigate('/login', { state: { from: '/requests' }, replace: true });
      return;
    }
    load();
  }, [booting, isAuthenticated, navigate, load]);

  const openMessage = async (counterpart) => {
    if (!counterpart?.id) return;
    try {
      const conversation = await messageService.startConversation(counterpart.id);
      navigate(`/messages/${conversation.id}`);
    } catch (err) {
      showToast(err?.message || 'Could not open a conversation.');
    }
  };

  const items = useMemo(() => {
    const normalised = [
      ...swaps.map((s) => ({ ...s, kind: 'SWAP' })),
      ...sessions.map((s) => ({ ...s, kind: 'LEARNING' })),
    ];
    return normalised.sort(
      (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
    );
  }, [swaps, sessions]);

  const grouped = useMemo(() => {
    const myId = user?.id;
    const isIncoming = (r) =>
      (r.kind === 'SWAP' ? r.receiver?.id : r.teacher?.id) === myId;
    const isOutgoing = (r) =>
      (r.kind === 'SWAP' ? r.sender?.id : r.learner?.id) === myId;

    const active = items.filter((r) => !HISTORY_STATUSES.includes(r.status));
    return {
      incoming: active.filter(isIncoming),
      outgoing: active.filter(isOutgoing),
      history: items.filter((r) => HISTORY_STATUSES.includes(r.status)),
    };
  }, [items, user]);

  const visible = useMemo(() => {
    const list = grouped[tab] || [];
    if (filter === 'all') return list;
    return list.filter((r) => (filter === 'swaps' ? r.kind === 'SWAP' : r.kind === 'LEARNING'));
  }, [grouped, tab, filter]);

  const counts = {
    incoming: grouped.incoming.filter((r) => r.status === 'PENDING').length,
    outgoing: grouped.outgoing.length,
    history: grouped.history.length,
    reviews: pendingReviews.length,
  };

  const handleSwapAction = async (action, swap) => {
    setBusyKey(`swap-${swap.id}`);
    try {
      const res = await swapService[`${action}Swap`](swap.id);
      const updated = res?.data;
      if (updated) {
        setSwaps((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
      } else {
        await load();
      }
      showToast(res?.message || `Swap ${ACTION_PAST_TENSE[action] || action}.`);
    } catch (err) {
      showToast(err?.message || `Could not ${action} this swap.`);
    } finally {
      setBusyKey(null);
    }
  };

  const handleLearningAction = async (action, request) => {
    setBusyKey(`learning-${request.id}`);
    try {
      const res = await learningService[action](request.id);
      showToast(res?.message || `Session ${ACTION_PAST_TENSE[action] || action}.`);
      await load();
    } catch (err) {
      showToast(err?.message || `Could not ${action} this session.`);
    } finally {
      setBusyKey(null);
    }
  };

  if (booting) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading your requests…</p>
        </div>
      </main>
    );
  }

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Requests &amp; Sessions</h1>
            <p className="muted">
              Manage your mutual skill swaps, learning requests, and scheduled sessions.
            </p>
          </div>
          <Link to="/explore" className="btn btn-primary">
            Find an exchange partner →
          </Link>
        </div>

        <div className="tabs" role="tablist">
          {TABS.map((t) => {
            const count = counts[t.key] ?? 0;
            return (
              <button
                key={t.key}
                role="tab"
                aria-selected={tab === t.key}
                className={`tab ${tab === t.key ? 'active' : ''}`}
                onClick={() => setTab(t.key)}
              >
                {t.label}
                {count > 0 && <span className="tab-count">{count}</span>}
              </button>
            );
          })}
        </div>

        {tab !== 'reviews' && (
          <div className="request-filters" role="group" aria-label="Filter by request type">
            {FILTERS.map((f) => (
              <button
                key={f.key}
                className={`filter-pill ${filter === f.key ? 'active' : ''}`}
                onClick={() => setFilter(f.key)}
              >
                {f.label}
              </button>
            ))}
          </div>
        )}

        {loading ? (
          <div className="empty-state">
            <p className="muted">Loading requests…</p>
          </div>
        ) : error ? (
          <div className="empty-state">
            <p className="muted">{error}</p>
            <button className="btn btn-outline" onClick={load} style={{ marginTop: '12px' }}>
              Retry
            </button>
          </div>
        ) : tab === 'reviews' ? (
          pendingReviews.length === 0 ? (
            <div className="empty-state">
              <div style={{ fontSize: '36px', marginBottom: '10px' }}>⭐</div>
              <h3>All caught up</h3>
              <p className="muted">You have reviewed all your completed sessions.</p>
            </div>
          ) : (
            <div className="request-grid">
              {pendingReviews.map((item) => (
                <div
                  key={`${item.type}-${item.contextId}`}
                  className="card"
                  style={{
                    padding: '18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '16px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <Avatar name={item.counterpartName} size={42} />
                    <div>
                      <strong>{item.counterpartName}</strong>
                      <div className="muted" style={{ fontSize: '13px' }}>
                        {item.skillName ? `Session: ${item.skillName}` : 'Skill swap'}
                      </div>
                    </div>
                  </div>
                  <button
                    className="btn btn-primary"
                    onClick={() => setReviewing(item)}
                    style={{ whiteSpace: 'nowrap' }}
                  >
                    Leave review
                  </button>
                </div>
              ))}
            </div>
          )
        ) : visible.length === 0 ? (
          <div className="empty-state">
            <div style={{ fontSize: '36px', marginBottom: '10px' }}>
              {tab === 'incoming' ? '📥' : tab === 'outgoing' ? '📤' : '📜'}
            </div>
            <h3>
              {tab === 'incoming'
                ? 'No incoming requests'
                : tab === 'outgoing'
                ? 'No outgoing requests'
                : 'No session history'}
            </h3>
            <p className="muted">
              {tab === 'incoming'
                ? 'When another student wants to swap skills with you, it will appear here.'
                : tab === 'outgoing'
                ? 'Explore skills and propose an exchange to get started.'
                : 'Completed and declined sessions will show up here.'}
            </p>
            {tab !== 'history' && (
              <Link to="/explore" className="btn btn-outline" style={{ marginTop: '12px' }}>
                Explore skills
              </Link>
            )}
          </div>
        ) : (
          <div className="request-grid">
            {visible.map((item) =>
              item.kind === 'SWAP' ? (
                <SwapCard
                  key={`swap-${item.id}`}
                  swap={item}
                  currentUserId={user?.id}
                  onAction={handleSwapAction}
                  onMessage={openMessage}
                  busy={busyKey === `swap-${item.id}`}
                />
              ) : (
                <LearningCard
                  key={`learning-${item.id}`}
                  request={item}
                  currentUserId={user?.id}
                  onAction={handleLearningAction}
                  onMessage={openMessage}
                  busy={busyKey === `learning-${item.id}`}
                />
              )
            )}
          </div>
        )}
      </div>

      <ReviewModal
        open={Boolean(reviewing)}
        onClose={() => setReviewing(null)}
        item={reviewing}
        onSubmitted={() => {
          setPendingReviews((prev) =>
            prev.filter(
              (p) => !(p.type === reviewing?.type && p.contextId === reviewing?.contextId)
            )
          );
        }}
      />
    </main>
  );
}
