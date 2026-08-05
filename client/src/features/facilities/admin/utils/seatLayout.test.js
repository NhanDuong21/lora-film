import { describe, expect, it } from 'vitest';
import { buildSeatItems, generateAutoSeatMatrix } from './seatLayout';

const typeMapping = {
  STANDARD: 'standard-id',
  VIP: 'vip-id',
  COUPLE: 'couple-id',
  DISABLED: 'disabled-id',
};

describe('seat layout utilities', () => {
  it('generates the default automatic layout without an unpaired couple seat', () => {
    const matrix = generateAutoSeatMatrix({ rows: 10, cols: 12 });

    expect(() => buildSeatItems({
      matrix,
      rows: 10,
      cols: 12,
      typeMapping,
    })).not.toThrow();

    const coupleCount = matrix.flat().filter((cell) => cell.type === 'COUPLE').length;
    expect(coupleCount).toBe(20);
  });

  it('creates a separate group for every adjacent couple pair', () => {
    const matrix = [[
      { type: 'COUPLE' },
      { type: 'COUPLE' },
      { type: 'AISLE' },
      { type: 'COUPLE' },
      { type: 'COUPLE' },
    ]];

    const seats = buildSeatItems({ matrix, rows: 1, cols: 5, typeMapping });

    expect(seats.map((seat) => seat.pairGroup)).toEqual(['A_P1', 'A_P1', 'A_P2', 'A_P2']);
    expect(seats.map((seat) => seat.positionColumn)).toEqual([1, 2, 4, 5]);
  });

  it('rejects a lone couple seat before sending the bulk request', () => {
    const matrix = [[
      { type: 'COUPLE' },
      { type: 'AISLE' },
      { type: 'COUPLE' },
    ]];

    expect(() => buildSeatItems({ matrix, rows: 1, cols: 3, typeMapping }))
      .toThrow('chưa có ghế đôi liền kề');
  });
});
