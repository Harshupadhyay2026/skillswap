import apiClient from './apiClient';

/**
 * Free shared learning requests for students who want to study a skill together.
 *
 * Endpoints return the standard `{ success, message, data }` envelope, so the
 * callers here unwrap `.data` and keep `message` accessible where useful.
 */
export const learningService = {
  /**
  * Request a shared learning session for `skillId` with `teacherId`.
   * @param {{ teacherId: number, skillId: number, message?: string, scheduledAt?: string }} payload
   */
  async createRequest(payload) {
    return apiClient.post('/learning', payload);
  },

  /** Requests where I am the teacher. */
  async getIncoming() {
    return apiClient.get('/learning/incoming');
  },

  /** Requests where I am the learner. */
  async getOutgoing() {
    return apiClient.get('/learning/outgoing');
  },

  /** Both directions. */
  async getAll() {
    return apiClient.get('/learning/all');
  },

  /** One learning session by id (participants only) — used by the video-room page. */
  async get(id) {
    return apiClient.get(`/learning/${id}`);
  },

  async accept(id) {
    return apiClient.put(`/learning/${id}/accept`);
  },

  async decline(id) {
    return apiClient.put(`/learning/${id}/decline`);
  },

  async complete(id) {
    return apiClient.put(`/learning/${id}/complete`);
  },

  async cancel(id) {
    return apiClient.put(`/learning/${id}/cancel`);
  },
};

export default learningService;
