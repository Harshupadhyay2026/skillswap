import { useEffect, useState } from 'react';
import Modal from '../ui/Modal';
import reviewService from '../../services/reviewService';
import { useToast } from '../../context/ToastContext';

const LABELS = {
  1: 'Poor',
  2: 'Fair',
  3: 'Good',
  4: 'Great',
  5: 'Excellent',
};

/**
 * Rate a completed swap or session. `item` comes from /api/reviews/pending.
 */
export default function ReviewModal({ open, onClose, item, onSubmitted }) {
  const { showToast } = useToast();
  const [rating, setRating] = useState(5);
  const [hover, setHover] = useState(0);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setRating(5);
      setHover(0);
      setComment('');
      setSubmitting(false);
    }
  }, [open]);

  if (!item) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const payload = { rating, comment: comment.trim() || undefined };
      if (item.type === 'SWAP') payload.swapId = item.contextId;
      else payload.learningRequestId = item.contextId;

      const res = await reviewService.createReview(payload);
      showToast(res?.message || 'Review submitted!');
      onSubmitted?.(res?.data);
      onClose?.();
    } catch (err) {
      showToast(err?.message || 'Could not submit your review.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Rate ${item.counterpartName}`}
      subtitle={
        item.skillName
          ? `${item.skillName} • ${item.type === 'SWAP' ? 'Skill swap' : 'Learning session'}`
          : item.type === 'SWAP'
            ? 'Skill swap'
            : 'Learning session'
      }
    >
      <form onSubmit={handleSubmit}>
        <div style={{ textAlign: 'center', margin: '4px 0 20px' }}>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '6px' }}>
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n}
                type="button"
                aria-label={`${n} star${n === 1 ? '' : 's'}`}
                onMouseEnter={() => setHover(n)}
                onMouseLeave={() => setHover(0)}
                onClick={() => setRating(n)}
                style={{
                  background: 'none',
                  fontSize: '34px',
                  lineHeight: 1,
                  color: (hover || rating) >= n ? '#f59e0b' : 'var(--line)',
                  transition: 'color 0.15s ease, transform 0.15s ease',
                  transform: (hover || rating) >= n ? 'scale(1.06)' : 'scale(1)',
                  padding: 0,
                }}
              >
                ★
              </button>
            ))}
          </div>
          <div style={{ marginTop: '8px', fontSize: '13px', fontWeight: 750 }}>
            {LABELS[hover || rating]}
          </div>
        </div>

        <div className="field">
          <label>Comment (optional)</label>
          <textarea
            placeholder={
              item.type === 'SWAP'
                ? 'How was the swap? Were they helpful and prepared?'
                : 'How was the session? Was the teacher clear and helpful?'
            }
            value={comment}
            maxLength={1000}
            onChange={(e) => setComment(e.target.value)}
          />
        </div>

        <p className="muted" style={{ fontSize: '12px', lineHeight: 1.6, marginBottom: '14px' }}>
          Reviews are public and help other students choose who to learn from.
        </p>

        <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={submitting}>
          {submitting ? 'Submitting…' : 'Submit review'}
        </button>
      </form>
    </Modal>
  );
}
