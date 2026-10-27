import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { searchTeachers, getMySkills } from '../services/skillService';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import RequestModal from '../components/swap/RequestModal';
import RatingStars from '../components/review/RatingStars';
import Avatar from '../components/ui/Avatar';

const CATEGORY_META = {
  Programming: { icon: '💻', tone: 'violet' },
  'Web Dev': { icon: '🌐', tone: 'cyan' },
  'Data & AI': { icon: '🤖', tone: 'pink' },
  'CS Fundamentals': { icon: '🧠', tone: 'amber' },
  Design: { icon: '🎨', tone: 'rose' },
  DevOps: { icon: '⚙️', tone: 'green' },
};

const HOW_IT_WORKS = [
  {
    icon: '🔎',
    title: 'Find your person',
    text: 'Search a skill or browse an interest. See who teaches it, how they explain it, and what starting feels like.',
  },
  {
    icon: '🔄',
    title: 'Choose how to learn',
    text: 'Swap different skills or invite someone to study and practice the same skill together.',
  },
  {
    icon: '🎥',
    title: 'Meet in-app',
    text: 'Once they accept, a private video room opens automatically. No external links, no awkward setup.',
  },
];

const SORT_OPTIONS = [
  { value: 'recommended', label: 'Recommended' },
  { value: 'rating', label: 'Top rated' },
  { value: 'newest', label: 'Newest members' },
];

const PAGE_SIZE = 9;

function categoryMeta(name) {
  return CATEGORY_META[name] || { icon: '✨', tone: 'slate' };
}

function firstName(name) {
  return (name || 'This student').split(' ')[0];
}

function truncate(text, max = 150) {
  if (!text) return '';
  return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text;
}

function joinedLabel(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString(undefined, { month: 'short', year: 'numeric' });
}

function fallbackBio(group) {
  const skill = group.skills[0]?.skillName || 'a skill';
  return `${firstName(group.name)} is open to sharing what they know. Ask about ${skill} and what a first session could look like.`;
}

function recommendedScore(group) {
  return (
    (group.avgRating || 0) * 8 +
    Math.min(group.reviewCount || 0, 20) +
    Math.min(group.helpfulPoints || 0, 50) / 5 +
    (group.hasFree ? 2 : 0)
  );
}

function TeacherCard({ group, index, onConnect, isAuthenticated }) {
  const primary = group.skills[0];
  const visibleSkills = group.skills.slice(0, 3);
  const remaining = group.skills.length - visibleSkills.length;
  const topRated = group.avgRating >= 4.5 && group.reviewCount > 0;
  const exchangeLabel = 'Always free';
  const exchangeNote = 'learn together';
  const joined = joinedLabel(group.joinedAt);

  return (
    <article className="explore-card" style={{ '--i': index }}>
      <div className="explore-card-sheen" aria-hidden="true" />
      <div className="explore-card-top">
        <Link to={`/profile/${group.id}`} className="explore-avatar-link" aria-label={`View ${group.name}'s profile`}>
          <Avatar name={group.name} src={group.avatarUrl} size={54} />
        </Link>
        <div className="explore-identity">
          <Link to={`/profile/${group.id}`} className="explore-name">
            {group.name}
          </Link>
          <div className="explore-subline">
            {[group.college, group.year].filter(Boolean).join(' · ') || 'SkillSwap member'}
          </div>
          <div className="explore-rating-line">
            {group.reviewCount > 0 ? (
              <RatingStars average={group.avgRating} count={group.reviewCount} size={12} />
            ) : (
              <span className="explore-new">🌱 New mentor</span>
            )}
            {group.city && <span className="explore-dot">·</span>}
            {group.city && <span>{group.city}</span>}
          </div>
        </div>
        <div className="explore-price is-free">
          <strong>{exchangeLabel}</strong>
          <span>{exchangeNote}</span>
        </div>
      </div>

      <div className="explore-badges">
        {topRated && <span className="explore-badge badge-gold">⭐ Top rated</span>}
        {group.hasFree && <span className="explore-badge badge-free">🎁 Free to learn</span>}
        {group.branch && <span className="explore-badge">{group.branch}</span>}
        {joined && <span className="explore-badge badge-quiet">Joined {joined}</span>}
      </div>

      <p className="explore-bio">{truncate(group.bio || fallbackBio(group), 155)}</p>

      <div className="explore-skill-list" aria-label={`${group.name} teaches`}>
        {visibleSkills.map((skill) => {
          const meta = categoryMeta(skill.skillCategory);
          return (
            <span className="explore-skill" key={`${group.id}-${skill.skillId}`}>
              <span aria-hidden="true">{skill.skillIcon || meta.icon}</span>
              {skill.skillName}
              <em>Swap</em>
            </span>
          );
        })}
        {remaining > 0 && <span className="explore-skill more">+{remaining} more</span>}
      </div>

      <div className="explore-card-meta">
        <span>
          🎓 {primary ? `${primary.skillName} · ${primary.proficiency?.toLowerCase() || 'all levels'}` : 'Student teacher'}
        </span>
        {group.helpfulPoints > 0 && <span>🏅 {group.helpfulPoints} helpful points</span>}
      </div>

      <div className="explore-card-actions">
        <Link to={`/profile/${group.id}`} className="explore-profile-link">
          View profile <span aria-hidden="true">→</span>
        </Link>
        <button
          type="button"
          className="btn btn-primary explore-connect"
          onClick={() => onConnect(group)}
        >
          {isAuthenticated ? 'Choose learning option' : 'Join to connect'}
          <span aria-hidden="true">→</span>
        </button>
      </div>
    </article>
  );
}

function SpotlightCard({ group, badge, onConnect, isAuthenticated }) {
  const skill = group.skills[0];
  return (
    <article className="explore-spotlight-card">
      <div className="explore-spotlight-head">
        <Avatar name={group.name} src={group.avatarUrl} size={42} />
        <div className="explore-spotlight-who">
          <Link to={`/profile/${group.id}`}>{group.name}</Link>
          <span>{skill?.skillName || 'SkillSwap mentor'}</span>
        </div>
        <span className="explore-spotlight-badge">{badge}</span>
      </div>
      <p>{truncate(group.bio || fallbackBio(group), 105)}</p>
      <div className="explore-spotlight-foot">
        <span className="spotlight-free">
          Free learning
        </span>
        <button type="button" className="explore-text-btn" onClick={() => onConnect(group)}>
          {isAuthenticated ? 'Start learning' : 'Join to connect'} <span aria-hidden="true">→</span>
        </button>
      </div>
    </article>
  );
}

function ExploreSkeleton() {
  return (
    <div className="explore-grid" aria-label="Loading mentors">
      {Array.from({ length: 6 }).map((_, index) => (
        <div className="explore-card explore-skeleton" key={index}>
          <div className="skel skel-avatar" />
          <div className="skel skel-line wide" />
          <div className="skel skel-line medium" />
          <div className="skel skel-line short" />
          <div className="skel skel-pills" />
          <div className="skel skel-line wide" />
        </div>
      ))}
    </div>
  );
}

export default function ExplorePage() {
  const { user, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [teachers, setTeachers] = useState([]);
  const [mySkills, setMySkills] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeCategory, setActiveCategory] = useState('ALL');
  const [freeOnly, setFreeOnly] = useState(false);
  const [sortBy, setSortBy] = useState('recommended');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTeacher, setActiveTeacher] = useState(null);
  const resultsRef = useRef(null);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await searchTeachers();
      setTeachers(data || []);
    } catch (err) {
      setError(err?.message || 'Could not load students right now.');
      setTeachers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTeachers();
  }, [loadTeachers]);

  useEffect(() => {
    if (!isAuthenticated) {
      setMySkills([]);
      return;
    }
    getMySkills()
      .then((data) => setMySkills(data || []))
      .catch(() => setMySkills([]));
  }, [isAuthenticated]);

  const teacherGroups = useMemo(() => {
    const groups = new Map();

    teachers.forEach((row) => {
      if (!row?.teacherId) return;
      let group = groups.get(row.teacherId);
      if (!group) {
        group = {
          id: row.teacherId,
          teacherId: row.teacherId,
          name: row.teacherName || 'SkillSwap member',
          teacherName: row.teacherName || 'SkillSwap member',
          college: row.teacherCollege || '',
          branch: row.teacherBranch || '',
          year: row.teacherYear || '',
          city: row.teacherCity || '',
          bio: row.teacherBio || '',
          avatarUrl: row.teacherAvatarUrl || null,
          helpfulPoints: row.teacherHelpfulPoints || 0,
          learningMode: row.teacherLearningMode || 'Both',
          joinedAt: row.teacherJoinedAt || null,
          avgRating: row.avgRating || 0,
          reviewCount: row.reviewCount || 0,
          skills: [],
          skillIds: new Set(),
          categories: new Set(),
          minPrice: null,
          hasFree: false,
        };
        groups.set(row.teacherId, group);
      }

      if (!group.skillIds.has(row.skillId)) {
        group.skillIds.add(row.skillId);
        group.skills.push({
          id: row.id,
          skillId: row.skillId,
          skillName: row.skillName,
          skillIcon: row.skillIcon,
          skillCategory: row.skillCategory,
          proficiency: row.proficiency,
        });

        if (row.skillCategory) group.categories.add(row.skillCategory);
        group.hasFree = true; // all exchanges are free barter
      }

      group.bio = group.bio || row.teacherBio || '';
      group.city = group.city || row.teacherCity || '';
      group.avatarUrl = group.avatarUrl || row.teacherAvatarUrl || null;
      group.helpfulPoints = Math.max(group.helpfulPoints, row.teacherHelpfulPoints || 0);
      group.avgRating = Math.max(group.avgRating, row.avgRating || 0);
      group.reviewCount = Math.max(group.reviewCount, row.reviewCount || 0);
      group.joinedAt = group.joinedAt || row.teacherJoinedAt || null;
    });

    return [...groups.values()].map((group) => {
      const skills = group.skills.sort(
        (a, b) => a.skillName.localeCompare(b.skillName)
      );
      return {
        ...group,
        skills,
        skillCount: skills.length,
        categories: [...group.categories],
        searchText: [
          group.name,
          group.college,
          group.branch,
          group.year,
          group.city,
          group.bio,
          ...skills.flatMap((skill) => [skill.skillName, skill.skillCategory, skill.proficiency]),
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase(),
      };
    });
  }, [teachers]);

  const visibleGroups = useMemo(
    () => teacherGroups.filter((group) => group.id !== user?.id),
    [teacherGroups, user]
  );

  const categories = useMemo(() => {
    const counts = new Map();
    visibleGroups.forEach((group) => {
      group.categories.forEach((category) => {
        counts.set(category, (counts.get(category) || 0) + 1);
      });
    });
    return [...counts.entries()]
      .map(([name, count]) => ({ name, count, ...categoryMeta(name) }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [visibleGroups]);

  const popularSkills = useMemo(() => {
    const counts = new Map();
    visibleGroups.forEach((group) => {
      group.skills.forEach((skill) => {
        if (!skill.skillName) return;
        const current = counts.get(skill.skillName) || {
          name: skill.skillName,
          icon: skill.skillIcon || categoryMeta(skill.skillCategory).icon,
          count: 0,
        };
        current.count += 1;
        counts.set(skill.skillName, current);
      });
    });
    return [...counts.values()].sort((a, b) => b.count - a.count).slice(0, 6);
  }, [visibleGroups]);

  const stats = useMemo(() => {
    const skillNames = new Set(
      visibleGroups.flatMap((group) => group.skills.map((skill) => skill.skillName))
    );
    const rated = visibleGroups.filter((group) => group.reviewCount > 0 && group.avgRating > 0);
    return {
      teachers: visibleGroups.length,
      skills: skillNames.size,
      free: visibleGroups.filter((group) => group.hasFree).length,
      rating: rated.length
        ? (rated.reduce((sum, group) => sum + group.avgRating, 0) / rated.length).toFixed(1)
        : null,
    };
  }, [visibleGroups]);

  const topRated = useMemo(
    () =>
      [...visibleGroups]
        .filter((group) => group.reviewCount > 0)
        .sort(
          (a, b) =>
            b.avgRating - a.avgRating || b.reviewCount - a.reviewCount || recommendedScore(b) - recommendedScore(a)
        )
        .slice(0, 3),
    [visibleGroups]
  );

  const freeTeachers = useMemo(
    () =>
      [...visibleGroups]
        .filter((group) => group.hasFree)
        .sort((a, b) => b.avgRating - a.avgRating || b.helpfulPoints - a.helpfulPoints)
        .slice(0, 3),
    [visibleGroups]
  );

  const filteredGroups = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    let list = visibleGroups;

    if (query) {
      list = list.filter((group) => group.searchText.includes(query));
    }
    if (activeCategory !== 'ALL') {
      list = list.filter((group) => group.categories.includes(activeCategory));
    }
    if (freeOnly) {
      list = list.filter((group) => group.hasFree);
    }

    return [...list].sort((a, b) => {
      if (sortBy === 'rating') {
        return b.avgRating - a.avgRating || b.reviewCount - a.reviewCount;
      }
      if (sortBy === 'newest') {
        const ta = a.joinedAt ? new Date(a.joinedAt).getTime() : 0;
        const tb = b.joinedAt ? new Date(b.joinedAt).getTime() : 0;
        return tb - ta;
      }
      return recommendedScore(b) - recommendedScore(a);
    });
  }, [visibleGroups, searchTerm, activeCategory, freeOnly, sortBy]);

  const hasActiveFilters = Boolean(searchTerm.trim() || activeCategory !== 'ALL' || freeOnly);
  const featuredTeacher = topRated[0] || visibleGroups[0] || null;
  const displayedGroups = filteredGroups.slice(0, visibleCount);

  useEffect(() => {
    setVisibleCount(PAGE_SIZE);
  }, [searchTerm, activeCategory, freeOnly, sortBy]);

  const scrollToResults = () => {
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
    resultsRef.current?.scrollIntoView({
      behavior: reduceMotion ? 'auto' : 'smooth',
      block: 'start',
    });
  };

  const selectCategory = (category) => {
    setActiveCategory(category);
    window.setTimeout(scrollToResults, 60);
  };

  const selectSkill = (skill) => {
    setSearchTerm(skill);
    setActiveCategory('ALL');
    window.setTimeout(scrollToResults, 60);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setActiveCategory('ALL');
    setFreeOnly(false);
    setSortBy('recommended');
  };

  const handleConnect = (group) => {
    if (!isAuthenticated) {
      showToast('Create a free account to request a session.');
      navigate('/login', { state: { from: '/explore' } });
      return;
    }
    setActiveTeacher(group);
  };

  return (
    <main className="page explore-page">
      <div className="container">
        <section className="explore-hero explore-reveal">
          <div className="explore-hero-glow glow-a" aria-hidden="true" />
          <div className="explore-hero-glow glow-b" aria-hidden="true" />
          <div className="explore-hero-grid" aria-hidden="true" />

          <div className="explore-hero-copy">
            <div className="explore-eyebrow">
              <span className="explore-eyebrow-dot" aria-hidden="true" />
              Student-powered learning
            </div>
            <h1>
              Learn something new from someone who{' '}
              <span className="explore-gradient-text">just figured it out.</span>
            </h1>
            <p className="explore-hero-sub">
              Swap a skill you know for one you want to learn — completely free, no subscriptions.
              Real students, real projects, no long courses.
            </p>

            <form
              className="explore-search"
              onSubmit={(event) => {
                event.preventDefault();
                scrollToResults();
              }}
            >
              <span className="explore-search-icon" aria-hidden="true">
                ⌕
              </span>
              <input
                id="skillSearch"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="What do you want to learn?"
                aria-label="Search skills or mentors"
              />
              {searchTerm && (
                <button
                  type="button"
                  className="explore-search-clear"
                  onClick={() => setSearchTerm('')}
                  aria-label="Clear search"
                >
                  ×
                </button>
              )}
              <button type="submit" className="btn btn-primary explore-search-btn">
                Find a mentor <span aria-hidden="true">→</span>
              </button>
            </form>

            {popularSkills.length > 0 && (
              <div className="explore-quick-row">
                <span className="explore-quick-label">Popular now</span>
                {popularSkills.slice(0, 5).map((skill) => (
                  <button
                    type="button"
                    className={`explore-chip ${searchTerm === skill.name ? 'active' : ''}`}
                    key={skill.name}
                    onClick={() => selectSkill(skill.name)}
                  >
                    <span aria-hidden="true">{skill.icon}</span> {skill.name}
                  </button>
                ))}
              </div>
            )}

            <div className="explore-hero-actions">
              {isAuthenticated ? (
                <>
                  <Link to="/requests" className="btn btn-outline">
                    My requests
                  </Link>
                  <Link to="/dashboard" className="btn btn-ghost">
                    Add a skill to teach <span aria-hidden="true">→</span>
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/register" className="btn btn-primary">
                    Join free
                  </Link>
                  <Link to="/qa" className="btn btn-outline">
                    Browse free Q&A
                  </Link>
                </>
              )}
            </div>
          </div>

          <div className="explore-hero-art" aria-hidden="true">
            <div className="explore-halo halo-one" />
            <div className="explore-halo halo-two" />
            <div className="explore-float-orbit orbit-one" />
            <div className="explore-float-orbit orbit-two" />

            {featuredTeacher ? (
              <div className="explore-float-card">
                <div className="explore-float-top">
                  <Avatar name={featuredTeacher.name} src={featuredTeacher.avatarUrl} size={48} />
                  <div className="explore-float-who">
                    <strong>{featuredTeacher.name}</strong>
                    <span>{featuredTeacher.skills[0]?.skillName || 'SkillSwap mentor'}</span>
                  </div>
                  <span className="explore-float-live">LIVE</span>
                </div>
                <div className="explore-float-meta">
                  <span>
                    ⭐{' '}
                    {featuredTeacher.reviewCount > 0
                      ? featuredTeacher.avgRating.toFixed(1)
                      : 'New mentor'}
                  </span>
                  <span>
                    🎁 Free skill swap
                  </span>
                </div>
                <div className="explore-float-progress">
                  <span style={{ width: '78%' }} />
                </div>
                <div className="explore-float-foot">
                  <span>Open to a first session</span>
                  <span>→</span>
                </div>
              </div>
            ) : (
              <div className="explore-float-card explore-float-empty">
                <div className="explore-float-top">
                  <div className="explore-float-avatar">✦</div>
                  <div className="explore-float-who">
                    <strong>Your name here</strong>
                    <span>Teach what you love</span>
                  </div>
                </div>
                <div className="explore-float-meta">
                  <span>🌱 Be the first</span>
                </div>
              </div>
            )}

            <div className="explore-float-pill pill-a">🎥 In-app video rooms</div>
            <div className="explore-float-pill pill-b">🔄 Free skill swaps</div>
            <div className="explore-float-pill pill-c">⭐ Real student reviews</div>
          </div>
        </section>

        {!loading && stats.teachers > 0 && (
          <div className="explore-trust explore-reveal">
            <div className="explore-trust-item">
              <strong>{stats.teachers}</strong>
              <span>students teaching</span>
            </div>
            <div className="explore-trust-item">
              <strong>{stats.skills}</strong>
              <span>skills to explore</span>
            </div>
            <div className="explore-trust-item">
              <strong>{stats.teachers}</strong>
              <span>open to skill swaps</span>
            </div>
            <div className="explore-trust-item">
              <strong>{stats.rating || '—'}</strong>
              <span>{stats.rating ? 'average rating' : 'ratings coming soon'}</span>
            </div>
          </div>
        )}

        <section className="explore-how explore-reveal">
          <div className="explore-section-head">
            <div>
              <span className="explore-kicker">How it works</span>
              <h2>From “I wish I knew this” to your first session</h2>
            </div>
          </div>
          <div className="explore-steps">
            {HOW_IT_WORKS.map((step, index) => (
              <div className="explore-step" key={step.title}>
                <span className="explore-step-number">0{index + 1}</span>
                <div className="explore-step-icon" aria-hidden="true">
                  {step.icon}
                </div>
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="explore-categories explore-reveal">
          <div className="explore-section-head">
            <div>
              <span className="explore-kicker">Browse by interest</span>
              <h2>Find your next rabbit hole</h2>
            </div>
            <button type="button" className="explore-text-btn" onClick={() => selectCategory('ALL')}>
              View all <span aria-hidden="true">→</span>
            </button>
          </div>
          <div className="explore-category-grid">
            <button
              type="button"
              className={`explore-category-tile explore-cat-all ${activeCategory === 'ALL' ? 'active' : ''}`}
              onClick={() => selectCategory('ALL')}
            >
              <span className="explore-category-icon" aria-hidden="true">
                ✦
              </span>
              <span className="explore-category-name">All skills</span>
              <span className="explore-category-count">{visibleGroups.length} mentors</span>
              <span className="explore-category-arrow" aria-hidden="true">
                ↗
              </span>
            </button>
            {categories.map((category) => (
              <button
                type="button"
                key={category.name}
                className={`explore-category-tile explore-cat-${category.tone} ${
                  activeCategory === category.name ? 'active' : ''
                }`}
                onClick={() => selectCategory(category.name)}
              >
                <span className="explore-category-icon" aria-hidden="true">
                  {category.icon}
                </span>
                <span className="explore-category-name">{category.name}</span>
                <span className="explore-category-count">
                  {category.count} {category.count === 1 ? 'mentor' : 'mentors'}
                </span>
                <span className="explore-category-arrow" aria-hidden="true">
                  ↗
                </span>
              </button>
            ))}
          </div>
        </section>

        {topRated.length > 0 && (
          <section className="explore-spotlight explore-reveal">
            <div className="explore-section-head">
              <div>
                <span className="explore-kicker">Community favourites</span>
                <h2>Top-rated this week</h2>
              </div>
              <span className="explore-section-note">Based on real student reviews</span>
            </div>
            <div className="explore-spotlight-row">
              {topRated.map((group) => (
                <SpotlightCard
                  key={group.id}
                  group={group}
                  badge="⭐ Top rated"
                  onConnect={handleConnect}
                  isAuthenticated={isAuthenticated}
                />
              ))}
            </div>
          </section>
        )}

        {freeTeachers.length > 0 && (
          <section className="explore-spotlight explore-reveal">
            <div className="explore-section-head">
              <div>
                <span className="explore-kicker">Pure skill exchange</span>
                <h2>Most active mentors</h2>
              </div>
              <span className="explore-section-note">Ready to swap — start with a conversation</span>
            </div>
            <div className="explore-spotlight-row">
              {freeTeachers.map((group) => (
                <SpotlightCard
                  key={group.id}
                  group={group}
                  badge="🎁 Free"
                  onConnect={handleConnect}
                  isAuthenticated={isAuthenticated}
                />
              ))}
            </div>
          </section>
        )}

        <section className="explore-results explore-reveal" ref={resultsRef}>
          <div className="explore-section-head explore-results-head">
            <div>
              <span className="explore-kicker">The people behind the skills</span>
              <h2>{hasActiveFilters ? 'Your matches' : 'Meet your next mentor'}</h2>
              <p className="muted">
                Every profile shows what they teach, how they teach, and how to start.
              </p>
            </div>
            <span className="explore-result-count">
              {filteredGroups.length} {filteredGroups.length === 1 ? 'mentor' : 'mentors'}
            </span>
          </div>

          <div className="explore-filterbar">
            <label className="explore-sort">
              <span>Sort by</span>
              <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
                {SORT_OPTIONS.map((option) => (
                  <option value={option.value} key={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            {hasActiveFilters && (
              <button type="button" className="explore-clear" onClick={clearFilters}>
                Clear filters <span aria-hidden="true">×</span>
              </button>
            )}
          </div>

          {loading ? (
            <ExploreSkeleton />
          ) : error ? (
            <div className="explore-empty explore-reveal">
              <div className="explore-empty-icon">🫥</div>
              <h3>We lost the thread</h3>
              <p className="muted">{error}</p>
              <button type="button" className="btn btn-outline" onClick={loadTeachers}>
                Try again
              </button>
            </div>
          ) : filteredGroups.length === 0 ? (
            <div className="explore-empty explore-reveal">
              <div className="explore-empty-icon">🌱</div>
              <h3>{hasActiveFilters ? 'No matches yet' : 'No skills shared yet'}</h3>
              <p className="muted">
                {hasActiveFilters
                  ? 'Try a different skill, category, or clear the filters. Someone may be one search away.'
                  : 'Be the first to add a skill you can teach — you will show up here.'}
              </p>
              {hasActiveFilters ? (
                <button type="button" className="btn btn-outline" onClick={clearFilters}>
                  Clear filters
                </button>
              ) : (
                <Link to={isAuthenticated ? '/dashboard' : '/register'} className="btn btn-primary">
                  {isAuthenticated ? 'Add a skill' : 'Create your profile'}
                </Link>
              )}
            </div>
          ) : (
            <>
              <div className="explore-grid">
                {displayedGroups.map((group, index) => (
                  <TeacherCard
                    key={group.id}
                    group={group}
                    index={index}
                    onConnect={handleConnect}
                    isAuthenticated={isAuthenticated}
                  />
                ))}
              </div>
              {filteredGroups.length > displayedGroups.length && (
                <div className="explore-more">
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => setVisibleCount((count) => count + PAGE_SIZE)}
                  >
                    Show{' '}
                    {Math.min(PAGE_SIZE, filteredGroups.length - displayedGroups.length)} more
                    mentors <span aria-hidden="true">↓</span>
                  </button>
                  <span className="muted">
                    Showing {displayedGroups.length} of {filteredGroups.length}
                  </span>
                </div>
              )}
            </>
          )}
        </section>

        <section className="explore-cta-band explore-reveal">
          <div className="explore-cta-glow" aria-hidden="true" />
          <div>
            <span className="explore-kicker">Your turn</span>
            <h2>Know something worth sharing?</h2>
            <p>
                  Add a skill you can share and become someone’s next learning partner.
            </p>
          </div>
          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="btn btn-primary explore-cta-btn"
          >
            {isAuthenticated ? 'Add a skill' : 'Join free'} <span aria-hidden="true">→</span>
          </Link>
        </section>
      </div>

      <RequestModal
        open={Boolean(activeTeacher)}
        onClose={() => setActiveTeacher(null)}
        teacher={activeTeacher}
        mySkills={mySkills}
        onCreated={(_data, mode) =>
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
