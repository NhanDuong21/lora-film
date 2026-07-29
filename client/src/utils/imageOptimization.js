const DEFAULT_QUALITY = 72;

const clampDimension = value => Math.max(32, Math.min(2400, Math.round(Number(value) || 320)));

const optimizeUnsplashUrl = (url, width, height, quality) => {
  const optimized = new URL(url);
  optimized.searchParams.set('auto', 'format');
  optimized.searchParams.set('fit', 'crop');
  optimized.searchParams.set('w', String(width));
  optimized.searchParams.set('q', String(quality));
  if (height) optimized.searchParams.set('h', String(height));
  return optimized.toString();
};

const optimizeCloudinaryUrl = (url, width, height, quality) => {
  const uploadMarker = '/image/upload/';
  const markerIndex = url.indexOf(uploadMarker);
  if (markerIndex < 0) return url;

  const transformation = [
    'f_auto',
    `q_auto:${quality <= 60 ? 'eco' : 'good'}`,
    'c_fill',
    `w_${width}`,
    height ? `h_${height}` : null
  ].filter(Boolean).join(',');

  const prefixEnd = markerIndex + uploadMarker.length;
  return `${url.slice(0, prefixEnd)}${transformation}/${url.slice(prefixEnd)}`;
};

/**
 * Requests a thumbnail-sized asset from image CDNs that support on-the-fly
 * transformations. Unknown URLs are preserved so catalog compatibility remains
 * intact.
 */
export const getOptimizedImageUrl = (
  imageUrl,
  { width = 320, height, quality = DEFAULT_QUALITY } = {}
) => {
  if (!imageUrl || typeof imageUrl !== 'string') return '';

  const normalizedWidth = clampDimension(width);
  const normalizedHeight = height ? clampDimension(height) : undefined;
  const normalizedQuality = Math.max(30, Math.min(90, Math.round(Number(quality) || DEFAULT_QUALITY)));

  try {
    const parsed = new URL(imageUrl);
    if (parsed.hostname === 'images.unsplash.com') {
      return optimizeUnsplashUrl(imageUrl, normalizedWidth, normalizedHeight, normalizedQuality);
    }
    if (parsed.hostname.endsWith('res.cloudinary.com')) {
      return optimizeCloudinaryUrl(imageUrl, normalizedWidth, normalizedHeight, normalizedQuality);
    }
  } catch {
    return imageUrl;
  }

  return imageUrl;
};

