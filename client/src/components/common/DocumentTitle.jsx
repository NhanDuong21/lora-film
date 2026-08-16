import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

const siteName = 'LoraFilm';

const roleLabels = {
  ADMIN: 'Quản trị viên',
  MANAGER: 'Quản lý rạp',
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
  if (path === '/support/payment') return 'Hướng dẫn thanh toán';
  if (path === '/support/refunds') return 'Chính sách đổi, hủy và hoàn vé';
  if (path === '/support/faq') return 'Câu hỏi thường gặp';
  if (path === '/support/terms') return 'Điều khoản sử dụng';
  if (path === '/support/privacy') return 'Chính sách bảo mật';

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
    ['/admin/roles', 'Tài khoản và phân quyền'],
    ['/admin/accounts', 'Tài khoản và phân quyền'],
    ['/admin/permissions', 'Tài khoản và phân quyền'],
    ['/admin/audits', 'Nhật ký hoạt động'],
    ['/admin/user-audits', 'Nhật ký hoạt động'],
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
    ['/admin/accounting', 'Bàn làm việc kế toán'],
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

  if (path === '/manager') return 'Tổng quan vận hành rạp';
  if (path === '/manager/showtimes') return 'Lịch chiếu tại rạp';
  if (path === '/manager/rooms') return 'Phòng chiếu và bảo trì';
  if (path === '/manager/bookings') return 'Đơn đặt vé và giữ ghế';
  if (path.startsWith('/manager/bookings/')) return 'Chi tiết đơn đặt vé';
  if (path === '/manager/payments') return 'Giao dịch tại rạp';
  if (path.startsWith('/manager/payments/')) return 'Chi tiết giao dịch tại rạp';
  if (path === '/manager/staff') return 'Nhân sự và ca làm tại rạp';
  if (path === '/manager/ticket-control') return 'Soát vé và bàn giao tại rạp';
  if (path === '/manager/reports') return 'Báo cáo vận hành rạp';
  if (path === '/manager/cinema') return 'Thông tin và giờ mở cửa';

  if (path === '/employee' || path === '/employee/dashboard') return 'Tổng quan ca';
  if (path === '/employee/box-office') return 'Bán vé tại quầy';
  if (path === '/employee/orders') return 'Đơn tại quầy';
  if (path.startsWith('/employee/orders/')) return 'Chi tiết đơn tại quầy';
  if (path === '/employee/cash-session') return 'Chốt ca và bàn giao';
  if (path === '/employee/ticket-scan') return 'Soát vé tại rạp';
  if (path === '/employee/ticket-showtimes') return 'Suất chiếu và cửa phòng';
  if (path === '/employee/ticket-history') return 'Lịch sử soát vé và sự cố';
  if (path === '/employee/ticket-handoff') return 'Bàn giao ca soát vé';
  if (path === '/employee/checkin') return 'Chấm công nhân viên';
  if (path === '/employee/schedules') return 'Lịch làm việc';
  if (path === '/employee/payroll') return 'Bảng lương nhân viên';
  if (path === '/employee/payments/cash') return 'Thu tiền mặt tại quầy';
  if (path === '/employee/payments/refunds') return 'Hỗ trợ và hoàn tiền';

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
