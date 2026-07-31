import { describe, expect, it } from 'vitest';
import { buildSeatUnits, removeUnavailableSeatUnits } from './seatUnits';

const seat = (overrides = {}) => ({
  publicId: 'seat-1',
  seatCode: 'I1',
  rowLabel: 'I',
  positionRow: 9,
  positionColumn: 1,
  seatType: 'COUPLE',
  pairGroup: 'I-01',
  price: 78000,
  priced: true,
  operationalStatus: 'ACTIVE',
  blockedForShowtime: false,
  sellable: true,
  ...overrides
});

describe('seatUnits', () => {
  it('groups two adjacent couple seat records into one double-width unit', () => {
    const units = buildSeatUnits([
      seat(),
      seat({ publicId: 'seat-2', seatCode: 'I2', positionColumn: 2 })
    ]);

    expect(units).toHaveLength(1);
    expect(units[0]).toMatchObject({
      seatCode: 'I1–I2',
      pairGroup: 'I-01',
      pairValid: true,
      columnSpan: 2,
      price: 156000,
      sellable: true
    });
    expect(units[0].seats.map(item => item.publicId)).toEqual(['seat-1', 'seat-2']);
  });

  it('fails closed when a couple group is missing its partner', () => {
    const [unit] = buildSeatUnits([seat()]);

    expect(unit.pairValid).toBe(false);
    expect(unit.sellable).toBe(false);
    expect(unit.columnSpan).toBe(1);
  });

  it('removes both selected partners when either one becomes unavailable', () => {
    const selected = [
      seat(),
      seat({ publicId: 'seat-2', seatCode: 'I2', positionColumn: 2 }),
      seat({
        publicId: 'standard-1',
        seatCode: 'H1',
        rowLabel: 'H',
        positionRow: 8,
        seatType: 'VIP',
        pairGroup: null
      })
    ];

    expect(removeUnavailableSeatUnits(selected, new Set(['seat-2'])).map(item => item.publicId))
      .toEqual(['standard-1']);
  });
});
