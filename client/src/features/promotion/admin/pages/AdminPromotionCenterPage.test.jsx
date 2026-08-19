import { render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AdminPromotionCenterPage from "./AdminPromotionCenterPage";
import adminPromotionService from "../services/adminPromotionService";

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({
    user: {
      permissions: [
        "PROMOTION_VIEW",
        "PROMOTION_AUTHOR",
        "PROMOTION_AUDIT_VIEW",
      ],
    },
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
  default: {},
}));

describe("AdminPromotionCenterPage", () => {
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
});
