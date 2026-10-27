import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import communityService from '../services/communityService';
import Avatar from '../components/ui/Avatar';
import RatingStars from '../components/review/RatingStars';

const MEDALS = { 1: '🥇', 2: '🥈', 3: '🥉' };

/**
 * Public leaderboard. Reputation is earned only by having an answer accepted
 * on a Q&A question (10 points each), so this is a genuine helpfulness board.
 */
export default function CommunityPage() {
  const [contributors, setContributors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    communityService
      .getTopContributors(20)
      .then((data) => {
        if (!cancelled) setContributors(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err?.message || 'Could not load the leaderboard.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="page">
      <div className="container narrow">
        <div className="page-head">
          <div>
            <h1>Top contributors</h1>
            <p className="muted">
              Earn 10 reputation every time your answer is accepted. Help for free,
              build a name for yourself.
            </p>
          </div>
          <Link to="/qa" className="btn btn-outline">
            Browse Q&amp;A
          </Link>
        </div>

        {loading ? (
          <p className="muted">Loading leaderboard…</p>
        ) : error ? (
          <div className="card empty-state">
            <p className="muted">{error}</p>
          </div>
        ) : contributors.length === 0 ? (
          <div className="card empty-state">
            <div style={{ fontSize: '34px', marginBottom: '10px' }}>🏅</div>
            <h3>No contributions yet</h3>
            <p className="muted" style={{ maxWidth: '420px', margin: '8px auto 16px' }}>
              Accepted answers will appear here. Be the first to help someone in the
              Q&amp;A.
            </p>
            <Link to="/qa" className="btn btn-primary">
              Answer a question
            </Link>
          </div>
        ) : (
          <div className="leaderboard">
            {contributors.map((person) => (
              <div
                key={person.userId}
                className={`leader-row card${person.rank <= 3 ? ' top' : ''}`}
              >
                <div className="leader-rank" aria-label={`Rank ${person.rank}`}>
                  {MEDALS[person.rank] || `#${person.rank}`}
                </div>

                <Link to={`/profile/${person.userId}`} style={{ display: 'flex' }}>
                  <Avatar name={person.fullName} src={person.avatarUrl} size={48} />
                </Link>

                <div className="leader-main">
                  <Link to={`/profile/${person.userId}`} className="leader-name">
                    {person.fullName}
                  </Link>
                  <div className="muted leader-meta">
                    @{person.username}
                    {person.college ? ` • ${person.college}` : ''}
                    {person.yearOfStudy ? ` • ${person.yearOfStudy}` : ''}
                  </div>
                  <div className="leader-rating">
                    <RatingStars average={person.avgRating} count={person.reviewCount} size={12} />
                  </div>
                </div>

                <div className="leader-stats">
                  <div className="leader-points">
                    <strong>{person.helpfulPoints}</strong>
                    <span className="muted">reputation</span>
                  </div>
                  <div className="leader-sub">
                    <span>{person.acceptedAnswers} accepted</span>
                    <span>{person.answersGiven} answers</span>
                    <span>{person.questionsAsked} questions</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
