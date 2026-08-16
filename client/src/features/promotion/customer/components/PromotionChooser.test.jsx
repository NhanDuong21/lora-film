import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  getCinemaBySlug,
  getMovieById,
  getMovies,
} from "@/features/catalog/customer/services/movieService";
import PromotionChooser from "./PromotionChooser";

vi.mock("@/features/catalog/customer/services/movieService", () => ({
  getCinemaBySlug: vi.fn(),
  getMovieById: vi.fn(),
  getMovies: vi.fn(),
}));

const defaultProps = {
  open: true,
  bookingAmount: 200000,
  selectedPromotionId: "",
  onSelect: vi.fn(),
  onClear: vi.fn(),
  onClose: vi.fn(),
  onRefresh: vi.fn(),
};

const voucher = (overrides) => ({
  publicId: "voucher-1",
  name: "Voucher 20K",
  code: "SAVE20",
  status: "ACTIVE",
  validTo: "2099-12-31T23:59:59Z",
  conditionsJson: "{}",
  actionsJson: JSON.stringify([
    {
      discountType: "FIXED_AMOUNT",
      discountValue: 20000,
    },
  ]),
  ...overrides,
});

describe("PromotionChooser", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getMovieById.mockImplementation((id) =>
      Promise.resolve({ publicId: id, title: `Phim ${id}` }),
    );
    getMovies.mockResolvedValue({ content: [] });
    getCinemaBySlug.mockImplementation((id) =>
      Promise.resolve({ publicId: id, name: `Rạp ${id}` }),
    );
  });

  it("shows an empty wallet state without inventing promotions", () => {
    render(<PromotionChooser {...defaultProps} vouchers={[]} />);

    expect(screen.getByText(/chưa có voucher ví/i)).toBeInTheDocument();
    expect(screen.getByText(/voucher đã nhận/i)).toBeInTheDocument();
  });

  it("disables a voucher when the booking is below its minimum amount", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        bookingAmount={100000}
        vouchers={[
          voucher({
            minimumOrderAmount: 300000,
            conditionsJson: JSON.stringify({ minimumOrderAmount: 300000 }),
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-1",
            eligible: false,
            reasonCode: "MINIMUM_ORDER_NOT_MET",
            reason: "Chưa đủ giá trị đơn hàng tối thiểu",
          },
        ]}
      />,
    );

    expect(
      screen.getByText(/cần thêm .* để sử dụng voucher này/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /không khả dụng/i }),
    ).toBeDisabled();
  });

  it("explains conditional AUTO stacking for a stackable voucher", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        vouchers={[voucher({ stackable: true })]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-1",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
        ]}
      />,
    );

    expect(
      screen.getByText(/cộng dồn với 1 ưu đãi auto khi chiến dịch cho phép/i),
    ).toBeInTheDocument();
  });

  it("does not expose AUTO promotions as customer-selectable vouchers", () => {
    const onSelect = vi.fn();
    render(
      <PromotionChooser
        {...defaultProps}
        onSelect={onSelect}
        backendAppliedIds={["system-voucher"]}
        vouchers={[
          voucher({ publicId: "wallet-voucher", name: "Voucher ví" }),
          voucher({
            publicId: "system-voucher",
            promotionPublicId: "system-voucher",
            promotionType: "AUTO",
            source: "SYSTEM_AUTO",
            name: "Voucher hệ thống 50K",
            actionsJson: JSON.stringify([
              { discountType: "FIXED_AMOUNT", discountValue: 50000 },
            ]),
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "wallet-voucher",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
          {
            promotionPublicId: "system-voucher",
            eligible: true,
            discountAmount: 50000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
        ]}
      />,
    );

    expect(screen.getByText("Voucher ví")).toBeInTheDocument();
    expect(screen.queryByText("Voucher hệ thống 50K")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /voucher hệ thống/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText(/một số ưu đãi tự động có thể được cộng thêm/i),
    ).toBeInTheDocument();
    expect(onSelect).not.toHaveBeenCalled();
  });

  it("dims condition-mismatched vouchers and explains the mismatch", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        bookingContext={{
          moviePublicId: "movie-current",
          movieTitle: "Phim hiện tại",
        }}
        vouchers={[
          voucher({
            conditionsJson: JSON.stringify({
              moviePublicIds: ["movie-other"],
            }),
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-1",
            eligible: false,
            reasonCode: "MOVIE_NOT_APPLICABLE",
            reason: "Không áp dụng cho phim hiện tại",
          },
        ]}
      />,
    );

    expect(
      screen.getByText(/không áp dụng cho phim Phim hiện tại/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /không khả dụng/i }),
    ).toBeDisabled();
  });

  it("shows configured movie and cinema names instead of the current checkout scope", async () => {
    getMovieById.mockResolvedValueOnce({
      publicId: "movie-other",
      title: "12 Người Đàn Ông Giận Dữ",
    });
    getCinemaBySlug.mockResolvedValueOnce({
      publicId: "cinema-other",
      name: "LoraFilm Gò Vấp",
    });

    render(
      <PromotionChooser
        {...defaultProps}
        bookingContext={{
          moviePublicId: "movie-current",
          movieTitle: "Minions & Quái Vật",
          cinemaPublicId: "cinema-current",
          cinemaName: "LoraFilm Tân Phú",
        }}
        vouchers={[
          voucher({
            conditionsJson: JSON.stringify({
              moviePublicIds: ["movie-other"],
              cinemaPublicIds: ["cinema-other"],
            }),
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-1",
            eligible: false,
            reasonCode: "MOVIE_NOT_APPLICABLE",
            reason: "Không áp dụng cho phim hiện tại",
          },
        ]}
      />,
    );

    expect(
      await screen.findByText(/Chỉ áp dụng cho phim 12 Người Đàn Ông Giận Dữ/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Chỉ áp dụng cho rạp LoraFilm Gò Vấp/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/Chỉ áp dụng cho phim được cấu hình; phim hiện tại/i),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Chỉ áp dụng cho rạp được cấu hình; rạp hiện tại/i),
    ).not.toBeInTheDocument();
  });

  it("falls back to the movie list and never renders raw UUIDs as configured names", async () => {
    getMovieById.mockRejectedValueOnce(new Error("hidden"));
    getMovies.mockResolvedValueOnce({
      content: [
        {
          publicId: "9c6b383e-3a56-43a5-a28b-19b82c9f66a9",
          title: "12 Người Đàn Ông Giận Dữ",
        },
      ],
    });

    render(
      <PromotionChooser
        {...defaultProps}
        vouchers={[
          voucher({
            conditionsJson: JSON.stringify({
              moviePublicIds: ["9c6b383e-3a56-43a5-a28b-19b82c9f66a9"],
            }),
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-1",
            eligible: false,
            reasonCode: "MOVIE_NOT_APPLICABLE",
            reason: "Không áp dụng cho phim hiện tại",
          },
        ]}
      />,
    );

    expect(
      await screen.findByText(/Chỉ áp dụng cho phim 12 Người Đàn Ông Giận Dữ/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/9c6b383e-3a56-43a5-a28b-19b82c9f66a9/i),
    ).not.toBeInTheDocument();
  });

  it("marks only promotions confirmed by the backend preview", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        backendAppliedIds={["voucher-50"]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-20",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
          {
            promotionPublicId: "voucher-50",
            eligible: true,
            discountAmount: 50000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
        ]}
        vouchers={[
          voucher({ publicId: "voucher-20", name: "Voucher 20K" }),
          voucher({
            publicId: "voucher-50",
            name: "Voucher 50K",
            code: "SAVE50",
            actionsJson: JSON.stringify([
              {
                discountType: "FIXED_AMOUNT",
                discountValue: 50000,
              },
            ]),
          }),
        ]}
      />,
    );

    const bestVoucher = screen.getByText("Voucher 50K").closest("article");
    const lowerVoucher = screen.getByText("Voucher 20K").closest("article");

    expect(
      within(bestVoucher).getByRole("button", { name: "Chọn cố định" }),
    ).toBeInTheDocument();
    expect(
      within(lowerVoucher).queryByText("Đang áp dụng"),
    ).not.toBeInTheDocument();
    expect(
      within(bestVoucher).queryByText(/ước tính giảm/i),
    ).not.toBeInTheDocument();
  });

  it("hides coupons and recommends the highest-value wallet voucher first", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        vouchers={[
          voucher({ publicId: "voucher-low", name: "Voucher 20K" }),
          voucher({
            publicId: "voucher-high",
            name: "Voucher 50K",
            actionsJson: JSON.stringify([
              { discountType: "FIXED_AMOUNT", discountValue: 50000 },
            ]),
          }),
          voucher({
            publicId: "coupon-hidden",
            promotionType: "COUPON",
            name: "Private coupon",
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "voucher-low",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
          {
            promotionPublicId: "voucher-high",
            eligible: true,
            discountAmount: 50000,
            reasonCode: "ELIGIBLE",
            reason: "Có thể sử dụng cho đơn hiện tại",
          },
        ]}
      />,
    );

    const highestVoucher = screen.getByText("Voucher 50K").closest("article");
    expect(within(highestVoucher).getByText("Đề xuất")).toBeInTheDocument();
    expect(screen.queryByText("Private coupon")).not.toBeInTheDocument();
  });

  it("hides used and exhausted wallet promotions", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        vouchers={[
          voucher({
            publicId: "used-wallet",
            name: "Voucher da dung",
            source: "CUSTOMER_WALLET",
            walletPublicId: "used-wallet",
            status: "USED",
            usageCount: 1,
            maxUsage: 1,
          }),
          voucher({
            publicId: "exhausted-wallet",
            name: "Voucher het luot",
            source: "CUSTOMER_WALLET",
            walletPublicId: "exhausted-wallet",
            status: "AVAILABLE",
            usageCount: 2,
            maxUsage: 2,
          }),
          voucher({
            publicId: "available-wallet",
            name: "Voucher con luot",
            source: "CUSTOMER_WALLET",
            walletPublicId: "available-wallet",
            status: "AVAILABLE",
            usageCount: 1,
            maxUsage: 3,
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "available-wallet",
            userPromotionPublicId: "available-wallet",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Co the su dung cho don hien tai",
          },
        ]}
      />,
    );

    expect(screen.getByText("Voucher con luot")).toBeInTheDocument();
    expect(screen.queryByText("Voucher da dung")).not.toBeInTheDocument();
    expect(screen.queryByText("Voucher het luot")).not.toBeInTheDocument();
  });

  it("hides AUTO and terminal promotions but keeps condition-mismatched vouchers", () => {
    render(
      <PromotionChooser
        {...defaultProps}
        vouchers={[
          voucher({ publicId: "used-event", name: "Voucher su kien da dung" }),
          voucher({
            publicId: "minimum-event",
            name: "Voucher chua du don",
          }),
          voucher({ publicId: "eligible-event", name: "Voucher dung duoc" }),
          voucher({
            publicId: "used-system",
            promotionType: "AUTO",
            source: "SYSTEM_AUTO",
            name: "Voucher he thong da dung",
          }),
          voucher({
            publicId: "eligible-system",
            promotionType: "AUTO",
            source: "SYSTEM_AUTO",
            name: "Voucher he thong dung duoc",
          }),
        ]}
        promotionEvaluations={[
          {
            promotionPublicId: "used-event",
            eligible: false,
            reasonCode: "USAGE_LIMIT_REACHED",
            reason: "Voucher da het luot su dung",
          },
          {
            promotionPublicId: "minimum-event",
            eligible: false,
            reasonCode: "MINIMUM_ORDER_NOT_MET",
            reason: "Chua du gia tri don hang toi thieu",
          },
          {
            promotionPublicId: "eligible-event",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Co the su dung cho don hien tai",
          },
          {
            promotionPublicId: "used-system",
            eligible: false,
            reasonCode: "USAGE_LIMIT_REACHED",
            reason: "Voucher da het luot su dung",
          },
          {
            promotionPublicId: "eligible-system",
            eligible: true,
            discountAmount: 20000,
            reasonCode: "ELIGIBLE",
            reason: "Co the su dung cho don hien tai",
          },
        ]}
      />,
    );

    expect(
      screen.queryByText("Voucher su kien da dung"),
    ).not.toBeInTheDocument();
    expect(screen.getByText("Voucher chua du don")).toBeInTheDocument();
    expect(screen.getByText("Voucher dung duoc")).toBeInTheDocument();

    expect(
      screen.queryByText("Voucher he thong da dung"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("Voucher he thong dung duoc"),
    ).not.toBeInTheDocument();
  });
});
