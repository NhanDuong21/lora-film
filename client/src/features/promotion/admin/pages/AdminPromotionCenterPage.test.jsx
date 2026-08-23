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
    getPromotionOpportunities: vi.fn(),
    getPromotionPlaybooks: vi.fn(),
    getPromotionRuns: vi.fn(),
    getPromotionRun: vi.fn(),
    getPromotionMonitoring: vi.fn(),
    getBookingMonitoring: vi.fn(),
    searchPromotionOperations: vi.fn(),
    getPromotionAnomalyCases: vi.fn(),
    assignPromotionAnomaly: vi.fn(),
    resolvePromotionAnomaly: vi.fn(),
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
  fireEvent.click(screen.getByRole("button", { name: "Chương trình" }));
  fireEvent.click(screen.getByRole("button", { name: "Tạo tùy chỉnh nâng cao" }));
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
    fireEvent.change(screen.getAllByRole("combobox").at(-1), {
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
    adminPromotionService.getPromotionOpportunities.mockResolvedValue([]);
    adminPromotionService.getPromotionPlaybooks.mockResolvedValue([]);
    adminPromotionService.getPromotionRuns.mockResolvedValue([]);
    adminPromotionService.getPromotionRun.mockResolvedValue({});
    adminPromotionService.getPromotionMonitoring.mockResolvedValue({
      expirationBacklog: 0,
      oldestExpiredAgeSeconds: 0,
      reversalCount: 0,
      reversalsLastHour: 0,
      activeBudgetReserved: 0,
      activeBudgetExposure: 0,
      campaignsAtExposureThreshold: 0,
      activeAlerts: [],
    });
    adminPromotionService.getBookingMonitoring.mockResolvedValue({
      promotionReconciliationMismatch: 0,
    });
    adminPromotionService.searchPromotionOperations.mockResolvedValue({
      reservations: [], redemptions: [], adjustments: [],
      reservationTotal: 0, redemptionTotal: 0, adjustmentTotal: 0,
    });
    adminPromotionService.getPromotionAnomalyCases.mockResolvedValue([]);
    adminPromotionService.assignPromotionAnomaly.mockResolvedValue({});
    adminPromotionService.resolvePromotionAnomaly.mockResolvedValue({});
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
    expect(screen.queryByRole("button", { name: "Tạo tùy chỉnh nâng cao" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Tạo chương trình mới" })).not.toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Việc cần làm" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Luồng tự động" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Chương trình" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Phân phối" })).toBeInTheDocument();
    expect(within(navigation).getByRole("button", { name: "Sự cố & đối soát" })).toBeInTheDocument();

    await screen.findByText("Landmark tháng 8");
    expect(screen.getByText("Đang soạn chương trình")).toBeInTheDocument();
    expect(screen.getByText("Bước 4/6")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /Tiếp tục thiết lập/ })).toHaveLength(1);
    expect(screen.queryByText("SUMMER-2026")).not.toBeInTheDocument();
  });

  it("counts an unsubmitted automation playbook as draft instead of pending review", async () => {
    adminPromotionService.getPromotionPlaybooks.mockResolvedValue([
      {
        publicId: "playbook-draft",
        code: "SECOND_BOOKING_INCENTIVE",
        name: "Khuyến khích booking lần hai",
        status: "DRAFT",
      },
    ]);

    render(<AdminPromotionCenterPage />);

    expect(await screen.findByText("Khuyến khích booking lần hai")).toBeInTheDocument();
    expect(screen.getByText(/1 bản nháp, 0 chờ kiểm tra, 0 sẵn sàng phát hành và 0 cảnh báo/)).toBeInTheDocument();
  });

  it("uses entitlement issuance instead of order redemption for automated campaign KPIs", async () => {
    adminPromotionService.searchCampaigns.mockResolvedValue({
      ...emptyPage,
      content: [
        {
          publicId: "campaign-birthday",
          code: "BIRTHDAY-UAT",
          name: "Quà sinh nhật",
          status: "ACTIVE",
          approvalStatus: "APPROVED",
          legalStatus: "PASSED",
          maxRedemptions: 100,
          redemptionCount: 0,
          budgetAmount: 5000000,
          budgetUsed: 0,
          budgetReserved: 0,
          allowedActions: ["VIEW", "PAUSE"],
        },
      ],
      totalElements: 1,
      totalPages: 1,
    });
    adminPromotionService.getPromotionPlaybooks.mockResolvedValue([
      {
        publicId: "birthday-playbook",
        campaignPublicId: "campaign-birthday",
        code: "BIRTHDAY_REWARD",
        name: "Quà sinh nhật",
        status: "ACTIVE",
        entitlements: { issued: 2, walletIssuedCommitted: 100000 },
      },
    ]);

    render(<AdminPromotionCenterPage />);
    await screen.findByText("Không có việc khẩn cấp cần xử lý");
    fireEvent.click(screen.getByRole("button", { name: "Chương trình" }));

    expect(await screen.findByText("2 / 100 lượt cấp")).toBeInTheDocument();
    expect(screen.getByText(/Đã cam kết trong ví 100\.000đ \/ 5\.000\.000đ/)).toBeInTheDocument();
  });

  it("opens a traceable automation run with audience, liability and technical snapshot", async () => {
    const run = {
      publicId: "run-uat",
      playbookCode: "BIRTHDAY_REWARD",
      playbookVersion: 3,
      status: "SUCCESS",
      audienceCount: 3,
      issuedCount: 2,
      excludedCount: 1,
      failedCount: 0,
      completedAt: "2026-08-21T00:09:00Z",
      authorizedByDisplayName: "Admin Checker",
    };
    adminPromotionService.getPromotionPlaybooks.mockResolvedValue([
      {
        publicId: "birthday-playbook",
        code: "BIRTHDAY_REWARD",
        name: "Quà sinh nhật",
        status: "ACTIVE",
      },
    ]);
    adminPromotionService.getPromotionRuns.mockResolvedValue([run]);
    adminPromotionService.getPromotionRun.mockResolvedValue({
      ...run,
      runActorDisplayName: "Hệ thống tự động",
      snapshotPublicId: "snapshot-uat",
      approvedConfigHash: "hash-uat",
      committedCost: 100000,
      retryingCount: 0,
      exclusionReasons: [{ reasonCode: "IDEMPOTENCY_KEY_ALREADY_GRANTED", count: 1 }],
      members: [
        {
          publicId: "member-issued",
          customerPublicId: "customer-1",
          status: "ISSUED",
          walletPublicId: "wallet-1",
          committedAmount: 50000,
        },
      ],
    });

    render(<AdminPromotionCenterPage />);
    fireEvent.click(screen.getByRole("button", { name: "Luồng tự động" }));

    fireEvent.click(await screen.findByRole("button", { name: "Xem danh sách người nhận" }));
    expect(await screen.findByText("snapshot-uat")).toBeInTheDocument();
    expect(screen.getByText("100.000đ")).toBeInTheDocument();
    expect(screen.getByText("wallet-1")).toBeInTheDocument();
    expect(screen.getByText(/Đã nhận quyền lợi trong kỳ/)).toBeInTheDocument();
  });

  it("keeps UAT playbooks and runs out of operational read models until explicitly included", async () => {
    const playbook = {
      publicId: "playbook-uat",
      code: "SECOND_BOOKING_INCENTIVE",
      name: "Ưu đãi cho lần đặt vé thứ hai",
      status: "ACTIVE",
      testData: true,
      environmentTag: "UAT",
    };
    const run = {
      publicId: "run-uat",
      playbookCode: "SECOND_BOOKING_INCENTIVE",
      status: "REVIEW_REQUIRED",
      openAnomalyCount: 1,
      testData: true,
      environmentTag: "UAT",
    };
    adminPromotionService.getPromotionPlaybooks.mockImplementation(
      (includeTestData) => Promise.resolve(includeTestData ? [playbook] : []),
    );
    adminPromotionService.getPromotionRuns.mockImplementation(
      (includeTestData) => Promise.resolve(includeTestData ? [run] : []),
    );

    render(<AdminPromotionCenterPage />);

    await waitFor(() => {
      expect(adminPromotionService.getPromotionPlaybooks).toHaveBeenCalledWith(false);
      expect(adminPromotionService.getPromotionRuns).toHaveBeenCalledWith(false);
    });
    expect(screen.queryByText("Ưu đãi cho lần đặt vé thứ hai")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("checkbox", { name: "Hiển thị dữ liệu UAT/kiểm thử" }));
    fireEvent.click(screen.getByRole("button", { name: "Luồng tự động" }));

    expect((await screen.findAllByText("Ưu đãi cho lần đặt vé thứ hai")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("UAT · Không ảnh hưởng khách thật").length).toBeGreaterThan(0);
    expect(adminPromotionService.getPromotionPlaybooks).toHaveBeenCalledWith(true);
    expect(adminPromotionService.getPromotionRuns).toHaveBeenCalledWith(true);
  });

  it("closes a UAT anomaly with a persisted business conclusion and no rollback action", async () => {
    const anomaly = {
      publicId: "case-uat",
      businessName: "Ưu đãi cho lần đặt vé thứ hai",
      summary: "Booking đầu tiên đã được hoàn tiền sau khi ưu đãi cho lần đặt vé thứ hai được sử dụng.",
      technicalReasonCode: "SOURCE_BOOKING_REFUNDED_AFTER_BENEFIT_USED",
      customerPublicId: "customer-0009",
      costAmount: 30000,
      status: "OPEN",
      testData: true,
      environmentTag: "UAT",
      createdAt: "2026-08-23T12:00:00Z",
    };
    adminPromotionService.getPromotionAnomalyCases.mockResolvedValue([anomaly]);

    render(<AdminPromotionCenterPage />);
    fireEvent.click(screen.getByRole("button", { name: "Sự cố & đối soát" }));

    expect(await screen.findByText(anomaly.summary)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /rollback|xóa ledger/i })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Ghi kết luận & đóng" }));
    fireEvent.click(screen.getByText("Đóng vì là dữ liệu kiểm thử"));
    fireEvent.change(screen.getByPlaceholderText(/Ghi căn cứ và kết luận/), {
      target: { value: "Đã hoàn tất walkthrough UAT" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Lưu kết luận & đóng" }));

    await waitFor(() => expect(
      adminPromotionService.resolvePromotionAnomaly,
    ).toHaveBeenCalledWith("case-uat", "TEST_DATA", "Đã hoàn tất walkthrough UAT"));
  });

  it("presents operational status and reversal reasons as business language", async () => {
    adminPromotionService.searchPromotionOperations.mockResolvedValue({
      reservations: [],
      redemptions: [{
        entryType: "REDEMPTION",
        publicId: "redemption-1",
        status: "ROLLBACKED",
        reasonDetail: "PAYMENT_REVERSED: Authoritative full Payment refund result",
        discountAmount: 6000,
        occurredAt: "2026-08-23T12:00:00Z",
      }],
      adjustments: [{
        entryType: "ADJUSTMENT",
        publicId: "adjustment-1",
        status: "REVERSE",
        releaseReasonType: "PAYMENT_REVERSED",
        discountAmount: 6000,
        occurredAt: "2026-08-23T12:00:00Z",
      }],
      reservationTotal: 0,
      redemptionTotal: 1,
      adjustmentTotal: 1,
    });

    render(<AdminPromotionCenterPage />);
    fireEvent.click(screen.getByRole("button", { name: "Sự cố & đối soát" }));

    expect(await screen.findByRole("heading", { name: "Bất thường cần kiểm tra" })).toBeInTheDocument();
    expect((await screen.findAllByText("Thanh toán đã được hoàn")).length).toBeGreaterThan(0);
    expect(screen.getByText("Hoàn lại")).toBeInTheDocument();
    expect(screen.queryByText(/Authoritative full Payment refund result/)).not.toBeInTheDocument();
    expect(screen.queryByText("REVERSE")).not.toBeInTheDocument();
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
    fireEvent.click(screen.getByRole("button", { name: "Phân phối" }));

    await waitFor(() =>
      expect(adminPromotionService.searchPromotions).toHaveBeenCalledWith(
        expect.objectContaining({
          distributionMode: "ASSIGNED_WALLET,AUTOMATION_ONLY",
          testData: false,
        }),
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
    expect(screen.queryByRole("button", { name: /Tạo tùy chỉnh/ })).not.toBeInTheDocument();
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
    fireEvent.click(screen.getByRole("button", { name: "Chương trình" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Tạo tùy chỉnh nâng cao" })).toBeInTheDocument());

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
