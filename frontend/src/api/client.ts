import axios from 'axios';
import type { ApiErrorBody } from '../types';

export const TOKEN_STORAGE_KEY = 'rentacar_token';

// In dev, '/api' goes through Vite's proxy (vite.config.ts) to the local backend.
// In a production build where frontend and backend are on different hosts, set
// VITE_API_BASE_URL (e.g. "https://api.example.com/api") at build time.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
    return Promise.reject(error);
  },
);

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return error.response?.data?.message ?? error.message;
  }
  return 'An unexpected error occurred';
}

export default client;
