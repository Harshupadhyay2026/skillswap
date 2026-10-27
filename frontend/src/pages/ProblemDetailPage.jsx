import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import problemService from '../services/problemService';
import Avatar from '../components/ui/Avatar';
import ReportButton from '../components/moderation/ReportButton';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export default function ProblemDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const { showToast } = useToast();

  const [problem, setProblem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [draft, setDraft] = useState('');
  const [posting, setPosting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await problemService.get(id);
      setProblem(data);
    } catch (err) {
      setError(err?.message || 'Could not load this question.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const handleAnswer = async (e) => {
    e.preventDefault();
    if (!draft.trim()) return;
    setPosting(true);
    try {
      const res = await problemService.answer(id, draft.trim());
      showToast(res?.message || 'Answer posted!');
      setDraft('');
      load();
    } catch (err) {
      showToast(err?.message || 'Could not post your answer.');
    } finally {
      setPosting(false);
    }
  };

  const handleAccept = async (answerId) => {
    try {
      const res = await problemService.accept(id, answerId);
      showToast(res?.message || 'Answer accepted!');
      setProblem(res?.data || problem);
    } catch (err) {
      showToast(err?.message || 'Could not accept that answer.');
    }
  };

  const handleStatusToggle = async () => {
    try {
      const res = problem.status === 'CLOSED'
        ? await problemService.reopen(id)
        : await problemService.close(id);
      showToast(res?.message || 'Updated.');
      setProblem(res?.data || problem);
    } catch (err) {
      showToast(err?.message || 'Could not update the question.');
    }
  };

  if (loading) {
    return (
      <main className="page">
        <div className="container empty-state">
          <p className="muted">🔄 Loading question…</p>
        </div>
      </main>
    );
  }

  if (error || !problem) {
    return (
      <main className="page">
        <div className="container empty-state">
          <div style={{ fontSize: '34px', marginBottom: '10px' }}>🤔</div>
          <h3 style={{ marginBottom: '8px' }}>Question not found</h3>
          <p className="muted" style={{ marginBottom: '16px' }}>{error}</p>
          <Link to="/qa" className="btn btn-primary">
            Back to Q&amp;A
          </Link>
        </div>
      </main>
    );
  }

  const answers = problem.answers || [];
  const isAuthor = isAuthenticated && user?.id === problem.author?.id;
  const alreadyAnswered = answers.some((a) => a.author?.id === user?.id);
  const canAnswer = isAuthenticated && !isAuthor && !alreadyAnswered && problem.status !== 'CLOSED';

  return (
    <main className="page">
      <div className="container" style={{ maxWidth: '860px' }}>
        <Link to="/qa" className="muted" style={{ fontSize: '13px', fontWeight: 700 }}>
          ← Back to Q&amp;A
        </Link>

        <div className="card" style={{ marginTop: '14px', padding: '22px' }}>
          <div className="problem-card-top" style={{ marginBottom: '12px' }}>
            <span className={`tag status-${problem.status.toLowerCase()}`}>
              {problem.status === 'SOLVED' ? '✅ Solved' : problem.status === 'CLOSED' ? '🚫 Closed' : '🟢 Open'}
            </span>
            {problem.level && <span className="tag">{problem.level}</span>}
            <span className="tag">
              {problem.skillIcon ? `${problem.skillIcon} ` : ''}
              {problem.category}
            </span>
            <span className="muted" style={{ fontSize: '12px' }}>👁️ {problem.views} views</span>
          </div>

          {problem.hidden && (
            <div className="moderation-banner">
              <strong>🛡️ Hidden by a moderator</strong>
              <span>
                This question no longer appears in public feeds. Only its author and
                moderators can see this page.
              </span>
            </div>
          )}

          <h1 style={{ fontSize: '24px', lineHeight: 1.3, marginBottom: '14px' }}>{problem.title}</h1>

          <p style={{ whiteSpace: 'pre-wrap', lineHeight: 1.7, marginBottom: '18px' }}>
            {problem.description}
          </p>

          <div className="problem-card-bottom" style={{ borderTop: '1px solid var(--border)', paddingTop: '14px' }}>
            <Link
              to={`/profile/${problem.author?.id}`}
              style={{ display: 'flex', gap: '10px', alignItems: 'center' }}
            >
              <Avatar name={problem.author?.fullName} src={problem.author?.avatarUrl} size={36} />
              <div>
                <strong style={{ fontSize: '13px' }}>{problem.author?.fullName || 'Someone'}</strong>
                <div className="muted" style={{ fontSize: '12px' }}>
                  asked {formatDate(problem.createdAt)}
                </div>
              </div>
            </Link>

            <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
              {!problem.hidden && !isAuthor && (
                <ReportButton
                  targetType="PROBLEM"
                  targetId={problem.id}
                  targetLabel={problem.title}
                />
              )}
              {isAuthor && problem.status !== 'SOLVED' && (
                <button className="btn btn-outline" style={{ fontSize: '12px', padding: '8px 12px' }} onClick={handleStatusToggle}>
                  {problem.status === 'CLOSED' ? 'Reopen' : 'Close question'}
                </button>
              )}
            </div>
          </div>
        </div>

        <h2 style={{ fontSize: '18px', margin: '26px 0 12px' }}>
          {answers.length} {answers.length === 1 ? 'Answer' : 'Answers'}
        </h2>

        {answers.length === 0 ? (
          <div className="empty-state" style={{ padding: '30px 20px' }}>
            <div style={{ fontSize: '28px', marginBottom: '8px' }}>🙋</div>
            <p className="muted">No answers yet — be the first to help.</p>
          </div>
        ) : (
          answers.map((a) => (
            <div
              className={`card answer-card ${a.accepted ? 'answer-accepted' : ''}`}
              key={a.id}
              style={{ marginBottom: '12px', padding: '18px' }}
            >
              {a.accepted && (
                <div className="accepted-flag">✅ Accepted answer</div>
              )}
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '10px' }}>
                <Link to={`/profile/${a.author?.id}`} style={{ display: 'flex' }}>
                  <Avatar name={a.author?.fullName} src={a.author?.avatarUrl} size={32} />
                </Link>
                <div>
                  <Link to={`/profile/${a.author?.id}`} style={{ fontSize: '13px', fontWeight: 750 }}>
                    {a.author?.fullName || 'Someone'}
                  </Link>
                  <div className="muted" style={{ fontSize: '12px' }}>{formatDate(a.createdAt)}</div>
                </div>
                {!problem.hidden && a.author?.id !== user?.id && (
                  <ReportButton
                    targetType="ANSWER"
                    targetId={a.id}
                    targetLabel={`Answer by ${a.author?.fullName || 'a member'}`}
                    className="report-trigger-answer"
                  />
                )}
              </div>

              <p style={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>{a.body}</p>

              {isAuthor && problem.status !== 'SOLVED' && (
                <button
                  className="btn btn-outline"
                  style={{ marginTop: '14px', fontSize: '12px', padding: '8px 14px' }}
                  onClick={() => handleAccept(a.id)}
                >
                  ✅ Accept this answer (+10 pts)
                </button>
              )}
            </div>
          ))
        )}

        {canAnswer && (
          <form className="card" style={{ marginTop: '18px', padding: '18px' }} onSubmit={handleAnswer}>
            <h3 style={{ fontSize: '15px', marginBottom: '10px' }}>Your answer</h3>
            <div className="field">
              <textarea
                rows={5}
                placeholder="Share what you know. Clear, step-by-step answers get accepted most often."
                value={draft}
                maxLength={10000}
                onChange={(e) => setDraft(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={posting}>
              {posting ? 'Posting…' : 'Post answer'}
            </button>
          </form>
        )}

        {!isAuthenticated && (
          <div className="card" style={{ marginTop: '18px', padding: '18px', textAlign: 'center' }}>
            <p className="muted" style={{ marginBottom: '12px' }}>
              Log in to answer this question and earn reputation points.
            </p>
            <Link to="/login" className="btn btn-primary">
              Log in to answer
            </Link>
          </div>
        )}

        {isAuthor && (
          <p className="muted" style={{ marginTop: '16px', fontSize: '13px' }}>
            Accepting an answer marks the question solved and awards the answerer 10 reputation points.
          </p>
        )}
      </div>
    </main>
  );
}
