import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { getOptimizedImageUrl } from '@/utils/imageOptimization';
import RoleAvatar from './RoleAvatar';

describe('RoleAvatar', () => {
  it('uses the uploaded avatar before the role fallback', () => {
    const avatarUrl = 'https://res.cloudinary.com/demo/image/upload/avatar.jpg';
    render(<RoleAvatar user={{ role: 'MANAGER', fullName: 'Manager', avatarUrl }} />);

    expect(screen.getByRole('img')).toHaveAttribute('src', getOptimizedImageUrl(avatarUrl, {
      width: 256,
      height: 256,
      quality: 90,
      gravity: 'face',
    }));
  });

  it('uses the matching role image when there is no uploaded avatar', () => {
    render(<RoleAvatar user={{
      role: 'EMPLOYEE',
      fullName: 'Accountant',
      permissions: ['PAYMENT_RECONCILE'],
    }} />);

    expect(screen.getByRole('img')).toHaveAttribute('src', '/images/ketoan.png');
  });

  it('falls back to the matching role image when the uploaded avatar fails', () => {
    render(<RoleAvatar user={{
      role: 'MANAGER',
      fullName: 'Manager',
      avatarUrl: '/uploads/missing.jpg',
    }} />);

    fireEvent.error(screen.getByRole('img'));

    expect(screen.getByRole('img')).toHaveAttribute('src', '/images/manager_avt.png');
  });
});
