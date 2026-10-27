import apiClient from './apiClient';

/**
 * Reporting endpoints.
 *
 * Reports are deliberately separate from the content APIs: a report is a
 * request for review, not an edit to the reported item.
 */
export const reportService = {
  /**
   * File a report against a question, an answer, or a user profile.
   * @param {{ targetType: 'PROBLEM'|'ANSWER'|'USER', targetId: number,
   *           reason: string, details?: string }} payload
   */
  async create(payload) {
    const res = await apiClient.post('/reports', payload);
    return res;
  },

  /** The signed-in member's own reports, newest first. */
  async mine() {
    const res = await apiClient.get('/reports/mine');
    return res.data;
  },
};

export default reportService;
