import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { getRoleFallbackAvatar } from './avatarUtils';
import { PersonAvatar } from './HrWorkspace';

describe('PersonAvatar', () => {
  it('uses the employee avatar when one is available', () => {
    render(<PersonAvatar name="Đặng Thành Nhân" avatarUrl="https://cdn.example.com/nhan.jpg" role="OPS_MANAGER" />);

    expect(screen.getByRole('img', { name: 'Ảnh đại diện của Đặng Thành Nhân' })).toHaveAttribute(
      'src',
      'https://cdn.example.com/nhan.jpg'
    );
  });

  it.each([
    ['OPS_MANAGER', '/images/manager_avt.png'],
    ['BOX_OFFICE', '/images/employee_banve.png'],
    ['CUSTOMER_CARE', '/images/chamsockhachhang.png'],
    ['FINANCE_ADMIN', '/images/ketoan.png']
  ])('chọn ảnh mặc định theo vai trò %s', (role, expectedUrl) => {
    expect(getRoleFallbackAvatar(role)).toBe(expectedUrl);
  });

  it('shows a role fallback when the uploaded avatar cannot load', () => {
    render(<PersonAvatar name="Đặng Thành Nhân" avatarUrl="/uploads/missing.jpg" role="OPS_MANAGER" />);

    fireEvent.error(screen.getByRole('img', { name: 'Ảnh đại diện của Đặng Thành Nhân' }));

    expect(screen.getByRole('img', { name: 'Ảnh mặc định theo vai trò của Đặng Thành Nhân' })).toHaveAttribute(
      'src',
      '/images/manager_avt.png'
    );
  });
});
