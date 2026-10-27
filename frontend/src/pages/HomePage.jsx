import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const STEPS = [
  {
    num: '01',
    title: 'Create your profile',
    body: 'Add your college, course, and the skills you can teach and want to learn.',
  },
  {
    num: '02',
    title: 'Find your match',
    body: 'Search students by skill, or let SkillSwap find people whose needs mirror yours.',
  },
  {
    num: '03',
    title: 'Swap, learn together, or ask',
    body: 'Trade skills with a peer, learn the same skill together, or get help in the Q&A.',
  },
];

export default function HomePage() {
  const { isAuthenticated } = useAuth();

  return (
    <main id="home">
      <section className="hero">
        <div className="container">
          <div className="hero-grid">
            <div>
              <div className="badge">🎓 Built for students, by students</div>
              <h1>
                Learn something.
                <br />
                <span>Teach something.</span>
                <br />
                SkillSwap.
              </h1>
              <p>
                A student-powered community where you swap what you know for what you want to
                learn, and grow together. Swap skills with a peer or study the same skill together —
                every learning session is free. You can also ask the community in Q&amp;A.
              </p>
              <div className="hero-buttons">
                <Link className="btn btn-primary" to="/explore">
                  Explore Skills →
                </Link>
                {isAuthenticated ? (
                  <Link className="btn btn-outline" to="/requests">
                    My Requests
                  </Link>
                ) : (
                  <Link className="btn btn-outline" to="/register">
                    Join Free
                  </Link>
                )}
              </div>
            </div>

            <div className="hero-card">
              <div className="profile-mini">
                <div className="avatar">RK</div>
                <div>
                  <strong>Rahul Kumar</strong>
                  <div className="muted">CSE • 2nd Year</div>
                </div>
                <span style={{ marginLeft: 'auto', fontWeight: 700, fontSize: '13px' }}>🔄 Swaps</span>
              </div>

              <div className="skill-row">
                <div>
                  <strong>Teaches</strong>
                  <br />
                  <span className="tag">Python</span>
                  <span className="tag">DSA</span>
                </div>
                <span className="price">→</span>
              </div>

              <div className="skill-row">
                <div>
                  <strong>Wants to learn</strong>
                  <br />
                  <span className="tag">React</span>
                  <span className="tag">UI/UX</span>
                </div>
                <span className="price">←</span>
              </div>

              <div style={{ paddingTop: '17px', textAlign: 'center' }}>
                <span className="muted">🔁 Share what you know. Learn from each other.</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section>
        <div className="container">
          <div className="section-head">
            <h2>How SkillSwap works</h2>
            <p>Simple, fair, and built around the way students actually learn.</p>
          </div>
          <div className="steps">
            {STEPS.map((step) => (
              <div className="card" key={step.num}>
                <div className="step-num">{step.num}</div>
                <h3>{step.title}</h3>
                <p>{step.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section>
        <div className="container">
          <div className="feature-grid">
            <div className="card swap">
              <div className="feature-icon">🔄</div>
              <h3>Skill for skill</h3>
              <p>
                You teach Python, they teach design. Both of you gain something. Swaps are always
                free — the only currency is knowledge.
              </p>
              <br />
              <Link className="btn btn-primary" to="/explore">
                Find a Skill Swap
              </Link>
            </div>

            <div className="card free">
              <div className="feature-icon">🤝</div>
              <h3>Learn together</h3>
              <p>
                No skill to trade? No problem. Request a shared session to study the same skill,
                practice together, and learn from each other. All learning is free.
              </p>
              <br />
              <Link className="btn btn-outline" to={isAuthenticated ? '/dashboard' : '/register'}>
                {isAuthenticated ? 'Find a learning partner' : 'Create your profile'}
              </Link>
              <p className="muted" style={{ marginTop: '12px', fontSize: '13px' }}>
                Stuck on something?{' '}
                <Link to="/qa" style={{ fontWeight: 700, color: 'var(--primary)' }}>
                  Ask the community — free →
                </Link>
              </p>
            </div>
          </div>
        </div>
      </section>

      <section>
        <div className="container">
          <div className="cta">
            <h2>Know something? Teach it.</h2>
            <p>
              Someone out there wants to learn exactly what you already know — and they know
              something you want. Find them.
            </p>
            <Link className="btn btn-primary" to={isAuthenticated ? '/explore' : '/register'}>
              {isAuthenticated ? 'Find your swap →' : 'Create your profile →'}
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
