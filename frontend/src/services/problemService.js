import apiClient from './apiClient';

/**
 * Public Q&A — the free help path.
 *
 * Anyone can ask and anyone can answer. Reading is
 * public; posting requires an account (enforced by the API).
 */
export const problemService = {
  /**
   * Browse questions.
   * @param {{ query?: string, category?: string, status?: string, page?: number, size?: number }} params
   * @returns {Promise<{ items: Array, page: number, totalPages: number, hasNext: boolean }>}
   */
  async list(params = {}) {
    const res = await apiClient.get('/problems', { params });
    return res.data;
  },

  /** Questions asked by the logged-in user. */
  async mine() {
    const res = await apiClient.get('/problems/mine');
    return res.data;
  },

  /** Question detail plus every answer. */
  async get(id) {
    const res = await apiClient.get(`/problems/${id}`);
    return res.data;
  },

  /**
   * Ask a question.
   * @param {{ title: string, description: string, category: string, level?: string, skillId?: number }} payload
   */
  async create(payload) {
    return apiClient.post('/problems', payload);
  },

  /** Post an answer. */
  async answer(problemId, body) {
    return apiClient.post(`/problems/${problemId}/answers`, { body });
  },

  /** Accept an answer (author only). Marks the question solved. */
  async accept(problemId, answerId) {
    return apiClient.put(`/problems/${problemId}/answers/${answerId}/accept`);
  },

  /** Close your own question without accepting an answer. */
  async close(problemId) {
    return apiClient.put(`/problems/${problemId}/close`);
  },

  /** Reopen a question you closed. */
  async reopen(problemId) {
    return apiClient.put(`/problems/${problemId}/reopen`);
  },
};

export default problemService;
