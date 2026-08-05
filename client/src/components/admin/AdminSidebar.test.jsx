import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminSidebar from './AdminSidebar';
import { getOptimizedImageUrl } from '@/utils/imageOptimization';

const renderSidebar = (user = { role: 'ADMIN', permissions: [] }, activeTab = 'dashboard') => render(
  <MemoryRouter>
    <AdminSidebar
      activeTab={activeTab}
      setActiveTab={vi.fn()}
      user={user}
      onBackHome={vi.fn()}
      handleLogout={vi.fn()}
    />
  </MemoryRouter>
);

const openSection = name => fireEvent.click(screen.getByRole('button', { name }));

describe('AdminSidebar', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('groups movie operations into readable admin sections', () => {
    renderSidebar({ role: 'ADMIN', permissions: [] }, 'auto-schedule-history');

    expect(screen.getByText('Trung tâm vận hành phim')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nội dung phim' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Rạp & lịch chiếu' })).toBeInTheDocument();
    expect(screen.getByText('Lịch chiếu')).toBeInTheDocument();
    expect(screen.getByText('Tạo lịch tuần')).toBeInTheDocument();
    expect(screen.getByText('Lịch tạo tự động')).toBeInTheDocument();
    expect(screen.getByText('Bảng giá')).toBeInTheDocument();
  });

  it('removes the two placeholder admin entries', () => {
    renderSidebar();

    expect(screen.queryByText('Khuyến mãi & sự kiện')).not.toBeInTheDocument();
    expect(screen.queryByText('Cấu hình chung')).not.toBeInTheDocument();
  });

  it('separates booking, payment and reporting operations', () => {
    renderSidebar();

    openSection('Bán vé & dịch vụ');
    expect(screen.getByText('Đơn đặt vé & giữ ghế')).toBeInTheDocument();
    expect(screen.getByText('Danh mục bắp nước')).toBeInTheDocument();

    openSection('Thanh toán & báo cáo');
    expect(screen.getByText('Giao dịch & đối soát')).toBeInTheDocument();
    expect(screen.getByText('Báo cáo doanh thu')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu bắp nước')).toBeInTheDocument();
  });

  it('uses one admin account entry instead of customer profile links', () => {
    renderSidebar();

    expect(screen.getByRole('button', { name: 'Tài khoản của tôi' })).toBeInTheDocument();
    expect(screen.queryByText('Hồ sơ của tôi')).not.toBeInTheDocument();
    expect(screen.queryByText('Bảo mật')).not.toBeInTheDocument();
  });

  it('renders the current avatar in the admin account card', () => {
    const avatarUrl = 'https://res.cloudinary.com/demo/image/upload/avatar.jpg';
    renderSidebar({
      role: 'ADMIN',
      permissions: [],
      fullName: 'LoraFilm Administrator',
      avatarUrl,
    });

    expect(screen.getByRole('img', { name: 'Ảnh đại diện LoraFilm Administrator' }))
      .toHaveAttribute('src', getOptimizedImageUrl(avatarUrl, {
        width: 256,
        height: 256,
        quality: 90,
        gravity: 'face',
      }));
  });

  it('shows notification administration links for full administrators', () => {
    renderSidebar();
    openSection('Hệ thống & thông báo');

    expect(screen.getByRole('button', { name: 'Tổng quan thông báo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mẫu thông báo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Vận hành gửi thông báo' })).toBeInTheDocument();
  });

  it('allows each operational group to be collapsed independently', () => {
    renderSidebar();

    openSection('Bán vé & dịch vụ');
    expect(screen.getByText('Đơn đặt vé & giữ ghế')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Bán vé & dịch vụ' }));

    expect(screen.queryByText('Đơn đặt vé & giữ ghế')).not.toBeInTheDocument();
    expect(screen.queryByText('Danh mục bắp nước')).not.toBeInTheDocument();
  });

  it('shows accountants only payment and report operations', () => {
    renderSidebar({ role: 'ACCOUNTANT', permissions: ['PERM_VIEW_FINANCE'] }, 'payments');

    expect(screen.queryByRole('button', { name: 'Bán vé & dịch vụ' })).not.toBeInTheDocument();
    expect(screen.queryByText('Đơn đặt vé & giữ ghế')).not.toBeInTheDocument();
    expect(screen.queryByText('Danh mục bắp nước')).not.toBeInTheDocument();
    expect(screen.getByText('Giao dịch & đối soát')).toBeInTheDocument();
    expect(screen.getByText('Báo cáo doanh thu')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu bắp nước')).toBeInTheDocument();
  });
});
