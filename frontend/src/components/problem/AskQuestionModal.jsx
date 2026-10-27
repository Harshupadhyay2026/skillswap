import { useEffect, useState } from 'react';
import Modal from '../ui/Modal';
import problemService from '../../services/problemService';
import { useToast } from '../../context/ToastContext';

const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

/**
 * Ask the community a question. Q&A is a free community-help feature.
 */
export default function AskQuestionModal({ open, onClose, categories = [], onCreated }) {
  const { showToast } = useToast();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [level, setLevel] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    setTitle('');
    setDescription('');
    setLevel('');
    setSubmitting(false);
    setCategory(categories[0] || 'General');
    // Reset only when the modal opens; later category loads must not wipe input.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !description.trim() || !category.trim()) {
      showToast('Title, description and category are all required.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await problemService.create({
        title: title.trim(),
        description: description.trim(),
        category: category.trim(),
        level: level || undefined,
      });
      showToast(res?.message || 'Question posted!');
      onCreated?.(res?.data);
      onClose?.();
    } catch (err) {
      showToast(err?.message || 'Could not post your question.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Ask the community"
      subtitle="Anyone can answer and help the community learn."
      width="620px"
    >
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>Question *</label>
          <input
            placeholder="e.g. How do I reverse a linked list in Python?"
            value={title}
            maxLength={200}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label>Details *</label>
          <textarea
            rows={6}
            placeholder="Explain what you have tried and where you are stuck. The more context, the better the answers."
            value={description}
            maxLength={10000}
            onChange={(e) => setDescription(e.target.value)}
            required
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div className="field">
            <label>Category *</label>
            <select value={category} onChange={(e) => setCategory(e.target.value)} required>
              {categories.length === 0 && <option value="General">General</option>}
              {categories.map((c) => (
                <option value={c} key={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label>Level (optional)</label>
            <select value={level} onChange={(e) => setLevel(e.target.value)}>
              <option value="">Any</option>
              {LEVELS.map((l) => (
                <option value={l} key={l}>
                  {l.charAt(0) + l.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={submitting}>
          {submitting ? 'Posting…' : 'Post question'}
        </button>
      </form>
    </Modal>
  );
}
