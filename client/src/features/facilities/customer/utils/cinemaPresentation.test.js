import { describe, expect, it } from 'vitest';
import {
  buildCinemaMap,
  formatOperatingHour,
  getCinemaImages,
  summarizeTicketPrices
} from './cinemaPresentation';

describe('customer cinema presentation', () => {
  it('uses active banner and gallery media from the cinema API in display order', () => {
    const images = getCinemaImages({
      gallery: [
        { publicId: 'gallery', mediaType: 'GALLERY', url: 'gallery.jpg', displayOrder: 2, status: 'ACTIVE' },
        { publicId: 'logo', mediaType: 'LOGO', url: 'logo.jpg', displayOrder: 0, status: 'ACTIVE' },
        { publicId: 'banner', mediaType: 'BANNER', url: 'banner.jpg', displayOrder: 1, isPrimary: true, status: 'ACTIVE' }
      ]
    });

    expect(images.map(image => image.publicId)).toEqual(['banner', 'gallery']);
  });

  it('summarizes real seat-layout prices into a range per seat type', () => {
    const prices = summarizeTicketPrices([
      {
        seats: [
          { seatType: 'STANDARD', seatTypeName: 'Ghế thường', price: 66000, currency: 'VND', priced: true },
          { seatType: 'VIP', seatTypeName: 'Ghế VIP', price: 86000, currency: 'VND', priced: true }
        ]
      },
      {
        seats: [
          { seatType: 'STANDARD', seatTypeName: 'Ghế thường', price: 76000, currency: 'VND', priced: true },
          { seatType: 'VIP', seatTypeName: 'Ghế VIP', price: null, currency: 'VND', priced: false }
        ]
      }
    ]);

    expect(prices).toEqual([
      expect.objectContaining({ code: 'STANDARD', minPrice: 66000, maxPrice: 76000 }),
      expect.objectContaining({ code: 'VIP', minPrice: 86000, maxPrice: 86000 })
    ]);
  });

  it('formats operating hours and builds a map only from API coordinates', () => {
    expect(formatOperatingHour({ openTime: '08:30', closeTime: '01:00', isClosed: false }))
      .toBe('08:30 – 01:00');
    expect(formatOperatingHour({ isClosed: true })).toBe('Đóng cửa');
    expect(buildCinemaMap({ latitude: 10.034185, longitude: 105.783461 }))
      .toMatchObject({
        embedUrl: expect.stringContaining('openstreetmap.org'),
        externalUrl: expect.stringContaining('mlat=10.034185')
      });
    expect(buildCinemaMap({})).toBeNull();
  });
});
