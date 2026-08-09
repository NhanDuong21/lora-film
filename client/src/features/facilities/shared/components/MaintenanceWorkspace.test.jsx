import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MaintenanceWorkspace from './MaintenanceWorkspace';

const room = {
  publicId: 'room-01',
  name: 'Screen 01 - Standard',
  capacity: 96,
  screenType: 'STANDARD',
  status: 'ACTIVE',
};

const maintenanceWindow = {
  id: 7,
  auditoriumPublicId: 'room-01',
  startTime: '2099-08-09T05:00:00.000Z',
  endTime: '2099-08-09T07:00:00.000Z',
  reason: 'Bảo trì máy chiếu',
  status: 'ACTIVE',
  createdAt: '2099-08-01T03:00:00.000Z',
  createdBy: 42,
  updatedBy: 42,
};

function renderWorkspace(overrides = {}) {
  const props = {
    rooms: [room],
    loadWindows: vi.fn().mockResolvedValue([]),
    createWindow: vi.fn().mockResolvedValue({}),
    cancelWindow: vi.fn().mockResolvedValue({}),
    previewImpact: vi.fn().mockResolvedValue({
      affectedShowtimeCount: 0,
      draftShowtimeCount: 0,
      openForBookingCount: 0,
      occupiedSeatCount: 0,
      bookingDataComplete: true,
      showtimes: [],
    }),
    viewerRole: 'manager',
    ...overrides,
  };

  render(<MaintenanceWorkspace {...props} />);
  return props;
}

describe('MaintenanceWorkspace', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('explains manager scope and requires an explicit acknowledgement before cancellation', async () => {
    const loadWindows = vi.fn().mockResolvedValue([maintenanceWindow]);
    const cancelWindow = vi.fn().mockResolvedValue({});
    renderWorkspace({ loadWindows, cancelWindow });

    const listCancelButton = await screen.findByRole('button', { name: 'Hủy lịch bảo trì' });
    fireEvent.click(listCancelButton);

    const dialog = screen.getByRole('dialog', { name: 'Xác nhận hủy lịch bảo trì' });
    expect(within(dialog).getByText('Đây là hủy lịch bảo trì, không phải hủy suất chiếu.')).toBeInTheDocument();
    expect(within(dialog).getByText(/Bạn có thể hủy cả lịch do Admin tạo trong rạp này/)).toBeInTheDocument();
    expect(within(dialog).getByText(/Tạo bởi Tài khoản #42/)).toBeInTheDocument();

    const confirmButton = within(dialog).getByRole('button', { name: 'Hủy lịch bảo trì' });
    expect(confirmButton).toBeDisabled();

    fireEvent.click(within(dialog).getByRole('checkbox'));
    expect(confirmButton).toBeEnabled();
    fireEvent.click(confirmButton);

    await waitFor(() => expect(cancelWindow).toHaveBeenCalledWith(maintenanceWindow));
    expect(await screen.findByRole('status')).toHaveTextContent('Phòng có thể được xếp suất trở lại');
  });

  it('states that creating maintenance does not automatically cancel affected showtimes', async () => {
    const createWindow = vi.fn().mockResolvedValue({});
    const previewImpact = vi.fn().mockResolvedValue({
      affectedShowtimeCount: 2,
      draftShowtimeCount: 2,
      openForBookingCount: 0,
      occupiedSeatCount: 0,
      bookingDataComplete: true,
      showtimes: [
        {
          showtimePublicId: 'showtime-01',
          movieTitle: 'Chỉ Một Đêm',
          startTime: '2099-08-09T05:30:00.000Z',
          endTime: '2099-08-09T06:45:00.000Z',
          status: 'DRAFT',
          occupiedSeatCount: 0,
          bookingDataAvailable: true,
        },
        {
          showtimePublicId: 'showtime-02',
          movieTitle: 'Venganza',
          startTime: '2099-08-09T06:00:00.000Z',
          endTime: '2099-08-09T07:30:00.000Z',
          status: 'DRAFT',
          occupiedSeatCount: 0,
          bookingDataAvailable: true,
        },
      ],
    });
    renderWorkspace({ createWindow, previewImpact });

    fireEvent.click(screen.getByRole('button', { name: 'Tạo lịch bảo trì' }));
    const dialog = screen.getByRole('dialog', { name: 'Tạo lịch bảo trì phòng chiếu' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Kiểm tra ảnh hưởng' }));

    expect(await within(dialog).findByText(/Tạo lịch bảo trì không tự động hủy hoặc đóng bán/)).toBeInTheDocument();
    expect(within(dialog).getByText('2 đang soạn · 0 đang mở bán · 0 ghế đang được giữ hoặc đã bán.')).toBeInTheDocument();

    fireEvent.click(within(dialog).getByRole('checkbox'));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Tạo lịch bảo trì · 2 suất cần xử lý' }));

    await waitFor(() => expect(createWindow).toHaveBeenCalledOnce());
    expect(await screen.findByRole('status')).toHaveTextContent('2 suất chiếu vẫn cần được xử lý riêng');
  });
});
