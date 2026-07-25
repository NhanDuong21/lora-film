import { describe, expect, it } from 'vitest';
import {
  seatPresentation,
  seatStatePresentation,
  seatTypePresentation,
  sortSeatLegend
} from './seatPresentation';

describe('customer seat presentation', () => {
  it.each([
    ['STANDARD', 'Ghế thường', false],
    ['VIP', 'Ghế VIP', false],
    ['COUPLE', 'Ghế đôi', true],
    ['SUPPORT', 'Ghế hỗ trợ', false]
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

  it('does not invent availability language', () => {
    const result = seatPresentation({
      seatType: 'VIP',
      priced: true,
      price: 100000,
      operationalStatus: 'ACTIVE',
      blockedForShowtime: false,
      sellable: true
    });
    expect(result.reason).toBe('chưa xác nhận tình trạng');
    expect(JSON.stringify(result)).not.toMatch(/AVAILABLE|HELD|BOOKED/);
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
});
