import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import ShowtimeSeatControlDrawer from './ShowtimeSeatControlDrawer';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn() },
}));

const seat = (overrides = {}) => ({
  publicId: 'seat-a1',
  seatCode: 'A1',
  rowLabel: 'A',
  seatNumber: 1,
  positionRow: 0,
  positionColumn: 0,
  seatTypeCode: 'STANDARD',
  seatTypeName: 'Ghế thường',
  pairGroup: null,
  operationalStatus: 'ACTIVE',
  blocked: false,
  blockReason: null,
  ...overrides,
});

const layout = {
  showtimePublicId: 'showtime-1',
  showtimeStatus: 'OPEN_FOR_BOOKING',
  movieTitle: 'Phim thử nghiệm',
  cinemaTimezone: 'Asia/Ho_Chi_Minh',
  auditoriumName: 'Phòng 1',
  startTime: '2026-08-20T12:00:00Z',
  editable: true,
  seats: [
    seat(),
    seat({
      publicId: 'seat-a2',
      seatCode: 'A2',
      seatNumber: 2,
      positionColumn: 1,
      seatTypeCode: 'VIP',
      seatTypeName: 'Ghế VIP',
    }),
    seat({
      publicId: 'seat-b1',
      seatCode: 'B1',
      rowLabel: 'B',
      seatNumber: 1,
      positionRow: 1,
      positionColumn: 0,
      seatTypeCode: 'COUPLE',
      seatTypeName: 'Ghế đôi',
      pairGroup: 'couple-b1-b2',
    }),
    seat({
      publicId: 'seat-b2',
      seatCode: 'B2',
      rowLabel: 'B',
      seatNumber: 2,
      positionRow: 1,
      positionColumn: 1,
      seatTypeCode: 'COUPLE',
      seatTypeName: 'Ghế đôi',
      pairGroup: 'couple-b1-b2',
    }),
  ],
};

describe('ShowtimeSeatControlDrawer', () => {
  beforeEach(() => {
    apiClient.get.mockResolvedValue({ data: { occupiedSeats: [] } });
  });

  it('centers the dialog and presents seat types consistently with the customer layout', async () => {
    const seatControlApi = {
      getSeatControl: vi.fn().mockResolvedValue(layout),
      blockSeats: vi.fn(),
      releaseBlockedSeats: vi.fn(),
    };

    render(
      <ShowtimeSeatControlDrawer
        showtimePublicId="showtime-1"
        seatControlApi={seatControlApi}
        onClose={vi.fn()}
      />,
    );

    const dialog = await screen.findByRole('dialog', { name: 'Phim thử nghiệm' });
    expect(dialog.parentElement).toHaveClass('items-center', 'justify-center');
    expect(dialog).toHaveClass('max-w-7xl', 'sm:rounded-3xl');

    expect(screen.getByText('Ghế thường')).toBeInTheDocument();
    expect(screen.getByText('Ghế VIP')).toBeInTheDocument();
    expect(screen.getByText('Ghế đôi')).toBeInTheDocument();

    const vipSeat = screen.getByRole('button', { name: /Ghế A2 · Ghế VIP/ });
    expect(vipSeat).toHaveClass('bg-amber-950');

    const coupleSeat = screen.getByRole('button', { name: /Ghế đôi B1–B2 · Ghế đôi/ });
    expect(coupleSeat).toHaveClass('bg-purple-950');
    expect(coupleSeat).toHaveStyle({ gridColumnEnd: 'span 2' });

    fireEvent.click(coupleSeat);
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Xác nhận khóa 2 ghế' })).toBeEnabled();
    });
  });
});
