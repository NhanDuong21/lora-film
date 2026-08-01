import { fireEvent, render, screen, within } from "@testing-library/react";
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
  selectedPromotionIds: [],
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
      screen.getByText(/chưa đủ giá trị đơn hàng tối thiểu/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /không khả dụng/i }),
    ).toBeDisabled();
  });

  it("shows system vouchers in a separate selectable tab", () => {
    const onSelect = vi.fn();
    const onClose = vi.fn();
    render(
      <PromotionChooser
        {...defaultProps}
        onSelect={onSelect}
        onClose={onClose}
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

    fireEvent.click(screen.getByRole("button", { name: /voucher hệ thống/i }));

    expect(screen.getByText("Voucher hệ thống 50K")).toBeInTheDocument();
    expect(screen.queryByText("Voucher ví")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Chọn" }));

    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ publicId: "system-voucher" }),
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it("keeps stackable vouchers selectable and dims non-stackable vouchers", () => {
    const onSelect = vi.fn();
    const promotions = [
      voucher({
        publicId: "stackable-1",
        name: "Cộng dồn một",
        stackable: true,
      }),
      voucher({
        publicId: "stackable-2",
        name: "Cộng dồn hai",
        stackable: true,
      }),
      voucher({
        publicId: "exclusive-1",
        name: "Dùng riêng",
        stackable: false,
      }),
    ];
    render(
      <PromotionChooser
        {...defaultProps}
        onSelect={onSelect}
        selectedPromotionIds={["stackable-1"]}
        vouchers={promotions}
        promotionEvaluations={promotions.map((promotion) => ({
          promotionPublicId: promotion.publicId,
          eligible: true,
          discountAmount: 20000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        }))}
      />,
    );

    const nextStackable = screen.getByText("Cộng dồn hai").closest("article");
    const exclusive = screen.getByText("Dùng riêng").closest("article");
    expect(within(nextStackable).getByRole("button", { name: "Chọn" }))
      .toBeEnabled();
    expect(exclusive).toHaveAttribute("aria-disabled", "true");
    expect(
      within(exclusive).getByRole("button", { name: "Không cộng dồn" }),
    ).toBeDisabled();

    fireEvent.click(
      within(nextStackable).getByRole("button", { name: "Chọn" }),
    );
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ publicId: "stackable-2" }),
    );
  });

  it("dims every other voucher when a non-stackable voucher is selected", () => {
    const promotions = [
      voucher({
        publicId: "exclusive-1",
        name: "Dùng riêng",
        stackable: false,
      }),
      voucher({
        publicId: "stackable-1",
        name: "Có thể cộng dồn",
        stackable: true,
      }),
    ];
    render(
      <PromotionChooser
        {...defaultProps}
        selectedPromotionIds={["exclusive-1"]}
        vouchers={promotions}
        promotionEvaluations={promotions.map((promotion) => ({
          promotionPublicId: promotion.publicId,
          eligible: true,
          discountAmount: 20000,
          reasonCode: "ELIGIBLE",
          reason: "Có thể sử dụng cho đơn hiện tại",
        }))}
      />,
    );

    const selected = screen.getByText("Dùng riêng").closest("article");
    const blocked = screen.getByText("Có thể cộng dồn").closest("article");
    expect(
      within(selected).getByRole("button", { name: "Bỏ chọn" }),
    ).toBeEnabled();
    expect(blocked).toHaveAttribute("aria-disabled", "true");
    expect(
      within(blocked).getByRole("button", { name: "Không cộng dồn" }),
    ).toBeDisabled();
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
      within(bestVoucher).getByRole("button", { name: "Tự động áp dụng" }),
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
});
