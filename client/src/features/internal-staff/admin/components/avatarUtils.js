const normalizeRole = value => String(value || '')
  .normalize('NFD')
  .replace(/\p{Diacritic}/gu, '')
  .toLowerCase();

export const getEmployeeAvatarRole = employee => [
  employee?.positionCode,
  employee?.positionName,
  employee?.departmentCode,
  employee?.departmentName
].filter(Boolean).join(' ');

export const getRoleFallbackAvatar = role => {
  const normalized = normalizeRole(role);
  if (/(ops_manager|manager|quan ly|giam sat)/.test(normalized)) return '/images/manager_avt.png';
  if (/(finance|tai chinh|ke toan|accounting|fin)/.test(normalized)) return '/images/ketoan.png';
  if (/(customer_care|customer care|cham soc|support|cs)/.test(normalized)) return '/images/chamsockhachhang.png';
  if (/(box_office|ban ve|quay ve|soat ve|ticket)/.test(normalized)) return '/images/employee_banve.png';
  return '/images/main-logo.png';
};

const hasAnyPermission = (permissions, ...requiredPermissions) => (
  requiredPermissions.some(permission => permissions.includes(permission))
);

export const getSignedInUserFallbackAvatar = user => {
  const normalizedRole = normalizeRole(user?.role);
  const permissions = Array.isArray(user?.permissions) ? user.permissions : [];

  if (/(^|role_)admin$/.test(normalizedRole) || permissions.includes('PERM_ROOT_ACCESS')) {
    return '/images/main-logo.png';
  }
  if (/(manager|quan ly|giam sat)/.test(normalizedRole)) {
    return '/images/manager_avt.png';
  }
  if (/(accountant|accounting|finance|ke toan)/.test(normalizedRole)
    || hasAnyPermission(
      permissions,
      'PERM_VIEW_FINANCE',
      'PAYMENT_RECONCILE',
      'ANALYTICS_VIEW',
      'PAYROLL_APPROVE',
      'PAYROLL_CREATE',
      'PAYROLL_UPDATE',
    )) {
    return '/images/ketoan.png';
  }
  if (/(customer_service|customer_care|cham soc|support)/.test(normalizedRole)
    || hasAnyPermission(permissions, 'CUSTOMER_VIEW', 'SCORE_MANAGE')) {
    return '/images/chamsockhachhang.png';
  }
  if (/(employee|box_office|ticket)/.test(normalizedRole)) {
    return '/images/employee_banve.png';
  }

  return getRoleFallbackAvatar(user?.role);
};
