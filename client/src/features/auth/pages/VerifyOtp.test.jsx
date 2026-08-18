import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import VerifyOtp from './VerifyOtp';
import { resendOtp } from '@/features/auth/services/authService';

vi.mock('@/features/auth/services/authService', () => ({
  resendOtp: vi.fn(),
  verifyOtp: vi.fn(),
}));

const renderFromLogin = () => render(
  <MemoryRouter initialEntries={[{
    pathname: '/verify-otp',
    state: {
      email: 'member@example.com',
      purpose: 'REGISTRATION',
      resendImmediately: true,
    },
  }]}
  >
    <Routes>
      <Route path="/verify-otp" element={<VerifyOtp />} />
      <Route path="/register" element={<p>Trang đăng ký</p>} />
    </Routes>
  </MemoryRouter>
);

describe('VerifyOtp resend recovery', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
  });

  it('allows immediate resend after an unverified login redirect', () => {
    renderFromLogin();

    expect(screen.getByRole('button', { name: 'Gửi lại mã' })).toBeInTheDocument();
    expect(screen.queryByText(/Gửi lại mã sau/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Hãy yêu cầu một mã mới/i)).toBeInTheDocument();
  });

  it('explains an expired registration and provides a registration action', async () => {
    resendOtp.mockRejectedValue({ errorCode: 'AUTH_REGISTRATION_EXPIRED' });
    renderFromLogin();

    fireEvent.click(screen.getByRole('button', { name: 'Gửi lại mã' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Phiên đăng ký đã hết hạn');
    fireEvent.click(screen.getByRole('button', { name: 'Quay lại đăng ký' }));
    await waitFor(() => expect(screen.getByText('Trang đăng ký')).toBeInTheDocument());
  });

  it('keeps resend available when the email provider rejects the OTP', async () => {
    resendOtp.mockRejectedValue({ errorCode: 'AUTH_OTP_DELIVERY_FAILED' });
    renderFromLogin();

    fireEvent.click(screen.getByRole('button', { name: 'Gửi lại mã' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Máy chủ email đã từ chối thư');
    expect(screen.getByRole('button', { name: 'Gửi lại mã' })).toBeInTheDocument();
  });
});
