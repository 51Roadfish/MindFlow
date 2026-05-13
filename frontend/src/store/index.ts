import { create } from 'zustand';

interface AuthState {
  token: string | null;
  username: string | null;
  setAuth: (token: string, username: string) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  username: localStorage.getItem('username'),
  setAuth: (token, username) => {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    set({ token, username });
  },
  clearAuth: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    set({ token: null, username: null });
  },
}));
