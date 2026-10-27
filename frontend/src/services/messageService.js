import apiClient from './apiClient';

/** One-to-one messaging between users. */
export const messageService = {
  async listConversations() {
    const res = await apiClient.get('/conversations');
    return res.data || [];
  },

  /** Get-or-create the thread with another user. */
  async startConversation(userId) {
    const res = await apiClient.post('/conversations', { userId });
    return res.data;
  },

  async getConversation(id) {
    const res = await apiClient.get(`/conversations/${id}`);
    return res.data;
  },

  /**
   * Newest page of messages, returned oldest-first.
   * @param {number} id conversation id
   * @param {{ before?: number, size?: number }} params
   */
  async getMessages(id, params = {}) {
    const res = await apiClient.get(`/conversations/${id}/messages`, { params });
    return res.data || [];
  },

  async sendMessage(id, body) {
    const res = await apiClient.post(`/conversations/${id}/messages`, { body });
    return res.data;
  },

  async markRead(id) {
    const res = await apiClient.put(`/conversations/${id}/read`);
    return res.data?.updated ?? 0;
  },

  async unreadCount() {
    const res = await apiClient.get('/conversations/unread-count');
    return res.data?.count ?? 0;
  },
};

export default messageService;
