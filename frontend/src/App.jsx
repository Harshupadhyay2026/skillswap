import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import Navbar from './components/layout/Navbar';
import HomePage from './pages/HomePage';
import ExplorePage from './pages/ExplorePage';
import RequestsPage from './pages/RequestsPage';
import ProblemsPage from './pages/ProblemsPage';
import ProblemDetailPage from './pages/ProblemDetailPage';
import ProfilePage from './pages/ProfilePage';
import DashboardPage from './pages/DashboardPage';
import MessagesPage from './pages/MessagesPage';
import NotificationsPage from './pages/NotificationsPage';
import CommunityPage from './pages/CommunityPage';
import VideoRoomPage from './pages/VideoRoomPage';
import AdminReportsPage from './pages/AdminReportsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

function NotFound() {
  return (
    <main className="page">
      <div className="container empty-state">
        <div style={{ fontSize: '40px', marginBottom: '12px' }}>🧭</div>
        <h2>Page not found</h2>
        <p className="muted" style={{ margin: '8px 0 18px' }}>
          That page does not exist.
        </p>
        <a href="/" className="btn btn-primary">
          Back home
        </a>
      </div>
    </main>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Navbar />
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/explore" element={<ExplorePage />} />
            <Route path="/qa" element={<ProblemsPage />} />
            <Route path="/qa/:id" element={<ProblemDetailPage />} />
            <Route path="/problems" element={<Navigate to="/qa" replace />} />
            <Route path="/requests" element={<RequestsPage />} />
            <Route path="/profile/:userId" element={<ProfilePage />} />
            <Route path="/messages" element={<MessagesPage />} />
            <Route path="/messages/:conversationId" element={<MessagesPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/community" element={<CommunityPage />} />
            <Route path="/admin/reports" element={<AdminReportsPage />} />
            <Route path="/session/swap/:id" element={<VideoRoomPage sessionType="swap" />} />
            <Route path="/session/learning/:id" element={<VideoRoomPage sessionType="learning" />} />
            <Route path="/swaps" element={<Navigate to="/requests" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/home" element={<Navigate to="/" replace />} />
            <Route path="*" element={<NotFound />} />
          </Routes>

          <footer className="footer">
            <div className="container footer-inner">
              <div>
                <strong style={{ color: 'var(--text)' }}>SkillSwap</strong> — Learn. Teach. Swap.
                Grow.
              </div>
              <div>
                <Link to="/qa">Q&amp;A</Link> •{' '}
                <Link to="/community">Community</Link> • Community Guidelines • Safety
              </div>
            </div>
          </footer>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
