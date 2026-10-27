import { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import swapService from '../../services/swapService';
import learningService from '../../services/learningService';
import systemService from '../../services/systemService';
import messageService from '../../services/messageService';
import Avatar from '../ui/Avatar';
import NotificationBell from './NotificationBell';

const THEME_KEY = 'skillswap_theme';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [backendStatus, setBackendStatus] = useState('CHECKING');
  const [pendingSwaps, setPendingSwaps] = useState(0);
  const [unreadMessages, setUnreadMessages] = useState(0);
  const [theme, setTheme] = useState(() => localStorage.getItem(THEME_KEY) || 'light');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  useEffect(() => {
    systemService
      .getHealthStatus()
      .then((res) => setBackendStatus(res?.success ? 'CONNECTED' : 'OFFLINE'))
      .catch(() => setBackendStatus('DISCONNECTED'));
  }, []);

  // Poll incoming pending requests and unread messages so badges stay fresh.
  useEffect(() => {
    if (!isAuthenticated) {
      setPendingSwaps(0);
      setUnreadMessages(0);
      return undefined;
    }

    let cancelled = false;
    const refresh = async () => {
      try {
        const [swapsRes, sessionsRes, unread] = await Promise.all([
          swapService.getIncomingSwaps().catch(() => ({ data: [] })),
          learningService.getIncoming().catch(() => ({ data: [] })),
          messageService.unreadCount().catch(() => 0),
        ]);
        if (cancelled) return;
        const swaps = (swapsRes?.data || []).filter((s) => s.status === 'PENDING').length;
        const sessions = (sessionsRes?.data || []).filter((s) => s.status === 'PENDING').length;
        setPendingSwaps(swaps + sessions);
        setUnreadMessages(unread);
      } catch {
        /* silent — the badge is non-critical */
      }
    };

    refresh();
    const interval = setInterval(refresh, 45000);
    window.addEventListener('focus', refresh);
    window.addEventListener('skillswap:messages-changed', refresh);

    return () => {
      cancelled = true;
      clearInterval(interval);
      window.removeEventListener('focus', refresh);
      window.removeEventListener('skillswap:messages-changed', refresh);
    };
  }, [isAuthenticated]);

  const handleLogout = () => {
    logout();
    showToast('Logged out successfully.');
    navigate('/');
  };

  const toggleTheme = () => {
    const next = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    showToast(`Switched to ${next === 'dark' ? 'Dark' : 'Light'} Mode`);
  };

  const navClass = ({ isActive }) => `nav-link${isActive ? ' active' : ''}`;
  const closeMenu = () => setMobileMenuOpen(false);

  return (
    <header className="navbar">
      <div className="container nav">
        <Link className="logo" to="/" onClick={closeMenu}>
          Skill<span>Swap</span>
        </Link>

        <nav className={`navlinks${mobileMenuOpen ? ' open' : ''}`}>
          <NavLink to="/explore" className={navClass} onClick={closeMenu}>
            Explore
          </NavLink>
          <NavLink to="/qa" className={navClass} onClick={closeMenu}>
            Q&amp;A
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/requests" className={navClass} onClick={closeMenu}>
              My Requests
              {pendingSwaps > 0 && <span className="nav-badge">{pendingSwaps}</span>}
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/messages" className={navClass} onClick={closeMenu}>
              Messages
              {unreadMessages > 0 && (
                <span className="nav-badge">{unreadMessages > 9 ? '9+' : unreadMessages}</span>
              )}
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/dashboard" className={navClass} onClick={closeMenu}>
              Dashboard
            </NavLink>
          )}
          {isAuthenticated && user?.roles?.includes('ROLE_ADMIN') && (
            <NavLink to="/admin/reports" className={navClass} onClick={closeMenu}>
              🛡️ Moderation
            </NavLink>
          )}
        </nav>

        <div className="nav-actions">
          <div
            className={`api-pill ${backendStatus === 'CONNECTED' ? 'connected' : 'disconnected'}`}
            title={`Backend API: ${backendStatus}`}
          >
            <span className={`dot ${backendStatus === 'CONNECTED' ? 'connected' : 'disconnected'}`} />
            {backendStatus === 'CONNECTED' ? 'API Active' : 'API Standby'}
          </div>

          {isAuthenticated && <NotificationBell />}

          <button
            className="btn-icon"
            onClick={toggleTheme}
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>

          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Link to="/dashboard" title="Go to dashboard" style={{ display: 'flex' }}>
                <Avatar name={user?.fullName} src={user?.avatarUrl} size={38} />
              </Link>
              <button
                className="btn btn-outline"
                style={{ padding: '8px 12px', fontSize: '12px' }}
                onClick={handleLogout}
              >
                Logout
              </button>
            </div>
          ) : (
            <>
              <Link className="btn btn-light" to="/login">
                Login
              </Link>
              <Link className="btn btn-primary" to="/register">
                Join SkillSwap
              </Link>
            </>
          )}

          <button
            className="menu"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle navigation menu"
          >
            ☰
          </button>
        </div>
      </div>
    </header>
  );
}
