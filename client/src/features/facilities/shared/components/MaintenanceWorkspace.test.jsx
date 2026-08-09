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
    resolveWindow: vi.fn().mockResolvedValue({}),
    extendWindow: vi.fn().mockResolvedValue({}),
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
    expect(within(dialog).getByText(/Bạn có thể hủy cả lịch do Quản trị viên tạo trong rạp này/)).toBeInTheDocument();
    expect(within(dialog).getByText(/Tạo bởi Tài khoản #42/)).toBeInTheDocument();

    const confirmButton = within(dialog).getByRole('button', { name: 'Hủy lịch bảo trì' });
    expect(confirmButton).toBeDisabled();

    fireEvent.click(within(dialog).getByRole('checkbox'));
    expect(confirmButton).toBeEnabled();
    fireEvent.click(confirmButton);

    await waitFor(() => expect(cancelWindow).toHaveBeenCalledWith(maintenanceWindow));
    expect(await screen.findByRole('status')).toHaveTextContent('Phòng có thể được xếp suất trở lại');
  });

  it('blocks planned maintenance until affected showtimes are handled', async () => {
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

    fireEvent.click(screen.getByRole('button', { name: 'Lập lịch bảo trì' }));
    const dialog = screen.getByRole('dialog', { name: 'Lập lịch bảo trì phòng chiếu' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Kiểm tra ảnh hưởng' }));

    expect(await within(dialog).findByText(/Chưa thể lập lịch bảo trì/)).toBeInTheDocument();
    expect(within(dialog).getByText('2 đang soạn · 0 đang mở bán · 0 ghế đang được giữ hoặc đã bán.')).toBeInTheDocument();
    expect(within(dialog).queryByRole('checkbox')).not.toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Chưa thể lập lịch' })).toBeDisabled();
    expect(createWindow).not.toHaveBeenCalled();
  });

  it('allows an emergency closure and clearly explains automatic sale closing', async () => {
    const createWindow = vi.fn().mockResolvedValue({});
    const previewImpact = vi.fn().mockResolvedValue({
      affectedShowtimeCount: 2,
      draftShowtimeCount: 1,
      openForBookingCount: 1,
      occupiedSeatCount: 3,
      bookingDataComplete: true,
      showtimes: [{
        showtimePublicId: 'showtime-open',
        movieTitle: 'Suất có khách',
        startTime: '2099-08-09T05:30:00.000Z',
        endTime: '2099-08-09T06:45:00.000Z',
        status: 'OPEN_FOR_BOOKING',
        occupiedSeatCount: 3,
        bookingDataAvailable: true,
      }],
    });
    renderWorkspace({ createWindow, previewImpact });

    fireEvent.click(screen.getByRole('button', { name: 'Đóng phòng khẩn cấp' }));
    const dialog = screen.getByRole('dialog', { name: 'Đóng phòng khẩn cấp' });
    fireEvent.change(within(dialog).getByPlaceholderText('Ví dụ: Máy chiếu mất hình đột ngột'), {
      target: { value: 'Máy chiếu mất hình' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Kiểm tra ảnh hưởng' }));

    expect(await within(dialog).findByText(/1 suất đang mở bán sẽ được đóng bán tự động/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('checkbox'));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Đóng phòng ngay · 2 suất cần xử lý' }));

    await waitFor(() => expect(createWindow).toHaveBeenCalledWith(
      'room-01',
      expect.objectContaining({ maintenanceType: 'EMERGENCY', reason: 'Máy chiếu mất hình' }),
    ));
    expect(await screen.findByRole('status')).toHaveTextContent('1 suất đang mở bán đã được đóng bán');
  });

  it('uses a separate readiness flow when an active room can operate again', async () => {
    const activeIncident = {
      ...maintenanceWindow,
      startTime: '2020-08-09T05:00:00.000Z',
      endTime: '2099-08-09T07:00:00.000Z',
      maintenanceType: 'EMERGENCY',
      reason: 'Máy chiếu mất hình',
    };
    const resolveWindow = vi.fn().mockResolvedValue({});
    renderWorkspace({
      loadWindows: vi.fn().mockResolvedValue([activeIncident]),
      resolveWindow,
    });

    fireEvent.click(await screen.findByRole('button', { name: 'Phòng hoạt động trở lại' }));
    const dialog = screen.getByRole('dialog', { name: 'Xác nhận phòng hoạt động trở lại' });
    fireEvent.change(within(dialog).getByPlaceholderText(/Đã thay bóng đèn máy chiếu/), {
      target: { value: 'Đã chạy thử hình ảnh và âm thanh ổn định' },
    });
    fireEvent.click(within(dialog).getByRole('checkbox'));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận phòng hoạt động' }));

    await waitFor(() => expect(resolveWindow).toHaveBeenCalledWith(
      activeIncident,
      {
        readinessConfirmed: true,
        resolutionNote: 'Đã chạy thử hình ảnh và âm thanh ổn định',
      },
    ));
    expect(await screen.findByRole('status')).toHaveTextContent('Đã xác nhận phòng hoạt động trở lại');
  });

  it('checks affected showtimes before extending an emergency closure', async () => {
    const activeIncident = {
      ...maintenanceWindow,
      startTime: '2020-08-09T05:00:00.000Z',
      endTime: '2099-08-09T07:00:00.000Z',
      maintenanceType: 'EMERGENCY',
      reason: 'Máy chiếu mất hình',
    };
    const extendWindow = vi.fn().mockResolvedValue({});
    const previewImpact = vi.fn().mockResolvedValue({
      affectedShowtimeCount: 1,
      draftShowtimeCount: 0,
      openForBookingCount: 1,
      occupiedSeatCount: 2,
      bookingDataComplete: true,
      showtimes: [],
    });
    renderWorkspace({
      loadWindows: vi.fn().mockResolvedValue([activeIncident]),
      extendWindow,
      previewImpact,
    });

    fireEvent.click(await screen.findByRole('button', { name: 'Gia hạn thời gian' }));
    const dialog = screen.getByRole('dialog', { name: 'Gia hạn thời gian ngừng phục vụ' });
    fireEvent.change(within(dialog).getByPlaceholderText(/Cần thay thêm bo mạch/), {
      target: { value: 'Cần thêm thời gian chạy thử thiết bị' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Kiểm tra phần thời gian gia hạn' }));

    expect(await within(dialog).findByText(/1 suất đang mở bán sẽ được đóng bán tự động/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận gia hạn' }));

    await waitFor(() => expect(extendWindow).toHaveBeenCalledWith(
      activeIncident,
      expect.objectContaining({
        endTime: expect.any(String),
        note: 'Cần thêm thời gian chạy thử thiết bị',
      }),
    ));
  });
});
