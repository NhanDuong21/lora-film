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
  'PAYMENT_RECONCILE',
  'ANALYTICS_VIEW',
  'ACCOUNTING_VIEW_ALL_CINEMAS',
  'SETTLEMENT_IMPORT',
  'SETTLEMENT_LOCK',
  'CASH_CLOSE_VERIFY',
  'REFUND_REQUEST',
  'REFUND_APPROVE',
  'ACCOUNTING_PERIOD_VIEW',
  'ACCOUNTING_PERIOD_CREATE',
  'ACCOUNTING_PERIOD_RECONCILE',
  'ACCOUNTING_PERIOD_CLOSE',
  'ACCOUNTING_EXPORT',
  'AUDIT_VIEW',
  'PAYROLL_SUBMIT_PAYMENT',
  'PAYROLL_RECONCILE',
  'PAYROLL_CANCEL',
  'PROMOTION_VIEW',
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

export const ACCOUNTING_OPERATION_PERMISSIONS = Object.freeze([
  'SETTLEMENT_IMPORT',
  'CASH_CLOSE_VERIFY',
  'REFUND_REQUEST',
  'ACCOUNTING_PERIOD_CREATE',
  'ACCOUNTING_PERIOD_RECONCILE',
  'PAYROLL_CREATE',
  'PAYROLL_UPDATE',
  'PAYROLL_SUBMIT_PAYMENT',
  'PAYROLL_RECONCILE',
]);

export const ACCOUNTING_CONTROL_PERMISSIONS = Object.freeze([
  'SETTLEMENT_LOCK',
  'REFUND_APPROVE',
  'ACCOUNTING_PERIOD_CLOSE',
  'PAYROLL_APPROVE',
]);

export const getAccountingWorkspaceMode = (permissions = []) => {
  const hasOperationalWork = ACCOUNTING_OPERATION_PERMISSIONS
    .some(permission => permissions.includes(permission));
  const hasControlWork = ACCOUNTING_CONTROL_PERMISSIONS
    .some(permission => permissions.includes(permission));

  return hasControlWork && !hasOperationalWork ? 'control' : 'operations';
};

export const getAccountingLandingPath = permissions => (
  getAccountingWorkspaceMode(permissions) === 'control'
    ? '/admin/accounting/control'
    : '/admin/accounting/operations'
);

export const getAccountingRoleLabel = permissions => (
  getAccountingWorkspaceMode(permissions) === 'control'
    ? 'Kế toán kiểm soát'
    : 'Kế toán vận hành'
);

const ADMIN_LANDING_ROUTES = [
  ['PROMOTION_VIEW', '/admin/promotions'],
  ['DASHBOARD_VIEW', '/admin'],
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

  const hasAccountingAccess = [
    'PAYMENT_RECONCILE',
    'SETTLEMENT_IMPORT',
    'SETTLEMENT_LOCK',
    'CASH_CLOSE_VERIFY',
    'ANALYTICS_VIEW',
    'PERM_VIEW_FINANCE',
  ].some(permission => permissions.includes(permission));
  if (hasAccountingAccess) return getAccountingLandingPath(permissions);

  return ADMIN_LANDING_ROUTES.find(([permission]) => permissions.includes(permission))?.[1]
    || '/403';
};
