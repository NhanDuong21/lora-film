export const MEDIA_TYPES = {
  POSTER: 'Poster',
  BACKDROP: 'Backdrop',
  BANNER: 'Banner',
  TRAILER: 'Trailer',
  TEASER: 'Teaser',
  STILL_IMAGE: 'Still Image',
  BEHIND_THE_SCENES: 'Behind The Scenes'
};

export const getMediaTypeLabel = (type) => {
  return MEDIA_TYPES[type] || type || 'Không xác định';
};
