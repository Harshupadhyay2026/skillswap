import apiClient from './apiClient';

/**
 * Reviews / ratings.
 *
 * A review is anchored to a completed swap or learning session, so ratings
 * cannot be left for people you never actually met.
 */
export const reviewService = {
  /**
   * Rate a completed interaction.
   * @param {{ rating: number, comment?: string, swapId?: number, learningRequestId?: number }} payload
   */
  async createReview(payload) {
    return apiClient.post('/reviews', payload);
  },

  /** Completed swaps/sessions I have not reviewed yet. */
  async getPending() {
    return apiClient.get('/reviews/pending');
  },

  /** Public reviews written about a user. */
  async getUserReviews(userId) {
    return apiClient.get(`/reviews/user/${userId}`);
  },
};

export default reviewService;
