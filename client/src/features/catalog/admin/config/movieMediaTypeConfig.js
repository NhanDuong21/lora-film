export const MEDIA_TYPES = {
  POSTER: 'Poster',
  BACKDROP: 'Ảnh nền',
  BANNER: 'Ảnh bìa / Phông nền',
  TRAILER: 'Video giới thiệu',
  TEASER: 'Video giới thiệu ngắn',
  STILL_IMAGE: 'Ảnh tĩnh',
  BEHIND_THE_SCENES: 'Hậu trường'
};

export const getMediaTypeLabel = (type) => {
  return MEDIA_TYPES[type] || type || 'Không xác định';
};
