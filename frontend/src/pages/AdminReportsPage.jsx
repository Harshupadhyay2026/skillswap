import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useSearchParams } from 'react-router-dom';
import moderationService from '../services/moderationService';
import Avatar from '../components/ui/Avatar';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { timeAgo } from '../lib/format';

const STATUS_TABS = [
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_REVIEW', label: 'In review' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'DISMISSED', label: 'Dismissed' },
  { value: '', label: 'All' },
];

const REASON_LABELS = {
  SPAM: 'Spam',
  HARASSMENT: 'Harassment',
  HATE: 'Hate speech',
  MISINFORMATION: 'Misinformation',
  IMPERSONATION: 'Impersonation',
  OTHER: 'Other',
};

const TYPE_LABELS = {
  PROBLEM: 'Question',
  ANSWER: 'Answer',
  USER: 'Profile',
};

const ACTION_LABELS = {
  DISMISS: 'Dismiss report',
  HIDE_CONTENT: 'Hide content',
  SUSPEND_USER: 'Suspend account',
  RESTORE_CONTENT: 'Restore content',
  REACTIVATE_USER: 'Reactivate account',
};

const ACTION_HELP = {
  DISMISS: 'Close the report without changing anything. Add a note if it helps future reviews.',
  HIDE_CONTENT: 'Remove the reported item from public reads. Accepted-answer reputation is rolled back.',
  SUSPEND_USER: 'Block login and invalidate existing sessions. The account can be reactivated later.',
  RESTORE_CONTENT: 'Make the content public again. Reputation is not automatically re-awarded.',
  REACTIVATE_USER: 'Allow the account to log in and participate again.',
};

function statusClass(status) {
  return status ? status.toLowerCase().replace('_', '-') : '';
}

function moderationTime(value) {
  if (!value) return '';
  return timeAgo(value);
}

/**
 * Moderator queue.
 *
 * The list is intentionally triage-first: priority and age are visible, while
 * the detail pane keeps every decision reversible and auditable.
 */
export default function AdminReportsPage() {
  const { user, isAuthenticated, booting } = useAuth();
  const { showToast } = useToast();
  const [searchParams] = useSearchParams();
  const reportParam = searchParams.get('report');

  const isAdmin = Boolean(user?.roles?.includes('ROLE_ADMIN'));

  const [status, setStatus] = useState('OPEN');
  const [targetType, setTargetType] = useState('');
  const [reports, setReports] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    totalPages: 0,
    totalItems: 0,
    hasNext: false,
  });
  const [stats, setStats] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [pendingAction, setPendingAction] = useState('');
  const [note, setNote] = useState('');
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);

  const loadStats = useCallback(async () => {
    try {
      const data = await moderationService.stats();
      setStats(data);
    } catch {
      /* stats are supporting information; keep the queue usable */
    }
  }, []);

  const loadQueue = useCallback(
    async (nextPage = 0) => {
      setLoading(true);
      setError('');
      try {
        const data = await moderationService.listReports({
          status,
          targetType,
          page: nextPage,
          size: 20,
        });
        setReports(data?.items || []);
        setPageInfo({
          page: data?.page || 0,
          totalPages: data?.totalPages || 0,
          totalItems: data?.totalItems || 0,
          hasNext: Boolean(data?.hasNext),
        });
      } catch (err) {
        setError(err?.message || 'Could not load the moderation queue.');
      } finally {
        setLoading(false);
      }
    },
    [status, targetType]
  );

  const loadDetail = useCallback(async (id) => {
    if (!id) return;
    setDetailLoading(true);
    setActionError('');
    try {
      const data = await moderationService.getReport(id);
      setSelected(data);
    } catch (err) {
      showToast(err?.message || 'Could not load that report.');
    } finally {
      setDetailLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    if (booting || !isAdmin) return;
    loadQueue(0);
    loadStats();
  }, [booting, isAdmin, loadQueue, loadStats]);

  // Notification links carry ?report=ID, including reports outside the current tab.
  useEffect(() => {
    if (booting || !isAdmin || !reportParam) return;
    loadDetail(reportParam);
  }, [booting, isAdmin, reportParam, loadDetail]);

  const refresh = async (keepId) => {
    await Promise.all([loadQueue(pageInfo.page), loadStats()]);
    if (keepId) loadDetail(keepId);
  };

  const selectReport = (report) => {
    setSelected(report);
    setPendingAction('');
    setNote('');
    setActionError('');
    loadDetail(report.id);
  };

  const handleClaim = async () => {
    if (!selected) return;
    setBusy(true);
    setActionError('');
    try {
      const updated = await moderationService.claim(selected.id);
      setSelected(updated);
      showToast('Report claimed. It is now in review.');
      await refresh(selected.id);
    } catch (err) {
      setActionError(err?.message || 'Could not claim this report.');
    } finally {
      setBusy(false);
    }
  };

  const handleAction = async () => {
    if (!selected || !pendingAction) return;

    const requiresNote = pendingAction === 'HIDE_CONTENT' || pendingAction === 'SUSPEND_USER';
    if (requiresNote && note.trim().length < 3) {
      setActionError('Add a short note explaining this decision.');
      return;
    }

    setBusy(true);
    setActionError('');
    try {
      if (pendingAction === 'RESTORE_CONTENT') {
        if (selected.targetType === 'PROBLEM') {
          await moderationService.restoreProblem(selected.targetId, note.trim() || undefined);
        } else if (selected.targetType === 'ANSWER') {
          await moderationService.restoreAnswer(selected.targetId, note.trim() || undefined);
        }
        showToast('Content restored to the community.');
      } else if (pendingAction === 'REACTIVATE_USER') {
        await moderationService.reactivateUser(selected.targetOwnerId, note.trim() || undefined);
        showToast('Account reactivated.');
      } else {
        const updated = await moderationService.resolve(
          selected.id,
          pendingAction,
          note.trim() || undefined
        );
        setSelected(updated);
        showToast(
          pendingAction === 'DISMISS'
            ? 'Report dismissed.'
            : 'Decision recorded and the reporter’s item was updated.'
        );
      }

      setPendingAction('');
      setNote('');
      await refresh(selected.id);
    } catch (err) {
      setActionError(err?.message || 'Could not complete that action.');
    } finally {
      setBusy(false);
    }
  };

  if (booting) {
    return (
      <main className="page">
        <div className="container empty-state">
          <p className="muted">🔐 Checking moderator access…</p>
        </div>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: '/admin/reports' }} />;
  }

  if (!isAdmin) {
    return (
      <main className="page">
        <div className="container empty-state">
          <div style={{ fontSize: '34px', marginBottom: '10px' }}>🛡️</div>
          <h2>Moderator access only</h2>
          <p className="muted" style={{ margin: '8px 0 18px' }}>
            Your account does not have the moderator role.
          </p>
          <Link to="/" className="btn btn-primary">Back home</Link>
        </div>
      </main>
    );
  }

  const canModerate = selected && ['OPEN', 'IN_REVIEW'].includes(selected.status);
  const canHide =
    canModerate && selected.targetType !== 'USER' && selected.targetExists && !selected.targetHidden;
  const canSuspend =
    canModerate &&
    selected.targetOwnerId &&
    selected.targetOwnerActive &&
    !selected.targetOwnerAdmin;
  const canRestore =
    selected && selected.targetHidden && selected.targetExists && selected.targetType !== 'USER';
  const canReactivate =
    selected && selected.targetOwnerId && !selected.targetOwnerActive && !selected.targetOwnerAdmin;

  return (
    <main className="page">
      <div className="container admin-page">
        <div className="page-head admin-head">
          <div>
            <div className="admin-eyebrow">🛡️ Community safety</div>
            <h1>Moderation queue</h1>
            <p>
              Review reports, hide harmful content, and suspend accounts. Every
              decision is logged and can be reversed.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => refresh(selected?.id)}
            disabled={loading}
          >
            ↻ Refresh
          </button>
        </div>

        <div className="admin-stats">
          <div className="admin-stat">
            <strong>{stats?.openReports ?? '—'}</strong>
            <span>Open</span>
          </div>
          <div className="admin-stat">
            <strong>{stats?.inReviewReports ?? '—'}</strong>
            <span>In review</span>
          </div>
          <div className="admin-stat">
            <strong>{stats?.resolvedToday ?? '—'}</strong>
            <span>Resolved today</span>
          </div>
          <div className="admin-stat admin-stat-warn">
            <strong>{stats?.suspendedUsers ?? '—'}</strong>
            <span>Suspended accounts</span>
          </div>
        </div>

        <div className="tabs">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value || 'ALL'}
              type="button"
              className={`tab${status === tab.value ? ' active' : ''}`}
              onClick={() => {
                setStatus(tab.value);
                setPendingAction('');
                setNote('');
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="admin-filters">
          <label>
            <span className="muted">Target</span>
            <select value={targetType} onChange={(e) => setTargetType(e.target.value)}>
              <option value="">All types</option>
              <option value="PROBLEM">Questions</option>
              <option value="ANSWER">Answers</option>
              <option value="USER">Profiles</option>
            </select>
          </label>
          <span className="muted admin-result-count">
            {pageInfo.totalItems} {pageInfo.totalItems === 1 ? 'report' : 'reports'}
          </span>
        </div>

        <div className="admin-layout">
          <section className="card admin-queue" aria-label="Report queue">
            {loading ? (
              <p className="muted" style={{ padding: '20px' }}>Loading reports…</p>
            ) : error ? (
              <div className="empty-state" style={{ padding: '24px' }}>
                <p className="muted">{error}</p>
                <button type="button" className="btn btn-outline" onClick={() => loadQueue(0)}>
                  Try again
                </button>
              </div>
            ) : reports.length === 0 ? (
              <div className="empty-state" style={{ padding: '30px 20px' }}>
                <div style={{ fontSize: '30px', marginBottom: '8px' }}>🌿</div>
                <h3>No reports here</h3>
                <p className="muted">The queue is clear for this filter.</p>
              </div>
            ) : (
              reports.map((report) => (
                <button
                  type="button"
                  key={report.id}
                  className={`report-row${selected?.id === report.id ? ' active' : ''}`}
                  onClick={() => selectReport(report)}
                >
                  <div className="report-row-top">
                    <span className={`tag priority-${(report.priority || 'normal').toLowerCase()}`}>
                      {report.priority === 'HIGH' ? 'High' : report.priority === 'LOW' ? 'Low' : 'Normal'}
                    </span>
                    <span className="tag">{TYPE_LABELS[report.targetType] || report.targetType}</span>
                    <span className={`report-status status-${statusClass(report.status)}`}>
                      {report.status.replace('_', ' ')}
                    </span>
                    <span className="muted report-age">{moderationTime(report.createdAt)}</span>
                  </div>
                  <strong className="report-row-title">
                    {report.targetPreview || 'Reported item no longer available'}
                  </strong>
                  <div className="muted report-row-sub">
                    {REASON_LABELS[report.reason] || report.reason} • reported by @
                    {report.reporterUsername || 'unknown'}
                    {report.duplicateCount > 1 ? ` • ${report.duplicateCount} open reports` : ''}
                  </div>
                </button>
              ))
            )}

            <div className="admin-pagination">
              <button
                type="button"
                className="btn btn-outline"
                disabled={pageInfo.page <= 0 || loading}
                onClick={() => loadQueue(pageInfo.page - 1)}
              >
                ← Newer
              </button>
              <span className="muted">
                Page {pageInfo.page + 1} of {Math.max(1, pageInfo.totalPages)}
              </span>
              <button
                type="button"
                className="btn btn-outline"
                disabled={!pageInfo.hasNext || loading}
                onClick={() => loadQueue(pageInfo.page + 1)}
              >
                Older →
              </button>
            </div>
          </section>

          <aside className="card admin-detail" aria-live="polite">
            {detailLoading && !selected ? (
              <p className="muted">Loading report…</p>
            ) : !selected ? (
              <div className="empty-state admin-detail-empty">
                <div style={{ fontSize: '32px', marginBottom: '8px' }}>👈</div>
                <h3>Select a report</h3>
                <p className="muted">The full context and moderation actions appear here.</p>
              </div>
            ) : (
              <>
                <div className="admin-detail-head">
                  <div>
                    <span className="muted" style={{ fontSize: '12px' }}>
                      Report #{selected.id}
                    </span>
                    <h2>{REASON_LABELS[selected.reason] || selected.reason}</h2>
                  </div>
                  <span className={`tag priority-${(selected.priority || 'normal').toLowerCase()}`}>
                    {selected.priority}
                  </span>
                </div>

                <div className="report-target">
                  <div className="report-target-kicker">
                    {TYPE_LABELS[selected.targetType] || selected.targetType}
                    {selected.targetHidden ? ' • hidden from public' : ''}
                    {!selected.targetExists ? ' • deleted' : ''}
                  </div>
                  <p className="report-target-preview">{selected.targetPreview}</p>
                  {selected.targetContext && (
                    <div className="muted" style={{ fontSize: '12px', marginTop: '6px' }}>
                      {selected.targetContext}
                    </div>
                  )}
                  {selected.targetOwnerId && (
                    <div className="report-owner">
                      <Avatar
                        name={selected.targetOwnerName}
                        src={selected.targetOwnerAvatarUrl}
                        size={30}
                      />
                      <div>
                        <Link to={`/profile/${selected.targetOwnerId}`} className="report-owner-link">
                          {selected.targetOwnerName}
                        </Link>
                        <div className="muted" style={{ fontSize: '11.5px' }}>
                          @{selected.targetOwnerUsername}
                          {selected.targetOwnerAdmin ? ' • moderator' : ''}
                          {selected.targetOwnerActive ? '' : ' • suspended'}
                        </div>
                      </div>
                    </div>
                  )}
                  {selected.targetOwnerSuspensionReason && (
                    <div className="report-suspension-note">
                      Suspension note: {selected.targetOwnerSuspensionReason}
                    </div>
                  )}
                </div>

                <div className="report-facts">
                  <div>
                    <span className="muted">Reported by</span>
                    <strong>@{selected.reporterUsername || 'unknown'}</strong>
                  </div>
                  <div>
                    <span className="muted">Filed</span>
                    <strong>{moderationTime(selected.createdAt)}</strong>
                  </div>
                  <div>
                    <span className="muted">Open reports on this item</span>
                    <strong>{selected.duplicateCount}</strong>
                  </div>
                  <div>
                    <span className="muted">Status</span>
                    <strong>{selected.status.replace('_', ' ')}</strong>
                  </div>
                </div>

                {selected.details && (
                  <div className="report-comment">
                    <span className="muted">Reporter’s note</span>
                    <p>{selected.details}</p>
                  </div>
                )}

                {selected.status === 'OPEN' && (
                  <button
                    type="button"
                    className="btn btn-outline admin-claim"
                    onClick={handleClaim}
                    disabled={busy}
                  >
                    {busy ? 'Claiming…' : '✋ Claim for review'}
                  </button>
                )}

                {canModerate && !pendingAction && (
                  <div className="admin-actions">
                    <button
                      type="button"
                      className="btn btn-outline"
                      onClick={() => { setPendingAction('DISMISS'); setNote(''); setActionError(''); }}
                    >
                      Dismiss
                    </button>
                    {canHide && (
                      <button
                        type="button"
                        className="btn btn-outline admin-danger"
                        onClick={() => { setPendingAction('HIDE_CONTENT'); setNote(''); setActionError(''); }}
                      >
                        Hide content
                      </button>
                    )}
                    {canSuspend && (
                      <button
                        type="button"
                        className="btn btn-primary admin-danger-solid"
                        onClick={() => { setPendingAction('SUSPEND_USER'); setNote(''); setActionError(''); }}
                      >
                        Suspend account
                      </button>
                    )}
                  </div>
                )}

                {pendingAction && (
                  <div className="admin-action-form">
                    <h3>{ACTION_LABELS[pendingAction]}</h3>
                    <p className="muted">{ACTION_HELP[pendingAction]}</p>
                    <div className="field">
                      <label htmlFor="moderation-note">
                        Moderator note {(pendingAction === 'HIDE_CONTENT' || pendingAction === 'SUSPEND_USER') && '(required)'}
                      </label>
                      <textarea
                        id="moderation-note"
                        rows={3}
                        maxLength={1000}
                        value={note}
                        onChange={(e) => setNote(e.target.value)}
                        placeholder="Explain the decision for the audit trail."
                      />
                    </div>
                    {actionError && <div className="field-error">{actionError}</div>}
                    <div className="admin-action-buttons">
                      <button
                        type="button"
                        className="btn btn-outline"
                        onClick={() => { setPendingAction(''); setActionError(''); }}
                        disabled={busy}
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        className="btn btn-primary"
                        onClick={handleAction}
                        disabled={busy}
                      >
                        {busy ? 'Saving…' : `Confirm ${ACTION_LABELS[pendingAction].toLowerCase()}`}
                      </button>
                    </div>
                  </div>
                )}

                {selected.resolutionNote && (
                  <div className="report-resolution">
                    <span className="muted">Resolution note</span>
                    <p>{selected.resolutionNote}</p>
                    {selected.reviewedByName && (
                      <div className="muted" style={{ fontSize: '11.5px' }}>
                        {selected.reviewedByName} • {moderationTime(selected.reviewedAt)}
                      </div>
                    )}
                  </div>
                )}

                {(canRestore || canReactivate) && !pendingAction && (
                  <div className="admin-actions admin-reverse">
                    {canRestore && (
                      <button
                        type="button"
                        className="btn btn-outline"
                        onClick={() => { setPendingAction('RESTORE_CONTENT'); setNote(''); setActionError(''); }}
                      >
                        ↺ Restore content
                      </button>
                    )}
                    {canReactivate && (
                      <button
                        type="button"
                        className="btn btn-outline"
                        onClick={() => { setPendingAction('REACTIVATE_USER'); setNote(''); setActionError(''); }}
                      >
                        ↺ Reactivate account
                      </button>
                    )}
                  </div>
                )}

                {selected.actions?.length > 0 && (
                  <div className="moderation-history">
                    <h3>Decision history</h3>
                    {selected.actions.map((entry) => (
                      <div className="moderation-history-row" key={entry.id}>
                        <div>
                          <strong>{ACTION_LABELS[entry.action] || entry.action}</strong>
                          <span className="muted"> by {entry.adminName || 'Moderator'}</span>
                        </div>
                        <span className="muted">{moderationTime(entry.createdAt)}</span>
                        {entry.note && <p className="muted">{entry.note}</p>}
                      </div>
                    ))}
                  </div>
                )}

                {actionError && !pendingAction && (
                  <div className="field-error" style={{ marginTop: '12px' }}>{actionError}</div>
                )}
              </>
            )}
          </aside>
        </div>
      </div>
    </main>
  );
}
