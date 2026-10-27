import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getMySkills,
  addMySkill,
  removeMySkill,
  searchSkills,
} from '../services/skillService';
import swapService from '../services/swapService';
import learningService from '../services/learningService';
import sessionService from '../services/sessionService';
import userService from '../services/userService';
import Modal from '../components/ui/Modal';
import StatusBadge from '../components/swap/StatusBadge';
import Avatar from '../components/ui/Avatar';

const EMPTY_SKILL = {
  skillId: '',
  skillType: 'TEACH',
  proficiency: 'BEGINNER',
};

function formatShortDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

export default function DashboardPage() {
  const { user, isAuthenticated, booting, updateUser } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const fileInputRef = useRef(null);
  const [avatarBusy, setAvatarBusy] = useState(false);

  const [mySkills, setMySkills] = useState([]);
  const [swaps, setSwaps] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [progress, setProgress] = useState(null);
  const [loading, setLoading] = useState(true);

  const [skillModalOpen, setSkillModalOpen] = useState(false);
  const [skillForm, setSkillForm] = useState(EMPTY_SKILL);
  const [catalog, setCatalog] = useState([]);
  const [catalogSearch, setCatalogSearch] = useState('');
  const [saving, setSaving] = useState(false);
  const [removingId, setRemovingId] = useState(null);

  const handleAvatarChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = ''; // allow re-picking the same file
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      showToast('Choose an image under 2 MB.');
      return;
    }

    setAvatarBusy(true);
    try {
      const updated = await userService.uploadAvatar(file);
      updateUser(updated);
      showToast('Profile photo updated.');
    } catch (err) {
      showToast(err?.message || 'Could not upload that image.');
    } finally {
      setAvatarBusy(false);
    }
  };

  const handleRemoveAvatar = async () => {
    setAvatarBusy(true);
    try {
      const updated = await userService.removeAvatar();
      updateUser(updated);
      showToast('Profile photo removed.');
    } catch (err) {
      showToast(err?.message || 'Could not remove the photo.');
    } finally {
      setAvatarBusy(false);
    }
  };

  const loadAll = useCallback(async () => {
    setLoading(true);
    const [skillsResult, swapsResult, sessionsResult, progressResult] =
      await Promise.allSettled([
        getMySkills(),
        swapService.getAllMySwaps(),
        learningService.getAll(),
        sessionService.getMyProgress(),
      ]);
    if (skillsResult.status === 'fulfilled') setMySkills(skillsResult.value || []);
    if (swapsResult.status === 'fulfilled') setSwaps(swapsResult.value?.data || []);
    if (sessionsResult.status === 'fulfilled') setSessions(sessionsResult.value?.data || []);
    if (progressResult.status === 'fulfilled') setProgress(progressResult.value?.data || null);
    setLoading(false);
  }, []);

  useEffect(() => {
    if (booting) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: '/dashboard' }, replace: true });
      return;
    }
    loadAll();
  }, [booting, isAuthenticated, navigate, loadAll]);

  useEffect(() => {
    if (!skillModalOpen) return;
    searchSkills(catalogSearch)
      .then((data) => setCatalog(data || []))
      .catch(() => setCatalog([]));
  }, [skillModalOpen, catalogSearch]);

  const stats = useMemo(() => {
    const myId = user?.id;
    const pendingSwaps = swaps.filter(
      (s) => s.receiver?.id === myId && s.status === 'PENDING'
    ).length;
    const pendingSessions = sessions.filter(
      (s) => s.teacher?.id === myId && s.status === 'PENDING'
    ).length;
    const completed =
      swaps.filter((s) => s.status === 'COMPLETED').length +
      sessions.filter((s) => s.status === 'COMPLETED').length;

    return {
      teaching: mySkills.filter((s) => s.skillType === 'TEACH').length,
      learning: mySkills.filter((s) => s.skillType === 'LEARN').length,
      pending: pendingSwaps + pendingSessions,
      active:
        swaps.filter((s) => s.status === 'ACCEPTED').length +
        sessions.filter((s) => s.status === 'ACCEPTED').length,
      completed,
      reputation: user?.helpfulPoints || 0,
    };
  }, [mySkills, swaps, sessions, user]);

  const handleAddSkill = async (e) => {
    e.preventDefault();
    if (!skillForm.skillId) {
      showToast('Please select a skill from the list.');
      return;
    }
    setSaving(true);
    try {
      const created = await addMySkill({
        skillId: Number(skillForm.skillId),
        skillType: skillForm.skillType,
        proficiency: skillForm.proficiency,
      });
      setMySkills((prev) => [...prev, created]);
      setSkillForm(EMPTY_SKILL);
      setCatalogSearch('');
      setSkillModalOpen(false);
      showToast(`${created.skillName} added to your profile!`);
    } catch (err) {
      showToast(err?.message || 'Could not add skill. Maybe you already have it.');
    } finally {
      setSaving(false);
    }
  };

  const handleRemoveSkill = async (userSkillId) => {
    setRemovingId(userSkillId);
    try {
      await removeMySkill(userSkillId);
      setMySkills((prev) => prev.filter((s) => s.id !== userSkillId));
      showToast('Skill removed.');
    } catch (err) {
      showToast(err?.message || 'Could not remove skill.');
    } finally {
      setRemovingId(null);
    }
  };

  if (booting) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading your dashboard…</p>
        </div>
      </main>
    );
  }

  const teachSkills = mySkills.filter((s) => s.skillType === 'TEACH');
  const learnSkills = mySkills.filter((s) => s.skillType === 'LEARN');
  const recentSwaps = swaps.slice(0, 3);

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Welcome back, {user?.fullName?.split(' ')[0] || 'Student'} 👋</h1>
            <p className="muted">Your skills, swaps and activity in one place.</p>
          </div>
          <Link to="/explore" className="btn btn-primary">
            Find a teacher →
          </Link>
        </div>

        <div className="dash-grid">
          <div className="dash-stat">
            <span className="muted">Teaching</span>
            <strong>{loading ? '…' : stats.teaching}</strong>
          </div>
          <div className="dash-stat">
            <span className="muted">Learning Goals</span>
            <strong>{loading ? '…' : stats.learning}</strong>
          </div>
          <div className="dash-stat">
            <span className="muted">Active Exchanges</span>
            <strong style={{ color: stats.active > 0 ? 'var(--primary)' : undefined }}>
              {loading ? '…' : stats.active}
            </strong>
          </div>
          <div className="dash-stat">
            <span className="muted">Pending Requests</span>
            <strong>{loading ? '…' : stats.pending}</strong>
          </div>
          <div className="dash-stat">
            <span className="muted">Exchanges Completed</span>
            <strong style={{ color: stats.completed > 0 ? 'var(--green)' : undefined }}>
              {loading ? '…' : stats.completed}
            </strong>
          </div>
          <div className="dash-stat">
            <span className="muted">Reputation Points</span>
            <strong style={{ color: 'var(--amber)' }}>{loading ? '…' : stats.reputation}</strong>
          </div>
        </div>

        <div className="learning-progress card">
          <div className="learning-progress-head">
            <div>
              <span className="tag">📈 Learning progress</span>
              <h2>Small steps, visible progress</h2>
              <p className="muted">
                Built from your completed swaps and sessions. Add a note after each one so the next
                step is never a mystery.
              </p>
            </div>
            <Link
              to="/requests"
              className="btn btn-light"
              style={{ padding: '8px 14px', fontSize: '13px' }}
            >
              My sessions →
            </Link>
          </div>

          {loading && !progress ? (
            <p className="muted" style={{ fontSize: '13px', marginTop: '16px' }}>
              Loading your progress…
            </p>
          ) : !progress?.skills?.length ? (
            <div className="learning-progress-empty">
              <div aria-hidden="true">🌱</div>
              <div>
                <strong>Your first milestone is waiting</strong>
                <p className="muted">
                  Add a skill you want to learn, or complete a swap/session. Progress shows up here
                  automatically.
                </p>
                <Link
                  to="/explore"
                  className="btn btn-primary"
                  style={{ padding: '8px 14px', fontSize: '13px' }}
                >
                  Find a teacher
                </Link>
              </div>
            </div>
          ) : (
            <>
              <div className="progress-summary">
                <div>
                  <strong>{progress.learningSessions}</strong>
                  <span>sessions learned</span>
                </div>
                <div>
                  <strong>{progress.teachingSessions}</strong>
                  <span>sessions taught</span>
                </div>
                <div>
                  <strong>{progress.notesWritten}</strong>
                  <span>notes captured</span>
                </div>
                <div>
                  <strong>{progress.skillsInProgress}</strong>
                  <span>skills in motion</span>
                </div>
              </div>

              <div className="progress-list">
                {progress.skills.map((skill) => (
                  <div className="progress-row" key={skill.skillId}>
                    <div className="progress-skill-icon" aria-hidden="true">
                      {skill.skillIcon || '🎯'}
                    </div>
                    <div className="progress-main">
                      <div className="progress-title">
                        <strong>{skill.skillName}</strong>
                        <span className="progress-stage">{skill.stage}</span>
                      </div>
                      <div
                        className="progress-bar"
                        role="progressbar"
                        aria-label={`${skill.skillName} progress`}
                        aria-valuenow={skill.progressPercent}
                        aria-valuemin={0}
                        aria-valuemax={100}
                      >
                        <span style={{ width: `${skill.progressPercent}%` }} />
                      </div>
                      <div className="progress-meta">
                        <span>
                          {skill.completedSessions}/{skill.targetSessions || 5} sessions
                        </span>
                        {skill.teachers?.length > 0 ? (
                          <span>with {skill.teachers.join(', ')}</span>
                        ) : (
                          <span>ready for a first session</span>
                        )}
                        {skill.lastSessionAt && (
                          <span>last {formatShortDate(skill.lastSessionAt)}</span>
                        )}
                      </div>
                    </div>
                    {skill.latestSessionId && skill.latestSessionType && (
                      <Link
                        to={`/session/${skill.latestSessionType.toLowerCase()}/${skill.latestSessionId}`}
                        className="btn btn-light progress-notes-link"
                        style={{ padding: '7px 12px', fontSize: '12px' }}
                      >
                        📝 Notes
                      </Link>
                    )}
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        <div className="account-panel">
          <div className="card">
            <h3>My Profile</h3>
            <div style={{ marginTop: '15px' }}>
              <div className="person-top">
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  <Avatar name={user?.fullName} src={user?.avatarUrl} size={56} />
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/png,image/jpeg,image/webp,image/gif"
                    hidden
                    onChange={handleAvatarChange}
                  />
                  <button
                    type="button"
                    className="link-button"
                    disabled={avatarBusy}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    {avatarBusy ? 'Uploading…' : 'Change photo'}
                  </button>
                  {user?.avatarUrl && (
                    <button
                      type="button"
                      className="link-button subtle"
                      disabled={avatarBusy}
                      onClick={handleRemoveAvatar}
                    >
                      Remove
                    </button>
                  )}
                </div>
                <div style={{ minWidth: 0 }}>
                  <strong>{user?.fullName}</strong>
                  <div className="muted">@{user?.username}</div>
                </div>
              </div>
              <p className="muted" style={{ margin: '6px 0' }}>
                ✉️ {user?.email}
              </p>
              {user?.college && (
                <p className="muted" style={{ margin: '6px 0' }}>
                  🎓 {user.college}
                </p>
              )}
              {(user?.branch || user?.yearOfStudy) && (
                <p className="muted" style={{ margin: '6px 0' }}>
                  💻 {user.branch} {user.yearOfStudy ? `• ${user.yearOfStudy}` : ''}
                </p>
              )}
              {user?.city && (
                <p className="muted" style={{ margin: '6px 0' }}>
                  📍 {user.city}
                </p>
              )}
              {user?.bio && (
                <p className="muted" style={{ margin: '10px 0', lineHeight: 1.6 }}>
                  {user.bio}
                </p>
              )}
              {user?.roles?.length > 0 && (
                <div style={{ margin: '10px 0' }}>
                  {user.roles.map((r) => (
                    <span
                      key={r}
                      className="tag"
                      style={{ background: 'var(--badge-bg)', color: 'var(--primary)' }}
                    >
                      ✓ {r.replace('ROLE_', '')}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3>My Skills</h3>
              <button
                className="btn btn-light"
                style={{ padding: '7px 12px', fontSize: '13px' }}
                onClick={() => setSkillModalOpen(true)}
              >
                + Add
              </button>
            </div>

            <div style={{ marginTop: '15px' }}>
              {loading ? (
                <p className="muted" style={{ fontSize: '13px' }}>Loading…</p>
              ) : mySkills.length === 0 ? (
                <p className="muted" style={{ fontSize: '13px' }}>
                  No skills yet. Add what you can teach — that is what makes swaps possible.
                </p>
              ) : (
                <>
                  {teachSkills.length > 0 && (
                    <>
                      <div className="skill-group-label">I can teach</div>
                      {teachSkills.map((s) => (
                        <div className="skill-chip" key={s.id}>
                          <span>
                            {s.skillIcon} {s.skillName} — {s.proficiency.toLowerCase()}
                          </span>
                          <button
                            className="remove"
                            aria-label={`Remove ${s.skillName}`}
                            disabled={removingId === s.id}
                            onClick={() => handleRemoveSkill(s.id)}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </>
                  )}

                  {learnSkills.length > 0 && (
                    <>
                      <div className="skill-group-label" style={{ marginTop: '14px' }}>
                        I want to learn
                      </div>
                      {learnSkills.map((s) => (
                        <div className="skill-chip" key={s.id}>
                          <span>
                            {s.skillIcon} {s.skillName} — {s.proficiency.toLowerCase()}
                          </span>
                          <button
                            className="remove"
                            aria-label={`Remove ${s.skillName}`}
                            disabled={removingId === s.id}
                            onClick={() => handleRemoveSkill(s.id)}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </>
                  )}
                </>
              )}
            </div>
          </div>
        </div>

        <div className="card" style={{ marginTop: '18px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Recent swaps</h3>
            <Link to="/requests" className="btn btn-light" style={{ padding: '7px 12px', fontSize: '13px' }}>
              View all
            </Link>
          </div>

          <div style={{ marginTop: '15px' }}>
            {recentSwaps.length === 0 ? (
              <p className="muted" style={{ fontSize: '13px' }}>
                No swaps yet. Head to <Link to="/explore" style={{ color: 'var(--primary)' }}>Explore</Link> and
                propose one.
              </p>
            ) : (
              recentSwaps.map((s) => {
                const counterpart = s.sender?.id === user?.id ? s.receiver : s.sender;
                return (
                  <div
                    key={s.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '11px 0',
                      borderBottom: '1px solid var(--line)',
                    }}
                  >
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <strong style={{ fontSize: '13.5px' }}>{counterpart?.fullName}</strong>
                      <div className="muted" style={{ fontSize: '12px' }}>
                        {s.offeredSkill?.name} ⇄ {s.requestedSkill?.name}
                      </div>
                    </div>
                    <StatusBadge status={s.status} />
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      <Modal
        open={skillModalOpen}
        onClose={() => setSkillModalOpen(false)}
        title="Add a skill"
        subtitle="Pick from the catalog — it saves straight to your profile."
      >
        <form onSubmit={handleAddSkill}>
          <div className="field">
            <label>Search skill</label>
            <input
              placeholder="e.g. Python, React, DSA…"
              value={catalogSearch}
              onChange={(e) => setCatalogSearch(e.target.value)}
            />
          </div>

          <div className="field">
            <label>Select skill *</label>
            <select value={skillForm.skillId} onChange={(e) => setSkillForm({ ...skillForm, skillId: e.target.value })}>
              <option value="">— Choose a skill —</option>
              {catalog.map((s) => (
                <option value={s.id} key={s.id}>
                  {s.icon} {s.name} ({s.category})
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label>I want to</label>
            <select
              value={skillForm.skillType}
              onChange={(e) => setSkillForm({ ...skillForm, skillType: e.target.value })}
            >
              <option value="TEACH">Teach this skill</option>
              <option value="LEARN">Learn this skill</option>
            </select>
          </div>

          <div className="field">
            <label>My level</label>
            <select
              value={skillForm.proficiency}
              onChange={(e) => setSkillForm({ ...skillForm, proficiency: e.target.value })}
            >
              <option value="BEGINNER">Beginner</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="ADVANCED">Advanced</option>
            </select>
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={saving}>
            {saving ? 'Saving…' : 'Save Skill to Profile'}
          </button>
        </form>
      </Modal>
    </main>
  );
}
