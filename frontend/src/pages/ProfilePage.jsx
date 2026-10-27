import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getPublicProfile, getMySkills } from '../services/skillService';
import reviewService from '../services/reviewService';
import messageService from '../services/messageService';
import RatingStars from '../components/review/RatingStars';
import RequestModal from '../components/swap/RequestModal';
import Avatar from '../components/ui/Avatar';
import ReportButton from '../components/moderation/ReportButton';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export default function ProfilePage() {
  const { userId } = useParams();
  const { user: me, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [mySkills, setMySkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTeacher, setActiveTeacher] = useState(null);
  const [messageBusy, setMessageBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');

    Promise.all([
      getPublicProfile(userId),
      reviewService.getUserReviews(userId).catch(() => ({ data: [] })),
      isAuthenticated ? getMySkills().catch(() => []) : Promise.resolve([]),
    ])
      .then(([profileData, reviewsData, skillsData]) => {
        if (cancelled) return;
        setProfile(profileData);
        setReviews(reviewsData?.data || []);
        setMySkills(skillsData || []);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err?.message || 'Could not load this profile.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [userId, isAuthenticated]);

  if (loading) {
    return (
      <main className="page">
        <div className="container">
          <p className="muted">Loading profile…</p>
        </div>
      </main>
    );
  }

  if (error || !profile) {
    return (
      <main className="page">
        <div className="container empty-state">
          <div style={{ fontSize: '34px', marginBottom: '10px' }}>🤷</div>
          <h3>Profile not found</h3>
          <p className="muted" style={{ margin: '8px 0 18px' }}>
            {error}
          </p>
          <Link to="/explore" className="btn btn-primary">
            Back to Explore
          </Link>
        </div>
      </main>
    );
  }

  const person = profile.user;
  const teachSkills = (profile.skills || []).filter((s) => s.skillType === 'TEACH');
  const learnSkills = (profile.skills || []).filter((s) => s.skillType === 'LEARN');
  const isMe = me?.id === person.id;

  const openRequest = (skill) => {
    if (!isAuthenticated) {
      showToast('Log in to request a session.');
      navigate('/login', { state: { from: `/profile/${person.id}` } });
      return;
    }
    setActiveTeacher({
      teacherId: person.id,
      teacherName: person.fullName,
      skillId: skill.skillId,
      skillName: skill.skillName,
      skillIcon: skill.skillIcon,
      skillCategory: skill.skillCategory,
    });
  };

  const openMessage = async () => {
    if (!isAuthenticated) {
      showToast('Log in to send a message.');
      navigate('/login', { state: { from: `/profile/${person.id}` } });
      return;
    }

    setMessageBusy(true);
    try {
      const conversation = await messageService.startConversation(person.id);
      navigate(`/messages/${conversation.id}`);
    } catch (err) {
      showToast(err?.message || 'Could not open a conversation.');
    } finally {
      setMessageBusy(false);
    }
  };

  return (
    <main className="page">
      <div className="container">
        <div className="profile-head card">
          <Avatar name={person.fullName} src={person.avatarUrl} size={72} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <h1 style={{ fontSize: '26px', letterSpacing: '-0.8px' }}>{person.fullName}</h1>
            <div className="muted" style={{ fontSize: '13px', marginTop: '3px' }}>
              @{person.username}
              {person.college ? ` • ${person.college}` : ''}
            </div>
            <div style={{ marginTop: '8px' }}>
              <RatingStars average={profile.avgRating} count={profile.reviewCount} size={14} />
            </div>
          </div>
          {isMe ? (
            <Link to="/dashboard" className="btn btn-outline">
              Edit profile
            </Link>
          ) : (
            <div className="profile-actions">
              <button
                type="button"
                className="btn btn-primary"
                onClick={openMessage}
                disabled={messageBusy}
              >
                {messageBusy ? 'Opening…' : '💬 Message'}
              </button>
              <ReportButton
                targetType="USER"
                targetId={person.id}
                targetLabel={`${person.fullName} (@${person.username})`}
                label="Report profile"
              />
            </div>
          )}
        </div>

        <div className="card reputation-strip" style={{ marginTop: '16px' }}>
          <div className="reputation-stat">
            <strong>{profile.helpfulPoints ?? 0}</strong>
            <span className="muted">Reputation</span>
          </div>
          <div className="reputation-stat">
            <strong>{profile.questionsAsked ?? 0}</strong>
            <span className="muted">Questions</span>
          </div>
          <div className="reputation-stat">
            <strong>{profile.answersGiven ?? 0}</strong>
            <span className="muted">Answers</span>
          </div>
          <div className="reputation-stat">
            <strong>{profile.acceptedAnswers ?? 0}</strong>
            <span className="muted">Accepted</span>
          </div>
        </div>

        {(person.branch || person.yearOfStudy || person.city) && (
          <div className="card" style={{ marginTop: '16px' }}>
            <div className="muted" style={{ fontSize: '12.5px', lineHeight: 1.9 }}>
              {person.branch && <div>💻 {person.branch}</div>}
              {person.yearOfStudy && <div>🎓 {person.yearOfStudy}</div>}
              {person.city && <div>📍 {person.city}</div>}
            </div>
          </div>
        )}

        {person.bio && (
          <div className="card" style={{ marginTop: '16px' }}>
            <h3>About</h3>
            <p className="muted" style={{ marginTop: '10px', lineHeight: 1.7, fontSize: '13.5px' }}>
              {person.bio}
            </p>
          </div>
        )}

        <div className="card" style={{ marginTop: '16px' }}>
          <h3>Teaches</h3>
          <div style={{ marginTop: '14px' }}>
            {teachSkills.length === 0 ? (
              <p className="muted" style={{ fontSize: '13px' }}>
                {isMe ? 'You have not added any skills you can teach yet.' : 'No teaching skills listed yet.'}
              </p>
            ) : (
              teachSkills.map((s) => (
                <div
                  key={s.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '12px 0',
                    borderBottom: '1px solid var(--line)',
                  }}
                >
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <strong style={{ fontSize: '14px' }}>
                      {s.skillIcon} {s.skillName}
                    </strong>
                    <div className="muted" style={{ fontSize: '12px' }}>
                      {s.skillCategory} • {s.proficiency?.toLowerCase()}
                    </div>
                  </div>
                  <span className="price" style={{ fontSize: '13px', color: 'var(--accent)' }}>
                    🔄 SWAP
                  </span>
                  {!isMe && (
                    <button
                      className="btn btn-primary"
                      style={{ padding: '8px 14px', fontSize: '12.5px' }}
                      onClick={() => openRequest(s)}
                    >
                      Propose swap
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {learnSkills.length > 0 && (
          <div className="card" style={{ marginTop: '16px' }}>
            <h3>Wants to learn</h3>
            <div style={{ marginTop: '12px' }}>
              {learnSkills.map((s) => (
                <span className="tag" key={s.id}>
                  {s.skillIcon} {s.skillName}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="card" style={{ marginTop: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Reviews</h3>
            <RatingStars average={profile.avgRating} count={profile.reviewCount} size={13} />
          </div>

          <div style={{ marginTop: '14px' }}>
            {reviews.length === 0 ? (
              <p className="muted" style={{ fontSize: '13px' }}>
                No reviews yet. Reviews appear after a completed swap or session.
              </p>
            ) : (
              reviews.map((r) => (
                <div
                  key={r.id}
                  style={{ padding: '14px 0', borderBottom: '1px solid var(--line)' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <Avatar
                      name={r.reviewer?.fullName}
                      src={r.reviewer?.avatarUrl}
                      size={32}
                    />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <strong style={{ fontSize: '13px' }}>{r.reviewer?.fullName}</strong>
                      <div className="muted" style={{ fontSize: '11.5px' }}>
                        {r.skillName ? `${r.skillName} • ` : ''}
                        {formatDate(r.createdAt)}
                      </div>
                    </div>
                    <span style={{ color: '#f59e0b', fontSize: '13px', letterSpacing: '-1px' }}>
                      {'★'.repeat(r.rating)}
                      {'☆'.repeat(5 - r.rating)}
                    </span>
                  </div>
                  {r.comment && (
                    <p
                      className="muted"
                      style={{ fontSize: '12.5px', lineHeight: 1.6, marginTop: '8px', marginLeft: '42px' }}
                    >
                      {r.comment}
                    </p>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <RequestModal
        open={Boolean(activeTeacher)}
        onClose={() => setActiveTeacher(null)}
        teacher={activeTeacher}
        mySkills={mySkills}
        onCreated={(_d, mode) =>
          showToast(
            mode === 'SWAP'
              ? 'Swap proposal sent! Track it under My Requests.'
              : 'Learning request sent! Track it under My Requests.'
          )
        }
      />
    </main>
  );
}
