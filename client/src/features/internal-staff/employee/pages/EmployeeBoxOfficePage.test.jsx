import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EmployeeBoxOfficePage from './EmployeeBoxOfficePage';
import { getMyEmployeeWorkContext } from '../services/employeeBoxOfficeService';
import { getCinemas, getSeatLayout, getShowtimes } from '@/features/catalog/customer/services/movieService';
import { getSeatAvailability } from '@/features/booking/customer/services/seatReservationService';
import {
  cancelBooking,
  createBooking,
  finalizeCheckout,
  getBookingDetails,
  getBookingTickets,
  previewBookingPromotions,
} from '@/features/booking/customer/services/bookingService';
import {
  addFoodItem,
  getConcessions,
  removeFoodItem,
  updateFoodQuantity,
} from '@/features/booking/customer/services/foodService';
import {
  cancelCashPayment,
  collectCashPayment,
  createCashPayment,
} from '@/features/payment/services/paymentService';

vi.mock('../services/employeeBoxOfficeService', () => ({ getMyEmployeeWorkContext: vi.fn() }));
vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemas: vi.fn(),
  getSeatLayout: vi.fn(),
  getShowtimes: vi.fn(),
}));
vi.mock('@/features/booking/customer/services/seatReservationService', () => ({ getSeatAvailability: vi.fn() }));
vi.mock('@/features/booking/customer/services/bookingService', () => ({
  cancelBooking: vi.fn(),
  createBooking: vi.fn(),
  finalizeCheckout: vi.fn(),
  getBookingDetails: vi.fn(),
  getBookingTickets: vi.fn(),
  previewBookingPromotions: vi.fn(),
}));
vi.mock('@/features/booking/customer/services/foodService', () => ({
  addFoodItem: vi.fn(),
  getConcessions: vi.fn(),
  removeFoodItem: vi.fn(),
  updateFoodQuantity: vi.fn(),
}));
vi.mock('@/features/payment/services/paymentService', () => ({
  cancelCashPayment: vi.fn(),
  collectCashPayment: vi.fn(),
  createCashPayment: vi.fn(),
  paymentErrorMessage: vi.fn(() => 'Không thể xử lý'),
}));

const cinema = {
  publicId: 'cinema-1',
  slug: 'cinema-1',
  name: 'LoraFilm Landmark 81',
};
const showtime = {
  showtimePublicId: 'showtime-1',
  movie: { publicId: 'movie-1', title: 'Người Nhện: Khởi Đầu Mới', posterUrl: '/posters/nguoi-nhen.jpg' },
  movieVersion: { versionName: '2D - Phụ đề' },
  auditorium: { name: 'Phòng 01' },
  startTime: '2099-08-10T02:00:00Z',
  endTime: '2099-08-10T04:30:00Z',
  status: 'OPEN_FOR_BOOKING',
};
const booking = {
  publicId: 'booking-1',
  bookingCode: 'LORAFILM-001',
  ticketAmount: 90000,
  foodAmount: 0,
  finalAmount: 90000,
  foodOrder: null,
};
const concession = {
  id: 1,
  code: 'POP_S',
  name: 'Small popcorn',
  imageUrl: '/images/popcorn.png',
  price: 45000,
  sellable: true,
};

describe('EmployeeBoxOfficePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'operation-key') });
    vi.spyOn(window, 'print').mockImplementation(() => undefined);
    getMyEmployeeWorkContext.mockResolvedValue({ cinemaPublicId: cinema.publicId });
    getCinemas.mockResolvedValue({ data: [cinema] });
    getShowtimes.mockResolvedValue({ data: [showtime] });
    getSeatLayout.mockResolvedValue({
      showtimePublicId: showtime.showtimePublicId,
      seats: [{
        id: 1,
        publicId: 'seat-1',
        seatCode: 'A1',
        rowLabel: 'A',
        positionRow: 1,
        positionColumn: 1,
        seatType: 'STANDARD',
        price: 90000,
        priced: true,
        sellable: true,
        operationalStatus: 'ACTIVE',
      }],
    });
    getSeatAvailability.mockResolvedValue({ occupiedSeats: [], maxSeatsPerBooking: 8 });
    getConcessions.mockResolvedValue([concession]);
    createBooking.mockResolvedValue(booking);
    getBookingDetails.mockResolvedValue(booking);
    previewBookingPromotions.mockResolvedValue({
      eligible: true,
      originalAmount: 90000,
      discountAmount: 9000,
      finalAmount: 81000,
      appliedPromotions: [{ promotionPublicId: 'promotion-1', name: 'Ưu đãi hệ thống 10%' }],
    });
    finalizeCheckout.mockResolvedValue({ ...booking, discountAmount: 9000, finalAmount: 81000 });
    createCashPayment.mockResolvedValue({ paymentPublicId: 'payment-1', status: 'PENDING' });
    cancelCashPayment.mockResolvedValue({ status: 'CANCELLED' });
    cancelBooking.mockResolvedValue({ status: 'CANCELLED' });
    collectCashPayment.mockResolvedValue({
      status: 'SUCCESS',
      paymentMethod: 'CASH',
      amount: 81000,
      receivedAmount: 100000,
      changeAmount: 19000,
      collectedAt: '2026-08-10T03:15:30Z',
    });
    getBookingTickets.mockResolvedValue([{ publicId: 'ticket-1', ticketCode: 'TICKET-A1', seatLabel: 'A1' }]);
    addFoodItem.mockResolvedValue({
      items: [{ id: 11, productId: 1, quantity: 1, finalAmount: 45000 }],
      finalAmount: 45000,
    });
    updateFoodQuantity.mockResolvedValue({ items: [], finalAmount: 0 });
    removeFoodItem.mockResolvedValue(undefined);
  });

  const holdSeatAndOpenOrder = async () => {
    fireEvent.click((await screen.findAllByRole('button', { name: /Người Nhện/ }))[0]);
    fireEvent.click(await screen.findByRole('button', { name: 'A1' }));
    fireEvent.click(screen.getByRole('button', { name: 'Giữ ghế & chọn bắp nước' }));
    await waitFor(() => expect(createBooking).toHaveBeenCalledWith(expect.objectContaining({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-1'],
    })));
  };

  it('chỉ hiển thị ngày thực sự có suất chiếu', async () => {
    getShowtimes
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: [showtime] })
      .mockResolvedValue({ data: [] });

    render(<EmployeeBoxOfficePage />);

    expect((await screen.findAllByText('1 suất')).length).toBeGreaterThan(0);
    expect(screen.queryByText('0 suất')).not.toBeInTheDocument();
  });

  it('gom giờ chiếu theo poster lớn và mở sơ đồ ghế trong cửa sổ bán vé', async () => {
    const laterShowtime = {
      ...showtime,
      showtimePublicId: 'showtime-2',
      startTime: '2099-08-10T05:00:00Z',
      endTime: '2099-08-10T07:30:00Z',
      auditorium: { name: 'Phòng 02' },
    };
    getShowtimes.mockResolvedValue({ data: [showtime, laterShowtime] });

    render(<EmployeeBoxOfficePage />);

    expect(await screen.findByRole('img', { name: 'Poster Người Nhện: Khởi Đầu Mới' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /Người Nhện: Khởi Đầu Mới/ })).toHaveLength(2);
    expect(screen.getByText('Phòng 02')).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button', { name: /Người Nhện: Khởi Đầu Mới/ })[0]);
    expect(await screen.findByRole('dialog', { name: 'Người Nhện: Khởi Đầu Mới' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'A1' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '2. Chọn ghế' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Chọn suất khác' }));
    expect(screen.queryByRole('dialog', { name: 'Người Nhện: Khởi Đầu Mới' })).not.toBeInTheDocument();
  });

  it('giữ ghế, tự áp dụng ưu đãi, chọn bắp nước rồi thu tiền và phát hành vé', async () => {
    render(<EmployeeBoxOfficePage />);

    expect(await screen.findByText('LoraFilm Landmark 81')).toBeInTheDocument();
    await holdSeatAndOpenOrder();

    expect(await screen.findByText('Ưu đãi hệ thống 10%')).toBeInTheDocument();
    expect(screen.getByText('Không gửi vé vào email của tài khoản nhân viên.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Thêm Bắp rang cỡ nhỏ' }));
    await waitFor(() => expect(addFoodItem).toHaveBeenCalledWith('booking-1', {
      productId: 1,
      quantity: 1,
    }));
    expect(previewBookingPromotions).toHaveBeenLastCalledWith('booking-1', { paymentMethod: 'CASH' });

    fireEvent.click(screen.getByRole('button', { name: 'Chốt đơn & chuyển sang thu tiền' }));
    await waitFor(() => expect(finalizeCheckout).toHaveBeenCalledWith('booking-1', { paymentMethod: 'CASH' }));
    expect(createCashPayment).toHaveBeenCalledWith(
      { bookingPublicId: 'booking-1' },
      expect.any(String),
    );

    fireEvent.change(await screen.findByLabelText('Tiền khách đưa tại quầy'), { target: { value: '100000' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã nhận đủ tiền' }));
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã thu' }));

    await waitFor(() => expect(collectCashPayment).toHaveBeenCalledWith(
      'payment-1',
      100000,
      expect.any(String),
    ));
    expect((await screen.findAllByText('TICKET-A1')).length).toBeGreaterThan(0);
    const receipt = document.getElementById('counter-cash-receipt');
    expect(within(receipt).getByText('81.000 ₫')).toBeInTheDocument();
    expect(within(receipt).getByText('100.000 ₫')).toBeInTheDocument();
    expect(within(receipt).getByText('19.000 ₫')).toBeInTheDocument();
    expect(within(receipt).getByText(/tiền thối không tính vào doanh thu/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /In vé/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /In vé/ }));
    expect(window.print).toHaveBeenCalledOnce();
  });

  it('hủy đơn đang giữ ghế trước khi mở giao dịch tiền mặt', async () => {
    render(<EmployeeBoxOfficePage />);
    await holdSeatAndOpenOrder();

    fireEvent.click(await screen.findByRole('button', { name: 'Hủy đơn và trả ghế' }));
    fireEvent.click(screen.getAllByRole('button', { name: 'Hủy đơn và trả ghế' }).at(-1));

    await waitFor(() => expect(cancelBooking).toHaveBeenCalledWith(
      'booking-1',
      'Khách đổi ý trước khi thu tiền tại quầy',
    ));
    expect(cancelCashPayment).not.toHaveBeenCalled();
    expect(await screen.findByText('Đã hủy đơn chưa thu')).toBeInTheDocument();
  });
});
