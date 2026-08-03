import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BookingCheckoutPage from "./BookingCheckoutPage";
import { promotionDecision } from "../utils/promotionDecision";
import {
  cancelBooking,
  finalizeCheckout,
  getOrCreateScoreRedemptionKey,
  getBookingDetails,
  previewBookingPromotions,
} from "../services/bookingService";
import { getBookingFoodOrder, getConcessions } from "../services/foodService";
import {
  createPaymentHandoff,
  getOrCreatePaymentAttemptKey,
} from "../services/paymentHandoffService";
import scoreCustomerService from "@/features/score/customer/services/scoreCustomerService";
import customerPromotionService from "@/features/promotion/customer/services/customerPromotionService";

vi.mock("../services/bookingService", () => ({
  BOOKING_CHANGED_EVENT: "lorafilm:booking-changed",
  cancelBooking: vi.fn(),
  finalizeCheckout: vi.fn(),
  getOrCreateScoreRedemptionKey: vi.fn(),
  getBookingDetails: vi.fn(),
  previewBookingPromotions: vi.fn(),
}));

vi.mock("../services/foodService", () => ({
  addFoodItem: vi.fn(),
  getBookingFoodOrder: vi.fn(),
  getConcessions: vi.fn(),
  removeFoodItem: vi.fn(),
  updateFoodQuantity: vi.fn(),
}));

vi.mock("../services/paymentHandoffService", () => ({
  createPaymentHandoff: vi.fn(),
  getOrCreatePaymentAttemptKey: vi.fn(),
}));

vi.mock("@/features/score/customer/services/scoreCustomerService", () => ({
  default: {
    getScoreBalance: vi.fn(),
    redeemPreview: vi.fn(),
  },
}));

vi.mock(
  "@/features/promotion/customer/services/customerPromotionService",
  () => ({
    default: {
      getMyVouchers: vi.fn(),
      getPublicPromotions: vi.fn(),
      getSystemPromotions: vi.fn(),
      claimVoucher: vi.fn(),
    },
  }),
);

vi.mock("../components/BookingStepper", () => ({
  default: () => <div>Booking stepper</div>,
}));

describe("promotionDecision", () => {
  const walletVoucher = {
    publicId: "wallet-1",
    walletPublicId: "wallet-1",
    promotionPublicId: "voucher-1",
    source: "CUSTOMER_WALLET",
    promotionType: "VOUCHER",
    name: "WELCOME10K",
  };

  it("keeps a better automatic discount without consuming the voucher", () => {
    const decision = promotionDecision(
      {
        discountAmount: 21000,
        appliedPromotions: [
          {
            promotionPublicId: "auto-1",
            promotionType: "AUTO",
            name: "Giảm 10% ngày thường",
            discountAmount: 21000,
          },
        ],
      },
      { promotion: walletVoucher },
    );

    expect(decision.applied).toBe(false);
    expect(decision.notice?.variant).toBe("protected");
    expect(decision.notice?.message).toMatch(/voucher\/coupon chưa bị sử dụng/i);
  });

  it("reports the combined total when voucher and AUTO are both applied", () => {
    const decision = promotionDecision(
      {
        discountAmount: 51000,
        appliedPromotions: [
          {
            promotionPublicId: "auto-1",
            promotionType: "AUTO",
            name: "Giảm 10% ngày thường",
            discountAmount: 21000,
          },
          {
            promotionPublicId: "voucher-1",
            userPromotionPublicId: "wallet-1",
            promotionType: "VOUCHER",
            name: "Voucher gia đình",
            discountAmount: 30000,
          },
        ],
      },
      { promotion: walletVoucher },
    );

    expect(decision.applied).toBe(true);
    expect(decision.notice?.variant).toBe("stacked");
    expect(decision.notice?.message).toMatch(/tổng giảm 51\.000đ/i);
  });
});

describe("BookingCheckoutPage cancellation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBookingDetails.mockResolvedValue({
      publicId: "11111111-1111-4111-8111-111111111111",
      bookingCode: "BK-CHECKOUT",
      status: "PENDING_PAYMENT",
      paymentDeadline: "2099-07-26T12:05:00Z",
      ticketAmount: 285000,
      totalAmount: 335000,
      snapshot: {
        movieTitle: "Phim thử nghiệm",
        moviePosterUrl:
          "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
        originalTitle: "Test Movie",
        duration: 120,
        ageRating: "T13",
        cinemaName: "LoraFilm",
        auditoriumName: "Phòng 1",
        showtimeStart: "2099-07-26T13:00:00Z",
        seats: [
          { seatPublicId: "seat-d6", label: "D6", type: "VIP", price: 142500 },
          { seatPublicId: "seat-d7", label: "D7", type: "VIP", price: 142500 },
        ],
      },
    });
    getBookingFoodOrder.mockResolvedValue({
      items: [
        {
          id: 10,
          productName: "Bắp rang lớn",
          quantity: 1,
          finalAmount: 50000,
        },
      ],
      finalAmount: 50000,
    });
    getConcessions.mockResolvedValue([]);
    scoreCustomerService.getScoreBalance.mockResolvedValue({
      data: {
        currentPoints: 100,
        heldPoints: 0,
      },
    });
    customerPromotionService.getMyVouchers.mockResolvedValue({
      content: [],
      page: 0,
      totalElements: 0,
      totalPages: 0,
    });
    customerPromotionService.getPublicPromotions.mockResolvedValue({
      content: [],
      page: 0,
      totalElements: 0,
      totalPages: 0,
    });
    customerPromotionService.getSystemPromotions.mockResolvedValue({
      content: [],
      page: 0,
      totalElements: 0,
      totalPages: 0,
    });
    previewBookingPromotions.mockResolvedValueOnce({
      eligible: false,
      originalAmount: 335000,
      discountAmount: 0,
      finalAmount: 335000,
      currency: "VND",
      appliedPromotions: [],
      promotionEvaluations: [
        {
          promotionPublicId: "promotion-public-id",
          userPromotionPublicId: "voucher-public-id",
          promotionType: "VOUCHER",
          eligible: true,
          discountAmount: 50000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
      ],
      warnings: [],
    });
    previewBookingPromotions.mockResolvedValue({
      eligible: false,
      originalAmount: 335000,
      discountAmount: 0,
      finalAmount: 335000,
      currency: "VND",
      appliedPromotions: [],
      warnings: [],
    });
    finalizeCheckout.mockResolvedValue({});
    getOrCreateScoreRedemptionKey.mockReturnValue("score-redemption-key");
    cancelBooking.mockResolvedValue({ status: "CANCELLED" });
    getOrCreatePaymentAttemptKey.mockReturnValue("payment-attempt-key");
    createPaymentHandoff.mockResolvedValue({
      paymentPublicId: "22222222-2222-4222-8222-222222222222",
    });
  });

  it("uses a detailed modal instead of a browser confirm for cancellation", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    fireEvent.click(
      await screen.findByRole("button", { name: /hủy giao dịch/i }),
    );
    const cancellationDialog = screen.getByRole("dialog", {
      name: /xác nhận hủy giữ ghế/i,
    });
    expect(cancellationDialog).toBeInTheDocument();
    expect(screen.getByText(/ghế sẽ được trả lại ngay/i)).toBeInTheDocument();

    expect(
      within(cancellationDialog).queryByRole("textbox"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận hủy" }));

    await waitFor(() => {
      expect(cancelBooking).toHaveBeenCalledWith(
        "11111111-1111-4111-8111-111111111111",
        "Khách hàng chủ động hủy đặt chỗ tại checkout",
      );
    });
  });

  it("locks the Booking amount before handing public identity to Payment Service", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    fireEvent.click(
      await screen.findByRole("button", {
        name: /tiếp tục thanh toán/i,
      }),
    );
    const momoLogo = screen.getByRole("img", { name: "Logo MoMo" });
    expect(momoLogo).toHaveAttribute(
      "src",
      "https://upload.wikimedia.org/wikipedia/commons/a/a0/MoMo_Logo_App.svg",
    );
    fireEvent.error(momoLogo);
    expect(momoLogo).toHaveAttribute(
      "src",
      expect.stringContaining("res.cloudinary.com/dqc4hufot"),
    );
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(
      screen.getByRole("button", {
        name: /thanh toán qua vnpay/i,
      }),
    );

    await waitFor(() => {
      expect(finalizeCheckout).toHaveBeenCalledWith(
        "11111111-1111-4111-8111-111111111111",
        {
          scorePoints: 0,
          scoreIdempotencyKey: null,
          selectedUserPromotionPublicIds: [],
          selectedPromotionPublicIds: [],
          couponCode: null,
          paymentMethod: "VNPAY",
        },
      );
      expect(createPaymentHandoff).toHaveBeenCalledWith({
        bookingPublicId: "11111111-1111-4111-8111-111111111111",
        paymentMethod: "VNPAY",
        idempotencyKey: "payment-attempt-key",
      });
    });
    expect(finalizeCheckout.mock.invocationCallOrder[0]).toBeLessThan(
      createPaymentHandoff.mock.invocationCallOrder[0],
    );
    expect(screen.queryByText(/mô phỏng thanh toán/i)).not.toBeInTheDocument();
  });

  it("lets the customer apply Score points and sends the selection while finalizing", async () => {
    scoreCustomerService.redeemPreview.mockResolvedValue({
      data: {
        eligible: true,
        requestedPoints: 50,
        discountAmount: 50000,
        remainingAmount: 285000,
      },
    });

    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    fireEvent.change(
      await screen.findByRole("spinbutton", {
        name: /số điểm muốn dùng/i,
      }),
      { target: { value: "50" } },
    );
    fireEvent.click(screen.getByRole("button", { name: /^dùng điểm$/i }));

    expect(await screen.findByText(/đã chọn 50 điểm/i)).toBeInTheDocument();
    expect(scoreCustomerService.redeemPreview).toHaveBeenCalledWith({
      bookingPublicId: "11111111-1111-4111-8111-111111111111",
      points: 50,
    });

    fireEvent.click(
      screen.getByRole("button", {
        name: /tiếp tục thanh toán/i,
      }),
    );
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(
      screen.getByRole("button", {
        name: /thanh toán qua vnpay/i,
      }),
    );

    await waitFor(() => {
      expect(finalizeCheckout).toHaveBeenCalledWith(
        "11111111-1111-4111-8111-111111111111",
        {
          scorePoints: 50,
          scoreIdempotencyKey: "score-redemption-key",
          selectedUserPromotionPublicIds: [],
          selectedPromotionPublicIds: [],
          couponCode: null,
          paymentMethod: "VNPAY",
        },
      );
    });
  });

  it("revalidates the Booking before Payment and does not call MoMo for a cancelled order", async () => {
    getBookingDetails
      .mockResolvedValueOnce({
        publicId: "11111111-1111-4111-8111-111111111111",
        bookingCode: "BK-CHECKOUT",
        status: "PENDING_PAYMENT",
        paymentDeadline: "2099-07-26T12:05:00Z",
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: "Phim thử nghiệm",
          seats: [{ seatPublicId: "seat-d6", label: "D6", type: "VIP" }],
        },
      })
      .mockResolvedValueOnce({
        publicId: "11111111-1111-4111-8111-111111111111",
        bookingCode: "BK-CHECKOUT",
        status: "CANCELLED",
        paymentDeadline: "2099-07-26T12:05:00Z",
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: "Phim thử nghiệm",
          seats: [{ seatPublicId: "seat-d6", label: "D6", type: "VIP" }],
        },
      });

    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    fireEvent.click(
      await screen.findByRole("button", {
        name: /tiếp tục thanh toán/i,
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: /momo/i }));
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(
      screen.getByRole("button", {
        name: /thanh toán qua momo/i,
      }),
    );

    expect(await screen.findAllByText("Đơn đã được hủy")).not.toHaveLength(0);
    expect(screen.getAllByText(/ghế đã được trả lại/i)).not.toHaveLength(0);
    expect(finalizeCheckout).not.toHaveBeenCalled();
    expect(createPaymentHandoff).not.toHaveBeenCalled();
  });

  it("updates checkout immediately when the recovery banner cancels the same Booking", async () => {
    getBookingDetails
      .mockResolvedValueOnce({
        publicId: "11111111-1111-4111-8111-111111111111",
        bookingCode: "BK-CHECKOUT",
        status: "PENDING_PAYMENT",
        paymentDeadline: "2099-07-26T12:05:00Z",
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: "Phim thử nghiệm",
          seats: [{ seatPublicId: "seat-d6", label: "D6", type: "VIP" }],
        },
      })
      .mockResolvedValueOnce({
        publicId: "11111111-1111-4111-8111-111111111111",
        bookingCode: "BK-CHECKOUT",
        status: "CANCELLED",
        paymentDeadline: "2099-07-26T12:05:00Z",
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: "Phim thử nghiệm",
          seats: [{ seatPublicId: "seat-d6", label: "D6", type: "VIP" }],
        },
      });

    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    await screen.findByText("Phim thử nghiệm");
    window.dispatchEvent(
      new CustomEvent("lorafilm:booking-changed", {
        detail: {
          action: "CANCELLED",
          publicId: "11111111-1111-4111-8111-111111111111",
        },
      }),
    );

    expect(await screen.findAllByText("Đơn đã được hủy")).not.toHaveLength(0);
    expect(screen.getByText(/VNPay và MoMo đã được khóa/i)).toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: /thanh toán qua/i,
      }),
    ).not.toBeInTheDocument();
  });

  it("renders the authoritative movie, seats, food lines and price breakdown", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("Phim thử nghiệm")).toBeInTheDocument();
    expect(
      screen.getByRole("img", {
        name: "Áp phích phim Phim thử nghiệm",
      }),
    ).toHaveAttribute("src", expect.stringContaining("data:image/gif"));
    expect(screen.getByText(/D6 · VIP/)).toBeInTheDocument();
    expect(screen.getByText(/D7 · VIP/)).toBeInTheDocument();
    expect(screen.getByText("Tiền vé (2 ghế):")).toBeInTheDocument();
    expect(screen.getByText("Bắp rang lớn")).toBeInTheDocument();
    expect(screen.getAllByText("50.000đ")).toHaveLength(2);
    expect(screen.getByText("335.000đ")).toBeInTheDocument();
    expect(screen.queryByText(/Tiền vé \(0 ghế\)/)).not.toBeInTheDocument();
  });

  it("paginates the catalog and requests thumbnail-sized lazy images", async () => {
    getConcessions.mockResolvedValue(
      Array.from({ length: 13 }, (_, index) => ({
        id: index + 1,
        name: `Bắp nước ${index + 1}`,
        description: "Sản phẩm dùng khi xem phim",
        type: "FOOD",
        price: 39000,
        imageUrl: `https://images.unsplash.com/photo-${index + 1}`,
      })),
    );

    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    const firstThumbnail = await screen.findByRole("img", {
      name: "Bắp nước 1",
    });
    expect(firstThumbnail).toHaveAttribute("loading", "lazy");
    expect(firstThumbnail).toHaveAttribute("decoding", "async");
    expect(firstThumbnail.getAttribute("src")).toContain("w=192");
    expect(firstThumbnail.getAttribute("src")).toContain("h=192");
    expect(screen.queryByText("Bắp nước 13")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Xem tất cả" }));
    fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
    expect(await screen.findByText("Bắp nước 13")).toBeInTheDocument();
    expect(screen.queryByText("Bắp nước 1")).not.toBeInTheDocument();
  });

  it("loads and selects a system voucher alongside the customer wallet", async () => {
    customerPromotionService.getMyVouchers.mockResolvedValue({
      content: [
        {
          publicId: "voucher-public-id",
          walletPublicId: "voucher-public-id",
          promotionPublicId: "promotion-public-id",
          source: "CUSTOMER_WALLET",
          promotionType: "VOUCHER",
          code: "WELCOME50",
          name: "Voucher chào mừng",
          status: "AVAILABLE",
          voucherType: "FIXED_AMOUNT",
          faceValue: 50000,
          minimumOrderAmount: 100000,
          validFrom: "2020-01-01T00:00:00Z",
          validTo: "2099-12-31T23:59:59Z",
          usageCount: 0,
          maxUsage: 1,
          conditionsJson: { minimumOrderAmount: 100000 },
          actionsJson: { discountType: "FIXED_AMOUNT", discountValue: 50000 },
        },
        {
          publicId: "voucher-public-id-2",
          walletPublicId: "voucher-public-id-2",
          promotionPublicId: "promotion-public-id-2",
          source: "CUSTOMER_WALLET",
          promotionType: "VOUCHER",
          code: "STACK20",
          name: "Voucher cộng dồn",
          status: "AVAILABLE",
          validFrom: "2020-01-01T00:00:00Z",
          validTo: "2099-12-31T23:59:59Z",
          usageCount: 0,
          maxUsage: 1,
          conditionsJson: {},
          actionsJson: { discountType: "FIXED_AMOUNT", discountValue: 20000 },
        },
      ],
      page: 0,
      totalElements: 2,
      totalPages: 1,
    });
    customerPromotionService.getSystemPromotions.mockResolvedValue({
      content: [
        {
          publicId: "system-promotion-id",
          promotionPublicId: "system-promotion-id",
          source: "SYSTEM_AUTO",
          ownershipType: "SYSTEM",
          promotionType: "AUTO",
          name: "Voucher hệ thống 30K",
          status: "ACTIVE",
          validFrom: "2020-01-01T00:00:00Z",
          validTo: "2099-12-31T23:59:59Z",
          conditionsJson: {},
          actionsJson: {
            discountType: "FIXED_AMOUNT",
            discountValue: 30000,
          },
        },
      ],
      page: 0,
      totalElements: 1,
      totalPages: 1,
    });

    previewBookingPromotions.mockReset();
    previewBookingPromotions.mockResolvedValue({
      eligible: false,
      originalAmount: 335000,
      discountAmount: 0,
      finalAmount: 335000,
      currency: "VND",
      appliedPromotions: [],
      promotionEvaluations: [
        {
          promotionPublicId: "promotion-public-id",
          userPromotionPublicId: "voucher-public-id",
          promotionType: "VOUCHER",
          eligible: true,
          discountAmount: 50000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
        {
          promotionPublicId: "promotion-public-id-2",
          userPromotionPublicId: "voucher-public-id-2",
          promotionType: "VOUCHER",
          eligible: true,
          discountAmount: 20000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
        {
          promotionPublicId: "system-promotion-id",
          promotionType: "AUTO",
          eligible: true,
          discountAmount: 30000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
      ],
      warnings: [],
    });

    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(
        screen.getByText((text) =>
          /^3\s+/.test(text) &&
          (text.includes("ưu đãi") || text.includes("Æ°u Ä‘Ã£i")),
        ),
      ).toBeInTheDocument();
    });
    expect(customerPromotionService.getMyVouchers).toHaveBeenCalledWith({
      page: 0,
      size: 100,
      sort: "validTo,asc",
      status: "ALL",
    });
    expect(customerPromotionService.getPublicPromotions).toHaveBeenCalledWith({
      page: 0,
      size: 100,
      sort: "priority,asc",
    });
    expect(customerPromotionService.getSystemPromotions).toHaveBeenCalledWith({
      page: 0,
      size: 100,
      sort: "priority,asc",
    });

    fireEvent.click(screen.getByRole("button", { name: /chọn ưu đãi/i }));
    const dialog = screen.getByRole("dialog", { name: /chọn ưu đãi/i });
    expect(within(dialog).getByText("Voucher chào mừng")).toBeInTheDocument();
    fireEvent.click(
      within(dialog).getByRole("button", { name: /voucher hệ thống/i }),
    );
    expect(
      within(dialog).getByText("Voucher hệ thống 30K"),
    ).toBeInTheDocument();
    const systemVoucher = within(dialog)
      .getByText("Voucher hệ thống 30K")
      .closest("article");
    previewBookingPromotions.mockResolvedValueOnce({
      eligible: false,
      originalAmount: 335000,
      discountAmount: 0,
      finalAmount: 335000,
      currency: "VND",
      appliedPromotions: [],
      promotionEvaluations: [
        {
          promotionPublicId: "system-promotion-id",
          promotionType: "AUTO",
          eligible: false,
          discountAmount: 0,
          reasonCode: "PROMOTION_CONDITION_NOT_MET",
          reason: "Voucher chưa tạo ra mức giảm cho đơn hàng hiện tại",
        },
      ],
      warnings: [],
    });
    fireEvent.click(
      within(systemVoucher).getByRole("button", { name: /^chọn$/i }),
    );
    expect(
      await within(dialog).findByText(/chưa tạo ra mức giảm/i),
    ).toBeInTheDocument();

    previewBookingPromotions.mockResolvedValueOnce({
      eligible: true,
      originalAmount: 335000,
      discountAmount: 30000,
      finalAmount: 305000,
      currency: "VND",
      appliedPromotions: [
        {
          promotionPublicId: "system-promotion-id",
          userPromotionPublicId: null,
          promotionType: "AUTO",
          name: "Voucher hệ thống 30K",
          discountAmount: 30000,
        },
      ],
      promotionEvaluations: [
        {
          promotionPublicId: "promotion-public-id",
          userPromotionPublicId: "voucher-public-id",
          eligible: true,
          discountAmount: 50000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
        {
          promotionPublicId: "promotion-public-id-2",
          userPromotionPublicId: "voucher-public-id-2",
          eligible: true,
          discountAmount: 20000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
        {
          promotionPublicId: "system-promotion-id",
          promotionType: "AUTO",
          eligible: true,
          discountAmount: 30000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        },
      ],
      warnings: [],
    });
    fireEvent.click(
      within(systemVoucher).getByRole("button", { name: /^chọn$/i }),
    );

    expect(
      await screen.findByText(/engine xác nhận giảm/i),
    ).toBeInTheDocument();
    expect(previewBookingPromotions).toHaveBeenLastCalledWith(
      "11111111-1111-4111-8111-111111111111",
      {
        selectedUserPromotionPublicIds: [],
        selectedPromotionPublicIds: ["system-promotion-id"],
        evaluationUserPromotionPublicIds: [
          "voucher-public-id",
          "voucher-public-id-2",
        ],
        evaluationPromotionPublicIds: ["system-promotion-id"],
        paymentMethod: "VNPAY",
      },
    );

    expect(finalizeCheckout).not.toHaveBeenCalled();
  });

  it("does not rerender concession cards when the hold countdown ticks", async () => {
    let nameReads = 0;
    const concession = {
      id: 99,
      description: "Sản phẩm kiểm tra render",
      type: "FOOD",
      price: 39000,
      imageUrl: "https://images.unsplash.com/photo-render-test",
    };
    Object.defineProperty(concession, "name", {
      enumerable: true,
      get: () => {
        nameReads += 1;
        return "Bắp tối ưu render";
      },
    });
    getConcessions.mockResolvedValue([concession]);

    render(
      <MemoryRouter
        initialEntries={[
          "/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111",
        ]}
      >
        <BookingCheckoutPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("Bắp tối ưu render")).toBeInTheDocument();
    const readsAfterInitialRender = nameReads;

    await act(async () => {
      await new Promise((resolve) => window.setTimeout(resolve, 1100));
    });

    expect(nameReads).toBe(readsAfterInitialRender);
  });
});
