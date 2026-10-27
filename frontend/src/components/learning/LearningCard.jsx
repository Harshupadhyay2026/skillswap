import { useState } from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from '../swap/StatusBadge';
import Avatar from '../ui/Avatar';

function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * One 100% free learning request card.
 */
export default function LearningCard({
  request,
  currentUserId,
  onAction,
  onMessage,
  busy,
}) {
  const [confirming, setConfirming] = useState(null);

  const isLearner = request.learner?.id === currentUserId;
  const isTeacher = request.teacher?.id === currentUserId;
  const counterpart = isLearner ? request.teacher : request.learner;
  const status = request.status;

  const run = (action) => {
    setConfirming(null);
    onAction?.(action, request);
  };

  const confirmAction = (action) => {
    if (confirming === action) {
      run(action);
      return;
    }
    setConfirming(action);
    setTimeout(() => setConfirming((c) => (c === action ? null : c)), 3000);
  };

  const canAccept = isTeacher && status === 'PENDING';
  const canDecline = isTeacher && status === 'PENDING';
  const canCancel = isLearner && status === 'PENDING';
  const canJoin = status === 'ACCEPTED' && request.meetingLink;
  const canComplete = (isLearner || isTeacher) && status === 'ACCEPTED';

  return (
    <div className="card" style={{ padding: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
        <Avatar name={counterpart?.fullName} src={counterpart?.avatarUrl} size={42} />
        <div style={{ minWidth: 0, flex: 1 }}>
          <strong style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {counterpart?.fullName || 'Student'}
          </strong>
          <div className="muted" style={{ fontSize: '12px' }}>
            {isLearner ? 'You proposed this shared session' : 'They invited you to learn together'}
          </div>
        </div>
        <StatusBadge status={status} />
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'var(--tag-bg)',
          borderRadius: '12px',
          padding: '12px',
          marginBottom: '12px',
        }}
      >
        <div>
          <div
            className="muted"
            style={{
              fontSize: '10px',
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.04em',
            }}
          >
            Skill
          </div>
          <div style={{ fontWeight: 750, fontSize: '13px', marginTop: '2px' }}>
            {request.skill?.icon} {request.skill?.name || '—'}
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <span
            style={{
              display: 'inline-block',
              padding: '4px 10px',
              borderRadius: '20px',
              fontSize: '11px',
              fontWeight: 800,
              color: '#15803d',
              background: 'rgba(34, 197, 94, 0.14)',
            }}
          >
            SHARED LEARNING · FREE
          </span>
        </div>
      </div>

      {request.message && (
        <p
          className="muted"
          style={{ fontSize: '12.5px', lineHeight: 1.55, marginBottom: '12px', fontStyle: 'italic' }}
        >
          “{request.message}”
        </p>
      )}

      <div className="muted" style={{ fontSize: '11.5px', marginBottom: '14px' }}>
        Requested {formatDate(request.createdAt)}
        {request.scheduledAt && ` • Preferred time ${formatDate(request.scheduledAt)}`}
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
        {canJoin && (
          <Link
            to={`/session/learning/${request.id}`}
            className="btn btn-primary"
            style={{ padding: '8px 14px', fontSize: '12.5px' }}
          >
            🎥 Join Meeting
          </Link>
        )}

        {status === 'COMPLETED' && (
          <Link
            to={`/session/learning/${request.id}`}
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
