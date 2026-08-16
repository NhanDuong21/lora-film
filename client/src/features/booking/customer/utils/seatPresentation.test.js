import { describe, expect, it } from 'vitest';
import {
  seatPresentation,
  seatStatePresentation,
  seatTypePresentation,
  sortSeatLegend
} from './seatPresentation';

describe('customer seat presentation', () => {
  it.each([
    ['STANDARD', 'Ghế tiêu chuẩn', false],
    ['VIP', 'Ghế VIP', false],
    ['COUPLE', 'Ghế đôi', true],
    ['SUPPORT', 'Ghế hỗ trợ tiếp cận', false]
  ])('maps %s to a distinct accessible presentation', (code, label, wide) => {
    const result = seatTypePresentation(code);
    expect(result.label).toBe(label);
    expect(Boolean(result.wide)).toBe(wide);
    expect(result.className).toBeTruthy();
  });

  it('fails blocked, inactive and unpriced seats closed', () => {
    expect(seatStatePresentation({ priced: true, price: 100, operationalStatus: 'ACTIVE', blockedForShowtime: true }).sellable).toBe(false);
    expect(seatStatePresentation({ priced: true, price: 100, operationalStatus: 'INACTIVE' }).sellable).toBe(false);
    expect(seatStatePresentation({ priced: false, operationalStatus: 'ACTIVE' }).sellable).toBe(false);
  });

  it('describes a sellable seat as available', () => {
    const result = seatPresentation({
      seatType: 'VIP',
      priced: true,
      price: 100000,
      operationalStatus: 'ACTIVE',
      blockedForShowtime: false,
      sellable: true
    });
    expect(result.reason).toBe('còn trống');
    expect(result.state).toBe('available');
  });

  it('keeps the seat type styling when another customer is holding it', () => {
    const result = seatPresentation({
      seatType: 'COUPLE',
      priced: true,
      price: 180000,
      operationalStatus: 'ACTIVE',
      blockedForShowtime: false,
      reservationStatus: 'HELD',
      sellable: false
    });

    expect(result.state).toBe('held');
    expect(result.className).toContain('bg-purple-950');
    expect(result.className).toContain('opacity-45');
  });

  it('sorts and deduplicates the legend deterministically', () => {
    const result = sortSeatLegend([
      { seatType: 'COUPLE' },
      { seatType: 'STANDARD' },
      { seatType: 'VIP' },
      { seatType: 'STANDARD' }
    ]);
    expect(result.map(seat => seat.seatType)).toEqual(['STANDARD', 'VIP', 'COUPLE']);
  });

  it('uses a complete couple unit for the legend price', () => {
    const invalidUnit = { seatType: 'COUPLE', pairValid: false, price: 90000 };
    const completePair = { seatType: 'COUPLE', pairValid: true, price: 180000 };

    expect(sortSeatLegend([invalidUnit, completePair])).toEqual([completePair]);
  });
});
