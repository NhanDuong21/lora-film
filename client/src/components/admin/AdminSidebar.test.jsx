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

  it('organizes human resources around daily workflows', () => {
    renderSidebar({ role: 'ADMIN', permissions: [] }, 'hr');

    expect(screen.getByRole('button', { name: 'Nhân sự & tiền lương' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Trung tâm nhân sự' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Việc chờ duyệt' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lịch ca & chấm công' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quy trình bảng lương' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sơ đồ tổ chức' })).toBeInTheDocument();
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
    openSection('Thông báo');

    expect(screen.getByRole('button', { name: 'Tổng quan' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cần xử lý' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lịch sử gửi' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mẫu thông báo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cấu hình & độ phủ' })).toBeInTheDocument();
  });

  it('separates system administration from notification operations', () => {
    renderSidebar();

    expect(screen.getByRole('button', { name: 'Hệ thống' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thông báo' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hệ thống & thông báo' })).not.toBeInTheDocument();

    openSection('Hệ thống');
    expect(screen.getByRole('button', { name: 'Tài khoản & phân quyền' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nhật ký hoạt động' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Quản lý vai trò' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Quản lý quyền hạn' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Nhật ký truy cập' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Nhật ký nghiệp vụ' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Tổng quan' })).not.toBeInTheDocument();
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
    expect(screen.getByText('Giao dịch thanh toán')).toBeInTheDocument();
    expect(screen.getByText('Bàn vận hành kế toán')).toBeInTheDocument();
    expect(screen.getByText('Báo cáo doanh thu')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu bắp nước')).toBeInTheDocument();
  });

  it('recognizes the granular ACCOUNTING access profile on an employee account', () => {
    renderSidebar({
      role: 'EMPLOYEE',
      permissions: [
        'PAYMENT_VIEW',
        'PAYMENT_RECONCILE',
        'ANALYTICS_VIEW',
        'PAYROLL_VIEW',
      ],
    }, 'accounting');

    expect(screen.getByText('Bàn vận hành kế toán')).toBeInTheDocument();
    expect(screen.getAllByText('Kế toán vận hành')).toHaveLength(2);
    expect(screen.getByText('Giao dịch & xử lý đối soát')).toBeInTheDocument();
    expect(screen.getByText('Báo cáo doanh thu')).toBeInTheDocument();
    openSection('Nhân sự & tiền lương');
    expect(screen.getByText('Chuẩn bị & thanh toán lương')).toBeInTheDocument();
    expect(screen.queryByText('Doanh thu bắp nước')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Bán vé & dịch vụ' })).not.toBeInTheDocument();
  });

  it('labels the independent controller workspace explicitly', () => {
    renderSidebar({
      role: 'EMPLOYEE',
      permissions: [
        'PAYMENT_VIEW',
        'PAYMENT_RECONCILE',
        'ANALYTICS_VIEW',
        'PAYROLL_VIEW',
        'PAYROLL_APPROVE',
        'REFUND_APPROVE',
        'SETTLEMENT_LOCK',
        'ACCOUNTING_PERIOD_CLOSE',
      ],
    }, 'accounting');

    expect(screen.getByText('Bàn kiểm soát kế toán')).toBeInTheDocument();
    expect(screen.getAllByText('Kế toán kiểm soát')).toHaveLength(2);
    expect(screen.getByText('Duyệt hoàn & kiểm tra giao dịch')).toBeInTheDocument();
    expect(screen.getByText('Kiểm tra & khóa lô')).toBeInTheDocument();
    expect(screen.getByText('Kiểm tra biên bản tiền mặt')).toBeInTheDocument();
    expect(screen.getByText('Kiểm tra & khóa kỳ')).toBeInTheDocument();
    openSection('Nhân sự & tiền lương');
    expect(screen.getByText('Duyệt bảng lương')).toBeInTheDocument();
  });
});
