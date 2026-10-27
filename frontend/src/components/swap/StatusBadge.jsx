const STYLES = {
  PENDING: { label: 'Pending', color: '#b45309', bg: 'rgba(245, 158, 11, 0.14)' },
  ACCEPTED: { label: 'Accepted', color: '#15803d', bg: 'rgba(34, 197, 94, 0.14)' },
  COMPLETED: { label: 'Completed', color: '#4f46e5', bg: 'rgba(99, 91, 255, 0.14)' },
  DECLINED: { label: 'Declined', color: '#b91c1c', bg: 'rgba(239, 68, 68, 0.13)' },
  CANCELLED: { label: 'Cancelled', color: '#6b7280', bg: 'rgba(107, 114, 128, 0.15)' },
};

export default function StatusBadge({ status }) {
  const style = STYLES[status] || STYLES.PENDING;
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '4px 10px',
        borderRadius: '20px',
        fontSize: '11px',
        fontWeight: 800,
        letterSpacing: '0.02em',
        color: style.color,
        background: style.bg,
      }}
    >
      {style.label}
    </span>
  );
}
