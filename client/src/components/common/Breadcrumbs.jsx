import { useLocation, Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { getAccountingRoleLabel } from '@/features/internal-staff/admin/permissionAccess';

const routeNameMap = {
  'admin': 'Quản trị viên',
  'employee': 'Nhân viên',
  'manager': 'Quản lý rạp',
  'staff': 'Hồ sơ nhân viên',
  'hr': 'Trung tâm nhân sự',
  'approvals': 'Việc chờ duyệt',
  'organization': 'Sơ đồ tổ chức',
  'workforce': 'Ca làm & chấm công',
  'departments': 'Phòng ban',
  'positions': 'Vị trí',
  'payroll': 'Quy trình bảng lương',
  'accounts': 'Tài khoản & phân quyền',
  'roles': 'Tài khoản & phân quyền',
  'permissions': 'Tài khoản & phân quyền',
  'settings': 'Cấu hình',
  'audits': 'Nhật ký hoạt động',
  'user-audits': 'Nhật ký hoạt động',
  'members': 'Khách hàng',
  'scores': 'Điểm thưởng',
  'tiers': 'Hạng thẻ',
  'viewer': 'Tra cứu',
  'dashboard': 'Tổng quan',
  'pos': 'Đặt vé',
  'checkin': 'Chấm công',
  'schedules': 'Lịch làm & nghỉ phép',
  'movies': 'Phim',
  'genres': 'Thể loại',
  'events': 'Sự kiện',
  'promotions': 'Khuyến mãi',
  'cinemas': 'Cụm rạp',
  'rooms': 'Phòng chiếu',
  'seat-types': 'Loại ghế',
  'pricing': 'Chính sách giá',
  'showtime-schedules': 'Lịch tạo tự động',
  'showtimes': 'Lịch chiếu',
  'create': 'Thêm mới',
  'bookings': 'Đơn hàng',
  'payments': 'Giao dịch & đối soát',
  'finance': 'Tài chính',
  'accounting': 'Bàn làm việc kế toán',
  'operations': 'Bàn vận hành',
  'control': 'Bàn kiểm soát',
  'analytics': 'Báo cáo doanh thu',
  'settlements': 'Đối soát ngân hàng',
  'cash-control': 'Chốt ca & tiền mặt',
  'accounting-periods': 'Kỳ kế toán',
  'accounting-audit': 'Nhật ký kiểm soát',
  'concessions': 'Bắp nước',
  'concession-sales': 'Doanh thu bắp nước',
  'me': 'Tài khoản của tôi',
};

export default function Breadcrumbs() {
  const location = useLocation();
  const { user } = useAuth();
  const pathnames = location.pathname.split('/').filter(x => x);
  const permissions = user?.permissions || [];
  const hasAccountingAccess = permissions.some(permission => [
    'PAYMENT_RECONCILE', 'SETTLEMENT_IMPORT', 'SETTLEMENT_LOCK',
    'CASH_CLOSE_VERIFY', 'ACCOUNTING_PERIOD_VIEW', 'AUDIT_VIEW',
  ].includes(permission));

  // If we are at root, no need for breadcrumb
  if (pathnames.length === 0) return null;

  return (
    <div className="hidden lg:flex items-center text-sm">
      <Link to="/" className="text-zinc-500 hover:text-brand-orange transition-colors flex items-center p-1 rounded hover:bg-zinc-800/50">
        <Home className="w-4 h-4" />
      </Link>
      
      {pathnames.map((value, index) => {
        const isLast = index === pathnames.length - 1;
        const to = `/${pathnames.slice(0, index + 1).join('/')}`;
        
        // Use mapping if available, else format nicely
        let label = value === 'admin' && String(user?.role || '').replace(/^ROLE_/, '') !== 'ADMIN'
          && hasAccountingAccess
          ? getAccountingRoleLabel(permissions)
          : routeNameMap[value];
        if (!label) {
          // Check if it's a UUID or ID (e.g., number)
          if (value.length > 20 || !isNaN(value)) {
            label = 'Chi tiết';
          } else {
            label = value.charAt(0).toUpperCase() + value.slice(1).replace(/-/g, ' ');
          }
        }

        return (
          <div key={to} className="flex items-center">
            <ChevronRight className="w-4 h-4 text-zinc-600 mx-1 shrink-0" />
            {isLast ? (
              <span className="text-zinc-200 font-semibold">{label}</span>
            ) : (
              <Link to={to} className="text-zinc-500 hover:text-white transition-colors font-medium">
                {label}
              </Link>
            )}
          </div>
        );
      })}
    </div>
  );
}
