import apiClient from './apiClient';

/**
 * Shared session notes and the dashboard's learning-progress summary.
 *
 * `sessionType` is the same `swap` / `learning` value used by the session
 * routes, so callers can pass the route parameter through unchanged.
 */
export const sessionService = {
  async getNotes(sessionType, sessionId) {
    return apiClient.get(`/sessions/${sessionType}/${sessionId}/notes`);
  },

  async addNote(sessionType, sessionId, payload) {
    return apiClient.post(`/sessions/${sessionType}/${sessionId}/notes`, payload);
  },

  async updateNote(sessionType, sessionId, noteId, payload) {
    return apiClient.put(`/sessions/${sessionType}/${sessionId}/notes/${noteId}`, payload);
  },

  async deleteNote(sessionType, sessionId, noteId) {
    return apiClient.delete(`/sessions/${sessionType}/${sessionId}/notes/${noteId}`);
  },

  async getMyProgress() {
    return apiClient.get('/progress/me');
  },
};

export default sessionService;
