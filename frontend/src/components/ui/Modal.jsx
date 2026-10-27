import { useEffect } from 'react';

/**
 * Shared modal shell: backdrop click-to-close, Escape key, scroll lock.
 */
export default function Modal({ open, onClose, title, subtitle, children, width }) {
  useEffect(() => {
    if (!open) return undefined;

    const onKeyDown = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    document.addEventListener('keydown', onKeyDown);

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="modal-bg show"
      onClick={(e) => {
        if (e.target.classList.contains('modal-bg')) onClose?.();
      }}
    >
      <div
        className="modal"
        style={width ? { width: `min(${width}, 100%)` } : undefined}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <button className="close" onClick={onClose} aria-label="Close dialog">
          ×
        </button>
        {title && <h2>{title}</h2>}
        {subtitle && <div className="modal-sub">{subtitle}</div>}
        {children}
      </div>
    </div>
  );
}
