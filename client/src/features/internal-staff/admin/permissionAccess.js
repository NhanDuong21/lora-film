export const ADMIN_AREA_PERMISSIONS = [
  'DASHBOARD_VIEW',
  'CUSTOMER_VIEW',
  'CUSTOMER_CREATE',
  'CUSTOMER_UPDATE',
  'CUSTOMER_DELETE',
  'EMPLOYEE_VIEW',
  'EMPLOYEE_CREATE',
  'EMPLOYEE_UPDATE',
  'EMPLOYEE_DELETE',
  'EMPLOYEE_ASSIGN_POSITION',
  'DEPARTMENT_VIEW',
  'DEPARTMENT_CREATE',
  'DEPARTMENT_UPDATE',
  'DEPARTMENT_DELETE',
  'POSITION_VIEW',
  'POSITION_CREATE',
  'POSITION_UPDATE',
  'POSITION_DELETE',
  'PAYROLL_VIEW',
  'PAYROLL_CREATE',
  'PAYROLL_UPDATE',
  'PAYROLL_DELETE',
  'PAYROLL_APPROVE',
  'ROLE_VIEW',
  'ROLE_CREATE',
  'ROLE_UPDATE',
  'ROLE_DELETE',
  'PERMISSION_VIEW',
  'PERMISSION_CREATE',
  'PERMISSION_UPDATE',
  'PERMISSION_DELETE',
  'SYSTEM_CONFIGURATION',
  'USER_AUDIT_VIEW',
  'PERM_VIEW_FINANCE',
  'PAYMENT_VIEW',
  'PAYMENT_RECONCILE',
  'ANALYTICS_VIEW'
];

export const hasPermissionAccess = (role, permissions = [], ...requiredPermissions) => {
  const normalizedRole = String(role || '').replace(/^ROLE_/, '');
  if (normalizedRole === 'ADMIN' || permissions.includes('PERM_ROOT_ACCESS')) {
    return true;
  }
  return requiredPermissions.some(permission => permissions.includes(permission));
};

export const hasAdminAreaAccess = (role, permissions = []) =>
  hasPermissionAccess(role, permissions, ...ADMIN_AREA_PERMISSIONS);

const ADMIN_LANDING_ROUTES = [
  ['DASHBOARD_VIEW', '/admin'],
  ['PAYMENT_RECONCILE', '/admin/accounting'],
  ['PAYMENT_VIEW', '/admin/accounting'],
  ['ANALYTICS_VIEW', '/admin/accounting'],
  ['PERM_VIEW_FINANCE', '/admin/accounting'],
  ['CUSTOMER_VIEW', '/admin/members'],
  ['EMPLOYEE_VIEW', '/admin/hr'],
  ['DEPARTMENT_VIEW', '/admin/departments'],
  ['POSITION_VIEW', '/admin/positions'],
  ['PAYROLL_VIEW', '/admin/payroll'],
  ['ROLE_VIEW', '/admin/accounts?tab=access'],
  ['PERMISSION_VIEW', '/admin/accounts?tab=access'],
  ['SYSTEM_CONFIGURATION', '/admin/accounts'],
  ['USER_AUDIT_VIEW', '/admin/audits?tab=operations']
];

export const getAdminLandingPath = (role, permissions = []) => {
  const normalizedRole = String(role || '').replace(/^ROLE_/, '');
  if (normalizedRole === 'ADMIN' || permissions.includes('PERM_ROOT_ACCESS')) {
    return '/admin';
  }

  return ADMIN_LANDING_ROUTES.find(([permission]) => permissions.includes(permission))?.[1]
    || '/403';
};
