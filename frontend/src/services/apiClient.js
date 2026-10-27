import axios from 'axios';

// Centralized Axios API client
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

/**
 * Turns a backend asset path such as `/api/files/avatars/1-abcd.png` into a URL
 * the browser can load.
 *
 * In development (no VITE_API_BASE_URL) the path stays relative so Vite's
 * `/api` proxy serves it from the same origin. When an absolute API base is
 * configured, the path is prefixed with that origin.
 */
export function assetUrl(path) {
  if (!path) return null;
  if (/^(https?:|data:|blob:)/i.test(path)) return path;

  const configured = import.meta.env.VITE_API_BASE_URL;
  if (!configured) return path;

  const origin = configured.replace(/\/api\/?$/, '');
  return `${origin}${path.startsWith('/') ? '' : '/'}${path}`;
}

// Request interceptor: Inject JWT token if available
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('skillswap_access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Standardize errors and handle token expiration
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status || 500;

    // Token expired / invalid — let AuthContext clear the session.
    if (status === 401) {
      localStorage.removeItem('skillswap_access_token');
      localStorage.removeItem('skillswapUser');
      window.dispatchEvent(new CustomEvent('skillswap:unauthorized'));
    }

    const rawMessage = error.response?.data?.message;
    const errorPayload = {
      status,
      message:
        rawMessage ||
        (status === 401
          ? 'Your session has expired. Please log in again.'
          : error.message || 'An unexpected error occurred'),
      details: error.response?.data?.data || null,
    };
    return Promise.reject(errorPayload);
  }
);

export default apiClient;
