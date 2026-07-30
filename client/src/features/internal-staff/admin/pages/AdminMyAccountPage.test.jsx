import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AdminMyAccountPage from './AdminMyAccountPage';

const mocks = vi.hoisted(() => ({
  logout: vi.fn(),
  updateUser: vi.fn(),
  refreshProfile: vi.fn(),
  getSessions: vi.fn(),
  triggerToast: vi.fn(),
  triggerConfirm: vi.fn()
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      role: 'ADMIN',
      fullName: 'Dương Thiện Nhân',
      avatarUrl: null
    },
    userRole: 'ADMIN',
    email: 'admin@lorafilm.vn',
    profile: {
      fullName: 'Dương Thiện Nhân',
      phoneNumber: '0900000001',
      avatarUrl: null
    },
    profileLoading: false,
    profilePending: false,
    profileError: null,
    refreshProfile: mocks.refreshProfile,
    updateUser: mocks.updateUser,
    logout: mocks.logout
  })
}));

vi.mock('@/features/auth/services/authService', () => ({
  changeEmail: vi.fn(),
  changePassword: vi.fn(),
  getSessions: mocks.getSessions,
  revokeAllSessions: vi.fn(),
  revokeSession: vi.fn()
}));

vi.mock('@/features/auth/services/userService', () => ({
  updateUserProfile: vi.fn(),
  uploadAvatar: vi.fn()
}));

function AccountRouteShell() {
  return (
    <Outlet
      context={{
        triggerToast: mocks.triggerToast,
        triggerConfirm: mocks.triggerConfirm
      }}
    />
  );
}

const renderPage = (entry = '/admin/me') => render(
  <MemoryRouter initialEntries={[entry]}>
    <Routes>
      <Route element={<AccountRouteShell />}>
        <Route path="/admin/me" element={<AdminMyAccountPage />} />
      </Route>
    </Routes>
  </MemoryRouter>
);

describe('AdminMyAccountPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getSessions.mockResolvedValue([]);
  });

  it('renders an admin-specific account workspace without customer membership content', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Tài khoản của tôi' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thông tin cá nhân' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Đổi mật khẩu' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Email đăng nhập' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Phiên đăng nhập' })).toBeInTheDocument();
    expect(screen.queryByText('Tài khoản thành viên')).not.toBeInTheDocument();
    expect(screen.queryByText('Lịch sử giao dịch')).not.toBeInTheDocument();
    expect(screen.queryByText('Điểm thưởng')).not.toBeInTheDocument();
  });

  it('shows the real password controls inside the admin layout', () => {
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }));

    expect(screen.getByLabelText('Mật khẩu hiện tại')).toBeInTheDocument();
    expect(screen.getByLabelText('Mật khẩu mới')).toBeInTheDocument();
    expect(screen.getByLabelText('Xác nhận mật khẩu mới')).toBeInTheDocument();
  });

  it('falls back from obsolete security query values and loads sessions only on demand', async () => {
    renderPage('/admin/me?tab=security');

    expect(screen.getByRole('heading', { name: 'Thông tin cá nhân' })).toBeInTheDocument();
    expect(mocks.getSessions).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Phiên đăng nhập' }));

    await waitFor(() => expect(mocks.getSessions).toHaveBeenCalledTimes(1));
    expect(screen.getByText('Không có phiên đăng nhập nào')).toBeInTheDocument();
  });
});
