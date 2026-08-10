import { useEffect, useMemo, useState } from 'react';
import { getUniqueBannerUrls } from '@/features/catalog/customer/utils/movieBanner';

const BANNER_ROTATION_INTERVAL_MS = 2_500;

export default function MovieBannerCrossfade({
  images = [],
  fallbackImage,
  movieTitle
}) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [failedUrls, setFailedUrls] = useState(() => new Set());
  const sources = useMemo(() => {
    const availableBanners = getUniqueBannerUrls(images).filter(url => !failedUrls.has(url));
    if (availableBanners.length) return availableBanners;
    return fallbackImage && !failedUrls.has(fallbackImage) ? [fallbackImage] : [];
  }, [failedUrls, fallbackImage, images]);
  const visibleIndex = sources.length ? activeIndex % sources.length : 0;

  useEffect(() => {
    if (sources.length <= 1) return undefined;

    const timer = window.setInterval(() => {
      setActiveIndex(current => (current + 1) % sources.length);
    }, BANNER_ROTATION_INTERVAL_MS);

    return () => window.clearInterval(timer);
  }, [sources.length]);

  const markAsFailed = url => {
    setFailedUrls(current => {
      if (current.has(url)) return current;
      const next = new Set(current);
      next.add(url);
      return next;
    });
  };

  return (
    <div
      className="absolute inset-0 -z-20 overflow-hidden bg-zinc-950"
      data-testid="movie-banner-crossfade"
    >
      {sources.map((url, index) => {
        const isActive = index === visibleIndex;
        return (
          <img
            key={url}
            src={url}
            alt=""
            aria-hidden="true"
            data-testid="movie-banner-image"
            data-active={isActive ? 'true' : 'false'}
            loading={index === 0 ? 'eager' : 'lazy'}
            fetchPriority={index === 0 ? 'high' : 'auto'}
            onError={() => markAsFailed(url)}
            className={`absolute inset-0 h-full w-full object-cover object-center transition-opacity duration-700 ease-in-out motion-reduce:transition-none ${
              isActive ? 'opacity-100' : 'pointer-events-none opacity-0'
            }`}
          />
        );
      })}

      {!sources.length && (
        <div
          className="absolute inset-0 bg-gradient-to-br from-zinc-900 via-zinc-950 to-black"
          role="img"
          aria-label={`Chưa có banner cho phim ${movieTitle || ''}`.trim()}
        />
      )}
    </div>
  );
}
