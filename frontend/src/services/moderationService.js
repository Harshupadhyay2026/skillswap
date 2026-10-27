import apiClient from './apiClient';

/** Moderator queue and reversible decisions. Admin role is enforced by the API. */
export const moderationService = {
  async stats() {
    const res = await apiClient.get('/admin/stats');
    return res.data;
  },

  async listReports({ status = '', targetType = '', page = 0, size = 20 } = {}) {
    const res = await apiClient.get('/admin/reports', {
      params: {
        status: status || undefined,
        targetType: targetType || undefined,
        page,
        size,
      },
    });
    return res.data;
  },

  async getReport(id) {
    const res = await apiClient.get(`/admin/reports/${id}`);
    return res.data;
  },

  async claim(id) {
    const res = await apiClient.put(`/admin/reports/${id}/claim`);
    return res.data;
  },

  async resolve(id, action, note) {
    const res = await apiClient.put(`/admin/reports/${id}/resolve`, { action, note });
    return res.data;
  },

  async restoreProblem(id, note) {
    const res = await apiClient.put(`/admin/problems/${id}/restore`, { note });
    return res;
  },

  async restoreAnswer(id, note) {
    const res = await apiClient.put(`/admin/answers/${id}/restore`, { note });
    return res;
  },

  async reactivateUser(id, note) {
    const res = await apiClient.put(`/admin/users/${id}/reactivate`, { note });
    return res;
  },
};

export default moderationService;
