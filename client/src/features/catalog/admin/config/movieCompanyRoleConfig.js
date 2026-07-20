export const COMPANY_ROLES = {
  PRODUCTION: 'Sản xuất',
  DISTRIBUTOR: 'Phân phối',
  STUDIO: 'Hãng phim'
};

export const getCompanyRoleLabel = (role) => {
  return COMPANY_ROLES[role] || role || 'Không xác định';
};
