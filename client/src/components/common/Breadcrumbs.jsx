import { useLocation, Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';

const routeNameMap = {
  'admin': 'Quản trị viên',
  'employee': 'Nhân viên',
  'staff': 'Nhân sự',
  'departments': 'Phòng ban',
  'positions': 'Vị trí',
  'payroll': 'Bảng lương',
  'roles': 'Vai trò',
  'permissions': 'Quyền hạn',
  'settings': 'Cấu hình',
  'audits': 'Nhật ký truy cập',
  'members': 'Khách hàng',
  'scores': 'Điểm thưởng',
  'tiers': 'Hạng thẻ',
  'viewer': 'Tra cứu',
  'dashboard': 'Dashboard',
  'pos': 'Đặt vé',
  'checkin': 'Kiểm soát vé',
  'schedules': 'Lịch chiếu',
  'movies': 'Phim',
  'genres': 'Thể loại',
  'events': 'Sự kiện',
  'cinemas': 'Cụm rạp',
  'rooms': 'Phòng chiếu',
  'seat-types': 'Loại ghế',
  'pricing': 'Chính sách giá',
  'showtime-schedules': 'Lịch tạo tự động',
  'create': 'Thêm mới',
  'bookings': 'Đơn hàng',
  'payments': 'Giao dịch & đối soát',
  'finance': 'Tài chính',
  'analytics': 'Báo cáo doanh thu',
  'concessions': 'Bắp nước',
  'concession-sales': 'Doanh thu bắp nước',
  'me': 'Tài khoản của tôi',
};

export default function Breadcrumbs() {
  const location = useLocation();
  const pathnames = location.pathname.split('/').filter(x => x);

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
        let label = routeNameMap[value];
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
