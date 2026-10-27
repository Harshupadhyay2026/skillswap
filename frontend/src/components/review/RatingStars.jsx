/**
 * Star rating display. `count` is optional — when 0 or missing it renders a
 * muted "No reviews yet" so a new teacher is not shown as "0.0 stars".
 */
export default function RatingStars({ average, count = 0, size = 13, showEmpty = true }) {
  if (!average || count === 0) {
    if (!showEmpty) return null;
    return (
      <span className="muted" style={{ fontSize: `${size - 1}px`, fontWeight: 600 }}>
        ☆ New
      </span>
    );
  }

  const rounded = Math.round(average);
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        fontSize: `${size}px`,
        fontWeight: 750,
        color: 'var(--text)',
      }}
      title={`${average} out of 5 from ${count} review${count === 1 ? '' : 's'}`}
    >
      <span style={{ color: '#f59e0b', letterSpacing: '-1px' }}>
        {'★'.repeat(rounded)}
        {'☆'.repeat(5 - rounded)}
      </span>
      <span>{average.toFixed(1)}</span>
      <span className="muted" style={{ fontWeight: 600 }}>
        ({count})
      </span>
    </span>
  );
}
