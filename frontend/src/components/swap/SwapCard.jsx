import { useState } from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from './StatusBadge';
import Avatar from '../ui/Avatar';

function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * One swap proposal. Shows the counterpart, the offered <-> requested skills,
 * and the actions permitted for this user's role in the swap.
 */
export default function SwapCard({ swap, currentUserId, onAction, onMessage, busy }) {
  const [confirming, setConfirming] = useState(null);

  const isSender = swap.sender?.id === currentUserId;
  const counterpart = isSender ? swap.receiver : swap.sender;
  const isReceiver = swap.receiver?.id === currentUserId;
  const status = swap.status;

  const run = (action) => {
    setConfirming(null);
    onAction?.(action, swap);
  };

  const confirmAction = (action) => {
    if (confirming === action) {
      run(action);
      return;
    }
    setConfirming(action);
    setTimeout(() => setConfirming((c) => (c === action ? null : c)), 3000);
  };

  const canAccept = isReceiver && status === 'PENDING';
  const canDecline = isReceiver && status === 'PENDING';
  const canCancel = isSender && status === 'PENDING';
  const canComplete = status === 'ACCEPTED';
  const canJoin = status === 'ACCEPTED' && swap.meetingLink;

  return (
    <div className="card" style={{ padding: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
        <Avatar name={counterpart?.fullName} src={counterpart?.avatarUrl} size={42} />
        <div style={{ minWidth: 0, flex: 1 }}>
          <strong style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {counterpart?.fullName || 'Student'}
          </strong>
          <div className="muted" style={{ fontSize: '12px' }}>
            {isSender ? 'You sent this request' : 'Sent you a request'}
            {counterpart?.college ? ` • ${counterpart.college}` : ''}
          </div>
        </div>
        <StatusBadge status={status} />
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr auto 1fr',
          gap: '10px',
          alignItems: 'center',
          background: 'var(--tag-bg)',
          borderRadius: '12px',
          padding: '12px',
          marginBottom: '12px',
        }}
      >
        <div>
          <div className="muted" style={{ fontSize: '10px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            {isSender ? 'You offer' : 'They offer'}
          </div>
          <div style={{ fontWeight: 750, fontSize: '13px', marginTop: '2px' }}>
            {swap.offeredSkill?.icon} {swap.offeredSkill?.name || '—'}
          </div>
        </div>
        <div style={{ color: 'var(--primary)', fontSize: '16px' }}>⇄</div>
        <div>
          <div className="muted" style={{ fontSize: '10px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            {isSender ? 'You want' : 'They want'}
          </div>
          <div style={{ fontWeight: 750, fontSize: '13px', marginTop: '2px' }}>
            {swap.requestedSkill?.icon} {swap.requestedSkill?.name || '—'}
          </div>
        </div>
      </div>

      {swap.message && (
        <p className="muted" style={{ fontSize: '12.5px', lineHeight: 1.55, marginBottom: '12px', fontStyle: 'italic' }}>
          “{swap.message}”
        </p>
      )}

      <div className="muted" style={{ fontSize: '11.5px', marginBottom: '14px' }}>
        Proposed {formatDate(swap.createdAt)}
        {swap.scheduledAt && ` • Preferred time ${formatDate(swap.scheduledAt)}`}
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
        {canJoin && (
          <Link
            to={`/session/swap/${swap.id}`}
            className="btn btn-primary"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
          >
            🎥 Join Meeting
          </Link>
        )}

        {status === 'COMPLETED' && (
          <Link
            to={`/session/swap/${swap.id}`}
            className="btn btn-light"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
          >
            📝 Session notes
          </Link>
        )}

        {canAccept && (
          <button
            className="btn btn-primary"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
            disabled={busy}
            onClick={() => run('accept')}
          >
            {busy ? '…' : '✓ Accept'}
          </button>
        )}

        {canDecline && (
          <button
            className="btn btn-outline"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
            disabled={busy}
            onClick={() => confirmAction('decline')}
          >
            {confirming === 'decline' ? 'Confirm decline?' : 'Decline'}
          </button>
        )}

        {canCancel && (
          <button
            className="btn btn-outline"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
            disabled={busy}
            onClick={() => confirmAction('cancel')}
          >
            {confirming === 'cancel' ? 'Confirm cancel?' : 'Cancel request'}
          </button>
        )}

        {canComplete && (
          <button
            className="btn btn-light"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
            disabled={busy}
            onClick={() => confirmAction('complete')}
          >
            {confirming === 'complete' ? 'Confirm complete?' : 'Mark complete'}
          </button>
        )}

        {counterpart && (
          <button
            type="button"
            className="btn btn-outline"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
            onClick={() => onMessage?.(counterpart)}
          >
            💬 Message
          </button>
        )}
      </div>
    </div>
  );
}
