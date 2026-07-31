import { describe, expect, it } from 'vitest';
import { hasSingleSeatGap } from './seatGapPolicy';

const seat = (seatCode, positionColumn, overrides = {}) => ({
  id: positionColumn,
  publicId: `seat-${seatCode.toLowerCase()}`,
  seatCode,
  rowLabel: 'A',
  positionRow: 1,
  positionColumn,
  seatType: 'STANDARD',
  operationalStatus: 'ACTIVE',
  sellable: true,
  priced: true,
  blockedForShowtime: false,
  ...overrides
});

describe('hasSingleSeatGap', () => {
  it('detects an empty edge seat left by the current selection', () => {
    const seats = [seat('A1', 1), seat('A2', 2), seat('A3', 3)];

    expect(hasSingleSeatGap(seats, new Set(['seat-a2', 'seat-a3']))).toBe(true);
  });

  it('treats a held seat as unavailable when detecting a gap', () => {
    const seats = [
      seat('A1', 1, { reservationStatus: 'HELD', sellable: false }),
      seat('A2', 2),
      seat('A3', 3)
    ];

    expect(hasSingleSeatGap(seats, new Set(['seat-a3']))).toBe(true);
  });

  it('does not treat seats separated by a missing column as adjacent', () => {
    const seats = [seat('A1', 1), seat('A3', 3)];

    expect(hasSingleSeatGap(seats, new Set(['seat-a3']))).toBe(false);
  });

  it('does not classify an available couple couch as a single-seat gap', () => {
    const seats = [
      seat('I1', 1, {
        rowLabel: 'I',
        positionRow: 9,
        seatType: 'COUPLE',
        pairGroup: 'I-01'
      }),
      seat('I2', 2, {
        rowLabel: 'I',
        positionRow: 9,
        seatType: 'COUPLE',
        pairGroup: 'I-01'
      }),
      seat('I3', 3, { rowLabel: 'I', positionRow: 9 }),
      seat('I4', 4, { rowLabel: 'I', positionRow: 9 })
    ];

    expect(hasSingleSeatGap(seats, new Set(['seat-i3', 'seat-i4']))).toBe(false);
  });

  it('allows a selection that leaves a sellable block of seats', () => {
    const seats = [
      seat('A1', 1),
      seat('A2', 2),
      seat('A3', 3),
      seat('A4', 4)
    ];

    expect(hasSingleSeatGap(seats, new Set(['seat-a1', 'seat-a2']))).toBe(false);
  });
});
