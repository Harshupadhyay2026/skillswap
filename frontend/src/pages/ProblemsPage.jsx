import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import problemService from '../services/problemService';
import { getCategories } from '../services/skillService';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import AskQuestionModal from '../components/problem/AskQuestionModal';

function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

function StatusBadge({ status }) {
  const label = status === 'SOLVED' ? '✅ Solved' : status === 'CLOSED' ? '🚫 Closed' : '🟢 Open';
  return <span className={`tag status-${status.toLowerCase()}`}>{label}</span>;
}

/**
 * Public Q&A feed.
 *
 * Community Q&A: ask and let whoever knows answer. Reads are public so this
 * page works logged out.
 */
export default function ProblemsPage() {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [problems, setProblems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [scope, setScope] = useState('all'); // 'all' | 'mine'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [askOpen, setAskOpen] = useState(false);

  useEffect(() => {
    getCategories()
      .then((data) => setCategories(data || []))
      .catch(() => setCategories([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      if (scope === 'mine') {
        if (!isAuthenticated) {
          setProblems([]);
          return;
        }
        const data = await problemService.mine();
        setProblems(data || []);
      } else {
        const data = await problemService.list({
          query: search.trim() || undefined,
          category: category || undefined,
          status: status || undefined,
          size: 20,
        });
        setProblems(data?.items || []);
      }
    } catch (err) {
      setError(err?.message || 'Could not load questions right now.');
      setProblems([]);
    } finally {
      setLoading(false);
    }
  }, [scope, search, category, status, isAuthenticated]);

  // Debounced reload for the public feed; instant when switching scope.
  useEffect(() => {
    const timer = setTimeout(load, scope === 'mine' ? 0 : 300);
    return () => clearTimeout(timer);
  }, [load, scope]);

  const handleAsk = () => {
    if (!isAuthenticated) {
      showToast('Log in to ask the community a question.');
      navigate('/login');
      return;
    }
    setAskOpen(true);
  };

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Q&amp;A</h1>
            <p className="muted">
              Stuck on something? Ask the community — help is always free.
            </p>
          </div>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <Link to="/community" className="btn btn-outline">
              🏅 Top contributors
            </Link>
            <button className="btn btn-primary" onClick={handleAsk}>
              ✏️ Ask a question
            </button>
          </div>
        </div>

        <div className="searchbar qa-filters">
          <input
            placeholder="🔎 Search questions…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search questions"
            disabled={scope === 'mine'}
          />
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            aria-label="Filter by category"
            disabled={scope === 'mine'}
          >
            <option value="">All categories</option>
            {categories.map((c) => (
              <option value={c} key={c}>
                {c}
              </option>
            ))}
          </select>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            aria-label="Filter by status"
            disabled={scope === 'mine'}
          >
            <option value="">Any status</option>
            <option value="OPEN">Open</option>
            <option value="SOLVED">Solved</option>
            <option value="CLOSED">Closed</option>
          </select>
        </div>

        <div className="tabs" role="tablist">
          {[
            { key: 'all', label: 'All questions' },
            { key: 'mine', label: 'My questions' },
          ].map((t) => (
            <button
              key={t.key}
              role="tab"
              aria-selected={scope === t.key}
              className={`tab ${scope === t.key ? 'active' : ''}`}
              onClick={() => setScope(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="empty-state">
            <p className="muted">🔄 Loading questions…</p>
          </div>
        ) : error ? (
          <div className="empty-state">
            <p className="muted">{error}</p>
            <button className="btn btn-outline" style={{ marginTop: '12px' }} onClick={load}>
              Retry
            </button>
          </div>
        ) : problems.length === 0 ? (
          <div className="empty-state">
            <div style={{ fontSize: '34px', marginBottom: '10px' }}>💡</div>
            <h3 style={{ marginBottom: '6px' }}>
              {scope === 'mine' ? 'You have not asked anything yet' : 'No questions yet'}
            </h3>
            <p className="muted" style={{ maxWidth: '420px', margin: '0 auto 16px' }}>
              {scope === 'mine'
                ? 'Ask your first question and the community will help — for free.'
                : 'Be the first to ask. Someone here probably knows the answer.'}
            </p>
            <button className="btn btn-primary" onClick={handleAsk}>
              Ask a question
            </button>
          </div>
        ) : (
          <div className="problems">
            {problems.map((p) => (
              <Link className="card problem-card" to={`/qa/${p.id}`} key={p.id}>
                <div className="problem-card-top">
                  <StatusBadge status={p.status} />
                  {p.level && <span className="tag">{p.level}</span>}
                  <span className="tag">
                    {p.skillIcon ? `${p.skillIcon} ` : ''}
                    {p.category}
                  </span>
                  {p.hidden && <span className="tag status-hidden">🛡️ Hidden</span>}
                </div>

                <h3 className="problem-title">{p.title}</h3>
                <p className="muted problem-excerpt">{p.description}</p>

                <div className="problem-card-bottom">
                  <span className="muted" style={{ fontSize: '12px' }}>
                    {p.author?.fullName || 'Someone'} • {formatDate(p.createdAt)}
                  </span>
                  <span className="problem-stats">
                    💬 {p.answerCount} {p.answerCount === 1 ? 'answer' : 'answers'} • 👁️ {p.views}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      <AskQuestionModal
        open={askOpen}
        onClose={() => setAskOpen(false)}
        categories={categories}
        onCreated={(created) => {
          if (created?.id) navigate(`/qa/${created.id}`);
          else load();
        }}
      />
    </main>
  );
}
