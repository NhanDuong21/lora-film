export const EMPLOYEE_PERMISSIONS = Object.freeze({
  DASHBOARD_VIEW: 'EMPLOYEE_DASHBOARD_VIEW',
  SCHEDULE_VIEW: 'EMPLOYEE_SCHEDULE_VIEW',
  LEAVE_CREATE: 'EMPLOYEE_LEAVE_CREATE',
  ATTENDANCE_VIEW: 'EMPLOYEE_ATTENDANCE_VIEW',
  ATTENDANCE_UPDATE: 'EMPLOYEE_ATTENDANCE_UPDATE',
  PAYROLL_VIEW: 'EMPLOYEE_PAYROLL_VIEW',
  CASH_PAYMENT_COLLECT: 'PAYMENT_CASH_COLLECT',
  BOOKING_VIEW: 'BOOKING_VIEW',
  BOOKING_MANAGE: 'BOOKING_MANAGE',
  MOVIE_VIEW: 'MOVIE_VIEW',
  TICKET_SCAN: 'TICKET_SCAN',
});

export const hasEmployeeAccess = (
  role,
  permissions = [],
  requiredPermissions = [],
  requireAll = false,
) => {
  const normalizedRole = String(role || '').replace(/^ROLE_/, '');
  if (normalizedRole === 'ADMIN' || permissions.includes('PERM_ROOT_ACCESS')) {
    return true;
  }
  if (normalizedRole !== 'EMPLOYEE') {
    return false;
  }
  if (requiredPermissions.length === 0) {
    return true;
  }
  return requireAll
    ? requiredPermissions.every(permission => permissions.includes(permission))
    : requiredPermissions.some(permission => permissions.includes(permission));
};

const EMPLOYEE_LANDING_ROUTES = [
  [EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW, '/employee/dashboard'],
  [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE, '/employee/box-office'],
  [EMPLOYEE_PERMISSIONS.TICKET_SCAN, '/employee/ticket-scan'],
  [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW, '/employee/schedules'],
  [EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW, '/employee/checkin'],
  [EMPLOYEE_PERMISSIONS.PAYROLL_VIEW, '/employee/payroll'],
  [EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT, '/employee/payments/cash'],
];

export const getEmployeeLandingPath = (role, permissions = []) => {
  const normalizedRole = String(role || '').replace(/^ROLE_/, '');
  if (normalizedRole === 'ADMIN' || permissions.includes('PERM_ROOT_ACCESS')) {
    return '/employee/dashboard';
  }
  if (normalizedRole !== 'EMPLOYEE') {
    return '/403';
  }
  return EMPLOYEE_LANDING_ROUTES.find(([permission]) => permissions.includes(permission))?.[1]
    || '/403';
};
