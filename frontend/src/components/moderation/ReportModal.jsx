import { useEffect, useState } from 'react';
import Modal from '../ui/Modal';
import reportService from '../../services/reportService';
import { useToast } from '../../context/ToastContext';

const REASONS = [
  {
    value: 'SPAM',
    label: 'Spam or advertising',
    hint: 'Repeated promotion, scams, or irrelevant links.',
  },
  {
    value: 'HARASSMENT',
    label: 'Harassment or bullying',
    hint: 'Targeted insults, threats, or intimidation.',
  },
  {
    value: 'HATE',
    label: 'Hate speech',
    hint: 'Attacks based on identity or a protected group.',
  },
  {
    value: 'MISINFORMATION',
    label: 'Misinformation',
    hint: 'Confidently wrong advice that could mislead learners.',
  },
  {
    value: 'IMPERSONATION',
    label: 'Impersonation',
    hint: 'Pretending to be another person or a moderator.',
  },
  {
    value: 'OTHER',
    label: 'Something else',
    hint: 'Tell us briefly what a moderator should look at.',
  },
];

/**
 * Report dialog. The reason is a small closed set so the queue can be triaged
 * consistently; free text is optional context, not the primary signal.
 */
export default function ReportModal({
  open,
  onClose,
  targetType,
  targetId,
  targetLabel,
}) {
  const { showToast } = useToast();
  const [reason, setReason] = useState('');
  const [details, setDetails] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) return;
    setReason('');
    setDetails('');
    setSubmitting(false);
    setError('');
  }, [open, targetId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason) {
      setError('Choose the reason that fits best.');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const res = await reportService.create({
        targetType,
        targetId,
        reason,
        details: details.trim() || undefined,
      });
      showToast(res?.message || 'Report submitted. A moderator will review it.');
      onClose?.(true);
    } catch (err) {
      setError(err?.message || 'Could not submit the report. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Report to moderators"
      subtitle={
        targetLabel
          ? `You are reporting: ${targetLabel}`
          : 'Reports are private. The person you report will not see your name.'
      }
      width="560px"
    >
      <form onSubmit={handleSubmit}>
        <div className="report-reasons" role="radiogroup" aria-label="Report reason">
          {REASONS.map((item) => (
            <label
              key={item.value}
              className={`report-reason${reason === item.value ? ' selected' : ''}`}
            >
              <input
                type="radio"
                name="report-reason"
                value={item.value}
                checked={reason === item.value}
                onChange={() => setReason(item.value)}
              />
              <span>
                <strong>{item.label}</strong>
                <small>{item.hint}</small>
              </span>
            </label>
          ))}
        </div>

        <div className="field" style={{ marginTop: '16px' }}>
          <label htmlFor="report-details">Anything else? (optional)</label>
          <textarea
            id="report-details"
            rows={3}
            maxLength={1000}
            value={details}
            onChange={(e) => setDetails(e.target.value)}
            placeholder="Add context that helps a moderator decide quickly."
          />
          <div className="report-char-count">{details.length}/1000</div>
        </div>

        {error && <div className="field-error" style={{ marginTop: '10px' }}>{error}</div>}

        <div className="report-modal-actions">
          <button type="button" className="btn btn-outline" onClick={() => onClose?.()}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit report'}
          </button>
        </div>

        <p className="report-privacy-note">
          False reports waste moderator time and may be rate-limited. Urgent safety
          concerns should go to your college administration as well.
        </p>
      </form>
    </Modal>
  );
}
