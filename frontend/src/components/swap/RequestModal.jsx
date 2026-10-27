import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import Modal from '../ui/Modal';
import swapService from '../../services/swapService';
import learningService from '../../services/learningService';
import { useToast } from '../../context/ToastContext';

/**
 * Choose between exchanging different skills and studying the same skill together.
 */
export default function RequestModal({ open, onClose, teacher, mySkills, onCreated }) {
  const { showToast } = useToast();
  const [offeredSkillId, setOfferedSkillId] = useState('');
  const [selectedSkillId, setSelectedSkillId] = useState('');
  const [mode, setMode] = useState('SWAP');
  const [message, setMessage] = useState('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const teachSkills = useMemo(
    () => (mySkills || []).filter((s) => s.skillType === 'TEACH'),
    [mySkills]
  );

  const availableSkills = useMemo(() => {
    if (Array.isArray(teacher?.skills) && teacher.skills.length > 0) {
      return teacher.skills;
    }
    if (teacher?.skillId) {
      return [{
        id: teacher.id,
        skillId: teacher.skillId,
        skillName: teacher.skillName,
        skillIcon: teacher.skillIcon,
        proficiency: teacher.proficiency,
      }];
    }
    return [];
  }, [teacher]);

  const selectedSkill = useMemo(
    () => availableSkills.find((s) => String(s.skillId) === selectedSkillId) || availableSkills[0],
    [availableSkills, selectedSkillId]
  );

  useEffect(() => {
    if (!open) return;
    setOfferedSkillId(teachSkills[0] ? String(teachSkills[0].skillId) : '');
    setSelectedSkillId(availableSkills[0] ? String(availableSkills[0].skillId) : '');
    setMode(teachSkills.length > 0 ? 'SWAP' : 'LEARN_TOGETHER');
    setMessage('');
    setScheduledAt('');
    setSubmitting(false);
  }, [open, teachSkills, availableSkills]);

  if (!teacher || !selectedSkill) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();

    const teacherId = teacher.teacherId ?? teacher.id;
    if (!teacherId || !selectedSkill.skillId) {
      showToast('This profile is missing its user ID or teaching skill. Please refresh and try again.');
      return;
    }

    if (mode === 'SWAP' && !offeredSkillId) {
      showToast('Please select a skill you will teach in return.');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        message: message.trim() || (mode === 'LEARN_TOGETHER'
          ? `Would you like to study and practice ${selectedSkill.skillName} together?`
          : undefined),
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : undefined,
      };
      const res = mode === 'SWAP'
        ? await swapService.proposeSwap({
            ...payload,
            receiverId: teacherId,
            offeredSkillId: Number(offeredSkillId),
            requestedSkillId: Number(selectedSkill.skillId),
          })
        : await learningService.createRequest({
            ...payload,
            teacherId,
            skillId: Number(selectedSkill.skillId),
          });

      showToast(res?.message || (mode === 'SWAP' ? 'Skill exchange proposal sent!' : 'Shared learning request sent!'));
      onCreated?.(res?.data, mode);
      onClose?.();
    } catch (err) {
      showToast(err?.message || 'Could not send the exchange proposal.');
    } finally {
      setSubmitting(false);
    }
  };

  const offeredSkill = teachSkills.find((s) => String(s.skillId) === offeredSkillId);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`${mode === 'SWAP' ? 'Swap skills with' : 'Learn with'} ${teacher.teacherName}`}
      subtitle="Choose how you would like to learn together. Every session is free."
    >
      <div className="field" style={{ marginBottom: '16px' }}>
        <label htmlFor="learning-mode">Learning option</label>
        <select id="learning-mode" value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="SWAP" disabled={teachSkills.length === 0}>Skill swap — teach one skill, learn another</option>
          <option value="LEARN_TOGETHER">Learn the same skill together</option>
        </select>
        <p className="muted" style={{ fontSize: '12px', marginTop: '6px' }}>
          {mode === 'SWAP'
            ? 'Trade knowledge: you teach a skill and learn one they teach.'
            : 'Request a shared study session for this skill. Compare approaches, practice, and help each other.'}
        </p>
      </div>

      {availableSkills.length > 1 && (
        <div className="field" style={{ marginBottom: '14px' }}>
          <label htmlFor="request-skill">Skill you want to learn</label>
          <select
            id="request-skill"
            value={selectedSkillId || String(selectedSkill.skillId)}
            onChange={(e) => setSelectedSkillId(e.target.value)}
          >
            {availableSkills.map((skill) => (
              <option value={skill.skillId} key={`${skill.id || skill.skillId}-${skill.skillId}`}>
                {skill.skillIcon} {skill.skillName} ({skill.proficiency?.toLowerCase() || 'intermediate'})
              </option>
            ))}
          </select>
        </div>
      )}

      {mode === 'SWAP' && teachSkills.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '12px 4px' }}>
          <div style={{ fontSize: '36px', marginBottom: '12px' }}>💡</div>
          <h3 style={{ marginBottom: '8px' }}>Add a Teaching Skill First</h3>
          <p className="muted" style={{ marginBottom: '18px', lineHeight: 1.6, fontSize: '13.5px' }}>
            SkillSwap is a mutual skill-exchange community. You must offer at least one skill you can
            <strong> teach</strong> before requesting to learn from someone else.
          </p>
          <Link
            to="/dashboard"
            className="btn btn-primary"
            style={{ display: 'inline-block', width: '100%', textAlign: 'center' }}
            onClick={onClose}
          >
            Go to Dashboard &amp; Add a Skill →
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit}>
          {mode === 'SWAP' ? <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr auto 1fr',
              gap: '10px',
              alignItems: 'center',
              background: 'var(--tag-bg)',
              borderRadius: '12px',
              padding: '12px',
              marginBottom: '18px',
            }}
          >
            <div>
              <div className="muted" style={{ fontSize: '10px', fontWeight: 700, textTransform: 'uppercase' }}>
                You teach
              </div>
              <div style={{ fontWeight: 750, fontSize: '13px', marginTop: '2px' }}>
                {offeredSkill ? `${offeredSkill.skillIcon || '🎯'} ${offeredSkill.skillName}` : '—'}
              </div>
            </div>
            <div style={{ color: 'var(--primary)', fontSize: '18px' }}>⇄</div>
            <div>
              <div className="muted" style={{ fontSize: '10px', fontWeight: 700, textTransform: 'uppercase' }}>
                You learn
              </div>
              <div style={{ fontWeight: 750, fontSize: '13px', marginTop: '2px' }}>
                {selectedSkill.skillIcon || '🎯'} {selectedSkill.skillName}
              </div>
            </div>
          </div> : (
            <div className="card" style={{ padding: '14px', marginBottom: '18px', background: 'var(--tag-bg)' }}>
              <div className="muted" style={{ fontSize: '10px', fontWeight: 700, textTransform: 'uppercase' }}>
                Learn together
              </div>
              <div style={{ fontWeight: 750, marginTop: '4px' }}>
                {selectedSkill.skillIcon || '🎯'} {selectedSkill.skillName}
              </div>
              <p className="muted" style={{ fontSize: '12px', margin: '6px 0 0' }}>
                Both of you can share what you know and practice this skill together.
              </p>
            </div>
          )}

          {mode === 'SWAP' && <div className="field">
            <label>Skill you will teach in return *</label>
            <select
              value={offeredSkillId}
              onChange={(e) => setOfferedSkillId(e.target.value)}
              required
            >
              {teachSkills.map((s) => (
                <option value={s.skillId} key={s.id}>
                  {s.skillIcon} {s.skillName} ({s.proficiency?.toLowerCase() || 'beginner'})
                </option>
              ))}
            </select>
          </div>}

          <div className="field">
            <label>Preferred time (optional)</label>
            <input
              type="datetime-local"
              value={scheduledAt}
              onChange={(e) => setScheduledAt(e.target.value)}
            />
          </div>

          <div className="field">
            <label>Message (optional)</label>
            <textarea
              placeholder={mode === 'SWAP'
                ? `Hi ${teacher.teacherName?.split(' ')[0] || 'there'}, I'd love to swap skills! I can teach ${offeredSkill?.skillName || 'my skill'} in exchange for ${selectedSkill.skillName}…`
                : `Hi ${teacher.teacherName?.split(' ')[0] || 'there'}, would you like to study and practice ${selectedSkill.skillName} together?`}
              value={message}
              maxLength={1000}
              onChange={(e) => setMessage(e.target.value)}
            />
          </div>

          <p className="muted" style={{ fontSize: '12px', lineHeight: 1.6, marginBottom: '14px' }}>
            Once accepted, a private video meeting room and shared session notes will unlock automatically.
          </p>

          <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={submitting}>
            {submitting ? 'Sending request…' : mode === 'SWAP' ? 'Send Skill Swap Proposal' : 'Request Shared Learning Session'}
          </button>
        </form>
      )}
    </Modal>
  );
}
