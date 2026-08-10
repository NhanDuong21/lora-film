import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OAuth2RedirectHandler from './OAuth2RedirectHandler';
import { useAuth } from '@/contexts/AuthContext';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn()
}));

const encode = value => btoa(JSON.stringify(value))
  .replaceAll('+', '-')
  .replaceAll('/', '_')
  .replaceAll('=', '');

const accessToken = [
  encode({ alg: 'none', typ: 'JWT' }),
  encode({
    sub: 'admin@example.com',
    role: 'ADMIN',
    userId: 16,
    exp: Math.floor(Date.now() / 1000) + 300
  }),
  'signature'
].join('.');

const customerAccessToken = [
  encode({ alg: 'none', typ: 'JWT' }),
  encode({
    sub: 'customer@example.com',
    role: 'CUSTOMER',
    userId: 17,
    exp: Math.floor(Date.now() / 1000) + 300
  }),
  'signature'
].join('.');

const employeeAccessToken = [
  encode({ alg: 'none', typ: 'JWT' }),
  encode({
    sub: 'employee@example.com',
    role: 'EMPLOYEE',
    permissions: ['EMPLOYEE_DASHBOARD_VIEW'],
    userId: 18,
    exp: Math.floor(Date.now() / 1000) + 300
  }),
  'signature'
].join('.');

const renderCallback = entry => render(
  <MemoryRouter initialEntries={[entry]}>
    <Routes>
      <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />
      <Route path="/" element={<div>Home destination</div>} />
      <Route path="/admin" element={<div>Admin destination</div>} />
      <Route path="/employee" element={<div>Employee destination</div>} />
      <Route path="/login" element={<div>Login destination</div>} />
    </Routes>
  </MemoryRouter>
);

describe('OAuth2RedirectHandler', () => {
  const login = vi.fn();

  beforeEach(() => {
    login.mockReset();
    login.mockResolvedValue(undefined);
    useAuth.mockReturnValue({ login });
  });

  it('stores a complete fragment session and redirects by role', async () => {
    renderCallback(
      `/oauth2/redirect#accessToken=${accessToken}&refreshToken=refresh-token&expiresIn=900000`
    );

    expect(await screen.findByText('Admin destination')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith(expect.objectContaining({
      accessToken,
      refreshToken: 'refresh-token',
      accountId: 16,
      email: 'admin@example.com',
      role: 'ADMIN'
    }));
  });

  it('rejects an incomplete callback without storing auth state', async () => {
    renderCallback(`/oauth2/redirect#accessToken=${accessToken}`);

    expect(await screen.findByText('Login destination')).toBeInTheDocument();
    await waitFor(() => expect(login).not.toHaveBeenCalled());
  });

  it('redirects a Google customer login to home', async () => {
    renderCallback(
      `/oauth2/redirect#accessToken=${customerAccessToken}&refreshToken=refresh-token&expiresIn=900000`
    );

    expect(await screen.findByText('Home destination')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith(expect.objectContaining({
      accessToken: customerAccessToken,
      role: 'CUSTOMER'
    }));
  });

  it('redirects the canonical EMPLOYEE role to the employee workspace', async () => {
    renderCallback(
      `/oauth2/redirect#accessToken=${employeeAccessToken}&refreshToken=refresh-token&expiresIn=900000`
    );

    expect(await screen.findByText('Employee destination')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith(expect.objectContaining({
      accessToken: employeeAccessToken,
      role: 'EMPLOYEE'
    }));
  });
});
