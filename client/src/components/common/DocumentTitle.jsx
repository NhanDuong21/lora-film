import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

const siteName = 'LoraFilm';

const roleLabels = {
  ADMIN: 'Quản trị viên',
  STAFF: 'Nhân viên',
  EMPLOYEE: 'Nhân viên',
  CUSTOMER: 'Khách hàng',
};

const pageTitle = pathname => {
  const path = pathname.replace(/\/+$/, '') || '/';

  if (path === '/' || path === '/home') return 'Trang chủ';
  if (path === '/login') return 'Đăng nhập';
  if (path === '/register') return 'Đăng ký tài khoản';
  if (path === '/verify-otp') return 'Xác thực OTP';
  if (path === '/forgot-password') return 'Quên mật khẩu';
  if (path === '/reset-password') return 'Đặt lại mật khẩu';
  if (path === '/oauth2/redirect') return 'Đang đăng nhập Google';
  if (path === '/profile') return 'Hồ sơ cá nhân';
  if (path === '/change-password') return 'Đổi mật khẩu';
  if (path === '/change-email') return 'Đổi email đăng nhập';
  if (path === '/sessions') return 'Phiên đăng nhập';

  if (/^\/(movies|movie)\/[^/]+/.test(path)) return 'Chi tiết phim';
  if (path === '/movies') return 'Khám phá phim';
  if (path.startsWith('/cinema/')) return 'Thông tin rạp chiếu';
  if (path === '/booking') return 'Đặt vé xem phim';
  if (path === '/seat-selection') return 'Chọn ghế';
  if (path === '/bookings/checkout') return 'Thanh toán vé';
  if (path === '/bookings/success') return 'Đặt vé thành công';
  if (path === '/bookings/failed') return 'Đặt vé thất bại';
  if (path === '/bookings' || path === '/bookings/history') return 'Lịch sử đặt vé';
  if (path.startsWith('/bookings/')) return 'Chi tiết đặt vé';
  if (path === '/promotions') return 'Ưu đãi thành viên';
  if (path === '/loyalty') return 'Điểm thưởng thành viên';
  if (path === '/payments/return') return 'Kết quả thanh toán';

  const adminTitles = [
    ['/admin/hr', 'Trung tâm nhân sự'],
    ['/admin/approvals', 'Việc chờ duyệt'],
    ['/admin/organization', 'Sơ đồ tổ chức'],
    ['/admin/analytics', 'Phân tích và báo cáo'],
    ['/admin/members', 'Quản lý khách hàng'],
    ['/admin/staff', 'Quản lý nhân viên'],
    ['/admin/workforce', 'Ca làm và chấm công'],
    ['/admin/departments', 'Quản lý phòng ban'],
    ['/admin/positions', 'Quản lý chức vụ'],
    ['/admin/payroll', 'Quản lý bảng lương'],
    ['/admin/roles', 'Quản lý vai trò'],
    ['/admin/accounts', 'Quản lý tài khoản'],
    ['/admin/permissions', 'Quản lý quyền hạn'],
    ['/admin/audits', 'Nhật ký bảo mật'],
    ['/admin/user-audits', 'Nhật ký người dùng'],
    ['/admin/movie-operations', 'Vận hành phim'],
    ['/admin/movies', 'Quản lý phim'],
    ['/admin/genres', 'Quản lý thể loại'],
    ['/admin/cinemas', 'Quản lý rạp chiếu'],
    ['/admin/seat-types', 'Quản lý loại ghế'],
    ['/admin/rooms', 'Quản lý phòng chiếu'],
    ['/admin/showtimes', 'Quản lý suất chiếu'],
    ['/admin/showtime-schedules', 'Lịch sử xếp lịch'],
    ['/admin/pricing', 'Quản lý bảng giá'],
    ['/admin/bookings', 'Quản lý đặt vé'],
    ['/admin/payments', 'Quản lý thanh toán'],
    ['/admin/concessions', 'Quản lý đồ ăn và thức uống'],
    ['/admin/concession-sales', 'Bán hàng tại quầy'],
    ['/admin/promotions', 'Quản lý khuyến mãi'],
    ['/admin/scores', 'Quản lý điểm thưởng'],
    ['/admin/notifications', 'Trung tâm thông báo'],
    ['/admin/notification-templates', 'Mẫu thông báo'],
    ['/admin/notification-operations', 'Vận hành thông báo'],
    ['/admin/me', 'Tài khoản quản trị'],
  ];

  const adminTitle = adminTitles.find(([prefix]) => path === prefix || path.startsWith(`${prefix}/`));
  if (adminTitle) return adminTitle[1];
  if (path === '/admin') return 'Tổng quan quản trị';

  if (path === '/employee' || path === '/employee/dashboard') return 'Bảng điều khiển nhân viên';
  if (path === '/employee/checkin') return 'Chấm công nhân viên';
  if (path === '/employee/schedules') return 'Lịch làm việc';
  if (path === '/employee/payroll') return 'Bảng lương nhân viên';
  if (path === '/employee/payments/cash') return 'Thu tiền mặt tại quầy';

  return 'Trang LoraFilm';
};

export default function DocumentTitle() {
  const { pathname } = useLocation();
  const { isAuthenticated, userRole, user } = useAuth();
  const role = String(userRole || user?.role || '').replace(/^ROLE_/, '');

  useEffect(() => {
    const rolePrefix = isAuthenticated && roleLabels[role] ? `${roleLabels[role]} · ` : '';
    document.title = `${rolePrefix}${pageTitle(pathname)} | ${siteName}`;
  }, [isAuthenticated, pathname, role]);

  return null;
}
