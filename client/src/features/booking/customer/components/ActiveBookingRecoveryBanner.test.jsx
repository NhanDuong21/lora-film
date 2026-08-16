import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ActiveBookingRecoveryBanner from "./ActiveBookingRecoveryBanner";
import { cancelBooking, getBookingHistory } from "../services/bookingService";
import { useAuth } from "@/contexts/AuthContext";

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../services/bookingService", () => ({
  BOOKING_CHANGED_EVENT: "lorafilm:booking-changed",
  cancelBooking: vi.fn(),
  getBookingHistory: vi.fn(),
}));

const activeBooking = {
  publicId: "11111111-1111-4111-8111-111111111111",
  bookingCode: "BK-HOME",
  status: "PENDING_PAYMENT",
  expiredAt: "2099-07-26T12:05:00Z",
  createdAt: "2026-07-26T12:00:00Z",
  snapshot: {
    movieTitle: "Thám Tử Lừng Danh Conan",
    cinemaName: "LoraFilm Landmark 81",
    showtimeStart: "2099-07-26T10:15:00Z",
    seats: [{ label: "H10" }, { label: "H11" }],
  },
};

describe("ActiveBookingRecoveryBanner", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      isAuthenticated: true,
      isInitializing: false,
      userRole: "CUSTOMER",
    });
    getBookingHistory.mockResolvedValue({
      content: [
        {
          ...activeBooking,
          publicId: "expired-booking",
          expiredAt: "2000-07-26T12:05:00Z",
        },
        activeBooking,
      ],
    });
    cancelBooking.mockResolvedValue({ status: "CANCELLED" });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows a prominent recovery card for an authenticated customer", async () => {
    render(
      <MemoryRouter>
        <ActiveBookingRecoveryBanner />
      </MemoryRouter>,
    );

    expect(await screen.findByText("Bạn đang giữ 2 ghế")).toBeInTheDocument();
    expect(screen.getByText("Thám Tử Lừng Danh Conan")).toBeInTheDocument();
    expect(screen.getByText(/H10, H11/)).toBeInTheDocument();
    expect(screen.getByText("Mã đơn BK-HOME")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /tiếp tục đặt vé/i }),
    ).toHaveAttribute(
      "href",
      "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
    );
    expect(
      screen.getByRole("button", { name: /hủy giữ ghế/i }),
    ).toBeInTheDocument();
  });

  it("cancels the hold directly from the homepage card", async () => {
    render(
      <MemoryRouter>
        <ActiveBookingRecoveryBanner />
      </MemoryRouter>,
    );

    fireEvent.click(
      await screen.findByRole("button", { name: /hủy giữ ghế/i }),
    );
    expect(
      screen.getByRole("dialog", { name: /hủy đơn đang giữ/i }),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận hủy" }));

    await waitFor(() => {
      expect(cancelBooking).toHaveBeenCalledWith(
        activeBooking.publicId,
        "Khách hàng chủ động hủy giữ ghế từ thẻ khôi phục",
      );
    });
  });

  it("does not call Booking API for an unauthenticated visitor", async () => {
    useAuth.mockReturnValue({
      isAuthenticated: false,
      isInitializing: false,
      userRole: null,
    });

    render(
      <MemoryRouter>
        <ActiveBookingRecoveryBanner />
      </MemoryRouter>,
    );

    await waitFor(() => expect(getBookingHistory).not.toHaveBeenCalled());
    expect(
      screen.queryByLabelText("Đơn đặt vé đang giữ ghế"),
    ).not.toBeInTheDocument();
  });
});
