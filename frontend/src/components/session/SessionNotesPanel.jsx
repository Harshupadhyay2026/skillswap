import { useEffect, useState } from 'react';
import sessionService from '../../services/sessionService';
import { useToast } from '../../context/ToastContext';
import Avatar from '../ui/Avatar';

const EMPTY_NOTE = { title: '', content: '', nextStep: '' };

function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/**
 * Shared notes for one swap or learning session.
 *
 * The component owns its data so the video room page only has to decide
 * whether notes are unlocked (accepted or completed).
 */
export default function SessionNotesPanel({
  sessionType,
  sessionId,
  currentUserId,
  enabled,
}) {
  const { showToast } = useToast();

  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState(EMPTY_NOTE);
  const [busy, setBusy] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_NOTE);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  const loadNotes = async () => {
    if (!enabled) return;
    setLoading(true);
    setError('');
    try {
      const res = await sessionService.getNotes(sessionType, sessionId);
      setNotes(res?.data || []);
    } catch (err) {
      setError(err?.message || 'Could not load session notes.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!enabled) return;
    loadNotes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, sessionType, sessionId]);

  if (!enabled) return null;

  const handleCreate = async (event) => {
    event.preventDefault();
    if (!form.content.trim()) {
      showToast('Write a note before saving it.');
      return;
    }

    setBusy('create');
    try {
      const res = await sessionService.addNote(sessionType, sessionId, {
        title: form.title.trim() || undefined,
        content: form.content.trim(),
        nextStep: form.nextStep.trim() || undefined,
      });
      const created = res?.data;
      if (created) setNotes((prev) => [...prev, created]);
      setForm(EMPTY_NOTE);
      showToast('Session note added.');
    } catch (err) {
      showToast(err?.message || 'Could not save that note.');
    } finally {
      setBusy('');
    }
  };

  const startEditing = (note) => {
    setConfirmDeleteId(null);
    setEditingId(note.id);
    setEditForm({
      title: note.title || '',
      content: note.content || '',
      nextStep: note.nextStep || '',
    });
  };

  const handleUpdate = async (noteId) => {
    if (!editForm.content.trim()) {
      showToast('Write a note before saving it.');
      return;
    }

    setBusy(`edit-${noteId}`);
    try {
      const res = await sessionService.updateNote(sessionType, sessionId, noteId, {
        title: editForm.title.trim() || undefined,
        content: editForm.content.trim(),
        nextStep: editForm.nextStep.trim() || undefined,
      });
      const updated = res?.data;
      if (updated) {
        setNotes((prev) => prev.map((note) => (note.id === noteId ? updated : note)));
      }
      setEditingId(null);
      setEditForm(EMPTY_NOTE);
      showToast('Session note updated.');
    } catch (err) {
      showToast(err?.message || 'Could not update that note.');
    } finally {
      setBusy('');
    }
  };

  const handleDelete = async (noteId) => {
    setBusy(`delete-${noteId}`);
    try {
      await sessionService.deleteNote(sessionType, sessionId, noteId);
      setNotes((prev) => prev.filter((note) => note.id !== noteId));
      setConfirmDeleteId(null);
      showToast('Session note deleted.');
    } catch (err) {
      showToast(err?.message || 'Could not delete that note.');
    } finally {
      setBusy('');
    }
  };

  return (
    <section className="session-notes card" aria-labelledby="session-notes-title">
      <div className="session-notes-head">
        <div>
          <span className="tag">📝 Shared with your partner</span>
          <h2 id="session-notes-title">Session notes</h2>
          <p className="muted">
            Capture what you covered, useful examples, and the next small step. Both of you can read
            these notes after the room closes.
          </p>
        </div>
        <span className="session-notes-count">
          {notes.length} {notes.length === 1 ? 'note' : 'notes'}
        </span>
      </div>

      <form className="session-note-form" onSubmit={handleCreate}>
        <div className="session-note-form-row">
          <div className="field" style={{ margin: 0 }}>
            <label htmlFor="session-note-title">Title (optional)</label>
            <input
              id="session-note-title"
              maxLength={120}
              placeholder="e.g. Week 1 — Python basics"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label htmlFor="session-note-next">Next step (optional)</label>
            <input
              id="session-note-next"
              maxLength={500}
              placeholder="e.g. Practice list comprehensions"
              value={form.nextStep}
              onChange={(event) => setForm({ ...form, nextStep: event.target.value })}
            />
          </div>
        </div>

        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="session-note-content">What did you cover?</label>
          <textarea
            id="session-note-content"
            rows={4}
            maxLength={2000}
            placeholder="Key ideas, questions, examples, or what to revisit next time…"
            value={form.content}
            onChange={(event) => setForm({ ...form, content: event.target.value })}
          />
        </div>

        <div className="session-note-form-foot">
          <span className="muted">{form.content.length}/2000</span>
          <button
            type="submit"
            className="btn btn-primary"
            style={{ padding: '9px 16px', fontSize: '13px' }}
            disabled={busy === 'create' || !form.content.trim()}
          >
            {busy === 'create' ? 'Saving…' : '＋ Add note'}
          </button>
        </div>
      </form>

      {error && (
        <div className="session-notes-error">
          <span>{error}</span>
          <button type="button" className="btn btn-light" onClick={loadNotes}>
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <p className="muted session-notes-loading">Loading notes…</p>
      ) : notes.length === 0 ? (
        <div className="session-notes-empty">
          <div aria-hidden="true">🗒️</div>
          <strong>No notes yet</strong>
          <p className="muted">Start with one sentence — future you will thank you.</p>
        </div>
      ) : (
        <div className="session-note-list">
          {notes.map((note) => {
            const isAuthor = note.author?.id === currentUserId;
            const editing = editingId === note.id;

            return (
              <article className="session-note" key={note.id}>
                <div className="session-note-head">
                  <Avatar name={note.author?.fullName} src={note.author?.avatarUrl} size={32} />
                  <div className="session-note-meta">
                    <strong>{note.author?.fullName || 'Student'}</strong>
                    <span>
                      {formatDateTime(note.createdAt)}
                      {note.edited ? ' • edited' : ''}
                    </span>
                  </div>
                </div>

                {editing ? (
                  <div className="session-note-edit">
                    <div className="session-note-form-row">
                      <div className="field" style={{ margin: 0 }}>
                        <label htmlFor={`note-title-${note.id}`}>Title</label>
                        <input
                          id={`note-title-${note.id}`}
                          maxLength={120}
                          value={editForm.title}
                          onChange={(event) =>
                            setEditForm({ ...editForm, title: event.target.value })
                          }
                        />
                      </div>
                      <div className="field" style={{ margin: 0 }}>
                        <label htmlFor={`note-next-${note.id}`}>Next step</label>
                        <input
                          id={`note-next-${note.id}`}
                          maxLength={500}
                          value={editForm.nextStep}
                          onChange={(event) =>
                            setEditForm({ ...editForm, nextStep: event.target.value })
                          }
                        />
                      </div>
                    </div>
                    <div className="field" style={{ margin: 0 }}>
                      <label htmlFor={`note-content-${note.id}`}>Note</label>
                      <textarea
                        id={`note-content-${note.id}`}
                        rows={4}
                        maxLength={2000}
                        value={editForm.content}
                        onChange={(event) =>
                          setEditForm({ ...editForm, content: event.target.value })
                        }
                      />
                    </div>
                    <div className="session-note-form-foot">
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ padding: '8px 13px', fontSize: '12.5px' }}
                        onClick={() => {
                          setEditingId(null);
                          setEditForm(EMPTY_NOTE);
                        }}
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        className="btn btn-primary"
                        style={{ padding: '8px 13px', fontSize: '12.5px' }}
                        disabled={busy === `edit-${note.id}` || !editForm.content.trim()}
                        onClick={() => handleUpdate(note.id)}
                      >
                        {busy === `edit-${note.id}` ? 'Saving…' : 'Save changes'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <h3>{note.title}</h3>
                    <p className="session-note-content">{note.content}</p>
                    {note.nextStep && (
                      <div className="session-note-next">
                        <span>Next step</span>
                        <p>{note.nextStep}</p>
                      </div>
                    )}

                    {isAuthor && (
                      <div className="session-note-foot">
                        {confirmDeleteId === note.id ? (
                          <>
                            <span className="muted">Delete this note?</span>
                            <button
                              type="button"
                              className="btn btn-outline"
                              style={{ padding: '7px 12px', fontSize: '12px' }}
                              onClick={() => setConfirmDeleteId(null)}
                            >
                              Keep it
                            </button>
                            <button
                              type="button"
                              className="btn btn-primary"
                              style={{ padding: '7px 12px', fontSize: '12px' }}
                              disabled={busy === `delete-${note.id}`}
                              onClick={() => handleDelete(note.id)}
                            >
                              {busy === `delete-${note.id}` ? 'Deleting…' : 'Delete'}
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              type="button"
                              className="session-note-link"
                              onClick={() => startEditing(note)}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="session-note-link danger"
                              onClick={() => setConfirmDeleteId(note.id)}
                            >
                              Delete
                            </button>
                          </>
                        )}
                      </div>
                    )}
                  </>
                )}
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
