export const DEFAULT_AVATAR = 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png';

export const getTodayString = () => new Date().toISOString().split('T')[0];

export const AGE_RATINGS = ['P', 'K', 'T13', 'T16', 'T18'];

export const AGE_RATING_LABELS = {
  P: 'P – Phổ thông (Mọi lứa tuổi)',
  K: 'K – Trẻ em (cần giám hộ)',
  T13: 'T13 – Từ 13 tuổi',
  T16: 'T16 – Từ 16 tuổi',
  T18: 'T18 – Từ 18 tuổi',
};

export const STATUS_LABELS = {
  DRAFT: 'Nháp',
  UPCOMING: 'Sắp chiếu',
  NOW_SHOWING: 'Đang chiếu',
  ENDED: 'Ngừng chiếu',
  INACTIVE: 'Khóa',
};

export const STATUS_COLORS = {
  NOW_SHOWING: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  UPCOMING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  ENDED: 'bg-neutral-500/10 text-neutral-400 border-neutral-500/20',
  DRAFT: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  INACTIVE: 'bg-red-500/10 text-red-400 border-red-500/20',
};

export const mapTmdbCert = (cert) => {
  if (!cert) return 'P';
  const c = cert.toUpperCase();
  if (['G', 'P', 'ALL', 'PG', 'K', '12'].includes(c)) return 'T13';
  if (['C13', 'T13', 'PG-13'].includes(c)) return 'T13';
  if (['C16', 'T16', '16'].includes(c)) return 'T16';
  if (['R', 'NC-17', 'C18', 'T18', '18'].includes(c)) return 'T18';
  return 'P';
};

export const FORMAT_MAP_TO_API = {
  '2D': 'TWO_D',
  '3D': 'THREE_D',
  'IMAX': 'IMAX',
  '4DX': 'FOUR_DX',
  'SCREENX': 'SCREENX'
};

export const FORMAT_MAP_FROM_API = {
  'TWO_D': '2D',
  'THREE_D': '3D',
  'IMAX': 'IMAX',
  'FOUR_DX': '4DX',
  'SCREENX': 'SCREENX'
};

export const extractTrailerUrl = (bundle) => {
  if (!bundle) return '';
  if (bundle.videos?.primaryTrailer?.url) return bundle.videos.primaryTrailer.url;

  const vidTrailers = Array.isArray(bundle.videos?.trailers) ? bundle.videos.trailers : [];
  const firstVidTrailer = vidTrailers.find(t => t && t.url);
  if (firstVidTrailer) return firstVidTrailer.url;

  const vidTeasers = Array.isArray(bundle.videos?.teasers) ? bundle.videos.teasers : [];
  const firstVidTeaser = vidTeasers.find(t => t && t.url);
  if (firstVidTeaser) return firstVidTeaser.url;

  if (bundle.media?.primaryTrailer?.url) return bundle.media.primaryTrailer.url;

  const mediaTrailers = Array.isArray(bundle.media?.trailers) ? bundle.media.trailers : [];
  const firstMediaTrailer = mediaTrailers.find(t => t && t.url);
  if (firstMediaTrailer) return firstMediaTrailer.url;

  const trailers = Array.isArray(bundle.trailers) ? bundle.trailers : [];
  const youtubeTrailer = trailers.find(t => t && t.site?.toLowerCase() === 'youtube' && t.key);
  if (youtubeTrailer) return `https://www.youtube.com/watch?v=${youtubeTrailer.key}`;

  const yTrailer = vidTrailers.find(t => t && t.site?.toLowerCase() === 'youtube' && t.key);
  if (yTrailer) return `https://www.youtube.com/watch?v=${yTrailer.key}`;

  const yTeaser = vidTeasers.find(t => t && t.site?.toLowerCase() === 'youtube' && t.key);
  if (yTeaser) return `https://www.youtube.com/watch?v=${yTeaser.key}`;

  return '';
};

export const extractPosterUrl = (bundle) => {
  if (!bundle) return '';
  const mv = bundle.movie || {};
  const posters = Array.isArray(bundle.media?.posters) ? bundle.media.posters : [];
  const val = bundle.media?.primaryPoster?.url
    || mv.posterUrl
    || mv.poster?.url
    || mv.poster
    || (posters[0]?.url)
    || '';
  return typeof val === 'object' && val !== null ? (val.url || '') : String(val || '');
};

export const extractBackdropUrl = (bundle) => {
  if (!bundle) return '';
  const mv = bundle.movie || {};
  const backdrops = Array.isArray(bundle.media?.backdrops) ? bundle.media.backdrops : [];
  const val = bundle.media?.primaryBackdrop?.url
    || mv.backdropUrl
    || mv.backdrop?.url
    || mv.backdrop
    || (backdrops[0]?.url)
    || '';
  return typeof val === 'object' && val !== null ? (val.url || '') : String(val || '');
};

export const extractRuntime = (bundle) => {
  if (!bundle) return 0;
  const mv = bundle.movie || {};
  return mv.runtimeMinutes || mv.runtime || mv.durationMinutes || mv.duration || 0;
};

export const extractCountry = (bundle) => {
  if (!bundle) return '';
  const mv = bundle.movie || {};
  const countries = mv.countries || mv.productionCountries || [];
  if (Array.isArray(countries) && countries.length > 0) {
    return countries[0]?.name || countries[0]?.countryName || '';
  }
  return mv.country || mv.productionCountry || '';
};

export const getYoutubeEmbedUrl = (url) => {
  if (!url) return '';
  try {
    if (url.includes('youtube.com/watch')) {
      const k = new URLSearchParams(new URL(url).search).get('v');
      return k ? `https://www.youtube.com/embed/${k}` : '';
    }
    if (url.includes('youtu.be/')) return `https://www.youtube.com/embed/${url.split('youtu.be/')[1]?.split('?')[0]}`;
    if (url.includes('youtube.com/embed/')) return url.split('?')[0];
  } catch { return ''; }
  return '';
};

export const getYoutubeId = (url) => {
  if (!url) return '';
  try {
    if (url.includes('youtube.com/watch')) {
      return new URLSearchParams(new URL(url).search).get('v') || '';
    }
    if (url.includes('youtu.be/')) return url.split('youtu.be/')[1]?.split('?')[0] || '';
    if (url.includes('youtube.com/embed/')) {
      const parts = url.split('youtube.com/embed/');
      if (parts[1]) return parts[1].split('?')[0] || '';
    }
  } catch { return ''; }
  return '';
};

export const formatDate = (d) => {
  if (!d) return 'N/A';
  const dt = new Date(d);
  if (isNaN(dt)) return d;
  const day = String(dt.getDate()).padStart(2, '0');
  const month = String(dt.getMonth() + 1).padStart(2, '0');
  const year = dt.getFullYear();
  return `${day}/${month}/${year}`;
};
