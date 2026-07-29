import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AdminSidebar from './AdminSidebar';

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

describe('AdminSidebar', () => {
  it('organizes movie operations by the administrator workflow', () => {
    renderSidebar({ role: 'ADMIN', permissions: [] }, 'auto-schedule-history');

    expect(screen.getByText('Trung tâm vận hành phim')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nội dung & phát hành' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cơ sở rạp' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lịch chiếu & giá vé' })).toBeInTheDocument();
    expect(screen.getByText('Lịch vận hành')).toBeInTheDocument();
    expect(screen.getByText('Lập lịch tuần')).toBeInTheDocument();
    expect(screen.getByText('Các bản lịch nháp')).toBeInTheDocument();
    expect(screen.getByText('Mẫu giá vé')).toBeInTheDocument();
  });

  it('separates booking, payment and reporting operations for administrators', () => {
    renderSidebar();

    expect(screen.getByRole('button', { name: 'Vận hành đặt vé' })).toBeInTheDocument();
    expect(screen.getByText('Đơn đặt vé & giữ ghế')).toBeInTheDocument();
    expect(screen.getByText('Danh mục bắp nước')).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'Thanh toán' })).toBeInTheDocument();
    expect(screen.getByText('Giao dịch & Đối soát')).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'Báo cáo & phân tích' })).toBeInTheDocument();
    expect(screen.getByText('Doanh thu tổng hợp')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu bắp nước')).toBeInTheDocument();
    expect(screen.queryByText('Vận hành & Tài chính')).not.toBeInTheDocument();
  });

  it('uses one admin account entry instead of customer profile links', () => {
    renderSidebar();

    expect(screen.getByRole('button', { name: 'Tài khoản của tôi' })).toBeInTheDocument();
    expect(screen.queryByText('Hồ sơ của tôi')).not.toBeInTheDocument();
    expect(screen.queryByText('Bảo mật')).not.toBeInTheDocument();
  });

  it('allows each operational group to be collapsed independently', () => {
    renderSidebar();

    fireEvent.click(screen.getByRole('button', { name: 'Vận hành đặt vé' }));

    expect(screen.queryByText('Đơn đặt vé & giữ ghế')).not.toBeInTheDocument();
    expect(screen.queryByText('Danh mục bắp nước')).not.toBeInTheDocument();
    expect(screen.getByText('Giao dịch & Đối soát')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu tổng hợp')).toBeInTheDocument();
  });

  it('shows accountants only payment and report operations', () => {
    renderSidebar({
      role: 'ACCOUNTANT',
      permissions: ['PERM_VIEW_FINANCE']
    }, 'payments');

    expect(screen.queryByRole('button', { name: 'Vận hành đặt vé' })).not.toBeInTheDocument();
    expect(screen.queryByText('Đơn đặt vé & giữ ghế')).not.toBeInTheDocument();
    expect(screen.queryByText('Danh mục bắp nước')).not.toBeInTheDocument();
    expect(screen.getByText('Giao dịch & Đối soát')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu tổng hợp')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu bắp nước')).toBeInTheDocument();
  });
});
