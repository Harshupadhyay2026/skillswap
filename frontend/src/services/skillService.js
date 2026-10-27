import apiClient from './apiClient';

// Every endpoint returns the standard `{ success, message, data }` envelope,
// so each function unwraps `.data` and returns just the payload.

// ── Skill Catalog ──────────────────────────────────────────────────────────

/**
 * Search the public skills catalog.
 * @param {string} query - partial skill name
 * @param {string} category - optional category filter
 * @returns {Promise<Array>} list of { id, name, category, icon }
 */
export async function searchSkills(query = '', category = '') {
  const params = {};
  if (query) params.query = query;
  if (category) params.category = category;
  const res = await apiClient.get('/skills', { params });
  return res.data;
}

/**
 * Get all distinct skill categories.
 * @returns {Promise<string[]>}
 */
export async function getCategories() {
  const res = await apiClient.get('/skills/categories');
  return res.data;
}

// ── Marketplace ─────────────────────────────────────────────────────────────

/**
 * Browse teachers in the marketplace.
 * @param {string} query
 * @param {number|null} skillId
 * @returns {Promise<Array>}
 */
export async function searchTeachers(query = '', skillId = null) {
  const params = {};
  if (query) params.query = query;
  if (skillId) params.skillId = skillId;
  const res = await apiClient.get('/marketplace', { params });
  return res.data;
}

/**
 * Find true mutual swap matches: users who teach `wanted` AND want to learn `offered`.
 * @param {string} offered - skill you can teach
 * @param {string} wanted - skill you want to learn
 * @returns {Promise<Array>}
 */
export async function findMutualMatches(offered, wanted) {
  const res = await apiClient.get('/marketplace/matches', {
    params: { offered, wanted },
  });
  return res.data;
}

/**
 * Public profile for any user (no email exposed).
 * @param {number} userId
 * @returns {Promise<{ user: Object, skills: Array, teachingCount: number, learningCount: number }>}
 */
export async function getPublicProfile(userId) {
  const res = await apiClient.get(`/users/${userId}/profile`);
  return res.data;
}

// ── My Skills (authenticated) ────────────────────────────────────────────────

/**
 * Get skills linked to the authenticated user.
 * @returns {Promise<Array>}
 */
export async function getMySkills() {
  const res = await apiClient.get('/users/skills');
  return res.data;
}

/**
 * Add a skill to the authenticated user's profile.
 * @param {{ skillId: number, skillType: 'TEACH'|'LEARN', proficiency: string }} payload
 * @returns {Promise<Object>}
 */
export async function addMySkill(payload) {
  const res = await apiClient.post('/users/skills', payload);
  return res.data;
}

/**
 * Remove a user-skill entry.
 * @param {number} userSkillId
 */
export async function removeMySkill(userSkillId) {
  const res = await apiClient.delete(`/users/skills/${userSkillId}`);
  return res.data;
}
