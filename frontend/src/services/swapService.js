import apiClient from './apiClient';

/**
 * Service for Peer-to-Peer (P2P) Skill Swap engine
 */
export const swapService = {
  /**
   * Propose a skill barter exchange with another user
   * @param {Object} payload - { receiverId, offeredSkillId, requestedSkillId, message, scheduledAt }
   */
  async proposeSwap(payload) {
    return apiClient.post('/swaps', payload);
  },

  /**
   * Get incoming swap proposals waiting for my response
   */
  async getIncomingSwaps() {
    return apiClient.get('/swaps/incoming');
  },

  /**
   * Get outgoing swap proposals I sent to other users
   */
  async getOutgoingSwaps() {
    return apiClient.get('/swaps/outgoing');
  },

  /**
   * Get all swaps (both incoming and outgoing)
   */
  async getAllMySwaps() {
    return apiClient.get('/swaps/all');
  },

  /**
   * Get one swap by id (participants only) — used by the video-room page.
   * @param {number|string} swapId
   */
  async getSwap(swapId) {
    return apiClient.get(`/swaps/${swapId}`);
  },

  /**
   * Accept an incoming swap proposal (generates Jitsi video room)
   * @param {number|string} swapId
   */
  async acceptSwap(swapId) {
    return apiClient.put(`/swaps/${swapId}/accept`);
  },

  /**
   * Decline an incoming swap proposal
   * @param {number|string} swapId
   */
  async declineSwap(swapId) {
    return apiClient.put(`/swaps/${swapId}/decline`);
  },

  /**
   * Mark an accepted swap session as completed
   * @param {number|string} swapId
   */
  async completeSwap(swapId) {
    return apiClient.put(`/swaps/${swapId}/complete`);
  },

  /**
   * Cancel a pending outgoing proposal
   * @param {number|string} swapId
   */
  async cancelSwap(swapId) {
    return apiClient.put(`/swaps/${swapId}/cancel`);
  },
};

export default swapService;
