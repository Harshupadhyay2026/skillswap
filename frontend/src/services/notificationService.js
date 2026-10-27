import apiClient from './apiClient';

/** In-app notification bell + full list. */
export const notificationService = {
  async list(page = 0, size = 20) {
    const res = await apiClient.get('/notifications', { params: { page, size } });
    return res.data;
  },

  async unreadCount() {
    const res = await apiClient.get('/notifications/unread-count');
    return res.data?.count ?? 0;
  },

  async markRead(id) {
    const res = await apiClient.put(`/notifications/${id}/read`);
    return res.data;
  },

  async markAllRead() {
    const res = await apiClient.put('/notifications/read-all');
    return res.data?.updated ?? 0;
  },

  async emailStatus() {
    const res = await apiClient.get('/notifications/email-status');
    return res.data;
  },

  async sendTestEmail() {
    const res = await apiClient.post('/notifications/test-email', null, { timeout: 20000 });
    return res.data;
  },
};

export default notificationService;
