import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminPromotionCenterPage from "./AdminPromotionCenterPage";
import adminPromotionService from "../services/adminPromotionService";
import adminCinemaService from "@/features/facilities/admin/services/adminCinemaService";

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
  useAuth: () => ({
    user: authState.user,
  }),
}));

vi.mock("../services/adminPromotionService", () => ({
  default: {
    searchCampaigns: vi.fn(),
  },
}));

vi.mock("@/features/internal-staff/admin/services/userAdminService", () => ({
  getCustomers: vi.fn(),
}));
vi.mock("@/features/catalog/admin/services/adminMovieService", () => ({
  default: {},
}));
vi.mock("@/features/facilities/admin/services/adminCinemaService", () => ({
  default: {
    getCinemas: vi.fn(),
  },
}));

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
  });

  it("renders the campaign-first navigation and only server-authorized actions", async () => {
    adminPromotionService.searchCampaigns.mockResolvedValue({
      content: [
        {
          publicId: "campaign-1",
          code: "SUMMER-2026",
          name: "Summer 2026",
          status: "ACTIVE",
          approvalStatus: "APPROVED",
          legalStatus: "PASSED",
          startAt: "2026-08-01T00:00:00Z",
          endAt: "2026-09-01T00:00:00Z",
          budgetAmount: 1000000,
          budgetUsed: 100000,
          budgetReserved: 20000,
          redemptionCount: 4,
          maxRedemptions: 100,
          allowedActions: ["VIEW", "PAUSE"],
        },
      ],
      page: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    render(<AdminPromotionCenterPage />);

    const navigation = screen.getByRole("navigation", {
      name: "Khu vực Promotion Center",
    });
    expect(within(navigation).getByRole("button", { name: "Tổng quan" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Chiến dịch" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Cấp phát" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Vận hành" })).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText("Summer 2026")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Xem chi tiết" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tạm dừng" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Sửa" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Phê duyệt" })).not.toBeInTheDocument();
  });

  it("derives a non-actionable legal label for a draft campaign", async () => {
    adminPromotionService.searchCampaigns.mockResolvedValue({
      content: [
        {
          publicId: "campaign-draft",
          code: "DRAFT-2026",
          name: "Draft campaign",
          status: "DRAFT",
          approvalStatus: "DRAFT",
          legalStatus: "PENDING",
          startAt: "2026-08-01T00:00:00Z",
          endAt: "2026-09-01T00:00:00Z",
          budgetAmount: 1000000,
          allowedActions: ["VIEW"],
        },
      ],
      page: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    render(<AdminPromotionCenterPage />);

    await waitFor(() =>
      expect(screen.getByText("Draft campaign")).toBeInTheDocument(),
    );
    expect(screen.getByText("Chưa yêu cầu")).toBeInTheDocument();
    expect(screen.queryByText("Chờ duyệt", { selector: "span" }))
      .not.toBeInTheDocument();
  });

  it("keeps author CTA off generic managers and scopes an authorized manager explicitly", async () => {
    authState.user = {
      role: "MANAGER",
      permissions: [
        "PROMOTION_VIEW",
        "PROMOTION_OPERATE",
        "PROMOTION_AUDIT_VIEW",
      ],
      cinemaPublicIds: ["cinema-a"],
    };
    adminPromotionService.searchCampaigns.mockResolvedValue({
      ...{
        content: [], page: 0, size: 12, totalElements: 0,
        totalPages: 0, last: true,
      },
    });
    const { unmount } = render(<AdminPromotionCenterPage />);
    await waitFor(() => expect(adminPromotionService.searchCampaigns).toHaveBeenCalled());
    expect(screen.getAllByRole("button", { name: "Chiến dịch" })).toHaveLength(1);
    unmount();

    authState.user = {
      ...authState.user,
      permissions: [...authState.user.permissions, "PROMOTION_AUTHOR"],
    };
    adminPromotionService.searchCampaigns.mockResolvedValue({
      content: [], page: 0, size: 12, totalElements: 0,
      totalPages: 0, last: true,
    });
    adminCinemaService.getCinemas.mockResolvedValue({
      content: [
        { publicId: "cinema-a", name: "LoraFilm Landmark 81" },
        { publicId: "cinema-b", name: "Outside cinema" },
      ],
    });
    render(<AdminPromotionCenterPage />);
    await waitFor(() =>
      expect(screen.getAllByRole("button", { name: "Chiến dịch" }))
        .toHaveLength(2),
    );
    fireEvent.click(screen.getAllByRole("button", { name: "Chiến dịch" })[0]);

    expect(screen.getByText("Rạp áp dụng chiến dịch")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole("button", { name: /LoraFilm Landmark 81/ }))
        .toBeInTheDocument(),
    );
    expect(screen.queryByRole("button", { name: /Outside cinema/ }))
      .not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /LoraFilm Landmark 81/ }));
    expect(screen.getByText(/Chiến dịch áp dụng tại: LoraFilm Landmark 81/))
      .toBeInTheDocument();
  });
});
