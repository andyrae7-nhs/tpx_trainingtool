'use client';

const TOKEN_KEY = 'tpxgrow.token';

export function getToken(): string | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) window.localStorage.setItem(TOKEN_KEY, token);
    else window.localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage unavailable */
  }
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

type Listener = () => void;
const activityListeners = new Set<Listener>();

/** Subscribe to "something happened" events (used by Pip to check for new badges). */
export function onActivity(fn: Listener) {
  activityListeners.add(fn);
  return () => activityListeners.delete(fn);
}

export async function api<T = unknown>(path: string, options: RequestInit & { json?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json', ...(options.headers as Record<string, string>) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  let body = options.body;
  if (options.json !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(options.json);
  }
  const res = await fetch(`/api${path}`, { ...options, headers, body });
  if (res.status === 401 && token) {
    setToken(null);
    if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
      window.location.href = '/login';
    }
  }
  if (!res.ok) {
    let message = res.statusText;
    try {
      const data = await res.json();
      message = data.message || message;
    } catch {
      /* not json */
    }
    throw new ApiError(res.status, message || 'Something went wrong');
  }
  const method = (options.method || 'GET').toUpperCase();
  if (method !== 'GET' && !path.startsWith('/achievements/unseen')) activityListeners.forEach((l) => l());
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const get = <T,>(path: string) => api<T>(path);
export const post = <T,>(path: string, json?: unknown) => api<T>(path, { method: 'POST', json });
export const put = <T,>(path: string, json?: unknown) => api<T>(path, { method: 'PUT', json });
export const del = <T,>(path: string) => api<T>(path, { method: 'DELETE' });
