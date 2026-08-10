import { act, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MovieBannerCrossfade from './MovieBannerCrossfade';
import { getMovieBannerUrls } from '@/features/catalog/customer/utils/movieBanner';

describe('MovieBannerCrossfade', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('orders active banners with the primary image first and removes duplicates', () => {
    expect(getMovieBannerUrls([
      { mediaType: 'BANNER', url: 'banner-b.jpg', displayOrder: 2, status: 'ACTIVE' },
      { mediaType: 'BANNER', url: 'banner-a.jpg', displayOrder: 1, status: 'ACTIVE' },
      { mediaType: 'BANNER', url: 'banner-primary.jpg', displayOrder: 3, isPrimary: true, status: 'ACTIVE' },
      { mediaType: 'BANNER', url: 'banner-a.jpg', displayOrder: 4, status: 'ACTIVE' },
      { mediaType: 'POSTER', url: 'poster.jpg', status: 'ACTIVE' },
      { mediaType: 'BANNER', url: 'inactive.jpg', status: 'INACTIVE' }
    ])).toEqual(['banner-primary.jpg', 'banner-a.jpg', 'banner-b.jpg']);
  });

  it('replaces the visible banner every 2.5 seconds using opacity crossfade', () => {
    vi.useFakeTimers();
    render(
      <MovieBannerCrossfade
        images={['banner-1.jpg', 'banner-2.jpg']}
        movieTitle="Phim thử nghiệm"
      />
    );

    const banners = screen.getAllByTestId('movie-banner-image');
    expect(banners[0]).toHaveAttribute('data-active', 'true');
    expect(banners[1]).toHaveAttribute('data-active', 'false');
    expect(screen.getByTestId('movie-banner-crossfade').className).not.toContain('translate');

    act(() => {
      vi.advanceTimersByTime(2_499);
    });
    expect(banners[0]).toHaveAttribute('data-active', 'true');

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(banners[0]).toHaveAttribute('data-active', 'false');
    expect(banners[1]).toHaveAttribute('data-active', 'true');
  });

  it('uses the legacy backdrop only when the movie has no banner', () => {
    render(
      <MovieBannerCrossfade
        images={[]}
        fallbackImage="legacy-backdrop.jpg"
        movieTitle="Phim cũ"
      />
    );

    expect(screen.getByTestId('movie-banner-image')).toHaveAttribute(
      'src',
      'legacy-backdrop.jpg'
    );
  });
});
