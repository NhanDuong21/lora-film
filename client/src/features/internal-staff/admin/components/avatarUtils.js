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
