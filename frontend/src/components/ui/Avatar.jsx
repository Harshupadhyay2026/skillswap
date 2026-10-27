import { assetUrl } from '../../services/apiClient';

export function initialsOf(name = '') {
  return (
    name
      .split(' ')
      .filter(Boolean)
      .map((n) => n[0])
      .slice(0, 2)
      .join('')
      .toUpperCase() || 'U'
  );
}

/**
 * Round profile image with an initials fallback.
 *
 * Uses the shared `avatar` class so sizing stays consistent across pages; the
 * image is lazy-loaded and cropped to a circle via CSS.
 */
export default function Avatar({ name = '', src, size = 48, fontSize, className = '', style }) {
  const url = assetUrl(src);

  return (
    <div
      className={`avatar ${className}`.trim()}
      style={{
        width: `${size}px`,
        height: `${size}px`,
        fontSize: `${fontSize || Math.max(10, Math.round(size * 0.34))}px`,
        ...style,
      }}
      title={name || undefined}
    >
      {url ? (
        <img src={url} alt={name ? `${name}'s profile photo` : 'Profile photo'} loading="lazy" />
      ) : (
        initialsOf(name)
      )}
    </div>
  );
}
