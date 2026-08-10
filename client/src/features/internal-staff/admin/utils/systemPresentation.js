export const SYSTEM_ROLE_ORDER = ['ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER'];

export const SYSTEM_ROLE_PRESENTATION = {
  ADMIN: {
    label: 'Quản trị hệ thống',
    description: 'Toàn quyền cấu hình và vận hành hệ thống LoraFilm.',
    scope: 'Toàn hệ thống',
  },
  MANAGER: {
    label: 'Quản lý rạp',
    description: 'Điều hành hoạt động và xem báo cáo của rạp được phân công.',
    scope: 'Theo rạp được phân công',
  },
  EMPLOYEE: {
    label: 'Nhân viên',
    description: 'Thực hiện công việc theo nhóm nghiệp vụ được phân công.',
    scope: 'Theo nhóm nghiệp vụ',
  },
  CUSTOMER: {
    label: 'Khách hàng',
    description: 'Đặt vé và sử dụng các tiện ích dành cho khách hàng.',
    scope: 'Kênh khách hàng',
  },
};

const PERMISSION_LABELS = {
  AUTH_LOGIN: 'Đăng nhập hệ thống',
  AUTH_LOGOUT: 'Đăng xuất hệ thống',
  AUTH_REFRESH_TOKEN: 'Duy trì phiên đăng nhập',
  AUTH_CHANGE_PASSWORD: 'Đổi mật khẩu cá nhân',
  AUTH_FORGOT_PASSWORD: 'Yêu cầu cấp lại mật khẩu',
  AUTH_RESET_PASSWORD: 'Đặt lại mật khẩu',
  AUTH_VIEW_PROFILE: 'Xem hồ sơ cá nhân',
  AUTH_UPDATE_PROFILE: 'Cập nhật hồ sơ cá nhân',
  CUSTOMER_VIEW: 'Xem danh sách khách hàng',
  CUSTOMER_CREATE: 'Thêm khách hàng',
  CUSTOMER_UPDATE: 'Cập nhật khách hàng',
  CUSTOMER_DELETE: 'Xóa khách hàng',
  EMPLOYEE_VIEW: 'Xem hồ sơ nhân viên',
  EMPLOYEE_CREATE: 'Thêm nhân viên',
  EMPLOYEE_UPDATE: 'Cập nhật hồ sơ nhân viên',
  EMPLOYEE_DELETE: 'Xóa hồ sơ nhân viên',
  EMPLOYEE_ASSIGN_POSITION: 'Phân công chức vụ cho nhân viên',
  EMPLOYEE_DASHBOARD_VIEW: 'Xem trang làm việc nhân viên',
  EMPLOYEE_SCHEDULE_VIEW: 'Xem lịch làm việc và đơn nghỉ phép',
  EMPLOYEE_LEAVE_CREATE: 'Tạo và hủy đơn nghỉ phép của mình',
  EMPLOYEE_ATTENDANCE_VIEW: 'Xem lịch sử chấm công của mình',
  EMPLOYEE_ATTENDANCE_UPDATE: 'Chấm công vào và ra ca',
  EMPLOYEE_PAYROLL_VIEW: 'Xem bảng lương của mình',
  PAYMENT_CASH_COLLECT: 'Thu tiền mặt tại quầy',
  DEPARTMENT_VIEW: 'Xem phòng ban',
  DEPARTMENT_CREATE: 'Thêm phòng ban',
  DEPARTMENT_UPDATE: 'Cập nhật phòng ban',
  DEPARTMENT_DELETE: 'Xóa phòng ban',
  POSITION_VIEW: 'Xem vị trí công việc',
  POSITION_CREATE: 'Thêm vị trí công việc',
  POSITION_UPDATE: 'Cập nhật vị trí công việc',
  POSITION_DELETE: 'Xóa vị trí công việc',
  PAYROLL_VIEW: 'Xem bảng lương',
  PAYROLL_CREATE: 'Lập bảng lương',
  PAYROLL_UPDATE: 'Điều chỉnh bảng lương',
  PAYROLL_DELETE: 'Xóa bảng lương',
  PAYROLL_APPROVE: 'Duyệt bảng lương',
  DASHBOARD_VIEW: 'Xem tổng quan quản trị',
  ROLE_VIEW: 'Xem vai trò hệ thống',
  ROLE_CREATE: 'Tạo vai trò hệ thống',
  ROLE_UPDATE: 'Cập nhật quyền của vai trò',
  ROLE_DELETE: 'Xóa vai trò hệ thống',
  PERMISSION_VIEW: 'Xem danh mục quyền',
  PERMISSION_CREATE: 'Tạo mã quyền',
  PERMISSION_UPDATE: 'Cập nhật mã quyền',
  PERMISSION_DELETE: 'Xóa mã quyền',
  SYSTEM_CONFIGURATION: 'Cấu hình hệ thống',
  USER_AUDIT_VIEW: 'Xem nhật ký hoạt động',
  PERM_VIEW_FINANCE: 'Xem báo cáo tài chính',
  PERM_ROOT_ACCESS: 'Toàn quyền hệ thống',
  BOOKING_CREATE: 'Tạo đơn đặt vé',
  BOOKING_MANAGE: 'Quản lý đơn đặt vé',
  BOOKING_VIEW: 'Xem đơn đặt vé',
  MOVIE_VIEW: 'Xem danh mục phim',
  MOVIE_MANAGE: 'Quản lý phim',
  CINEMA_MANAGE: 'Quản lý rạp chiếu',
  SHOWTIME_MANAGE: 'Quản lý suất chiếu',
  PRICING_MANAGE: 'Quản lý chính sách giá',
  PAYMENT_VIEW: 'Xem giao dịch thanh toán',
  PROMOTION_MANAGE: 'Quản lý khuyến mãi',
  ANALYTICS_MANAGE: 'Xem và quản lý báo cáo',
  ANALYTICS_VIEW: 'Xem báo cáo',
  SCORE_MANAGE: 'Quản lý điểm thưởng',
  MEMBERSHIP_TIER_MANAGE: 'Quản lý hạng thành viên',
  TICKET_SCAN: 'Soát vé tại rạp',
  USER_VIEW: 'Xem hồ sơ người dùng',
  USER_MANAGE: 'Quản lý hồ sơ người dùng',
  EMPLOYEE_MANAGE: 'Quản lý hồ sơ nhân viên',
  ACCOUNT_MANAGE: 'Quản lý tài khoản đăng nhập',
  SYSTEM_MANAGE: 'Quản lý cấu hình hệ thống',
  NOTIFICATION_MANAGE: 'Quản lý thông báo',
  PAYMENT_REFUND: 'Hoàn tiền giao dịch',
  PAYMENT_RECONCILE: 'Đối soát giao dịch',
};

const PERMISSION_GROUPS = {
  AUTH: 'Tài khoản cá nhân',
  EMPLOYEE_SELF: 'Quyền cá nhân dùng chung',
  BOOKING: 'Vé và đặt chỗ',
  PAYMENT: 'Thanh toán và đối soát',
  MOVIE: 'Phim, suất chiếu và giá vé',
  CINEMA: 'Rạp, phòng chiếu và ghế',
  CUSTOMER: 'Khách hàng và thành viên',
  COMMUNICATION: 'Khuyến mãi và thông báo',
  HR: 'Nhân sự và tổ chức',
  PAYROLL: 'Chấm công và bảng lương',
  REPORT: 'Báo cáo và thống kê',
  SYSTEM: 'Quản trị hệ thống',
  OTHER: 'Nghiệp vụ khác',
};

const DOMAIN_LABELS = {
  ACCOUNT: 'tài khoản', USER: 'người dùng', CUSTOMER: 'khách hàng', EMPLOYEE: 'nhân viên',
  DEPARTMENT: 'phòng ban', POSITION: 'chức vụ', PAYROLL: 'bảng lương', BOOKING: 'đơn đặt vé',
  MOVIE: 'phim', CINEMA: 'rạp chiếu', SHOWTIME: 'suất chiếu', PRICING: 'chính sách giá',
  PAYMENT: 'thanh toán', PROMOTION: 'khuyến mãi', NOTIFICATION: 'thông báo', SCORE: 'điểm thưởng',
  ROLE: 'vai trò', PERMISSION: 'quyền hạn', SYSTEM: 'hệ thống', ANALYTICS: 'báo cáo',
};

const ACTION_PREFIXES = {
  VIEW: 'Xem', CREATE: 'Thêm', UPDATE: 'Cập nhật', DELETE: 'Xóa', MANAGE: 'Quản lý',
  APPROVE: 'Duyệt', COLLECT: 'Thu', SCAN: 'Soát', CONFIGURATION: 'Cấu hình',
};

export const normalizeRoleCode = role => String(role?.code || role?.name || '').replace(/^ROLE_/, '').toUpperCase();

export const getRolePresentation = role => {
  const code = normalizeRoleCode(role);
  return SYSTEM_ROLE_PRESENTATION[code] || {
    label: role?.name || code || 'Chưa xác định',
    description: role?.description || 'Vai trò chưa có mô tả.',
    scope: 'Theo cấu hình hệ thống',
  };
};

const fallbackPermissionLabel = code => {
  const parts = String(code || '').split('_').filter(Boolean);
  const actionIndex = parts.findIndex(part => ACTION_PREFIXES[part]);
  const action = actionIndex >= 0 ? ACTION_PREFIXES[parts[actionIndex]] : 'Thực hiện';
  const domainKey = parts.find(part => DOMAIN_LABELS[part]);
  const domain = DOMAIN_LABELS[domainKey] || String(code || '').toLowerCase().replaceAll('_', ' ');
  return `${action} ${domain}`;
};

export const getPermissionLabel = permission => (
  PERMISSION_LABELS[permission?.code] || fallbackPermissionLabel(permission?.code || permission?.name)
);

export const getPermissionGroupKey = permission => {
  const code = String(permission?.code || '').toUpperCase();
  const module = String(permission?.module || '').toUpperCase();
  if (code === 'ACCOUNT_MANAGE' || code === 'SYSTEM_MANAGE') return 'SYSTEM';
  if (code.includes('PROMOTION') || code.includes('NOTIFICATION')) return 'COMMUNICATION';
  if (code === 'USER_VIEW' || code === 'USER_MANAGE') return 'CUSTOMER';
  if (code.startsWith('AUTH_') || module.includes('AUTH')) return 'AUTH';
  if (code.startsWith('EMPLOYEE_') && ['EMPLOYEE_DASHBOARD_VIEW', 'EMPLOYEE_SCHEDULE_VIEW', 'EMPLOYEE_LEAVE_CREATE', 'EMPLOYEE_ATTENDANCE_VIEW', 'EMPLOYEE_ATTENDANCE_UPDATE', 'EMPLOYEE_PAYROLL_VIEW'].includes(code)) return 'EMPLOYEE_SELF';
  if (code.includes('BOOKING') || code.includes('TICKET')) return 'BOOKING';
  if (code.includes('PAYMENT') || module.includes('PAYMENT')) return 'PAYMENT';
  if (code.includes('MOVIE') || code.includes('SHOWTIME') || code.includes('PRICING')) return 'MOVIE';
  if (code.includes('CINEMA') || code.includes('ROOM') || code.includes('SEAT')) return 'CINEMA';
  if (code.includes('CUSTOMER') || code.includes('SCORE') || code.includes('MEMBERSHIP') || code.includes('LOYALTY')) return 'CUSTOMER';
  if (code.includes('EMPLOYEE') || code.includes('DEPARTMENT') || code.includes('POSITION')) return 'HR';
  if (code.includes('PAYROLL') || code.includes('ATTENDANCE') || code.includes('WORK_SHIFT')) return 'PAYROLL';
  if (code.includes('ANALYTICS') || code.includes('FINANCE') || code.includes('REPORT') || code === 'DASHBOARD_VIEW') return 'REPORT';
  if (code.includes('ROLE') || code.includes('PERMISSION') || code.includes('SYSTEM') || code === 'PERM_ROOT_ACCESS') return 'SYSTEM';
  return 'OTHER';
};

export const getPermissionGroupLabel = key => PERMISSION_GROUPS[key] || PERMISSION_GROUPS.OTHER;

const AUDIT_ACTION_LABELS = {
  LOGIN_SUCCESS: 'Đăng nhập thành công',
  LOGOUT_SUCCESS: 'Đăng xuất thành công',
  LOGIN_FAILED_INVALID_PASSWORD: 'Đăng nhập thất bại do sai mật khẩu',
  LOGIN_FAILED: 'Đăng nhập thất bại',
  REFRESH_TOKEN_FAILED: 'Không thể làm mới phiên đăng nhập',
  PASSWORD_CHANGED: 'Đã đổi mật khẩu',
  REGISTER_SUCCESS: 'Đăng ký tài khoản thành công',
  CREATE_EMPLOYEE_ACCOUNT: 'Đã cấp tài khoản nhân viên',
  UPDATE_ACCOUNT_ROLE: 'Đã thay đổi vai trò tài khoản',
  UPDATE_ACCOUNT_STATUS: 'Đã thay đổi trạng thái tài khoản',
  UPDATE_ROLE: 'Đã cập nhật quyền của vai trò',
  CREATE_ROLE: 'Đã tạo vai trò',
  DELETE_ROLE: 'Đã xóa vai trò',
  ATTENDANCE_CHECKED_IN: 'Đã chấm công vào ca',
  ATTENDANCE_CHECKED_OUT: 'Đã chấm công ra ca',
  ATTENDANCE_CORRECTED: 'Đã điều chỉnh dữ liệu chấm công',
  WORK_SHIFT_CREATED: 'Đã tạo ca làm việc',
  WORK_SHIFT_CANCELLED: 'Đã hủy ca làm việc',
  AVATAR_UPDATED: 'Đã cập nhật ảnh đại diện',
  USER_PROFILE_UPDATED: 'Đã cập nhật hồ sơ người dùng',
  USER_PROFILE_CREATED: 'Đã tạo hồ sơ người dùng',
  EMPLOYEE_CREATED: 'Đã tạo hồ sơ nhân viên',
  EMPLOYEE_UPDATED: 'Đã cập nhật hồ sơ nhân viên',
  PAYROLL_CREATED: 'Đã lập bảng lương',
  PAYROLL_UPDATED: 'Đã điều chỉnh bảng lương',
  PAYROLL_APPROVED: 'Đã duyệt bảng lương',
  PAYROLL_RECONCILED: 'Đã đối soát bảng lương',
  PAYROLL_PAYMENT_SUBMITTED: 'Đã gửi yêu cầu thanh toán lương',
  PAYROLL_GENERATED_FROM_TIMEKEEPING: 'Đã tạo bảng lương từ dữ liệu chấm công',
  LEAVE_REQUEST_CREATED: 'Đã tạo đơn nghỉ phép',
  LEAVE_REQUEST_APPROVED: 'Đã duyệt đơn nghỉ phép',
  LEAVE_REQUEST_REJECTED: 'Đã từ chối đơn nghỉ phép',
  LEAVE_REQUESTED: 'Đã gửi đơn nghỉ phép',
  LEAVE_APPROVED: 'Đã duyệt đơn nghỉ phép',
  LEAVE_REJECTED: 'Đã từ chối đơn nghỉ phép',
};

const WORD_LABELS = {
  CREATED: 'đã tạo', UPDATED: 'đã cập nhật', DELETED: 'đã xóa', APPROVED: 'đã duyệt',
  REJECTED: 'đã từ chối', CANCELLED: 'đã hủy', CHECKED: 'đã chấm công', IN: 'vào', OUT: 'ra',
  SUCCESS: 'thành công', FAILED: 'thất bại', PROFILE: 'hồ sơ', SHIFT: 'ca làm', WORK: 'công việc',
};

export const getAuditActionLabel = action => {
  if (AUDIT_ACTION_LABELS[action]) return AUDIT_ACTION_LABELS[action];
  return String(action || 'Hoạt động hệ thống')
    .split('_')
    .map(word => WORD_LABELS[word] || DOMAIN_LABELS[word] || word.toLowerCase())
    .join(' ')
    .replace(/^./, value => value.toUpperCase());
};

export const getAuditTone = action => {
  const value = String(action || '').toUpperCase();
  if (value.includes('FAILED') || value.includes('REJECTED') || value.includes('DELETE') || value.includes('CANCEL')) return 'danger';
  if (value.includes('SUCCESS') || value.includes('APPROVED') || value.includes('CHECKED_IN')) return 'success';
  return 'neutral';
};

const TARGET_LABELS = {
  USER: 'Hồ sơ người dùng', CUSTOMER: 'Khách hàng', EMPLOYEE: 'Nhân viên', PAYROLL: 'Bảng lương',
  DEPARTMENT: 'Phòng ban', POSITION: 'Chức vụ', ATTENDANCE: 'Chấm công', WORK_SHIFT: 'Ca làm việc',
  LEAVE_REQUEST: 'Đơn nghỉ phép', ACCOUNT: 'Tài khoản', SYSTEM: 'Hệ thống',
};

export const getTargetLabel = (type, id) => {
  const label = TARGET_LABELS[type] || String(type || 'Hệ thống').toLowerCase().replaceAll('_', ' ');
  return id ? `${label} #${id}` : label;
};

export const getTargetTypeLabel = type => TARGET_LABELS[type] || String(type || 'Phân hệ khác').toLowerCase().replaceAll('_', ' ');

export const getDeviceLabel = userAgent => {
  const value = String(userAgent || '');
  const browser = value.includes('Edg/') ? 'Edge' : value.includes('Chrome/') ? 'Chrome' : value.includes('Firefox/') ? 'Firefox' : value.includes('Safari/') ? 'Safari' : 'Trình duyệt khác';
  const system = value.includes('Windows') ? 'Windows' : value.includes('Android') ? 'Android' : /iPhone|iPad/.test(value) ? 'iOS' : value.includes('Mac OS') ? 'macOS' : 'thiết bị khác';
  return `${browser} trên ${system}`;
};

export const summarizeAuditDetails = details => {
  if (!details) return 'Không có ghi chú bổ sung';
  const keyLabels = {
    employeeId: 'Nhân viên', batchSize: 'Số ca', sourceChecksum: 'Mã đối soát',
    departmentId: 'Phòng ban', positionId: 'Chức vụ', status: 'Trạng thái',
  };
  return String(details)
    .split(',')
    .map(part => {
      const [key, ...rest] = part.split('=');
      if (!rest.length) return part.trim();
      const value = rest.join('=').trim();
      const displayValue = key.trim() === 'sourceChecksum' && value.length > 12
        ? `${value.slice(0, 12)}…`
        : value;
      return `${keyLabels[key.trim()] || key.trim()}: ${displayValue}`;
    })
    .join(' · ');
};
