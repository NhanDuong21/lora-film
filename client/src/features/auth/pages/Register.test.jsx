import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Register from './Register';
import { inspectIdentityNumber, register } from '@/features/auth/services/authService';

vi.mock('@/features/auth/services/authService', () => ({
  inspectIdentityNumber: vi.fn(),
  register: vi.fn(),
}));

const renderPage = () => render(
  <MemoryRouter>
    <Register />
  </MemoryRouter>
);

const completeAccountStep = () => {
  fireEvent.change(screen.getByLabelText('Họ và tên'), { target: { name: 'fullName', value: 'Nguyen Van A' } });
  fireEvent.change(screen.getByLabelText('Địa chỉ email'), { target: { name: 'email', value: 'member@example.com' } });
  fireEvent.change(screen.getByLabelText('Số điện thoại'), { target: { name: 'phoneNumber', value: '0901234567' } });
  fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { name: 'password', value: 'Password@123' } });
  fireEvent.change(screen.getByLabelText('Xác nhận mật khẩu'), { target: { name: 'confirmPassword', value: 'Password@123' } });
  fireEvent.click(screen.getByRole('checkbox', { name: /Tôi đồng ý với/i }));
  fireEvent.click(screen.getByRole('button', { name: /^Tiếp tục$/i }));
};

describe('Register wizard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.scrollTo = vi.fn();
    inspectIdentityNumber.mockResolvedValue({
      success: true,
      data: {
        identityNumberMasked: '092******789',
        birthRegistrationProvinceName: 'Cần Thơ',
        legalSexLabel: 'Nam',
        birthYear: 2005,
      },
    });
    register.mockResolvedValue({
      success: true,
      message: 'Registration initiated',
      data: { requestId: 'registration-1' },
    });
  });

  it('keeps sensitive profile fields out of the first step', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Tạo tài khoản' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Số định danh cá nhân')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Tiếp tục với Google' })).toBeInTheDocument();
  });

  it('shows explicitly named derived data before registration is submitted', async () => {
    renderPage();
    completeAccountStep();

    expect(screen.getByRole('heading', { name: 'Hoàn thiện hồ sơ' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Số định danh cá nhân'), {
      target: { name: 'cccd', value: '092205006789' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra thông tin' }));

    expect(await screen.findByText('Cần Thơ')).toBeInTheDocument();
    expect(screen.getByText('Nơi đăng ký khai sinh')).toBeInTheDocument();
    expect(screen.getByText(/không phải xác minh danh tính/i)).toBeInTheDocument();
    expect(inspectIdentityNumber).toHaveBeenCalledWith('092205006789');

    fireEvent.change(screen.getByLabelText('Ngày sinh'), {
      target: { name: 'birthday', value: '2005-06-12' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục xác minh email' }));

    await waitFor(() => expect(register).toHaveBeenCalledWith(expect.objectContaining({
      email: 'member@example.com',
      cccd: '092205006789',
      birthday: '2005-06-12',
    })));
  });
});
