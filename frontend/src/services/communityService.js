import apiClient from './apiClient';

/** Public community data — no login required. */
export const communityService = {
  async getTopContributors(limit = 20) {
    const res = await apiClient.get('/community/top-contributors', { params: { limit } });
    return res.data || [];
  },
};

export default communityService;
