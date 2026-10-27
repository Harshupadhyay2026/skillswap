import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import ReportModal from './ReportModal';
import { useAuth } from '../../context/AuthContext';

/**
 * Small inline "Report" control used on questions, answers, and profiles.
 * Signed-out visitors are sent to login and returned to the same page.
 */
export default function ReportButton({
  targetType,
  targetId,
  targetLabel,
  label = 'Report',
  className = '',
}) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [reported, setReported] = useState(false);

  if (!targetId) return null;

  if (reported) {
    return <span className="report-done" title="Your report is awaiting review">⚑ Reported</span>;
  }

  const handleOpen = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }
    setOpen(true);
  };

  return (
    <>
      <button
        type="button"
        className={`report-trigger ${className}`.trim()}
        onClick={handleOpen}
        title="Report this to a moderator"
        aria-label={`Report ${targetType === 'USER' ? 'this profile' : 'this content'}`}
      >
        ⚑ {label}
      </button>
      <ReportModal
        open={open}
        onClose={(submitted) => {
          setOpen(false);
          if (submitted) setReported(true);
        }}
        targetType={targetType}
        targetId={targetId}
        targetLabel={targetLabel}
      />
    </>
  );
}
