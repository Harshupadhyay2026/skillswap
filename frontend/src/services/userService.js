import apiClient from './apiClient';

/** Current-user profile operations (including the avatar upload). */
export const userService = {
  async getMe() {
    const res = await apiClient.get('/users/me');
    return res.data;
  },

  async updateMe(payload) {
    const res = await apiClient.put('/users/me', payload);
    return res.data;
  },

  /**
   * Uploads an image. The browser/axios adds the multipart boundary.
   * @param {File} file
   */
  async uploadAvatar(file) {
    const form = new FormData();
    form.append('file', file);
    const res = await apiClient.post('/users/me/avatar', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

  async removeAvatar() {
    const res = await apiClient.delete('/users/me/avatar');
    return res.data;
  },
};

export default userService;
