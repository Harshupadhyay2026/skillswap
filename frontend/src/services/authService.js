import apiClient from './apiClient';

/**
 * Authentication and User Profile API service
 */
export const authService = {
  /**
   * Register a new student profile
   * @param {Object} userData
   * @returns {Promise<Object>}
   */
  async register(userData) {
    return apiClient.post('/auth/register', userData);
  },

  /**
   * Log in with username or email + password
   * @param {Object} credentials - { usernameOrEmail, password }
   * @returns {Promise<Object>}
   */
  async login(credentials) {
    return apiClient.post('/auth/login', credentials);
  },

  /**
   * Get currently authenticated user profile
   * @returns {Promise<Object>}
   */
  async getCurrentUser() {
    return apiClient.get('/users/me');
  },

  /**
   * Update authenticated user's profile
   * @param {Object} profileData
   * @returns {Promise<Object>}
   */
  async updateProfile(profileData) {
    return apiClient.put('/users/me', profileData);
  },
};

export default authService;
