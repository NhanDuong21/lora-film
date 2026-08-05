import { describe, expect, it } from 'vitest';
import { getOptimizedImageUrl } from './imageOptimization';

describe('getOptimizedImageUrl', () => {
  it('requests a bounded Unsplash thumbnail instead of the original image', () => {
    const result = new URL(getOptimizedImageUrl(
      'https://images.unsplash.com/photo-example',
      { width: 160, height: 160 }
    ));

    expect(result.searchParams.get('auto')).toBe('format');
    expect(result.searchParams.get('fit')).toBe('crop');
    expect(result.searchParams.get('w')).toBe('160');
    expect(result.searchParams.get('h')).toBe('160');
    expect(result.searchParams.get('q')).toBe('72');
  });

  it('adds a Cloudinary delivery transformation without changing the stored URL', () => {
    expect(getOptimizedImageUrl(
      'https://res.cloudinary.com/demo/image/upload/v123/catalog/popcorn.jpg',
      { width: 192, height: 192 }
    )).toBe(
      'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto:good,c_fill,w_192,h_192/v123/catalog/popcorn.jpg'
    );
  });

  it('preserves unknown providers and invalid relative values', () => {
    expect(getOptimizedImageUrl('https://cdn.example.com/popcorn.webp')).toBe(
      'https://cdn.example.com/popcorn.webp'
    );
    expect(getOptimizedImageUrl('/images/popcorn.webp')).toBe('/images/popcorn.webp');
  });

  it('supports high-quality face-focused avatar transformations', () => {
    expect(getOptimizedImageUrl(
      'https://res.cloudinary.com/demo/image/upload/v123/avatar.jpg',
      { width: 256, height: 256, quality: 90, gravity: 'face' }
    )).toBe(
      'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto:best,c_fill,g_face,w_256,h_256/v123/avatar.jpg'
    );
  });
});
