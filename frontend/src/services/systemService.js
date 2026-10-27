import apiClient from './apiClient';

/**
 * System and health monitoring API service
 */
export const systemService = {
  /**
   * Check backend server health status
   * @returns {Promise<Object>}
   */
  async getHealthStatus() {
    return apiClient.get('/health');
  },
};

export default systemService;
