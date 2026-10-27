import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const INITIAL = {
  name: '',
  username: '',
  email: '',
  password: '',
  college: '',
  branch: '',
  year: '1st Year',
  city: '',
  bio: '',
  mode: 'Both',
};

export default function RegisterPage() {
  const { register } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [form, setForm] = useState(INITIAL);
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});

    const { name, college, email, password } = form;
    if (!name.trim() || !college.trim() || !email.trim() || !password) {
      setError('Please fill in all required fields marked with *.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setSubmitting(true);
    try {
      const user = await register({
        name: name.trim(),
        username: form.username.trim() || name.toLowerCase().replace(/\s+/g, '_'),
        email: email.trim(),
        password,
        college: college.trim(),
        branch: form.branch.trim() || 'Student',
        year: form.year,
        city: form.city.trim() || 'India',
        bio: form.bio.trim(),
        mode: form.mode,
      });
      showToast(`Welcome to SkillSwap, ${user.fullName}!`);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      if (err?.details && typeof err.details === 'object') {
        setFieldErrors(err.details);
      }
      setError(err?.message || 'Registration failed. Please verify your inputs.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="page auth-page">
      <div className="container">
        <div className="auth-card card" style={{ maxWidth: '640px' }}>
          <h1>Join SkillSwap</h1>
          <p className="muted" style={{ marginBottom: '22px' }}>
            Create your student profile in less than a minute.
          </p>

          {error && <div className="form-error">{error}</div>}

          <form onSubmit={handleSubmit} autoComplete="on">
            <div className="form-grid">
              <div className="field">
                <label htmlFor="reg-name">Full Name *</label>
                <input id="reg-name" placeholder="Your name" value={form.name} onChange={update('name')} />
                {fieldErrors.name && <small className="field-error">{fieldErrors.name}</small>}
              </div>

              <div className="field">
                <label htmlFor="reg-username">Username *</label>
                <input
                  id="reg-username"
                  placeholder="@username"
                  autoComplete="username"
                  value={form.username}
                  onChange={update('username')}
                />
                {fieldErrors.username && <small className="field-error">{fieldErrors.username}</small>}
              </div>

              <div className="field">
                <label htmlFor="reg-email">Email *</label>
                <input
                  id="reg-email"
                  type="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={update('email')}
                />
                {fieldErrors.email && <small className="field-error">{fieldErrors.email}</small>}
              </div>

              <div className="field">
                <label htmlFor="reg-pass">Password *</label>
                <div className="password-input-wrap">
                  <input
                    id="reg-pass"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="At least 6 characters"
                    autoComplete="new-password"
                    value={form.password}
                    onChange={update('password')}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    aria-controls="reg-pass"
                    onClick={() => setShowPassword((visible) => !visible)}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
                {fieldErrors.password && <small className="field-error">{fieldErrors.password}</small>}
              </div>

              <div className="field full">
                <label htmlFor="reg-college">College / University *</label>
                <input
                  id="reg-college"
                  placeholder="e.g. GL Bajaj Institute of Technology"
                  value={form.college}
                  onChange={update('college')}
                />
                {fieldErrors.college && <small className="field-error">{fieldErrors.college}</small>}
              </div>

              <div className="field">
                <label htmlFor="reg-branch">Course / Branch</label>
                <input id="reg-branch" placeholder="e.g. CSE-IT" value={form.branch} onChange={update('branch')} />
              </div>

              <div className="field">
                <label htmlFor="reg-year">Year of Study</label>
                <select id="reg-year" value={form.year} onChange={update('year')}>
                  <option>1st Year</option>
                  <option>2nd Year</option>
                  <option>3rd Year</option>
                  <option>4th Year</option>
                </select>
              </div>

              <div className="field">
                <label htmlFor="reg-city">City</label>
                <input id="reg-city" placeholder="Your city" value={form.city} onChange={update('city')} />
              </div>

              <div className="field">
                <label>I want to</label>
                <div className="choice">
                  {['Learn', 'Teach', 'Both'].map((mode) => (
                    <label key={mode}>
                      <input
                        type="radio"
                        name="mode"
                        checked={form.mode === mode}
                        onChange={() => setForm({ ...form, mode })}
                      />
                      <span>{mode === 'Learn' ? 'Learn skills' : mode === 'Teach' ? 'Teach skills' : 'Both'}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div className="field full">
                <label htmlFor="reg-bio">Short Bio</label>
                <textarea
                  id="reg-bio"
                  placeholder="Tell students what you like learning or teaching…"
                  value={form.bio}
                  onChange={update('bio')}
                />
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '10px' }}
              disabled={submitting}
            >
              {submitting ? 'Creating profile…' : 'Create My Profile →'}
            </button>
          </form>

          <p style={{ textAlign: 'center', fontSize: '13px', marginTop: '20px' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 800 }}>
              Log in
            </Link>
          </p>
        </div>
      </div>
    </main>
  );
}
