import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export default function LoginPage() {
  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const redirectTo = location.state?.from || '/dashboard';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.usernameOrEmail.trim() || !form.password) {
      setError('Please enter your email/username and password.');
      return;
    }

    setSubmitting(true);
    try {
      const user = await login({
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
      });
      showToast(`Welcome back, ${user.fullName || user.username}!`);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err?.message || 'Invalid credentials. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="page auth-page">
      <div className="container">
        <div className="auth-card card">
          <h1>Welcome back</h1>
          <p className="muted" style={{ marginBottom: '22px' }}>
            Log in to your SkillSwap account.
          </p>

          {error && <div className="form-error">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="login-user">Email or Username</label>
              <input
                id="login-user"
                placeholder="you@example.com"
                autoComplete="username"
                value={form.usernameOrEmail}
                onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })}
              />
            </div>

            <div className="field">
              <label htmlFor="login-pass">Password</label>
              <div className="password-input-wrap">
                <input
                  id="login-pass"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <button
                  type="button"
                  className="password-toggle"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  aria-controls="login-pass"
                  onClick={() => setShowPassword((visible) => !visible)}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '8px' }}
              disabled={submitting}
            >
              {submitting ? 'Logging in…' : 'Login'}
            </button>
          </form>

          <p style={{ textAlign: 'center', fontSize: '13px', marginTop: '20px' }}>
            New here?{' '}
            <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 800 }}>
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </main>
  );
}
