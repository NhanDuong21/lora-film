export const CREDIT_ROLES = {
  DIRECTOR: 'Đạo diễn',
  MAIN_ACTOR: 'Diễn viên chính',
  SUPPORTING_ACTOR: 'Diễn viên phụ',
  VOICE_ACTOR: 'Diễn viên lồng tiếng',
  WRITER: 'Biên kịch',
  PRODUCER: 'Nhà sản xuất',
  GUEST: 'Khách mời'
};

export const getCreditRoleLabel = (role) => {
  return CREDIT_ROLES[role] || role || 'Không xác định';
};
