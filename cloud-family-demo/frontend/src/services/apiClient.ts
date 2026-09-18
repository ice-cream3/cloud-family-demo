import type { ApiResponse } from '../types/api';
import { clearSession, notifyUnauthorized, readSession } from './tokenStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

type RequestOptions = {
  body?: unknown;
  form?: Record<string, string | number | undefined>;
  auth?: boolean;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code?: number;

  constructor(message: string, status: number, code?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

export async function post<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers();
  const init: RequestInit = {
    method: 'POST',
    headers,
  };

  if (options.auth !== false) {
    const session = readSession();
    if (session?.accessToken) {
      headers.set('Authorization', `${session.tokenType} ${session.accessToken}`);
    }
  }

  let url = `${API_BASE_URL}${path}`;
  if (options.form) {
    const search = new URLSearchParams();
    Object.entries(options.form).forEach(([key, value]) => {
      if (value !== undefined) {
        search.set(key, String(value));
      }
    });
    init.body = search;
    headers.set('Content-Type', 'application/x-www-form-urlencoded');
  } else if (options.body !== undefined) {
    init.body = JSON.stringify(options.body);
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, init);
  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : null;

  if (response.status === 401 && options.auth !== false) {
    clearSession();
    notifyUnauthorized();
  }

  if (!response.ok) {
    throw new ApiError(payload?.message || response.statusText, response.status, payload?.code);
  }

  const apiResponse = payload as ApiResponse<T>;
  if (apiResponse && typeof apiResponse.code === 'number' && apiResponse.code !== 0) {
    throw new ApiError(apiResponse.message || 'Request failed', response.status, apiResponse.code);
  }

  return apiResponse?.data as T;
}
