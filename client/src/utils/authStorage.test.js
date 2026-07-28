import { beforeEach, describe, expect, it } from 'vitest';
import {
  clearAuthData,
  getAuthSession,
  getRefreshToken,
  getUserPermissions,
  isAuthenticated,
  setAuthData
} from './authStorage';

const token = (payload) => {
  const encode = (value) => btoa(JSON.stringify(value))
    .replaceAll('+', '-')
    .replaceAll('/', '_')
    .replaceAll('=', '');
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.signature`;
};

describe('authStorage', () => {
  beforeEach(() => {
    clearAuthData();
  });

  it('keeps non-remembered sessions in sessionStorage and reads JWT permissions', () => {
    const accessToken = token({
      sub: 'staff@example.com',
      userId: 12,
      role: 'EMPLOYEE',
      permissions: ['PAYROLL_VIEW'],
      exp: Math.floor(Date.now() / 1000) + 300
    });

    setAuthData({ accessToken, refreshToken: 'refresh-1', rememberMe: false });

    expect(sessionStorage.getItem('authToken')).toBe(accessToken);
    expect(localStorage.getItem('authToken')).toBeNull();
    expect(getUserPermissions()).toEqual(['PAYROLL_VIEW']);
    expect(getAuthSession()).toMatchObject({
      accountId: '12',
      email: 'staff@example.com',
      role: 'EMPLOYEE',
      rememberMe: false
    });
    expect(isAuthenticated()).toBe(true);
  });

  it('persists remembered sessions in localStorage', () => {
    const accessToken = token({ exp: Math.floor(Date.now() / 1000) + 300 });
    setAuthData({ accessToken, refreshToken: 'refresh-2', rememberMe: true });

    expect(localStorage.getItem('authToken')).toBe(accessToken);
    expect(sessionStorage.getItem('authToken')).toBeNull();
    expect(getRefreshToken()).toBe('refresh-2');
  });

  it('does not discard a refresh token when the access token expires', () => {
    const accessToken = token({ exp: Math.floor(Date.now() / 1000) - 1 });
    setAuthData({ accessToken, refreshToken: 'refresh-3', rememberMe: true });

    expect(isAuthenticated()).toBe(false);
    expect(getRefreshToken()).toBe('refresh-3');
  });
});
