import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminPromotionCenterPage from "./AdminPromotionCenterPage";
import adminPromotionService from "../services/adminPromotionService";
import adminCinemaService from "@/features/facilities/admin/services/adminCinemaService";

const emptyPage = {
  content: [],
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

const authState = vi.hoisted(() => ({
  user: {
    role: "ADMIN",
    permissions: [
      "PROMOTION_VIEW",
      "PROMOTION_AUTHOR",
      "PROMOTION_AUDIT_VIEW",
    ],
    cinemaPublicIds: [],
  },
}));

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({ user: authState.user }),
}));

vi.mock("../services/adminPromotionService", () => ({
  default: {
    searchCampaigns: vi.fn(),
    searchPromotions: vi.fn(),
    createCampaign: vi.fn(),
    createPromotion: vi.fn(),
    transitionCampaign: vi.fn(),
  },
}));

vi.mock("@/features/internal-staff/admin/services/userAdminService", () => ({
  getCustomers: vi.fn(),
}));
vi.mock("@/features/catalog/admin/services/adminMovieService", () => ({
  default: { getMovies: vi.fn() },
}));
vi.mock("@/features/facilities/admin/services/adminCinemaService", () => ({
  default: { getCinemas: vi.fn() },
}));

const openWizard = () => {
  fireEvent.click(screen.getByRole("button", { name: "Tạo chương trình" }));
};

const goToScopeStep = async (
  deliveryName,
  name = "Landmark ưu đãi",
  fixedAmount = null,
) => {
  openWizard();
  fireEvent.click(screen.getByRole("button", { name: deliveryName }));
  fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/ }));
  fireEvent.change(
    screen.getByPlaceholderText("VD: Chào thành viên mới - giảm 10%"),
    { target: { value: name } },
  );
  if (fixedAmount) {
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "FIXED_AMOUNT" },
    });
    fireEvent.change(screen.getAllByRole("spinbutton")[0], {
      target: { value: String(fixedAmount) },
    });
  }
  fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/ }));
  await screen.findByText("Rạp áp dụng");
};

describe("AdminPromotionCenterPage", () => {
  beforeEach(() => {
    authState.user = {
      role: "ADMIN",
      permissions: [
        "PROMOTION_VIEW",
        "PROMOTION_AUTHOR",
        "PROMOTION_AUDIT_VIEW",
      ],
      cinemaPublicIds: [],
    };
    vi.clearAllMocks();
    adminPromotionService.searchCampaigns.mockResolvedValue(emptyPage);
    adminPromotionService.searchPromotions.mockResolvedValue(emptyPage);
    adminPromotionService.createCampaign.mockResolvedValue({
      publicId: "campaign-created",
      version: 1,
    });
    adminPromotionService.createPromotion.mockResolvedValue({
      publicId: "promotion-created",
    });
    adminPromotionService.transitionCampaign.mockResolvedValue({});
    adminCinemaService.getCinemas.mockResolvedValue(emptyPage);
  });

  it("uses task-first navigation and one human primary action per campaign", async () => {
    adminPromotionService.searchCampaigns.mockResolvedValue({
      ...emptyPage,
      content: [
        {
          publicId: "campaign-1",
          code: "SUMMER-2026",
          name: "Landmark tháng 8",
          status: "DRAFT",
          approvalStatus: "DRAFT",
          legalStatus: "PENDING",
          promotionCount: 1,
          startAt: "2026-08-01T00:00:00Z",
          endAt: "2026-09-01T00:00:00Z",
          budgetAmount: 1000000,
          allowedActions: ["VIEW", "SUBMIT", "EDIT", "DELETE"],
        },
      ],
      totalElements: 1,
      totalPages: 1,
    });

    render(<AdminPromotionCenterPage />);

    const navigation = screen.getByRole("navigation", {
      name: "Khu vực quản lý khuyến mãi",
    });
    expect(within(navigation).getByRole("button", { name: "Việc cần làm" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Chương trình khuyến mãi" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Phân phối cho khách" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Sự cố & đối soát" })).toBeInTheDocument();

    await screen.findByText("Landmark tháng 8");
    expect(screen.getByText("Đang soạn chương trình")).toBeInTheDocument();
    expect(screen.getByText("Bước 4/6")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /Tiếp tục thiết lập/ })).toHaveLength(1);
    expect(screen.queryByText("SUMMER-2026")).not.toBeInTheDocument();
  });

  it("starts authoring from four customer delivery choices without technical fields", async () => {
    render(<AdminPromotionCenterPage />);
    await waitFor(() => expect(adminPromotionService.searchCampaigns).toHaveBeenCalled());

    openWizard();

    expect(screen.getByText("Khách hàng sẽ nhận ưu đãi này như thế nào?")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Tự động giảm tại thanh toán/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Khách tự nhận voucher/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Cấp voucher vào ví khách hàng/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Gửi mã ưu đãi riêng/ })).toBeInTheDocument();
    expect(screen.queryByText("Mã nội bộ")).not.toBeInTheDocument();
    expect(screen.queryByText("Thứ tự ưu tiên")).not.toBeInTheDocument();
  });

  it("keeps distribution focused on private voucher and personal coupon issuance", async () => {
    render(<AdminPromotionCenterPage />);
    fireEvent.click(screen.getByRole("button", { name: "Phân phối cho khách" }));

    await waitFor(() =>
      expect(adminPromotionService.searchPromotions).toHaveBeenCalledWith(
        expect.objectContaining({ type: "VOUCHER" }),
      ),
    );
    expect(screen.getByRole("button", { name: /Voucher cấp vào ví/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Mã ưu đãi cá nhân/ })).toBeInTheDocument();
    expect(screen.queryByText("Ưu đãi tự động")).not.toBeInTheDocument();
    expect(screen.queryByText("Khách tự nhận voucher")).not.toBeInTheDocument();
  });

  it("keeps authoring off generic managers and limits an authorized manager to assigned cinemas", async () => {
    authState.user = {
      role: "MANAGER",
      permissions: ["PROMOTION_VIEW", "PROMOTION_OPERATE", "PROMOTION_AUDIT_VIEW"],
      cinemaPublicIds: ["cinema-a"],
    };
    const { unmount } = render(<AdminPromotionCenterPage />);
    await waitFor(() => expect(adminPromotionService.searchCampaigns).toHaveBeenCalled());
    expect(screen.queryByRole("button", { name: /Tạo chương trình/ })).not.toBeInTheDocument();
    unmount();

    authState.user = {
      ...authState.user,
      permissions: [...authState.user.permissions, "PROMOTION_AUTHOR"],
    };
    adminCinemaService.getCinemas.mockResolvedValue({
      ...emptyPage,
      content: [
        { publicId: "cinema-a", name: "LoraFilm Landmark 81" },
        { publicId: "cinema-b", name: "Outside cinema" },
      ],
    });
    render(<AdminPromotionCenterPage />);
    await waitFor(() => expect(screen.getByRole("button", { name: "Tạo chương trình" })).toBeInTheDocument());

    await goToScopeStep(/Tự động giảm tại thanh toán/);

    expect(await screen.findByRole("button", { name: /LoraFilm Landmark 81/ })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Outside cinema/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /LoraFilm Landmark 81/ }));
    expect(screen.getByText(/Chương trình áp dụng tại:/)).toHaveTextContent("LoraFilm Landmark 81");
  });

  it.each([
    ["Tự động giảm tại thanh toán", "AUTO", false],
    ["Khách tự nhận voucher", "VOUCHER", true],
    ["Cấp voucher vào ví khách hàng", "VOUCHER", false],
  ])(
    "creates and submits the %s journey with the correct delivery model",
    async (deliveryName, expectedType, expectedPublicVisible) => {
      render(<AdminPromotionCenterPage />);
      await waitFor(() => expect(adminPromotionService.searchCampaigns).toHaveBeenCalled());

      await goToScopeStep(new RegExp(deliveryName), "Landmark ưu đãi", 50000);
      fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/ }));
      fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/ }));
      fireEvent.click(screen.getByRole("button", { name: /Tiếp tục/ }));
      fireEvent.click(screen.getByRole("button", { name: "Tạo và gửi duyệt" }));

      await waitFor(() => expect(adminPromotionService.createPromotion).toHaveBeenCalled());
      const promotionPayload = adminPromotionService.createPromotion.mock.calls.at(-1)[0];
      expect(promotionPayload).toEqual(
        expect.objectContaining({
          campaignPublicId: "campaign-created",
          promotionType: expectedType,
          publicVisible: expectedPublicVisible,
          actionsJson: expect.objectContaining({
            discountType: "FIXED_AMOUNT",
            discountValue: 50000,
          }),
        }),
      );
      expect(adminPromotionService.transitionCampaign).toHaveBeenCalledWith(
        "campaign-created",
        "SUBMIT",
        "Gửi duyệt từ hướng dẫn tạo chương trình",
        1,
      );
    },
  );
});
