import { useCallback, useEffect, useState } from "react";
import {
  Activity,
  AlertTriangle,
  BadgePercent,
  Banknote,
  CalendarClock,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Building2,
  Clock3,
  Copy,
  Edit3,
  Eye,
  Film,
  Gift,
  Globe2,
  Loader2,
  Plus,
  RefreshCw,
  RotateCw,
  Search,
  Send,
  Tag,
  Users,
  X,
} from "lucide-react";
import adminPromotionService from "../services/adminPromotionService";
import { useAuth } from "@/contexts/AuthContext";
import { getCustomers } from "@/features/internal-staff/admin/services/userAdminService";
import adminMovieService from "@/features/catalog/admin/services/adminMovieService";
import adminCinemaService from "@/features/facilities/admin/services/adminCinemaService";
import {
  conditionSummary,
  friendlyPromotionError,
  promotionModelFor,
  voucherDiscountSummary,
} from "../../shared/promotionPresentation";
import { promotionStackingState } from "./promotionStackingState";

const emptyPage = {
  content: [],
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0,
  last: true,
};
const fieldClass =
  "h-10 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500";
const buttonClass =
  "inline-flex h-9 items-center justify-center gap-2 rounded-lg px-3 text-xs font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-40";

const labels = {
  DRAFT: "Bản nháp",
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  SCHEDULED: "Đã lên lịch",
  ACTIVE: "Đang chạy",
  PAUSED: "Tạm dừng",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
  KILLED: "Đã dừng khẩn cấp",
  DISABLED: "Đã tắt",
  EXPIRED: "Hết hạn",
  PASSED: "Đạt pháp lý",
  FAILED: "Không đạt",
  AVAILABLE: "Có thể áp dụng",
  NOT_STARTED: "Chưa bắt đầu",
  EXHAUSTED: "Hết hạn mức đơn",
  BUDGET_EXHAUSTED: "Hết ngân sách",
  CAMPAIGN_BLOCKED: "Đã chặn áp dụng mới",
  CONFIRMED: "Đã xác nhận",
  RELEASED: "Đã giải phóng",
  REVERSED: "Đã hoàn lại",
  RESERVED: "Đang giữ",
  ROLLBACKED: "Đã thu hồi",
  AUTO: "Ưu đãi tự động",
  VOUCHER: "Voucher sự kiện",
  COUPON: "Coupon theo khách",
  PERCENTAGE: "Giảm phần trăm",
  FIXED_AMOUNT: "Giảm số tiền",
  FULL_DISCOUNT: "Miễn phí toàn bộ",
};

const promotionTabs = [
  {
    key: "private-voucher",
    type: "VOUCHER",
    label: "Voucher cấp vào ví",
    description: "Chọn khách hàng và cấp voucher trực tiếp vào ví.",
    icon: Gift,
    privateOnly: true,
  },
  {
    key: "coupon",
    type: "COUPON",
    label: "Mã ưu đãi cá nhân",
    description: "Chọn khách hàng và gửi mã riêng để dùng khi thanh toán.",
    icon: Tag,
  },
];

const deliveryChoices = [
  {
    key: "AUTO",
    promotionType: "AUTO",
    publicVisible: false,
    icon: Globe2,
    title: "Tự động giảm tại thanh toán",
    description:
      "Hệ thống tự chọn và áp dụng khi đơn hàng đủ điều kiện. Khách không cần nhận hoặc nhập mã.",
    customerOutcome: "Khoản giảm được tự động hiển thị khi khách thanh toán.",
  },
  {
    key: "VOUCHER_PUBLIC",
    promotionType: "VOUCHER",
    publicVisible: true,
    icon: Gift,
    title: "Khách tự nhận voucher",
    description:
      "Voucher xuất hiện trong mục “Có thể nhận”. Khách chủ động nhận vào ví.",
    customerOutcome:
      "Khách thấy voucher tại Ưu đãi của tôi → Có thể nhận và tự thêm vào ví.",
  },
  {
    key: "VOUCHER_PRIVATE",
    promotionType: "VOUCHER",
    publicVisible: false,
    icon: Users,
    title: "Cấp voucher vào ví khách hàng",
    description:
      "Sau khi phát hành, nhân viên chọn khách hàng và cấp voucher trực tiếp vào ví.",
    customerOutcome:
      "Voucher chỉ xuất hiện trong ví của những khách hàng được nhân viên chọn.",
  },
  {
    key: "COUPON_PRIVATE",
    promotionType: "COUPON",
    publicVisible: false,
    icon: Send,
    title: "Gửi mã ưu đãi riêng",
    description:
      "Hệ thống tạo và gửi mã cho từng khách hàng. Khách nhập mã khi thanh toán.",
    customerOutcome:
      "Khách nhận mã trong thông báo và nhập mã đó khi thanh toán.",
  },
];

const badge = (status) => {
  if (["ACTIVE", "APPROVED", "PASSED"].includes(status))
    return "border-emerald-500/30 bg-emerald-500/10 text-emerald-300";
  if (["PAUSED", "PENDING", "SCHEDULED"].includes(status))
    return "border-amber-500/30 bg-amber-500/10 text-amber-300";
  if (
    ["REJECTED", "FAILED", "CANCELLED", "DISABLED", "EXPIRED"].includes(status)
  )
    return "border-red-500/30 bg-red-500/10 text-red-300";
  return "border-zinc-700 bg-zinc-800 text-zinc-300";
};

const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")}đ`;
const dateTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZoneName: "short",
  }).format(date);
};

const releaseReasonLabels = {
  PAYMENT_FAILED: "Thanh toán thất bại",
  PAYMENT_TIMEOUT: "Quá hạn thanh toán",
  CUSTOMER_CANCELLED_BOOKING: "Khách hủy đơn",
  STAFF_CANCELLED_BOOKING: "Nhân viên hủy đơn",
  BOOKING_EXPIRED: "Đơn hết hạn",
  CAMPAIGN_PAUSED: "Chiến dịch tạm dừng",
  CAMPAIGN_KILL_SWITCH: "Chiến dịch dừng khẩn cấp",
  SYSTEM_COMPENSATION: "Hệ thống bù trừ",
};

const compactReference = (value) => {
  const text = String(value || "");
  if (text.length <= 26) return text || "-";
  return `${text.slice(0, 14)}…${text.slice(-6)}`;
};
const toLocalInput = (value) => {
  const date = value ? new Date(value) : new Date();
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};
const fromLocalInput = (value) =>
  value ? new Date(value).toISOString() : null;
const pageData = (payload) => payload?.data ?? payload ?? emptyPage;
const nestedPageData = (payload) =>
  payload?.data?.data ?? payload?.data ?? payload ?? emptyPage;
const errorText = (error) => friendlyPromotionError(error);

const legalStatusText = (campaign) => {
  if (campaign?.legalStatus === "PASSED") return "Đạt pháp lý";
  if (campaign?.legalStatus === "FAILED") return "Cần chỉnh sửa";
  if (
    campaign?.status === "DRAFT" &&
    [undefined, null, "DRAFT"].includes(campaign?.approvalStatus)
  )
    return "Chưa yêu cầu";
  if (campaign?.approvalStatus !== "APPROVED") return "Chưa bắt đầu";
  if (campaign?.legalStatus === "PENDING") return "Chờ đánh giá";
  return labels[campaign?.legalStatus] || "Chưa yêu cầu";
};

const campaignCustomerOutcome = (campaign) => {
  const promotions = Array.isArray(campaign?.promotions)
    ? campaign.promotions
    : [];
  if (!promotions.length)
    return "Chưa có ưu đãi nào được thiết lập cho khách hàng.";
  const outcomes = promotions.map((promotion) => {
    if (promotion.promotionType === "AUTO")
      return "Hệ thống sẽ tự áp dụng khoản giảm khi khách thanh toán.";
    if (promotion.promotionType === "COUPON")
      return "Sau khi phát hành, bạn cần chọn khách hàng và gửi mã ưu đãi riêng.";
    if (promotion.publicVisible)
      return "Khách có thể tự nhận voucher tại Ưu đãi của tôi → Có thể nhận.";
    return "Sau khi phát hành, bạn cần chọn khách hàng và cấp voucher vào ví.";
  });
  return [...new Set(outcomes)].join(" ");
};

const campaignJourney = (campaign) => {
  const actions = new Set(campaign?.allowedActions || []);
  const promotionCount = Array.isArray(campaign?.promotions)
    ? campaign.promotions.length
    : campaign?.promotionCount;

  if (campaign?.status === "KILLED" || campaign?.killSwitch) {
    return {
      headline: "Đã dừng khẩn cấp",
      description: "Theo dõi các lượt đang giữ cho đến khi kết thúc an toàn.",
      actionLabel: "Theo dõi lượt đang giữ",
      actionCode: "OPEN_WORKSPACE",
      step: 6,
      owner: "Nhân viên xử lý sự cố",
    };
  }
  if (campaign?.status === "PAUSED") {
    return {
      headline: "Chương trình đang tạm dừng",
      description: "Khách hàng chưa thể nhận hoặc áp dụng lượt ưu đãi mới.",
      actionLabel: actions.has("RESUME")
        ? "Tiếp tục chương trình"
        : "Xem chương trình",
      actionCode: actions.has("RESUME") ? "RESUME" : "OPEN_WORKSPACE",
      step: 6,
      owner: actions.has("RESUME") ? "Nhân viên vận hành" : "Người có quyền vận hành",
    };
  }
  if (["ACTIVE", "SCHEDULED"].includes(campaign?.status)) {
    return {
      headline:
        campaign.status === "ACTIVE" ? "Chương trình đang chạy" : "Đã hẹn lịch phát hành",
      description:
        campaign.status === "ACTIVE"
          ? "Theo dõi số khách sử dụng, ngân sách và hạn mức còn lại."
          : "Chương trình sẽ tự chạy khi đến thời gian bắt đầu.",
      actionLabel: "Theo dõi chương trình",
      actionCode: "OPEN_WORKSPACE",
      step: 6,
      owner: "Nhân viên vận hành",
    };
  }
  if (["COMPLETED", "CANCELLED"].includes(campaign?.status)) {
    return {
      headline:
        campaign.status === "COMPLETED" ? "Chương trình đã hoàn tất" : "Chương trình đã hủy",
      description: "Xem kết quả sử dụng và lịch sử xử lý của chương trình.",
      actionLabel: "Xem kết quả",
      actionCode: "OPEN_WORKSPACE",
      step: 6,
      owner: "Không còn bước cần xử lý",
    };
  }
  if (campaign?.approvalStatus === "PENDING") {
    return {
      headline: "Đang chờ người khác duyệt",
      description: "Người tạo không thể tự duyệt chương trình của mình.",
      actionLabel: actions.has("APPROVE") ? "Xem và phê duyệt" : "Xem tiến độ",
      actionCode: actions.has("APPROVE") ? "APPROVE" : "OPEN_WORKSPACE",
      step: 5,
      owner: "Người có quyền duyệt ngân sách",
    };
  }
  if (campaign?.approvalStatus === "APPROVED" && campaign?.legalStatus !== "PASSED") {
    return {
      headline: "Chờ kiểm tra pháp lý",
      description: "Chương trình đã được duyệt và đang chờ xác nhận pháp lý.",
      actionLabel: actions.has("LEGAL_REVIEW") ? "Kiểm tra pháp lý" : "Xem tiến độ",
      actionCode: actions.has("LEGAL_REVIEW") ? "LEGAL_REVIEW" : "OPEN_WORKSPACE",
      step: 5,
      owner: "Bộ phận pháp lý",
    };
  }
  if (actions.has("PUBLISH")) {
    return {
      headline: "Sẵn sàng phát hành",
      description: "Mọi bước kiểm tra đã hoàn tất. Khách hàng chưa thấy ưu đãi cho đến khi phát hành.",
      actionLabel: "Phát hành chương trình",
      actionCode: "PUBLISH",
      step: 6,
      owner: "Người có quyền phát hành",
    };
  }
  if (campaign?.status === "DRAFT") {
    const missingPromotion = promotionCount === 0;
    return {
      headline: missingPromotion ? "Cần thêm ít nhất một ưu đãi" : "Đang soạn chương trình",
      description: missingPromotion
        ? "Chọn cách khách nhận ưu đãi và thiết lập quyền lợi trước khi gửi duyệt."
        : "Kiểm tra nội dung, quyền lợi và phạm vi trước khi gửi duyệt.",
      actionLabel: missingPromotion ? "Thêm ưu đãi" : "Tiếp tục thiết lập",
      actionCode: "OPEN_WORKSPACE",
      step: missingPromotion ? 2 : 4,
      owner: "Người tạo chương trình",
    };
  }
  return {
    headline: labels[campaign?.status] || "Xem chương trình",
    description: "Mở chương trình để xem trạng thái và việc cần làm tiếp theo.",
    actionLabel: "Xem chương trình",
    actionCode: "OPEN_WORKSPACE",
    step: 1,
    owner: "Nhân viên vận hành",
  };
};

function StatusBadge({ value, text }) {
  if (!value) return null;
  return (
    <span
      className={`inline-flex rounded border px-2 py-1 text-[10px] font-bold ${badge(value)}`}
    >
      {text || labels[value] || value}
    </span>
  );
}

function IconButton({
  title,
  onClick,
  children,
  danger = false,
  disabled = false,
}) {
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      onClick={onClick}
      disabled={disabled}
      className={`inline-flex h-8 w-8 items-center justify-center rounded-lg border transition-colors ${danger ? "border-red-500/20 text-red-400 hover:bg-red-500/10" : "border-zinc-700 text-zinc-400 hover:bg-zinc-800 hover:text-white"} disabled:opacity-30`}
    >
      {children}
    </button>
  );
}

const campaignActionTitle = (action) =>
  ({
    SUBMIT: "Gửi chiến dịch để duyệt?",
    APPROVE: "Phê duyệt chiến dịch?",
    LEGAL: "Xác nhận kiểm tra pháp lý?",
    PUBLISH: "Xuất bản chiến dịch?",
    ACTIVATE: "Kích hoạt chiến dịch?",
    PAUSE: "Tạm dừng chiến dịch?",
    RESUME: "Tiếp tục chiến dịch?",
    REJECT: "Từ chối chiến dịch?",
    LEGAL_REVIEW: "Ghi nhận kết quả pháp lý?",
    CANCEL: "Hủy vĩnh viễn chiến dịch?",
    KILL_SWITCH: "Dừng khẩn cấp chiến dịch?",
  })[action] || "Cập nhật chiến dịch?";

const campaignActionDescription = (campaign, action) => {
  const name = `“${campaign.name}”`;
  if (action === "PUBLISH")
    return `${name} sẽ tự chạy theo thời gian hiệu lực. Các ưu đãi thuộc chiến dịch sẽ không được bật thủ công.`;
  if (action === "ACTIVATE")
    return `${name} cùng các ưu đãi hợp lệ sẽ được mở để áp dụng khi khách đặt vé.`;
  if (action === "PAUSE")
    return `${name} sẽ chặn các lượt kiểm tra và giữ ưu đãi mới. Các lượt đang giữ vẫn có thể được xác nhận sử dụng, giải phóng hoặc tự hết hạn. Có thể tiếp tục chiến dịch sau.`;
  if (action === "CANCEL")
    return `${name} sẽ bị chặn áp dụng mới vĩnh viễn và không thể tiếp tục lại.`;
  if (action === "KILL_SWITCH")
    return `${name} sẽ bị chặn áp dụng mới ngay lập tức. Các lượt đang giữ không tự giải phóng; thao tác thu hồi khẩn cấp được tách riêng để bảo vệ các đơn đang thanh toán.`;
  return `Bạn đang cập nhật bước ${campaignActionTitle(action).replace("?", "").toLowerCase()} cho ${name}.`;
};

export default function AdminPromotionCenterPage() {
  const { user } = useAuth();
  const permissions = user?.permissions || [];
  const canViewOperations = permissions.includes("PROMOTION_AUDIT_VIEW");
  const canAuthor = permissions.includes("PROMOTION_AUTHOR");
  const normalizedRole = String(user?.role || "")
    .replace(/^ROLE_/, "")
    .toUpperCase();
  const isManager = normalizedRole === "MANAGER";
  const managerCinemaPublicIds = isManager
    ? Array.from(
        new Set(
          (user?.cinemaPublicIds || user?.assignedCinemaPublicIds || [])
            .filter(Boolean)
            .map(String),
        ),
      )
    : [];
  const [view, setView] = useState("tasks");
  const [tab, setTab] = useState("private-voucher");
  const [campaigns, setCampaigns] = useState(emptyPage);
  const [campaignOptions, setCampaignOptions] = useState([]);
  const [promotions, setPromotions] = useState(emptyPage);
  const [operations, setOperations] = useState({
    promotion: null,
    booking: null,
  });
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [campaignFilter, setCampaignFilter] = useState("");
  const [sort, setSort] = useState("createdAt,desc");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [modal, setModal] = useState(null);
  const [highlightedPromotionId, setHighlightedPromotionId] = useState(null);

  const selectedTab =
    promotionTabs.find((item) => item.key === tab) || promotionTabs[0];

  const loadCampaignOptions = useCallback(async () => {
    const result = pageData(
      await adminPromotionService.searchCampaigns({
        page: 0,
        size: 100,
        sort: "name,asc",
      }),
    );
    setCampaignOptions(
      result.content.map((item) => ({
        value: item.publicId,
        label: `${item.name} (${item.code})`,
        item,
      })),
    );
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      if (view === "operations") {
        const [promotion, booking] = await Promise.all([
          adminPromotionService.getPromotionMonitoring(),
          adminPromotionService.getBookingMonitoring(),
        ]);
        setOperations({ promotion, booking });
      } else if (view === "tasks") {
        setCampaigns(
          pageData(
            await adminPromotionService.searchCampaigns({
              page: 0,
              size: 100,
              sort: "createdAt,desc",
            }),
          ),
        );
      } else if (view === "campaigns") {
        setCampaigns(
          pageData(
            await adminPromotionService.searchCampaigns({
              name: query || undefined,
              status: status || undefined,
              page,
              size: 12,
              sort,
            }),
          ),
        );
      } else {
        const [promotionResult] = await Promise.all([
          adminPromotionService.searchPromotions({
            keyword: query || undefined,
            status: status || undefined,
            type: selectedTab.type,
            campaignPublicId: campaignFilter || undefined,
            page,
            size: 12,
            sort,
          }),
          campaignOptions.length ? Promise.resolve() : loadCampaignOptions(),
        ]);
        const result = pageData(promotionResult);
        setPromotions(
          selectedTab.privateOnly
            ? {
                ...result,
                content: (result.content || []).filter(
                  (promotion) => !promotion.publicVisible,
                ),
              }
            : result,
        );
      }
    } catch (error) {
      setMessage({ kind: "error", text: errorText(error) });
    } finally {
      setLoading(false);
    }
  }, [
    campaignFilter,
    campaignOptions.length,
    loadCampaignOptions,
    page,
    query,
    selectedTab.type,
    selectedTab.privateOnly,
    sort,
    status,
    view,
  ]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    if (!highlightedPromotionId) return undefined;
    const frame = window.requestAnimationFrame(() => {
      document
        .querySelector(`[data-promotion-id="${highlightedPromotionId}"]`)
        ?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
    const timer = window.setTimeout(
      () => setHighlightedPromotionId(null),
      2000,
    );
    return () => {
      window.cancelAnimationFrame(frame);
      window.clearTimeout(timer);
    };
  }, [highlightedPromotionId]);

  const current = view === "campaigns" ? campaigns : promotions;

  const run = async (
    action,
    success,
    refreshCampaigns = false,
    rethrow = false,
  ) => {
    setBusy(true);
    try {
      const result = await action();
      setModal(null);
      setMessage({ kind: "success", text: success });
      if (refreshCampaigns) await loadCampaignOptions();
      await load();
      return result;
    } catch (error) {
      setMessage({ kind: "error", text: errorText(error) });
      if (rethrow) throw error;
      return null;
    } finally {
      setBusy(false);
    }
  };

  const openCloneModal = async (item) => {
    setBusy(true);
    setMessage(null);
    try {
      const draft = await adminPromotionService.getCloneDraft(item.publicId);
      setModal({
        type: "promotion",
        mode: "clone",
        record: draft,
        promotionType: draft.promotionType,
        cloneWarning: !draft.sourceCampaignEditable,
      });
    } catch (error) {
      setMessage({ kind: "error", text: errorText(error) });
    } finally {
      setBusy(false);
    }
  };

  const runConfirmed = ({
    title,
    description,
    action,
    success,
    danger = false,
    refreshCampaigns = false,
  }) => {
    setModal({
      type: "confirm",
      title,
      description,
      action,
      success,
      danger,
      refreshCampaigns,
    });
  };

  const campaignAction = (campaign, action) =>
    action === "APPROVE"
      ? adminPromotionService.approveCampaign(
          campaign.publicId,
          "Approved from Promotion Center",
        )
      : action === "REJECT"
        ? adminPromotionService.rejectCampaign(
            campaign.publicId,
            "Rejected from Promotion Center",
          )
      : action === "LEGAL_REVIEW"
        ? adminPromotionService.reviewCampaignLegal(
            campaign.publicId,
            "PASSED",
            "Legal review passed",
          )
        : adminPromotionService.transitionCampaign(
            campaign.publicId,
            action,
            action === "KILL_SWITCH" ? "Stopped by operator" : undefined,
            campaign.version,
          );

  const issueToUsers = async (promotion, userPublicIds) => {
    setBusy(true);
    setMessage(null);
    try {
      const result = await adminPromotionService.issuePromotion(
        promotion.publicId,
        userPublicIds,
      );
      setMessage({
        kind: "success",
        text:
          promotion.promotionType === "COUPON"
            ? `Đã gửi mã riêng cho ${result?.issuedCount || 0} khách; ${result?.alreadyOwnedCount || 0} khách đã có ưu đãi.`
            : `Đã thêm voucher vào ví của ${result?.issuedCount || 0} khách; ${result?.alreadyOwnedCount || 0} khách đã có voucher.`,
      });
      await load();
      return result;
    } catch (error) {
      setMessage({ kind: "error", text: errorText(error) });
      throw error;
    } finally {
      setBusy(false);
    }
  };

  const openCampaignAction = (item, action) => {
    if (["OPEN_WORKSPACE", "VIEW"].includes(action)) {
      setModal({ type: "campaign-detail", record: item });
      return;
    }
    if (action === "EDIT") {
      setModal({ type: "campaign", record: item });
      return;
    }
    if (action === "CLONE") {
      setModal({ type: "campaign", template: item });
      return;
    }
    if (action === "DELETE") {
      runConfirmed({
        title: "Xóa chương trình?",
        description: `Chương trình “${item.name}” sẽ bị hủy và không thể sử dụng lại.`,
        action: () => adminPromotionService.deleteCampaign(item.publicId),
        success: "Đã xóa chương trình.",
        danger: true,
        refreshCampaigns: true,
      });
      return;
    }
    if (action === "KILL_SWITCH" || action === "FORCE_RELEASE_HOLDS") {
      setModal({ type: "campaign-danger", record: item, action });
      return;
    }
    if (["APPROVE", "REJECT", "LEGAL_REVIEW", "OVERRIDE_APPROVE"].includes(action)) {
      setModal({ type: "campaign-review", record: item, action });
      return;
    }
    runConfirmed({
      title: campaignActionTitle(action),
      description: campaignActionDescription(item, action),
      action: () => campaignAction(item, action),
      success: "Đã cập nhật chương trình.",
      refreshCampaigns: true,
    });
  };

  const createProgramWorkflow = async ({ campaign, promotion }) => {
    setBusy(true);
    setMessage(null);
    let createdCampaign = null;
    try {
      createdCampaign = await adminPromotionService.createCampaign(campaign);
      await adminPromotionService.createPromotion({
        ...promotion,
        campaignPublicId: createdCampaign.publicId,
      });
      await adminPromotionService.transitionCampaign(
        createdCampaign.publicId,
        "SUBMIT",
        "Gửi duyệt từ hướng dẫn tạo chương trình",
        createdCampaign.version,
      );
      setModal(null);
      setView("tasks");
      setCampaigns(
        pageData(
          await adminPromotionService.searchCampaigns({
            page: 0,
            size: 100,
            sort: "createdAt,desc",
          }),
        ),
      );
      await loadCampaignOptions();
      setMessage({
        kind: "success",
        text: "Đã tạo chương trình, thêm ưu đãi đầu tiên và gửi sang bước phê duyệt.",
      });
    } catch (error) {
      setModal(null);
      setView("campaigns");
      setMessage({
        kind: "error",
        text: createdCampaign
          ? `Chương trình đã được lưu nhưng chưa hoàn tất ưu đãi hoặc gửi duyệt. Hãy mở “${createdCampaign.name}” để tiếp tục. ${errorText(error)}`
          : errorText(error),
      });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#09090b] text-zinc-100">
      <header className="border-b border-zinc-800 bg-zinc-950/80 px-5 py-5 lg:px-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-black text-white">Trung tâm khuyến mãi</h1>
            <p className="mt-1 text-xs text-zinc-500">
              {view === "operations"
                ? "Điều tra sự cố, lượt giữ và các khoản hoàn lại"
                : view === "campaigns"
                  ? "Tạo và theo dõi chương trình từ khi soạn đến khi kết thúc"
                  : view === "tasks"
                    ? "Những việc cần xử lý tiếp theo, theo đúng thứ tự"
                    : "Cấp voucher hoặc gửi mã cho những khách hàng được chọn"}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => void load()}
              className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
            >
              <RefreshCw
                className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
              />{" "}
              Làm mới
            </button>
            {canAuthor && ["tasks", "campaigns"].includes(view) && (
              <button
                type="button"
                onClick={() =>
                  setModal({ type: "authoring-wizard", initialNow: Date.now() })
                }
                className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
              >
                <Plus className="h-4 w-4" /> Tạo chương trình
              </button>
            )}
          </div>
        </div>
        <nav aria-label="Khu vực quản lý khuyến mãi" className="mt-5 flex gap-1 overflow-x-auto border-b border-zinc-800">
          {[
            ["tasks", "Việc cần làm"],
            ["campaigns", "Chương trình khuyến mãi"],
            ["promotions", "Phân phối cho khách"],
            ...(canViewOperations ? [["operations", "Sự cố & đối soát"]] : []),
          ].map(([key, label]) => (
            <button
              key={key}
              type="button"
              onClick={() => { setView(key); setPage(0); setQuery(""); setStatus(""); }}
              className={`whitespace-nowrap border-b-2 px-4 py-3 text-xs font-black ${view === key ? "border-orange-500 text-orange-300" : "border-transparent text-zinc-500 hover:text-white"}`}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>

      <main className="px-5 py-5 lg:px-8">
        {message && (
          <div
            role="alert"
            className={`mb-4 border-l-2 px-3 py-2 text-sm ${message.kind === "error" ? "border-red-500 bg-red-500/10 text-red-300" : "border-emerald-500 bg-emerald-500/10 text-emerald-300"}`}
          >
            {message.text}
          </div>
        )}

        {view === "promotions" && (
          <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {promotionTabs.map(
              ({ key, icon: Icon, label, description }) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => {
                    setTab(key);
                    setStatus("");
                    setPage(0);
                  }}
                  className={`flex min-h-20 items-start gap-3 rounded-xl border p-4 text-left text-xs font-bold ${tab === key ? "border-orange-500/60 bg-orange-500/[0.08] text-white" : "border-zinc-800 bg-zinc-950/30 text-zinc-400 hover:border-zinc-700 hover:text-zinc-200"}`}
                >
                  <Icon className="mt-0.5 h-5 w-5 shrink-0 text-orange-300" />
                  <span>
                    <span className="block">{label}</span>
                    <span className="mt-1 block text-[11px] font-medium leading-5 text-zinc-500">
                      {description}
                    </span>
                  </span>
                </button>
              ),
            )}
          </div>
        )}

        {["campaigns", "promotions"].includes(view) && (
          <div
            className={`mb-4 grid gap-3 ${view === "campaigns" ? "md:grid-cols-[minmax(240px,1fr)_180px_180px]" : "md:grid-cols-[minmax(220px,1fr)_200px_180px_180px]"}`}
          >
          <label className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-600" />
            <input
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setPage(0);
              }}
              placeholder={view === "campaigns" ? "Tìm theo tên chương trình" : "Tìm theo tên ưu đãi"}
              className={`${fieldClass} pl-10`}
            />
          </label>
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            className={fieldClass}
          >
            <option value="">Mọi trạng thái</option>
            {(view === "campaigns"
              ? [
                  "DRAFT",
                  "SCHEDULED",
                  "ACTIVE",
                  "PAUSED",
                  "KILLED",
                  "COMPLETED",
                  "CANCELLED",
                ]
              : ["DRAFT", "ACTIVE", "PAUSED", "DISABLED", "EXPIRED"]
            ).map((value) => (
              <option key={value} value={value}>
                {labels[value]}
              </option>
            ))}
          </select>
          {view === "promotions" && (
            <select
              value={campaignFilter}
              onChange={(event) => {
                setCampaignFilter(event.target.value);
                setPage(0);
              }}
              className={fieldClass}
            >
              <option value="">Mọi chương trình</option>
              {campaignOptions.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          )}
          <select
            value={sort}
            onChange={(event) => {
              setSort(event.target.value);
              setPage(0);
            }}
            className={fieldClass}
          >
            <option value="createdAt,desc">Mới tạo trước</option>
            <option value="validTo,asc">Sắp hết hạn</option>
            <option value="priority,asc">Ưu tiên cao</option>
            <option value="name,asc">Tên A-Z</option>
          </select>
          </div>
        )}

        {view === "tasks" ? (
          <TaskBoard
            campaigns={campaigns.content}
            loading={loading}
            canAuthor={canAuthor}
            onCreate={() =>
              setModal({ type: "authoring-wizard", initialNow: Date.now() })
            }
            onAction={openCampaignAction}
          />
        ) : view === "operations" ? (
          <OperationsDashboard data={operations} loading={loading} />
        ) : (
          <>
            <div className="overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950/20">
          {loading ? (
            <div className="flex h-48 items-center justify-center gap-2 text-sm text-zinc-500">
              <Loader2 className="h-4 w-4 animate-spin" /> Đang tải
            </div>
          ) : view === "campaigns" ? (
            <CampaignTable
              rows={campaigns.content}
              busy={busy}
              onAction={openCampaignAction}
            />
          ) : promotions.content.length === 0 ? (
            <EmptyPromotions type={selectedTab.type} />
          ) : (
            <PromotionTable
              rows={promotions.content}
              campaigns={campaignOptions}
              busy={busy}
              highlightedPromotionId={highlightedPromotionId}
              onDetail={(item) =>
                setModal({
                  type: "promotion-detail",
                  record: item,
                })
              }
              onEdit={(item) =>
                setModal({
                  type: "promotion",
                  mode: "edit",
                  record: item,
                  promotionType: item.promotionType,
                })
              }
              onIssue={(item) => setModal({ type: "issue", record: item })}
              onClone={openCloneModal}
              onDelete={(item) =>
                runConfirmed({
                  title: "Xóa ưu đãi?",
                  description: `Ưu đãi “${item.name}” sẽ bị xóa khỏi chương trình.`,
                  action: () =>
                    adminPromotionService.deletePromotion(item.publicId),
                  success: "Đã xóa ưu đãi.",
                  danger: true,
                })
              }
            />
          )}
            </div>

            <div className="mt-4 flex items-center justify-between text-xs text-zinc-500">
          <span>
            Trang {current.totalPages ? page + 1 : 0} /{" "}
            {current.totalPages || 0}
          </span>
          <div className="flex gap-2">
            <IconButton
              title="Trang trước"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
            >
              <ChevronLeft className="h-4 w-4" />
            </IconButton>
            <IconButton
              title="Trang sau"
              disabled={current.last || page + 1 >= current.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </IconButton>
          </div>
            </div>
          </>
        )}
      </main>

      {modal?.type === "authoring-wizard" && (
        <CampaignAuthoringWizard
          isManager={isManager}
          managerCinemaPublicIds={managerCinemaPublicIds}
          initialNow={modal.initialNow}
          busy={busy}
          onClose={() => setModal(null)}
          onSubmit={createProgramWorkflow}
        />
      )}
      {modal?.type === "benefit-choice" && (
        <BenefitChoiceModal
          campaign={modal.record}
          onClose={() => setModal(null)}
          onChoose={(choice) =>
            setModal({
              type: "promotion",
              mode: "create",
              promotionType: choice.promotionType,
              record: {
                campaignPublicId: modal.record.publicId,
                publicVisible: choice.publicVisible,
                metadataJson: { distributionModel: choice.key },
              },
            })
          }
        />
      )}
      {modal?.type === "campaign" && (
        <CampaignModal
          record={modal.record}
          template={modal.template}
          isManager={isManager}
          managerCinemaPublicIds={managerCinemaPublicIds}
          busy={busy}
          onClose={() => setModal(null)}
          onSave={(payload, editing) =>
            run(
              () =>
                editing
                  ? adminPromotionService.updateCampaign(
                      modal.record.publicId,
                      payload,
                    )
                  : adminPromotionService.createCampaign(payload),
              editing ? "Đã cập nhật chiến dịch." : "Đã tạo chiến dịch.",
              true,
            )
          }
        />
      )}
      {modal?.type === "campaign-detail" && (
        <CampaignDetailModal
          record={modal.record}
          onClose={() => setModal(null)}
          onPrimaryAction={(campaign, action) => {
            setModal(null);
            openCampaignAction(campaign, action);
          }}
          onAddBenefit={(campaign) =>
            setModal({ type: "benefit-choice", record: campaign })
          }
          onIssue={(promotion) => setModal({ type: "issue", record: promotion })}
        />
      )}
      {modal?.type === "campaign-danger" && (
        <CampaignDangerModal
          campaign={modal.record}
          action={modal.action}
          busy={busy}
          onClose={() => setModal(null)}
          onSubmit={(reason, campaignCode, impact, idempotencyKey) => run(
            () => modal.action === "KILL_SWITCH"
              ? adminPromotionService.transitionCampaign(
                modal.record.publicId, "KILL_SWITCH", reason, modal.record.version,
              )
              : adminPromotionService.forceReleaseCampaignHolds(
                modal.record.publicId, campaignCode, reason, impact, idempotencyKey,
              ),
            modal.action === "KILL_SWITCH"
              ? "Đã dừng khẩn cấp chiến dịch. Các lượt đang giữ chưa bị giải phóng."
              : "Đã giải phóng các lượt giữ ưu đãi đang hoạt động.",
            true,
          )}
        />
      )}
      {modal?.type === "campaign-review" && (
        <CampaignReviewModal
          campaign={modal.record}
          action={modal.action}
          busy={busy}
          onClose={() => setModal(null)}
          onSubmit={({ comment, legalStatus, legalReference, campaignCode, incidentReference }) => run(
            () => modal.action === "APPROVE"
              ? adminPromotionService.approveCampaign(modal.record.publicId, comment)
              : modal.action === "REJECT"
                ? adminPromotionService.rejectCampaign(modal.record.publicId, comment)
                : modal.action === "OVERRIDE_APPROVE"
                  ? adminPromotionService.overrideCampaignApproval(
                    modal.record.publicId, campaignCode, incidentReference, comment,
                  )
                  : adminPromotionService.reviewCampaignLegal(modal.record.publicId, legalStatus, comment, legalReference),
            "Đã ghi nhận quyết định và cập nhật luồng chiến dịch.",
            true,
          )}
        />
      )}
      {modal?.type === "promotion" && (
        <PromotionModal
          record={modal.record}
          mode={modal.mode}
          cloneWarning={modal.cloneWarning}
          promotionType={modal.promotionType}
          campaigns={campaignOptions}
          busy={busy}
          onClose={() => setModal(null)}
          onRefreshCampaigns={loadCampaignOptions}
          onCreateCampaign={() => setModal({ type: "campaign" })}
          onSave={(payload, mode) =>
            run(
              () =>
                mode === "edit"
                  ? adminPromotionService.updatePromotion(
                      modal.record.publicId,
                      payload,
                    )
                  : adminPromotionService.createPromotion(payload),
              mode === "edit"
                ? "Đã cập nhật ưu đãi."
                : mode === "clone"
                  ? "Đã tạo bản sao ưu đãi."
                  : "Đã tạo ưu đãi.",
              false,
              true,
            ).then((result) => {
              if (mode === "clone" && result?.publicId) {
                setHighlightedPromotionId(result.publicId);
              }
              return result;
            })
          }
        />
      )}
      {modal?.type === "promotion-detail" && (
        <PromotionDetailModal
          record={modal.record}
          campaigns={campaignOptions}
          onClose={() => setModal(null)}
          onEdit={(item) =>
            setModal({
              type: "promotion",
              mode: "edit",
              record: item,
              promotionType: item.promotionType,
            })
          }
          onIssue={(item) => setModal({ type: "issue", record: item })}
        />
      )}
      {modal?.type === "issue" && (
        <IssueModal
          promotion={modal.record}
          busy={busy}
          onClose={() => setModal(null)}
          onIssue={(ids) => issueToUsers(modal.record, ids)}
        />
      )}
      {modal?.type === "confirm" && (
        <ConfirmModal
          {...modal}
          busy={busy}
          onClose={() => setModal(null)}
          onConfirm={() =>
            void run(modal.action, modal.success, modal.refreshCampaigns)
          }
        />
      )}
    </div>
  );
}

function EmptyPromotions({ type }) {
  const isCoupon = type === "COUPON";
  return (
    <div className="flex min-h-52 flex-col items-center justify-center px-5 text-center">
      <BadgePercent className="h-8 w-8 text-zinc-700" />
      <p className="mt-3 text-sm font-black text-zinc-300">
        {isCoupon ? "Chưa có mã nào cần gửi" : "Chưa có voucher nào cần cấp"}
      </p>
      <p className="mt-1 max-w-md text-xs leading-5 text-zinc-600">
        {isCoupon
          ? "Mã cá nhân sẽ xuất hiện ở đây sau khi được tạo trong một chương trình."
          : "Voucher cấp riêng sẽ xuất hiện ở đây sau khi được tạo trong một chương trình."}
      </p>
    </div>
  );
}

function TaskBoard({ campaigns, loading, canAuthor, onCreate, onAction }) {
  if (loading) {
    return (
      <div className="flex min-h-64 items-center justify-center gap-2 text-sm text-zinc-500">
        <Loader2 className="h-4 w-4 animate-spin" /> Đang tổng hợp việc cần làm
      </div>
    );
  }

  const rows = Array.isArray(campaigns) ? campaigns : [];
  const drafting = rows.filter(
    (item) => item.status === "DRAFT" && item.approvalStatus !== "PENDING",
  ).length;
  const pending = rows.filter(
    (item) =>
      item.approvalStatus === "PENDING" ||
      (item.approvalStatus === "APPROVED" && item.legalStatus !== "PASSED"),
  ).length;
  const ready = rows.filter((item) =>
    (item.allowedActions || []).includes("PUBLISH"),
  ).length;
  const warnings = rows.filter((item) => {
    const budget = Number(item.budgetAmount || 0);
    const exposure = Number(item.budgetUsed || 0) + Number(item.budgetReserved || 0);
    return item.status === "KILLED" || (budget > 0 && exposure / budget >= 0.8);
  }).length;
  const workItems = rows.filter((item) => {
    const journey = campaignJourney(item);
    return (
      item.status === "DRAFT" ||
      item.status === "PAUSED" ||
      item.status === "KILLED" ||
      journey.actionCode === "PUBLISH" ||
      journey.actionCode === "APPROVE" ||
      journey.actionCode === "LEGAL_REVIEW"
    );
  });

  return (
    <div className="space-y-6">
      <section>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="text-xs font-black uppercase tracking-wide text-orange-300">
              Hôm nay
            </p>
            <h2 className="mt-1 text-2xl font-black text-white">Việc cần làm</h2>
            <p className="mt-1 text-sm text-zinc-500">
              Bắt đầu từ thẻ đầu tiên; mỗi chương trình chỉ hiển thị một hành động chính.
            </p>
          </div>
          {canAuthor && (
            <button
              type="button"
              onClick={onCreate}
              className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
            >
              <Plus className="h-4 w-4" /> Tạo chương trình mới
            </button>
          )}
        </div>
        <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {[
            [drafting, "Chương trình đang soạn", "Tiếp tục thiết lập nội dung"],
            [pending, "Đang chờ kiểm tra", "Phê duyệt hoặc pháp lý"],
            [ready, "Sẵn sàng phát hành", "Khách vẫn chưa thấy ưu đãi"],
            [warnings, "Cảnh báo vận hành", "Ngân sách hoặc dừng khẩn cấp"],
          ].map(([value, title, description]) => (
            <article key={title} className="rounded-xl border border-zinc-800 bg-zinc-950/35 p-4">
              <p className="text-2xl font-black text-white">{value}</p>
              <p className="mt-1 text-sm font-bold text-zinc-200">{title}</p>
              <p className="mt-1 text-xs leading-5 text-zinc-600">{description}</p>
            </article>
          ))}
        </div>
      </section>

      <section>
        <h3 className="text-sm font-black text-white">Theo thứ tự ưu tiên</h3>
        <div className="mt-3 grid gap-3">
          {workItems.map((campaign) => {
            const journey = campaignJourney(campaign);
            return (
              <article
                key={campaign.publicId}
                className="grid gap-4 rounded-xl border border-zinc-800 bg-zinc-950/30 p-4 md:grid-cols-[minmax(0,1fr)_220px] md:items-center"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h4 className="font-black text-white">{campaign.name}</h4>
                    <span className="rounded-full bg-orange-500/10 px-2 py-1 text-[10px] font-black text-orange-300">
                      Bước {journey.step}/6
                    </span>
                  </div>
                  <p className="mt-2 text-sm font-bold text-zinc-200">{journey.headline}</p>
                  <p className="mt-1 text-xs leading-5 text-zinc-500">{journey.description}</p>
                  <p className="mt-2 text-[11px] text-zinc-600">
                    Người xử lý: <span className="font-bold text-zinc-400">{journey.owner}</span>
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => onAction(campaign, journey.actionCode)}
                  className={`${buttonClass} h-10 bg-orange-500 text-white hover:bg-orange-600`}
                >
                  {journey.actionLabel} <ChevronRight className="h-4 w-4" />
                </button>
              </article>
            );
          })}
          {!workItems.length && (
            <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.05] p-8 text-center">
              <CheckCircle2 className="mx-auto h-8 w-8 text-emerald-400" />
              <p className="mt-3 font-black text-white">Không có việc tồn đọng</p>
              <p className="mt-1 text-sm text-zinc-500">Các chương trình hiện không cần xử lý ngay.</p>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

const alertLabels = {
  EXPIRATION_BACKLOG_HIGH: "Tồn đọng lượt giữ quá hạn vượt ngưỡng",
  EXPIRATION_OLDEST_AGE_HIGH: "Lượt giữ quá hạn lâu nhất vượt thời gian xử lý",
  REVERSAL_RATE_HIGH: "Tần suất hoàn lại trong một giờ tăng cao",
  CAMPAIGN_BUDGET_EXPOSURE_HIGH: "Chiến dịch chạm ngưỡng ngân sách đã dùng và đang giữ",
};

const durationText = (seconds) => {
  const value = Number(seconds || 0);
  if (value < 60) return `${value} giây`;
  if (value < 3600) return `${Math.floor(value / 60)} phút`;
  return `${Math.floor(value / 3600)} giờ`;
};

function OperationsDashboard({ data, loading }) {
  const [filters, setFilters] = useState({
    query: "",
    status: "",
    releaseReasonType: "",
    from: "",
    to: "",
  });
  const [ledger, setLedger] = useState(null);
  const [ledgerState, setLedgerState] = useState({ loading: false, error: "" });

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLedgerState({ loading: true, error: "" });
      adminPromotionService.searchPromotionOperations({
        ...filters,
        from: filters.from ? new Date(filters.from).toISOString() : undefined,
        to: filters.to ? new Date(filters.to).toISOString() : undefined,
      }).then((result) => {
        if (active) {
          setLedger(result);
          setLedgerState({ loading: false, error: "" });
        }
      }).catch((error) => {
        if (active) setLedgerState({ loading: false, error: errorText(error) });
      });
    }, 250);
    return () => { active = false; window.clearTimeout(timer); };
  }, [filters]);

  const updateFilter = (key, value) => setFilters((current) => ({
    ...current,
    [key]: value,
  }));

  if (loading || !data.promotion || !data.booking) {
    return (
      <div className="flex h-48 items-center justify-center gap-2 text-sm text-zinc-500">
        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải
      </div>
    );
  }
  const promotion = data.promotion;
  const mismatch = Number(data.booking.promotionReconciliationMismatch || 0);
  const alerts = [...(promotion.activeAlerts || [])];
  if (mismatch > 0) alerts.push("RECONCILIATION_MISMATCH");
  const metrics = [
    {
      label: "Reservation quá hạn",
      value: Number(promotion.expirationBacklog || 0).toLocaleString("vi-VN"),
      detail: `Lâu nhất ${durationText(promotion.oldestExpiredAgeSeconds)}`,
      icon: Clock3,
      tone: "text-amber-300 border-amber-500/30",
    },
    {
      label: "Reversal",
      value: Number(promotion.reversalCount || 0).toLocaleString("vi-VN"),
      detail: `${Number(promotion.reversalsLastHour || 0).toLocaleString("vi-VN")} trong 1 giờ`,
      icon: RotateCw,
      tone: "text-sky-300 border-sky-500/30",
    },
    {
      label: "Ngân sách giữ chỗ",
      value: money(promotion.activeBudgetReserved),
      detail: "Chương trình đang hoạt động",
      icon: Banknote,
      tone: "text-emerald-300 border-emerald-500/30",
    },
    {
      label: "Ngân sách đã dùng và đang giữ",
      value: money(promotion.activeBudgetExposure),
      detail: `${Number(promotion.campaignsAtExposureThreshold || 0)} chiến dịch chạm ngưỡng`,
      icon: Activity,
      tone: "text-orange-300 border-orange-500/30",
    },
    {
      label: "Lệch đối soát",
      value: mismatch.toLocaleString("vi-VN"),
      detail: mismatch ? "Cần xử lý" : "Đồng bộ",
      icon: AlertTriangle,
      tone: mismatch
        ? "text-red-300 border-red-500/30"
        : "text-zinc-300 border-zinc-700",
    },
  ];

  return (
    <section aria-label="Giám sát vận hành khuyến mãi">
      {alerts.length > 0 && (
        <div className="mb-4 border-l-2 border-red-500 bg-red-500/[0.07] px-4 py-3 text-sm text-red-200">
          <div className="flex items-center gap-2 font-bold">
            <AlertTriangle className="h-4 w-4" />
            {alerts.length} cảnh báo đang hoạt động
          </div>
          <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-xs text-red-300">
            {alerts.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => {
                  if (item === "EXPIRATION_BACKLOG_HIGH" || item === "EXPIRATION_OLDEST_AGE_HIGH") updateFilter("status", "ACTIVE");
                  else if (item === "REVERSAL_RATE_HIGH") updateFilter("status", "REVERSE");
                  else if (item === "CAMPAIGN_BUDGET_EXPOSURE_HIGH") updateFilter("query", "CAMPAIGN");
                  else updateFilter("query", "");
                }}
                className="text-left underline decoration-red-500/40 underline-offset-4 hover:text-white"
              >
                {item === "RECONCILIATION_MISMATCH"
                  ? `${mismatch} đơn đặt vé lệch ưu đãi`
                  : alertLabels[item] || item}
              </button>
            ))}
          </div>
        </div>
      )}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {metrics.map(({ label, value, detail, icon: Icon, tone }) => (
          <article key={label} className={`min-w-0 rounded-lg border bg-zinc-950 p-4 ${tone}`}>
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs font-bold text-zinc-400">{label}</span>
              <Icon className="h-4 w-4 shrink-0" />
            </div>
            <div className="mt-4 break-words text-xl font-black text-white">{value}</div>
            <div className="mt-1 text-[11px] text-zinc-500">{detail}</div>
          </article>
        ))}
      </div>
      <section className="mt-6 rounded-lg border border-zinc-800 bg-zinc-950/30">
        <header className="border-b border-zinc-800 p-4">
          <h2 className="text-sm font-black text-white">Tra cứu lượt giữ và điều chỉnh ưu đãi</h2>
          <p className="mt-1 text-xs text-zinc-500">Tìm theo chiến dịch, ưu đãi, mã lượt giữ, đơn đặt vé, thanh toán, khách hàng, lý do giải phóng, trạng thái hoặc thời gian.</p>
          <div className="mt-4 grid gap-2 md:grid-cols-5">
            <input
              value={filters.query}
              onChange={(event) => updateFilter("query", event.target.value)}
              placeholder="Mã hoặc tham chiếu bất kỳ"
              className="h-10 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-xs text-white outline-none focus:border-orange-500 md:col-span-2"
            />
            <select value={filters.status} onChange={(event) => updateFilter("status", event.target.value)} className={fieldClass}>
              <option value="">Mọi trạng thái</option>
              {["ACTIVE", "CONFIRMED", "RELEASED", "EXPIRED", "REVERSED", "RESERVED", "ROLLBACKED"].map((value) => <option key={value} value={value}>{labels[value] || value}</option>)}
            </select>
            <select value={filters.releaseReasonType} onChange={(event) => updateFilter("releaseReasonType", event.target.value)} className={fieldClass}>
              <option value="">Mọi lý do giải phóng</option>
              {["PAYMENT_FAILED", "PAYMENT_TIMEOUT", "CUSTOMER_CANCELLED_BOOKING", "STAFF_CANCELLED_BOOKING", "BOOKING_EXPIRED", "CAMPAIGN_PAUSED", "CAMPAIGN_KILL_SWITCH", "SYSTEM_COMPENSATION"].map((value) => <option key={value} value={value}>{releaseReasonLabels[value] || value}</option>)}
            </select>
            <button type="button" onClick={() => setFilters({ query: "", status: "", releaseReasonType: "", from: "", to: "" })} className="h-10 rounded-lg border border-zinc-700 text-xs font-bold text-zinc-300 hover:bg-zinc-800">Xóa lọc</button>
            <input type="datetime-local" value={filters.from} onChange={(event) => updateFilter("from", event.target.value)} className={fieldClass} />
            <input type="datetime-local" value={filters.to} onChange={(event) => updateFilter("to", event.target.value)} className={fieldClass} />
          </div>
        </header>
        <div className="p-4">
          {ledgerState.loading ? (
            <div className="flex min-h-24 items-center justify-center gap-2 text-xs text-zinc-500"><Loader2 className="h-4 w-4 animate-spin" /> Đang tra cứu lịch sử</div>
          ) : ledgerState.error ? (
            <p className="text-xs text-red-300">{ledgerState.error}</p>
          ) : (
            <div className="space-y-5">
              <LedgerTable title="Lượt giữ ưu đãi" total={ledger?.reservationTotal} rows={ledger?.reservations || []} />
              <LedgerTable title="Số lượt ưu đãi" total={ledger?.redemptionTotal} rows={ledger?.redemptions || []} />
              <LedgerTable title="Điều chỉnh / hoàn lại" total={ledger?.adjustmentTotal} rows={ledger?.adjustments || []} />
            </div>
          )}
        </div>
      </section>
      <div className="mt-4 text-right text-[11px] text-zinc-600">
        Cập nhật {dateTime(promotion.observedAt)}
      </div>
    </section>
  );
}

const campaignActionLabel = {
  VIEW: "Xem chi tiết",
  EDIT: "Sửa",
  CLONE: "Nhân bản",
  SUBMIT: "Gửi duyệt",
  APPROVE: "Phê duyệt",
  REJECT: "Từ chối",
  OVERRIDE_APPROVE: "Duyệt ngoại lệ",
  LEGAL_REVIEW: "Duyệt pháp lý",
  PUBLISH: "Xuất bản",
  ACTIVATE: "Kích hoạt",
  RESUME: "Tiếp tục",
  PAUSE: "Tạm dừng",
  CANCEL: "Hủy vĩnh viễn",
  KILL_SWITCH: "Dừng khẩn cấp",
  FORCE_RELEASE_HOLDS: "Thu hồi khẩn cấp",
  DELETE: "Xóa bản nháp",
};

function CampaignTable({ rows, busy, onAction }) {
  return (
    <table className="w-full min-w-[900px] text-left text-sm">
      <thead className="bg-zinc-950 text-[10px] font-bold uppercase text-zinc-500">
        <tr>
          {[
            "Chương trình",
            "Việc tiếp theo",
            "Hiệu lực",
            "Kết quả sử dụng",
            "Hành động",
          ].map((value) => (
            <th key={value} className="px-4 py-3">
              {value}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {rows.map((row) => {
          const journey = campaignJourney(row);
          const budgetAmount = Number(row.budgetAmount || 0);
          const budgetExposure = Number(row.budgetUsed || 0) + Number(row.budgetReserved || 0);
          return (
          <tr key={row.publicId} className="hover:bg-zinc-900/70">
            <td className="px-4 py-4">
              <p className="font-bold text-white">{row.name}</p>
              <p className="mt-1 text-xs text-zinc-500">Bước {journey.step}/6</p>
            </td>
            <td className="px-4 py-4">
              <p className="text-xs font-black text-zinc-200">{journey.headline}</p>
              <p className="mt-1 max-w-xs text-[11px] leading-5 text-zinc-500">{journey.description}</p>
            </td>
            <td className="whitespace-nowrap px-4 py-4 text-xs leading-6 text-zinc-400">
              <span className="text-zinc-600">Từ</span> {dateTime(row.startAt)}
              <br />
              <span className="text-zinc-600">Đến</span> {dateTime(row.endAt)}
            </td>
            <td className="px-4 py-4 text-xs text-zinc-300">
              <p className="font-bold text-white">
                {row.redemptionCount || 0}
                {row.maxRedemptions ? ` / ${row.maxRedemptions} đơn` : " đơn"}
              </p>
              <p className="mt-1 text-zinc-500">
                Đã dùng và đang giữ {money(budgetExposure)}
                {budgetAmount > 0 ? ` / ${money(budgetAmount)}` : ""}
              </p>
            </td>
            <td className="px-4 py-4">
              <div className="flex flex-col items-start gap-2">
                <button
                  type="button"
                  onClick={() => onAction(row, journey.actionCode)}
                  disabled={busy}
                  className={`${buttonClass} h-10 bg-orange-500 text-white hover:bg-orange-600`}
                >
                  {journey.actionLabel} <ChevronRight className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => onAction(row, "OPEN_WORKSPACE")}
                  disabled={busy}
                  className="text-[11px] font-bold text-zinc-500 hover:text-white"
                >
                  Xem chi tiết
                </button>
              </div>
            </td>
          </tr>
          );
        })}
        {!rows.length && (
          <tr>
            <td colSpan="5" className="px-4 py-12 text-center text-sm text-zinc-600">
              Chưa có chương trình khuyến mãi.
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}

const effectivePromotionStatus = (promotion, campaign) => {
  if (
    !campaign?.status ||
    promotion.status !== "ACTIVE" ||
    campaign.status === "ACTIVE"
  )
    return promotion.status;
  return ["SCHEDULED", "PAUSED", "COMPLETED", "CANCELLED"].includes(
    campaign.status,
  )
    ? campaign.status
    : promotion.status;
};

function PromotionStackingStatus({ promotion, campaign }) {
  const state = promotionStackingState(promotion, campaign);
  const effectiveLabel = state.effective
    ? "Cho phép"
    : state.blockedReason === "CAMPAIGN_STACKING_DISABLED"
      ? "Bị chương trình chặn"
      : state.blockedReason === "CAMPAIGN_NOT_AVAILABLE"
        ? "Chưa xác định"
        : "Không cho phép";

  return (
    <div className="space-y-1 text-[10px] leading-4">
      <p className="text-zinc-400">
        Cấu hình ưu đãi:{" "}
        <span className={state.configured ? "text-emerald-300" : "text-zinc-500"}>
          {state.configured ? "Cho phép" : "Không cho phép"}
        </span>
      </p>
      <p className="text-zinc-400">
        Hiệu lực thực tế:{" "}
        <span
          className={
            state.effective
              ? "text-emerald-300"
              : state.blockedReason === "CAMPAIGN_STACKING_DISABLED"
                ? "text-amber-300"
                : "text-zinc-500"
          }
        >
          {effectiveLabel}
        </span>
      </p>
    </div>
  );
}

function PromotionTable({
  rows,
  campaigns,
  busy,
  highlightedPromotionId,
  onDetail,
  onEdit,
  onIssue,
  onClone,
  onDelete,
}) {
  const campaignDetails = new Map(
    campaigns.map((item) => [item.value, item.item || { name: item.label }]),
  );
  return (
    <table className="w-full min-w-[900px] text-left text-sm">
      <thead className="bg-zinc-950 text-[10px] font-bold uppercase text-zinc-500">
        <tr>
          {[
            "Ưu đãi",
            "Thuộc chương trình",
            "Cách khách nhận",
            "Hiệu lực",
            "Hành động",
          ].map((value) => (
            <th key={value} className="px-4 py-3">
              {value}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {rows.map((row) => (
          <tr
            key={row.publicId}
            data-promotion-id={row.publicId}
            tabIndex={0}
            role="button"
            aria-label={`Xem chi tiết ${row.name}`}
            onClick={() => onDetail(row)}
            onKeyDown={(event) => {
              if (event.target !== event.currentTarget) return;
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onDetail(row);
              }
            }}
            className={`cursor-pointer transition-colors focus:outline-none focus:ring-1 focus:ring-orange-500/60 ${row.publicId === highlightedPromotionId ? "bg-emerald-500/10 ring-1 ring-inset ring-emerald-500/50" : "hover:bg-zinc-900/70"}`}
          >
            <td className="max-w-xs px-4 py-4">
              <p className="font-bold text-white">{row.name}</p>
              <p className="mt-2 text-xs font-black text-emerald-400">
                {voucherDiscountSummary(row)}
              </p>
            </td>
            <td className="max-w-xs px-4 py-4">
              <p className="truncate text-xs font-bold text-zinc-300">
                {campaignDetails.get(row.campaignPublicId)?.name ||
                  "Chương trình chưa tải"}
              </p>
              <p className="mt-1 text-[11px] text-zinc-500">
                {labels[campaignDetails.get(row.campaignPublicId)?.status] || "Chưa phát hành"}
              </p>
            </td>
            <td className="px-4 py-4">
              <p className="text-xs font-bold text-white">
                {row.promotionType === "COUPON"
                  ? "Gửi mã riêng"
                  : "Cấp vào ví khách hàng"}
              </p>
              <p className="mt-1 max-w-xs text-[11px] leading-5 text-zinc-500">
                {row.promotionType === "COUPON"
                  ? "Khách nhận mã trong thông báo và nhập khi thanh toán."
                  : "Chỉ khách được chọn mới thấy voucher trong ví."}
              </p>
            </td>
            <td className="px-4 py-4 text-xs leading-5 text-zinc-400">
              <span className="text-zinc-600">Từ</span>{" "}
              {dateTime(row.validFrom)}
              <br />
              <span className="text-zinc-600">Đến</span> {dateTime(row.validTo)}
            </td>
            <td className="px-4 py-4">
              <div
                className="flex flex-col items-start gap-2"
                onClick={(event) => event.stopPropagation()}
              >
                <button
                  type="button"
                  onClick={() => onIssue(row)}
                  disabled={busy || !["ACTIVE", "SCHEDULED"].includes(campaignDetails.get(row.campaignPublicId)?.status)}
                  className={`${buttonClass} h-10 bg-orange-500 text-white hover:bg-orange-600`}
                >
                  {row.promotionType === "COUPON" ? (
                    <Send className="h-4 w-4" />
                  ) : (
                    <Users className="h-4 w-4" />
                  )}
                  {row.promotionType === "COUPON"
                    ? "Gửi mã cho khách hàng"
                    : "Cấp cho khách hàng"}
                </button>
                <div className="flex gap-3 text-[11px] font-bold">
                  <button type="button" onClick={() => onDetail(row)} className="text-zinc-500 hover:text-white">
                    Xem chi tiết
                  </button>
                  {row.status === "DRAFT" && (
                    <button type="button" onClick={() => onEdit(row)} className="text-zinc-500 hover:text-white">
                      Chỉnh sửa
                    </button>
                  )}
                  <button type="button" onClick={() => onClone(row)} className="text-zinc-500 hover:text-white">
                    Nhân bản
                  </button>
                  {row.status !== "ACTIVE" && (
                    <button type="button" onClick={() => onDelete(row)} className="text-red-400 hover:text-red-300">
                      Xóa
                    </button>
                  )}
                </div>
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function DetailItem({ label, children }) {
  const empty = children === null || children === undefined || children === "";
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-950/40 p-3">
      <p className="text-[10px] font-black uppercase text-zinc-500">{label}</p>
      <div className="mt-1 break-words text-sm font-bold text-zinc-100">
        {empty ? "-" : children}
      </div>
    </div>
  );
}

function ChipList({
  values,
  labels = new Map(),
  emptyLabel = "Tất cả",
  hideRawValue = false,
}) {
  const items = Array.isArray(values) ? values.filter(Boolean) : [];
  if (items.length === 0) {
    return <span className="text-zinc-500">{emptyLabel}</span>;
  }
  return (
    <div className="flex flex-wrap gap-1.5">
      {items.map((value, index) => (
        <span
          key={`${value}-${index}`}
          className="rounded border border-orange-500/25 bg-orange-500/10 px-2 py-1 text-[10px] font-bold text-orange-100"
        >
          {labels.get(value) ||
            labels.get(String(value)) ||
            (hideRawValue ? "Đang tải tên..." : value)}
        </span>
      ))}
    </div>
  );
}

const conditionDetailKeys = new Set([
  "minimumOrderAmount",
  "minOrderAmount",
  "requiredTierCode",
  "requiresVerification",
  "dayOfWeek",
  "moviePublicIds",
  "movieIds",
  "cinemaPublicIds",
  "cinemaIds",
]);

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const conditionValueText = (key, value) => {
  if (Array.isArray(value)) {
    if (value.length === 0) return "-";
    if (value.every((item) => uuidPattern.test(String(item)))) {
      return `${value.length} mục đã cấu hình`;
    }
    return value.join(", ");
  }
  if (typeof value === "boolean") return value ? "Có" : "Không";
  if (value && typeof value === "object") return "Đã cấu hình";
  if (uuidPattern.test(String(value))) return "Đã cấu hình";
  if (
    ["minimumOrderAmount", "minOrderAmount"].includes(key) &&
    Number(value) > 0
  ) {
    return money(value);
  }
  return value === null || value === undefined || value === "" ? "-" : value;
};

const extraConditionRows = (conditions) =>
  Object.entries(conditions || {})
    .filter(([key]) => !conditionDetailKeys.has(key))
    .map(([key, value]) => ({
      key,
      value: conditionValueText(key, value),
    }));

const actionDiscountValue = (action) =>
  action?.discountValue ??
  action?.value ??
  action?.amount ??
  action?.percentage ??
  "";

const actionValueText = (actionType, action) => {
  const value = actionDiscountValue(action);
  if (actionType === "FULL_DISCOUNT") return "Miễn phí toàn bộ";
  if (actionType === "PERCENTAGE") return `${Number(value || 0)}%`;
  if (actionType === "FIXED_AMOUNT") return money(value);
  return value || "Chưa cấu hình";
};

function PromotionDetailModal({ record, campaigns, onClose, onEdit, onIssue }) {
  const [detail, setDetail] = useState(record);
  const [movieNames, setMovieNames] = useState(new Map());
  const [cinemaNames, setCinemaNames] = useState(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setDetail(record);
      setLoading(true);
      setError("");
      adminPromotionService
        .getPromotion(record.publicId)
        .then((result) => {
          if (active) setDetail(result || record);
        })
        .catch((requestError) => {
          if (active) setError(errorText(requestError));
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 0);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [record]);

  const promotion = detail || record;
  const conditions = parseJsonObject(promotion.conditionsJson);
  const actions = parseJsonObject(promotion.actionsJson);
  const action = Array.isArray(actions) ? actions[0] || {} : actions;
  const movieIds = Array.isArray(conditions.moviePublicIds)
    ? conditions.moviePublicIds
    : Array.isArray(conditions.movieIds)
      ? conditions.movieIds
      : [];
  const cinemaIds = Array.isArray(conditions.cinemaPublicIds)
    ? conditions.cinemaPublicIds
    : Array.isArray(conditions.cinemaIds)
      ? conditions.cinemaIds
      : [];
  const movieIdsKey = movieIds.join("|");
  const cinemaIdsKey = cinemaIds.join("|");
  const metadata = parseJsonObject(promotion.metadataJson);
  const campaign = campaigns.find(
    (item) => item.value === promotion.campaignPublicId,
  )?.item;
  const model = promotionModelFor(promotion.promotionType);
  const campaignStatus = campaign?.status
    ? labels[campaign.status] || campaign.status
    : "Chưa tải";
  const actualStatus = effectivePromotionStatus(promotion, campaign);
  const customerDelivery =
    promotion.promotionType === "AUTO"
      ? "Hệ thống tự giảm khi khách thanh toán"
      : promotion.promotionType === "COUPON"
        ? "Nhân viên gửi mã riêng cho khách được chọn"
        : promotion.publicVisible
          ? "Khách tự nhận voucher vào ví"
          : "Nhân viên cấp voucher vào ví khách được chọn";
  const canIssue =
    promotion.promotionType !== "AUTO" &&
    !promotion.publicVisible &&
    ["ACTIVE", "SCHEDULED"].includes(campaign?.status);

  useEffect(() => {
    let active = true;
    const ids = movieIdsKey ? movieIdsKey.split("|") : [];
    Promise.all(
      ids.map(async (id) => {
        try {
          const response = await adminMovieService.getMovieById(id);
          const movie = response?.data?.data ?? response?.data ?? response;
          return [
            id,
            movie?.title ||
              movie?.movieTitle ||
              movie?.name ||
              "Không tải được tên phim",
          ];
        } catch {
          return [id, "Không tải được tên phim"];
        }
      }),
    ).then((entries) => {
      if (active) setMovieNames(new Map(entries));
    });
    return () => {
      active = false;
    };
  }, [movieIdsKey]);

  useEffect(() => {
    let active = true;
    const ids = cinemaIdsKey ? cinemaIdsKey.split("|") : [];
    Promise.all(
      ids.map(async (id) => {
        try {
          const response = await adminCinemaService.getAdminCinemaDetail(id);
          const cinema = response?.data?.data ?? response?.data ?? response;
          return [
            id,
            cinema?.name || cinema?.cinemaName || "Không tải được tên rạp",
          ];
        } catch {
          return [id, "Không tải được tên rạp"];
        }
      }),
    ).then((entries) => {
      if (active) setCinemaNames(new Map(entries));
    });
    return () => {
      active = false;
    };
  }, [cinemaIdsKey]);

  const dayNames = Array.isArray(conditions.dayOfWeek)
    ? conditions.dayOfWeek.map(
        (day) => dayOptions.find(([value]) => value === day)?.[1] || day,
      )
    : [];
  const actionType = action.discountType || action.type || action.actionType;
  const minimumOrderAmount =
    conditions.minimumOrderAmount ?? conditions.minOrderAmount;
  const actionMaxDiscount = action.maxDiscountAmount;
  const advancedConditions = extraConditionRows(conditions);

  return (
    <ModalShell
      title="Chi tiết ưu đãi"
      icon={Eye}
      onClose={onClose}
    >
      <div className="space-y-4 p-5">
        {loading && (
          <div className="flex items-center gap-2 rounded-lg border border-zinc-800 bg-zinc-950/50 px-3 py-2 text-xs font-bold text-zinc-500">
            <Loader2 className="h-4 w-4 animate-spin" /> Đang tải chi tiết...
          </div>
        )}
        {error && (
          <div className="rounded-lg border border-amber-500/20 bg-amber-500/[0.06] px-3 py-2 text-xs leading-5 text-amber-200">
            Không thể tải dữ liệu mới nhất. Đang hiển thị thông tin từ danh sách.
          </div>
        )}

        <div className="rounded-lg border border-orange-500/20 bg-orange-500/[0.06] p-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-[10px] font-black uppercase text-orange-300">
                {model.label}
              </p>
              <h3 className="mt-1 break-words text-lg font-black text-white">
                {promotion.name}
              </h3>
            </div>
          </div>
          {promotion.description && (
            <p className="mt-3 text-sm leading-6 text-zinc-300">
              {promotion.description}
            </p>
          )}
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <DetailItem label="Chương trình">
            <span>{campaign?.name || "Chưa tải tên chương trình"}</span>
            <span className="mt-1 block text-xs text-zinc-500">
              {campaignStatus}
            </span>
          </DetailItem>
          <DetailItem label="Quyền lợi">
            <span className="text-emerald-300">
              {voucherDiscountSummary(promotion)}
            </span>
          </DetailItem>
          <DetailItem label="Hiệu lực">
            <span>Từ {dateTime(promotion.validFrom)}</span>
            <span className="mt-1 block">
              Đến {dateTime(promotion.validTo)}
            </span>
          </DetailItem>
          <DetailItem label="Hạn mức">
            <span>
              {promotion.redemptionCount || 0}
              {promotion.maxRedemptions
                ? ` / ${promotion.maxRedemptions}`
                : " / Không giới hạn"}
            </span>
            <span className="mt-1 block text-xs text-zinc-500">
              {promotion.maxRedemptionsPerUser || 1} lượt / khách
            </span>
          </DetailItem>
          <DetailItem label="Khách nhận thế nào">
            <span>{customerDelivery}</span>
          </DetailItem>
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          <DetailItem label="Phim áp dụng">
            <ChipList values={movieIds} labels={movieNames} hideRawValue />
          </DetailItem>
          <DetailItem label="Rạp áp dụng">
            <ChipList values={cinemaIds} labels={cinemaNames} hideRawValue />
          </DetailItem>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <DetailItem label="Kiểu giảm">
            {labels[actionType] || actionType || "Chưa cấu hình"}
          </DetailItem>
          <DetailItem label="Giá trị giảm">
            <span className="text-emerald-300">
              {actionValueText(actionType, action)}
            </span>
          </DetailItem>
          <DetailItem label="Trần giảm">
            {Number(actionMaxDiscount) > 0
              ? money(actionMaxDiscount)
              : "Không giới hạn"}
          </DetailItem>
          <DetailItem label="Đơn tối thiểu">
            {Number(minimumOrderAmount) > 0
              ? money(minimumOrderAmount)
              : "Không yêu cầu"}
          </DetailItem>
          <DetailItem label="Ngày áp dụng">
            <ChipList values={dayNames} emptyLabel="Mọi ngày" />
          </DetailItem>
          <DetailItem label="Hạng thành viên">
            {conditions.requiredTierCode || "Không yêu cầu"}
          </DetailItem>
          <DetailItem label="Xác thực tài khoản">
            {conditions.requiresVerification ? "Bắt buộc" : "Không yêu cầu"}
          </DetailItem>
          <DetailItem label="Tóm tắt điều kiện">
            {conditionSummary(conditions)}
          </DetailItem>
        </div>

        <details className="rounded-xl border border-zinc-800 p-4">
          <summary className="cursor-pointer text-xs font-black text-zinc-400">
            Thông tin kỹ thuật
          </summary>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <DetailItem label="Mã nội bộ">
              <span className="font-mono text-xs">{promotion.code || "Không dùng mã"}</span>
            </DetailItem>
            <DetailItem label="Trạng thái cấu hình">
              <StatusBadge value={promotion.status} />
            </DetailItem>
            <DetailItem label="Trạng thái hiệu lực">
              <StatusBadge value={actualStatus} />
            </DetailItem>
            <DetailItem label="Thứ tự ưu tiên">{promotion.priority ?? 0}</DetailItem>
            <DetailItem label="Dùng chung ưu đãi">
              <PromotionStackingStatus promotion={promotion} campaign={campaign} />
            </DetailItem>
            <DetailItem label="Mô hình phân phối">
              {metadata.distributionModel || "Mặc định"}
            </DetailItem>
            {advancedConditions.length > 0 && (
              <DetailItem label="Điều kiện mở rộng">
                <div className="space-y-1 text-xs font-semibold text-zinc-300">
                  {advancedConditions.map((item) => (
                    <p key={item.key}>
                      <span className="text-zinc-500">{item.key}:</span>{" "}
                      {item.value}
                    </p>
                  ))}
                </div>
              </DetailItem>
            )}
          </div>
        </details>

        <div className="flex flex-wrap justify-end gap-2 border-t border-zinc-800 pt-4">
          <button
            type="button"
            onClick={onClose}
            className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
          >
            Đóng
          </button>
          {promotion.status === "DRAFT" && (
            <button
              type="button"
              onClick={() => onEdit(promotion)}
              className={`${buttonClass} border border-zinc-700 text-zinc-200 hover:bg-zinc-800`}
            >
              <Edit3 className="h-4 w-4" /> Sửa
            </button>
          )}
          {canIssue && (
            <button
              type="button"
              onClick={() => onIssue(promotion)}
              className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
            >
              <Users className="h-4 w-4" />{" "}
              {promotion.promotionType === "COUPON"
                ? "Gửi mã cho khách hàng"
                : "Cấp cho khách hàng"}
            </button>
          )}
        </div>
      </div>
    </ModalShell>
  );
}

function ModalShell({ title, icon: Icon, onClose, children }) {
  return (
    <div
      className="fixed inset-0 z-[90] flex items-center justify-center bg-black/80 p-4"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <section
        role="dialog"
        aria-modal="true"
        className="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-xl border border-zinc-700 bg-zinc-900 shadow-2xl"
      >
        <header className="sticky top-0 z-10 flex items-center justify-between border-b border-zinc-800 bg-zinc-900 px-5 py-4">
          <div className="flex items-center gap-2">
            <Icon className="h-5 w-5 text-orange-400" />
            <h2 className="font-black text-white">{title}</h2>
          </div>
          <IconButton title="Đóng" onClick={onClose}>
            <X className="h-4 w-4" />
          </IconButton>
        </header>
        {children}
      </section>
    </div>
  );
}

function CampaignReviewModal({ campaign, action, busy, onClose, onSubmit }) {
  const legal = action === "LEGAL_REVIEW";
  const override = action === "OVERRIDE_APPROVE";
  const [comment, setComment] = useState("");
  const [legalStatus, setLegalStatus] = useState("PASSED");
  const [legalReference, setLegalReference] = useState(campaign.legalNotificationRef || "");
  const [campaignCode, setCampaignCode] = useState("");
  const [incidentReference, setIncidentReference] = useState("");
  const valid = comment.trim().length >= 3
    && (!override || (campaignCode === campaign.code && incidentReference.trim().length >= 3))
    && (!legal || legalReference.trim().length > 0);

  return (
    <ModalShell
      title={campaignActionLabel[action] || "Ghi nhận quyết định"}
      icon={legal ? CheckCircle2 : override ? AlertTriangle : Check}
      onClose={busy ? () => {} : onClose}
    >
      <form
        onSubmit={(event) => {
          event.preventDefault();
          if (valid) onSubmit({
            comment: comment.trim(),
            legalStatus,
            legalReference: legalReference.trim(),
            campaignCode,
            incidentReference: incidentReference.trim(),
          });
        }}
        className="space-y-4 p-5"
      >
        {legal && (
          <>
            <Field label="Kết quả pháp lý" required>
              <select value={legalStatus} onChange={(event) => setLegalStatus(event.target.value)} className={fieldClass}>
                <option value="PASSED">Đạt pháp lý</option>
                <option value="FAILED">Không đạt</option>
              </select>
            </Field>
            <Field label="Mã văn bản / ticket pháp lý" required>
              <input value={legalReference} onChange={(event) => setLegalReference(event.target.value)} className={fieldClass} />
            </Field>
          </>
        )}
        {override && (
          <div className="rounded-lg border border-red-500/30 bg-red-500/[0.06] p-4">
            <p className="text-xs leading-5 text-red-100">Duyệt ngoại lệ là hành động khẩn cấp, không phải quyền mặc định của quản trị viên. Nhập mã chiến dịch và mã sự cố để tạo bằng chứng kiểm tra rõ ràng.</p>
            <input value={campaignCode} onChange={(event) => setCampaignCode(event.target.value.toUpperCase())} placeholder={campaign.code} className={fieldClass + " mt-3"} />
            <input value={incidentReference} onChange={(event) => setIncidentReference(event.target.value)} placeholder="Mã sự cố / ticket (ví dụ INC-2026-08-001)" className={fieldClass + " mt-3"} />
          </div>
        )}
        <Field label={legal ? "Nhận xét pháp lý" : action === "REJECT" ? "Lý do từ chối" : override ? "Lý do override" : "Nhận xét phê duyệt"} required>
          <textarea required minLength="3" maxLength="1000" value={comment} onChange={(event) => setComment(event.target.value)} className="min-h-28 w-full rounded-lg border border-zinc-700 bg-zinc-950 p-3 text-sm text-white outline-none focus:border-orange-500" />
        </Field>
        <div className="flex justify-end gap-2 border-t border-zinc-800 pt-4">
          <button type="button" disabled={busy} onClick={onClose} className={buttonClass + " border border-zinc-700 text-zinc-300"}>Quay lại</button>
          <button disabled={busy || !valid} className={buttonClass + (action === "REJECT" || override ? " bg-red-500" : " bg-orange-500") + " text-white"}>
            {busy && <Loader2 className="h-4 w-4 animate-spin" />} Ghi nhận
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function CampaignDangerModal({ campaign, action, busy, onClose, onSubmit }) {
  const forceRelease = action === "FORCE_RELEASE_HOLDS";
  const [reason, setReason] = useState("");
  const [campaignCode, setCampaignCode] = useState("");
  const [impact, setImpact] = useState(null);
  const [impactError, setImpactError] = useState("");
  const [idempotencyKey] = useState(() =>
    globalThis.crypto?.randomUUID?.() || `force-release-${Date.now()}`);

  useEffect(() => {
    if (!forceRelease) return undefined;
    let active = true;
    adminPromotionService.getForceReleaseImpact(campaign.publicId)
      .then((result) => { if (active) setImpact(result); })
      .catch((error) => { if (active) setImpactError(errorText(error)); });
    return () => { active = false; };
  }, [campaign.publicId, forceRelease]);

  const valid = reason.trim().length >= 5
    && (!forceRelease || (campaignCode === campaign.code && impact?.executable));
  const impactItems = Array.isArray(impact?.bookings) ? impact.bookings : [];
  const paymentInProgressCount = impactItems.filter(
    (item) => item.disposition === "BLOCKED_PAYMENT_IN_PROGRESS",
  ).length;
  const unverifiableCount = impactItems.filter((item) =>
    [
      "BLOCKED_DEPENDENCY_UNAVAILABLE",
      "BLOCKED_MISSING_BOOKING_REFERENCE",
    ].includes(item.disposition),
  ).length;
  const alreadySettledCount = impactItems.filter((item) =>
    ["BLOCKED_PAYMENT_SUCCESSFUL", "BLOCKED_BOOKING_TERMINAL"].includes(
      item.disposition,
    ),
  ).length;
  const impactBlockingMessage = Number(impact?.repriceRequiredCount) > 0
    ? `Còn ${impact.repriceRequiredCount} đơn cần hủy hoặc tính lại giá ở hệ thống đặt vé. Hãy xử lý các đơn này, kết thúc phiên thanh toán liên quan rồi tải lại kết quả kiểm tra.`
    : paymentInProgressCount > 0
      ? `Còn ${paymentInProgressCount} đơn đang thanh toán. Hãy kết thúc các phiên thanh toán liên quan rồi tải lại kết quả kiểm tra.`
      : unverifiableCount > 0
        ? "Chưa thể xác minh đầy đủ trạng thái đặt vé hoặc thanh toán. Hãy kiểm tra kết nối hệ thống rồi tải lại kết quả."
        : alreadySettledCount > 0
          ? `Có ${alreadySettledCount} đơn đã thanh toán hoặc hoàn tất nên không thể thu hồi ưu đãi.`
          : "Trạng thái đã thay đổi. Hãy tải lại kết quả kiểm tra trước khi thực hiện.";

  return (
    <ModalShell
      title={forceRelease ? "Thu hồi khẩn cấp các lượt đang giữ" : "Dừng khẩn cấp chiến dịch"}
      icon={AlertTriangle}
      onClose={busy ? () => {} : onClose}
    >
      <form
        onSubmit={(event) => {
          event.preventDefault();
          if (valid) onSubmit(reason.trim(), campaignCode, impact, idempotencyKey);
        }}
        className="space-y-4 p-5"
      >
        <div className="rounded-lg border border-red-500/30 bg-red-500/[0.06] p-4 text-sm leading-6 text-red-100">
          {forceRelease
            ? "Chỉ lượt giữ có đơn đặt vé đã hủy hoặc hết hạn và không có thanh toán thành công hay đang xử lý mới được thu hồi. Hệ thống sẽ chặn toàn bộ thao tác nếu còn đơn cần xử lý."
            : "Các lượt kiểm tra và giữ ưu đãi mới sẽ bị chặn ngay. Những lượt đang giữ không tự bị giải phóng."}
        </div>
        {forceRelease && (
          impactError ? <p className="text-xs text-red-300">{impactError}</p>
            : !impact ? <p className="text-xs text-zinc-500">Đang tính phạm vi ảnh hưởng…</p>
              : (
                <div className="grid gap-3 sm:grid-cols-2">
                  <DetailCard label="Đơn đặt vé bị ảnh hưởng" value={impact.affectedBookingCount} />
                  <DetailCard label="Lượt ưu đãi đang giữ" value={impact.affectedReservationCount} />
                  <DetailCard label="Ưu đãi đang giữ" value={money(impact.reservedDiscount)} />
                  <DetailCard label="Ngân sách đã dùng và đang giữ" value={money(impact.budgetExposure)} />
                  <DetailCard label="Có thể thu hồi ngay" value={impact.safeToReleaseCount} />
                  <DetailCard label="Cần xử lý đặt vé trước" value={impact.repriceRequiredCount} />
                  <DetailCard label="Đang thanh toán" value={paymentInProgressCount} />
                  <DetailCard label="Không xác minh được" value={unverifiableCount} />
                  {alreadySettledCount > 0 && (
                    <DetailCard label="Đã thanh toán / hoàn tất" value={alreadySettledCount} />
                  )}
                  <DetailCard
                    label="Trạng thái thao tác"
                    value={impact.executable ? "Có thể thực hiện" : "Chưa thể thực hiện"}
                  />
                </div>
              )
        )}
        {forceRelease && impact && !impact.executable && (
          <p className="rounded-lg border border-amber-500/30 bg-amber-500/[0.07] p-3 text-xs leading-5 text-amber-100">
            {impactBlockingMessage}
          </p>
        )}
        <Field label="Lý do vận hành" required hint="Lý do được ghi vào lịch sử kiểm tra và từng lượt ưu đãi được giải phóng.">
          <textarea required minLength="5" maxLength="1000" value={reason} onChange={(event) => setReason(event.target.value)} className="min-h-24 w-full rounded-lg border border-zinc-700 bg-zinc-950 p-3 text-sm text-white outline-none focus:border-red-500" />
        </Field>
        {forceRelease && (
          <Field label={"Nhập lại mã " + campaign.code} required>
            <input value={campaignCode} onChange={(event) => setCampaignCode(event.target.value.toUpperCase())} className={fieldClass} />
          </Field>
        )}
        <div className="flex justify-end gap-2 border-t border-zinc-800 pt-4">
          <button type="button" disabled={busy} onClick={onClose} className={buttonClass + " border border-zinc-700 text-zinc-300"}>Quay lại</button>
          <button disabled={busy || !valid || (forceRelease && !impact)} className={buttonClass + " bg-red-500 text-white hover:bg-red-600"}>
            {busy && <Loader2 className="h-4 w-4 animate-spin" />}
            {forceRelease ? "Thu hồi các lượt an toàn" : "Dừng khẩn cấp"}
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function ConfirmModal({
  title,
  description,
  danger,
  busy,
  onClose,
  onConfirm,
}) {
  return (
    <ModalShell
      title={title}
      icon={danger ? AlertTriangle : CheckCircle2}
      onClose={busy ? () => {} : onClose}
    >
      <div className="p-5">
        <p className="text-sm leading-6 text-zinc-300">{description}</p>
        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            disabled={busy}
            onClick={onClose}
            className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
          >
            Hủy
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onConfirm}
            className={`${buttonClass} ${danger ? "bg-red-500 hover:bg-red-600" : "bg-orange-500 hover:bg-orange-600"} text-white`}
          >
            {busy && <Loader2 className="h-4 w-4 animate-spin" />}
            {danger ? "Xác nhận xóa" : "Xác nhận"}
          </button>
        </div>
      </div>
    </ModalShell>
  );
}

function LedgerTable({ title, total, rows }) {
  return (
    <div>
      <p className="mb-2 text-xs font-black uppercase tracking-wide text-zinc-500">{title} · {Number(total || 0).toLocaleString("vi-VN")}</p>
      <div className="overflow-x-auto rounded-lg border border-zinc-800">
        <table className="w-full min-w-[900px] text-left text-xs">
          <thead className="bg-zinc-950 text-zinc-600"><tr><th className="px-3 py-2">Mã nghiệp vụ</th><th className="px-3 py-2">Trạng thái</th><th className="px-3 py-2">Đơn / thanh toán</th><th className="px-3 py-2">Khách hàng</th><th className="px-3 py-2">Lý do</th><th className="px-3 py-2">Giảm</th><th className="px-3 py-2">Thời gian</th></tr></thead>
          <tbody className="divide-y divide-zinc-800">
            {rows.map((item) => (
              <tr key={item.entryType + item.publicId}>
                <td className="px-3 py-2 text-zinc-300">
                  <span className="font-mono font-bold" title={item.businessReference || undefined}>{compactReference(item.businessReference || "Chưa có mã")}</span>
                  <details className="mt-1 text-[10px] text-zinc-600"><summary className="cursor-pointer">Thông tin kỹ thuật</summary><span className="break-all font-mono">{item.publicId}</span></details>
                </td>
                <td className="px-3 py-2"><StatusBadge value={item.status} text={item.entryType === "RESERVATION" && item.status === "ACTIVE" ? "Đang giữ" : undefined} /></td>
                <td className="px-3 py-2 text-zinc-400" title={item.sourceReference || item.businessReference || undefined}>{compactReference(item.sourceReference || item.businessReference)}<br />{item.paymentPublicId ? "Có liên kết thanh toán" : "Chưa có thanh toán"}</td>
                <td className="px-3 py-2 text-zinc-400">{item.customerReference ? `Khách •••${String(item.customerReference).slice(-4)}` : "-"}</td>
                <td className="max-w-xs px-3 py-2 text-zinc-400">{releaseReasonLabels[item.releaseReasonType] || item.reasonDetail || "-"}</td>
                <td className="px-3 py-2 font-bold text-white">{money(item.discountAmount)}</td>
                <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{dateTime(item.occurredAt)}</td>
              </tr>
            ))}
            {!rows.length && <tr><td colSpan="7" className="px-3 py-6 text-center text-zinc-600">Không có dữ liệu phù hợp.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const campaignDetailTabs = [
  "Tổng quan",
  "Ưu đãi",
  "Đối tượng nhận",
  "Ngân sách & hạn mức",
  "Duyệt & pháp lý",
  "Lịch sử",
];

const blockedReasonLabel = {
  CAMPAIGN_APPROVAL_REQUIRED: "Chưa được phê duyệt",
  CAMPAIGN_LEGAL_REVIEW_REQUIRED: "Chưa đạt kiểm tra pháp lý",
  PROMOTION_QUOTA_EXHAUSTED: "Đã hết số đơn tối đa",
  CAMPAIGN_BUDGET_EXHAUSTED: "Đã hết ngân sách",
  CAMPAIGN_PAUSED: "Chiến dịch đang tạm dừng",
  CAMPAIGN_KILL_SWITCHED: "Đã dừng khẩn cấp",
  CAMPAIGN_CANCELLED: "Chiến dịch đã bị hủy",
  CAMPAIGN_EXPIRED: "Chiến dịch đã hết hạn",
};

function CampaignDetailModal({
  record,
  onClose,
  onPrimaryAction,
  onAddBenefit,
  onIssue,
}) {
  const [activeTab, setActiveTab] = useState("Tổng quan");
  const [detail, setDetail] = useState(record);
  const [history, setHistory] = useState([]);
  const [state, setState] = useState({ loading: true, error: "" });

  useEffect(() => {
    let active = true;
    Promise.all([
      adminPromotionService.getCampaign(record.publicId),
      adminPromotionService.getApprovalHistory(record.publicId).catch(() => []),
    ]).then(([campaign, approvalHistory]) => {
      if (!active) return;
      setDetail(campaign);
      setHistory(Array.isArray(approvalHistory) ? approvalHistory : []);
      setState({ loading: false, error: "" });
    }).catch((error) => {
      if (active) setState({ loading: false, error: errorText(error) });
    });
    return () => { active = false; };
  }, [record.publicId]);

  const journey = campaignJourney(detail);
  const customerOutcome = campaignCustomerOutcome(detail);

  return (
    <ModalShell title={"Chiến dịch · " + detail.name} icon={CalendarClock} onClose={onClose}>
      <div className="border-b border-zinc-800 px-5">
        <div className="flex gap-1 overflow-x-auto">
          {campaignDetailTabs.map((tabName) => (
            <button
              key={tabName}
              type="button"
              onClick={() => setActiveTab(tabName)}
              className={activeTab === tabName
                ? "whitespace-nowrap border-b-2 border-orange-500 px-3 py-3 text-xs font-black text-orange-300"
                : "whitespace-nowrap border-b-2 border-transparent px-3 py-3 text-xs font-bold text-zinc-500 hover:text-white"}
            >
              {tabName}
            </button>
          ))}
        </div>
      </div>
      <div className="p-5">
        {state.loading ? (
          <div className="flex min-h-40 items-center justify-center gap-2 text-sm text-zinc-500">
            <Loader2 className="h-4 w-4 animate-spin" /> Đang tải chi tiết chiến dịch
          </div>
        ) : state.error ? (
          <p className="text-sm text-red-300">{state.error}</p>
        ) : activeTab === "Tổng quan" ? (
          <div className="space-y-4">
            <div className="rounded-xl border border-orange-500/25 bg-orange-500/[0.06] p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="text-xs font-black uppercase tracking-wide text-orange-300">
                    Bước {journey.step}/6
                  </p>
                  <h3 className="mt-2 text-xl font-black text-white">{journey.headline}</h3>
                  <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-300">{journey.description}</p>
                  <p className="mt-3 text-xs text-zinc-500">
                    Người xử lý tiếp theo: <b className="text-zinc-200">{journey.owner}</b>
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    if (detail.status === "DRAFT" && !detail.promotions?.length) {
                      onAddBenefit(detail);
                      return;
                    }
                    if (journey.actionCode === "OPEN_WORKSPACE") {
                      setActiveTab(
                        ["ACTIVE", "PAUSED", "KILLED"].includes(detail.status)
                          ? "Ngân sách & hạn mức"
                          : "Ưu đãi",
                      );
                      return;
                    }
                    onPrimaryAction(detail, journey.actionCode);
                  }}
                  className={`${buttonClass} h-10 bg-orange-500 text-white hover:bg-orange-600`}
                >
                  {journey.actionLabel} <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
            <div className="rounded-xl border border-sky-500/20 bg-sky-500/[0.05] p-4">
              <p className="text-xs font-black uppercase tracking-wide text-sky-300">Khách hàng sẽ thấy gì?</p>
              <p className="mt-2 text-sm leading-6 text-zinc-200">{customerOutcome}</p>
            </div>
            <div className="rounded-lg border border-zinc-800 bg-zinc-950/50 p-4">
              <p className="text-xs font-black uppercase tracking-wide text-zinc-500">Vì sao chương trình chưa thể phát hành?</p>
              {detail.blockedReasons?.length ? (
                <ul className="mt-3 space-y-2 text-sm text-amber-200">
                  {detail.blockedReasons.map((reason) => (
                    <li key={reason}>• {blockedReasonLabel[reason] || reason}</li>
                  ))}
                </ul>
              ) : (
                <p className="mt-3 text-sm text-emerald-300">Không còn điều kiện nào đang ngăn chương trình hoạt động.</p>
              )}
            </div>
            <div className="rounded-lg border border-red-500/20 bg-red-500/[0.05] p-4 text-xs leading-5 text-zinc-300">
              Ngừng chiến dịch sẽ chặn các đơn hàng mới nhận ưu đãi. Các đơn đang thanh toán vẫn giữ ưu đãi đến khi thanh toán thành công, thất bại hoặc hết thời gian giữ. Việc thu hồi các lượt đang giữ là một thao tác khẩn cấp riêng.
            </div>
            {(detail.status === "KILLED" || detail.killSwitch) && (
              <div className="rounded-lg border border-amber-500/30 bg-amber-500/[0.08] p-4">
                <p className="text-xs font-black uppercase tracking-wide text-amber-300">
                  Việc cần làm sau khi dừng khẩn cấp
                </p>
                <p className="mt-2 text-sm leading-6 text-zinc-200">
                  Theo dõi các lượt đang giữ đến khi kết thúc. Chỉ dùng “Thu hồi khẩn cấp”
                  sau khi hệ thống đặt vé và hệ thống thanh toán xác nhận từng lượt an toàn.
                </p>
                <p className="mt-2 text-xs text-zinc-400">
                  Ngân sách ưu đãi đang giữ: <b className="text-white">{money(detail.budgetReserved)}</b>
                </p>
              </div>
            )}
            <details className="rounded-lg border border-zinc-800 bg-zinc-950/30 p-4">
              <summary className="cursor-pointer text-xs font-black text-zinc-400">
                Xem trạng thái kỹ thuật
              </summary>
              <div className="mt-4 grid gap-3 sm:grid-cols-3">
                <DetailCard label="Vòng đời" value={labels[detail.businessStatus] || detail.businessStatus} />
                <DetailCard label="Duyệt" value={labels[detail.approvalStatus] || detail.approvalStatus} />
                <DetailCard label="Pháp lý" value={legalStatusText(detail)} />
              </div>
            </details>
          </div>
        ) : activeTab === "Ưu đãi" ? (
          <div className="space-y-3">
            {(detail.promotions || []).map((promotion) => (
              <article key={promotion.publicId} className="rounded-lg border border-zinc-800 bg-zinc-950/40 p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div><p className="font-bold text-white">{promotion.name}</p><p className="mt-1 text-xs text-zinc-500">{labels[promotion.promotionType] || promotion.promotionType}</p></div>
                  <StatusBadge value={promotion.status} />
                </div>
                <p className="mt-3 text-xs leading-5 text-zinc-400">
                  {promotion.promotionType === "AUTO"
                    ? "Hệ thống tự áp dụng khi khách thanh toán."
                    : promotion.promotionType === "COUPON"
                      ? "Cần gửi mã riêng cho khách hàng sau khi chương trình phát hành."
                      : promotion.publicVisible
                        ? "Khách tự nhận voucher tại mục Có thể nhận."
                        : "Cần cấp voucher vào ví của khách hàng được chọn."}
                </p>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-zinc-400">
                  <span>Đã sử dụng: <b className="text-white">{promotion.redemptionCount || 0}</b></span>
                  {["VOUCHER", "COUPON"].includes(promotion.promotionType) && !promotion.publicVisible && ["ACTIVE", "SCHEDULED"].includes(detail.status) && (
                    <button
                      type="button"
                      onClick={() => onIssue(promotion)}
                      className={`${buttonClass} bg-orange-500 text-white`}
                    >
                      {promotion.promotionType === "COUPON" ? "Gửi mã cho khách hàng" : "Cấp cho khách hàng"}
                    </button>
                  )}
                </div>
              </article>
            ))}
            {!detail.promotions?.length && (
              <div className="rounded-xl border border-dashed border-zinc-700 p-8 text-center">
                <p className="text-sm text-zinc-500">Chưa có ưu đãi nào trong chương trình.</p>
                <button type="button" onClick={() => onAddBenefit(detail)} className={`${buttonClass} mt-4 bg-orange-500 text-white`}>
                  <Plus className="h-4 w-4" /> Thêm ưu đãi
                </button>
              </div>
            )}
          </div>
        ) : activeTab === "Đối tượng nhận" ? (
          <div className="space-y-3 text-sm leading-6 text-zinc-300">
            {(detail.promotions || []).map((promotion) => (
              <article key={promotion.publicId} className="rounded-lg border border-zinc-800 p-4">
                <p className="font-bold text-white">{promotion.name}</p>
                <p className="mt-2">
                  {promotion.promotionType === "AUTO"
                    ? "Hệ thống tự xét cho mọi khách đủ điều kiện; khách không cần nhận hoặc nhập mã."
                    : promotion.promotionType === "COUPON"
                      ? "Chỉ khách đã được cấp coupon mới có thể sử dụng mã."
                      : "Chỉ xuất hiện trong ví của khách đã nhận voucher."}
                </p>
                <p className="mt-1 text-xs text-zinc-500">{conditionSummary(promotion)}</p>
              </article>
            ))}
          </div>
        ) : activeTab === "Ngân sách & hạn mức" ? (
          <div className="grid gap-3 sm:grid-cols-2">
            <DetailCard label="Ngân sách đã dùng" value={money(detail.budgetUsed)} />
            <DetailCard label="Ngân sách đang giữ" value={money(detail.budgetReserved)} />
            <DetailCard label="Ngân sách còn lại" value={money(detail.budgetRemaining)} />
            <DetailCard label="Tổng ngân sách" value={money(detail.budgetAmount)} />
            <DetailCard label="Số đơn đã áp dụng" value={(detail.redemptionCount || 0) + (detail.maxRedemptions ? " / " + detail.maxRedemptions : "")} />
            <DetailCard label="Tối đa mỗi khách" value={detail.maxRedemptionsPerUser || 1} />
          </div>
        ) : activeTab === "Duyệt & pháp lý" ? (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2"><StatusBadge value={detail.approvalStatus} /><StatusBadge value={detail.legalStatus} text={legalStatusText(detail)} /></div>
            <DetailCard label="Tham chiếu pháp lý" value={detail.legalNotificationRef || "Chưa có"} />
            <HistoryList history={history} />
          </div>
        ) : (
          <div className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <DetailCard label="Tạo lúc" value={dateTime(detail.createdAt)} />
              <DetailCard label="Cập nhật lúc" value={dateTime(detail.updatedAt)} />
            </div>
            <HistoryList history={history} />
          </div>
        )}
      </div>
    </ModalShell>
  );
}

function DetailCard({ label, value }) {
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-950/40 p-4">
      <p className="text-[10px] font-black uppercase tracking-wide text-zinc-600">{label}</p>
      <p className="mt-2 text-sm font-bold text-white">{value ?? "-"}</p>
    </div>
  );
}

function HistoryList({ history }) {
  if (!history.length) return <p className="text-sm text-zinc-500">Chưa có lịch sử duyệt.</p>;
  return (
    <div className="divide-y divide-zinc-800 rounded-lg border border-zinc-800">
      {history.map((item) => (
        <div key={item.publicId || item.approvedAt} className="p-3 text-xs">
          <div className="flex justify-between gap-3"><b className="text-white">{labels[item.action] || item.action}</b><span className="text-zinc-600">{dateTime(item.approvedAt)}</span></div>
          <p className="mt-1 text-zinc-400">{item.comment || "Không có ghi chú"}</p>
        </div>
      ))}
    </div>
  );
}

function Field({ label, children, wide = false, required = false, hint = "" }) {
  return (
    <div className={wide ? "md:col-span-2" : ""}>
      <span className="mb-1.5 block text-xs font-bold text-zinc-400">
        {label}
        {required && <span className="ml-1 text-red-400">*</span>}
      </span>
      {children}
      {hint && (
        <span className="mt-1 block text-[10px] leading-4 text-zinc-600">
          {hint}
        </span>
      )}
    </div>
  );
}

function Toggle({ checked, onChange, label, disabled = false }) {
  return (
    <label
      className={`flex items-center gap-2 text-xs font-bold ${
        disabled
          ? "cursor-not-allowed text-zinc-600"
          : "cursor-pointer text-zinc-300"
      }`}
    >
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 accent-orange-500 disabled:cursor-not-allowed disabled:opacity-50"
      />
      {label}
    </label>
  );
}

function BenefitChoiceModal({ campaign, onClose, onChoose }) {
  return (
    <ModalShell title={`Thêm ưu đãi · ${campaign.name}`} icon={Gift} onClose={onClose}>
      <div className="p-5">
        <p className="text-sm font-black text-white">Khách hàng sẽ nhận ưu đãi này như thế nào?</p>
        <p className="mt-1 text-xs leading-5 text-zinc-500">
          Chọn theo trải nghiệm của khách; hệ thống sẽ tự cấu hình loại ưu đãi phù hợp.
        </p>
        <div className="mt-5 grid gap-3 sm:grid-cols-2">
          {deliveryChoices.map((choice) => {
            const Icon = choice.icon;
            return (
              <button
                key={choice.key}
                type="button"
                onClick={() => onChoose(choice)}
                className="rounded-xl border border-zinc-800 bg-zinc-950/35 p-4 text-left transition-colors hover:border-orange-500/50 hover:bg-orange-500/[0.05]"
              >
                <Icon className="h-6 w-6 text-orange-300" />
                <span className="mt-3 block text-sm font-black text-white">{choice.title}</span>
                <span className="mt-2 block text-xs leading-5 text-zinc-500">{choice.description}</span>
              </button>
            );
          })}
        </div>
      </div>
    </ModalShell>
  );
}

function CampaignAuthoringWizard({
  isManager,
  managerCinemaPublicIds,
  initialNow,
  busy,
  onClose,
  onSubmit,
}) {
  const steps = [
    "Cách khách nhận",
    "Quyền lợi",
    "Phạm vi áp dụng",
    "Thời gian & hạn mức",
    "Khách hàng sẽ thấy gì",
    "Kiểm tra & gửi duyệt",
  ];
  const [step, setStep] = useState(0);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    deliveryKey: "",
    name: "",
    description: "",
    actionType: "PERCENTAGE",
    actionValue: 10,
    maxDiscountAmount: "",
    minimumOrderAmount: "",
    cinemaPublicIds: [],
    cinemaScopeOptions: [],
    startAt: toLocalInput(initialNow),
    endAt: toLocalInput(initialNow + 30 * 86400_000),
    budgetAmount: 10000000,
    maxRedemptions: 1000,
    maxRedemptionsPerUser: 1,
    stackable: false,
  });
  const choice = deliveryChoices.find((item) => item.key === form.deliveryKey);
  const update = (key, value) => {
    setError("");
    setForm((current) => ({ ...current, [key]: value }));
  };
  const loadCinemas = useCallback(
    async (query) => {
      const result = nestedPageData(
        await adminCinemaService.getCinemas({
          keyword: query || undefined,
          page: 0,
          size: 100,
        }),
      );
      const rows = Array.isArray(result) ? result : result?.content || [];
      if (!isManager) return rows;
      const assigned = new Set(managerCinemaPublicIds.map(String));
      return rows.filter((item) => assigned.has(String(item.publicId || item.id || "")));
    },
    [isManager, managerCinemaPublicIds],
  );
  const normaliseCinema = useCallback(
    (item) => ({
      value: String(item.publicId || item.id || ""),
      label: item.name || item.cinemaName || item.publicId || item.id,
    }),
    [],
  );
  const discountPreview = voucherDiscountSummary({
    actionsJson:
      form.actionType === "FULL_DISCOUNT"
        ? { discountType: "FULL_DISCOUNT" }
        : {
            discountType: form.actionType,
            discountValue: Number(form.actionValue || 0),
            ...(form.maxDiscountAmount
              ? { maxDiscountAmount: Number(form.maxDiscountAmount) }
              : {}),
          },
  });
  const selectedCinemaNames = form.cinemaPublicIds.map(
    (id) =>
      form.cinemaScopeOptions.find((option) => option.value === id)?.label || id,
  );

  const validate = () => {
    if (step === 0 && !choice) {
      setError("Hãy chọn cách khách hàng nhận ưu đãi.");
      return false;
    }
    if (step === 1 && !form.name.trim()) {
      setError("Hãy nhập tên chương trình mà khách hàng có thể hiểu.");
      return false;
    }
    if (
      step === 1 &&
      form.actionType !== "FULL_DISCOUNT" &&
      Number(form.actionValue) <= 0
    ) {
      setError("Mức giảm phải lớn hơn 0.");
      return false;
    }
    if (step === 2 && isManager && !form.cinemaPublicIds.length) {
      setError("Hãy chọn ít nhất một rạp được phân công.");
      return false;
    }
    if (
      step === 3 &&
      new Date(form.endAt).getTime() <= new Date(form.startAt).getTime()
    ) {
      setError("Thời gian kết thúc phải sau thời gian bắt đầu.");
      return false;
    }
    if (step === 3 && Number(form.budgetAmount) <= 0) {
      setError("Ngân sách phải lớn hơn 0.");
      return false;
    }
    return true;
  };

  const next = () => {
    if (!validate()) return;
    setStep((current) => Math.min(steps.length - 1, current + 1));
  };

  const submit = () => {
    const campaignCode = generatedCode("PROGRAM");
    const conditionsJson = {
      ...(Number(form.minimumOrderAmount) > 0
        ? { minimumOrderAmount: Number(form.minimumOrderAmount) }
        : {}),
      ...(form.cinemaPublicIds.length
        ? { cinemaPublicIds: form.cinemaPublicIds }
        : {}),
    };
    const actionsJson =
      form.actionType === "FULL_DISCOUNT"
        ? { discountType: "FULL_DISCOUNT" }
        : {
            discountType: form.actionType,
            discountValue: Number(form.actionValue),
            ...(form.actionType === "PERCENTAGE" && Number(form.maxDiscountAmount) > 0
              ? { maxDiscountAmount: Number(form.maxDiscountAmount) }
              : {}),
          };
    const scopeType = form.cinemaPublicIds.length ? "ASSIGNED_CINEMAS" : "GLOBAL";
    onSubmit({
      campaign: {
        code: campaignCode,
        name: form.name.trim(),
        description: form.description.trim(),
        priority: 100,
        stackable: form.stackable,
        exclusiveCampaign: false,
        autoActivate: true,
        autoComplete: true,
        autoPauseWhenBudgetExceeded: true,
        timezone: "Asia/Ho_Chi_Minh",
        startAt: fromLocalInput(form.startAt),
        endAt: fromLocalInput(form.endAt),
        budgetAmount: Number(form.budgetAmount),
        maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
        maxRedemptionsPerUser: Number(form.maxRedemptionsPerUser || 1),
        scopeType,
        cinemaPublicIds: form.cinemaPublicIds,
      },
      promotion: {
        promotionType: choice.promotionType,
        code:
          choice.promotionType === "VOUCHER" ? generatedCode("VOUCHER") : null,
        name: form.name.trim(),
        description: form.description.trim(),
        publicVisible: choice.publicVisible,
        priority: 100,
        stackable: form.stackable,
        conditionsJson,
        actionsJson,
        metadataJson: {
          distributionModel: choice.key,
          customerOutcome: choice.customerOutcome,
        },
        maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
        maxRedemptionsPerUser:
          choice.promotionType === "AUTO"
            ? 1
            : Number(form.maxRedemptionsPerUser || 1),
        validFrom: fromLocalInput(form.startAt),
        validTo: fromLocalInput(form.endAt),
      },
    });
  };

  return (
    <ModalShell title="Tạo chương trình khuyến mãi" icon={Gift} onClose={onClose}>
      <div className="border-b border-zinc-800 px-5 py-4">
        <p className="text-xs font-black text-orange-300">Bước {step + 1}/6</p>
        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-zinc-800">
          <div
            className="h-full rounded-full bg-orange-500 transition-all"
            style={{ width: `${((step + 1) / steps.length) * 100}%` }}
          />
        </div>
        <p className="mt-2 text-sm font-black text-white">{steps[step]}</p>
      </div>
      <div className="p-5">
        {step === 0 && (
          <div>
            <h3 className="text-base font-black text-white">Khách hàng sẽ nhận ưu đãi này như thế nào?</h3>
            <p className="mt-1 text-xs leading-5 text-zinc-500">Bạn không cần biết tên kỹ thuật của loại ưu đãi.</p>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {deliveryChoices.map((item) => {
                const Icon = item.icon;
                const selected = form.deliveryKey === item.key;
                return (
                  <button
                    key={item.key}
                    type="button"
                    onClick={() => update("deliveryKey", item.key)}
                    className={`rounded-xl border p-4 text-left transition-colors ${selected ? "border-orange-500 bg-orange-500/[0.08]" : "border-zinc-800 bg-zinc-950/30 hover:border-zinc-700"}`}
                  >
                    <Icon className={`h-6 w-6 ${selected ? "text-orange-300" : "text-zinc-500"}`} />
                    <span className="mt-3 block text-sm font-black text-white">{item.title}</span>
                    <span className="mt-2 block text-xs leading-5 text-zinc-500">{item.description}</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}
        {step === 1 && (
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Tên chương trình" required wide>
              <input value={form.name} onChange={(event) => update("name", event.target.value)} placeholder="VD: Chào thành viên mới - giảm 10%" className={fieldClass} />
            </Field>
            <Field label="Mô tả khách hàng sẽ đọc" wide>
              <textarea value={form.description} onChange={(event) => update("description", event.target.value)} placeholder="Mô tả ngắn gọn quyền lợi và điều kiện chính" className={`${fieldClass} h-20 py-2`} />
            </Field>
            <Field label="Quyền lợi" required>
              <select value={form.actionType} onChange={(event) => update("actionType", event.target.value)} className={fieldClass}>
                <option value="PERCENTAGE">Giảm theo phần trăm</option>
                <option value="FIXED_AMOUNT">Giảm một số tiền</option>
                <option value="FULL_DISCOUNT">Miễn phí toàn bộ</option>
              </select>
            </Field>
            {form.actionType !== "FULL_DISCOUNT" && (
              <Field label={form.actionType === "PERCENTAGE" ? "Phần trăm giảm" : "Số tiền giảm"} required>
                <input type="number" min="1" max={form.actionType === "PERCENTAGE" ? 100 : undefined} value={form.actionValue} onChange={(event) => update("actionValue", event.target.value)} className={fieldClass} />
              </Field>
            )}
            {form.actionType === "PERCENTAGE" && (
              <Field label="Mức giảm tối đa">
                <input type="number" min="1" value={form.maxDiscountAmount} onChange={(event) => update("maxDiscountAmount", event.target.value)} placeholder="Không giới hạn" className={fieldClass} />
              </Field>
            )}
            <Field label="Giá trị đơn hàng tối thiểu">
              <input type="number" min="0" value={form.minimumOrderAmount} onChange={(event) => update("minimumOrderAmount", event.target.value)} placeholder="Không yêu cầu" className={fieldClass} />
            </Field>
          </div>
        )}
        {step === 2 && (
          <div>
            <EntityScopePicker
              icon={Building2}
              title="Rạp áp dụng"
              values={form.cinemaPublicIds}
              selectedOptions={form.cinemaScopeOptions}
              onChange={(cinemaPublicIds, cinemaScopeOptions) =>
                setForm((current) => ({ ...current, cinemaPublicIds, cinemaScopeOptions }))
              }
              loadOptions={loadCinemas}
              normalise={normaliseCinema}
              emptyHint={isManager ? "Quản lý phải chọn ít nhất một rạp được phân công." : "Không chọn rạp nghĩa là áp dụng toàn hệ thống."}
            />
            <div className="mt-4 rounded-xl border border-sky-500/20 bg-sky-500/[0.05] p-4 text-sm text-sky-100">
              Chương trình áp dụng tại: <b>{selectedCinemaNames.length ? selectedCinemaNames.join(", ") : "Toàn hệ thống"}</b>
            </div>
          </div>
        )}
        {step === 3 && (
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Bắt đầu" required><input type="datetime-local" value={form.startAt} onChange={(event) => update("startAt", event.target.value)} className={fieldClass} /></Field>
            <Field label="Kết thúc" required><input type="datetime-local" value={form.endAt} onChange={(event) => update("endAt", event.target.value)} className={fieldClass} /></Field>
            <Field label="Ngân sách tối đa" required><input type="number" min="1" value={form.budgetAmount} onChange={(event) => update("budgetAmount", event.target.value)} className={fieldClass} /></Field>
            <Field label="Số đơn tối đa"><input type="number" min="1" value={form.maxRedemptions} onChange={(event) => update("maxRedemptions", event.target.value)} placeholder="Không giới hạn" className={fieldClass} /></Field>
            {choice?.promotionType !== "AUTO" && (
              <Field label="Số lượt tối đa mỗi khách"><input type="number" min="1" value={form.maxRedemptionsPerUser} onChange={(event) => update("maxRedemptionsPerUser", event.target.value)} className={fieldClass} /></Field>
            )}
          </div>
        )}
        {step === 4 && choice && (
          <div className="space-y-4">
            <div className="rounded-2xl border border-orange-500/25 bg-gradient-to-br from-orange-500/10 to-zinc-950 p-5">
              <p className="text-[10px] font-black uppercase tracking-wide text-orange-300">Xem trước phía khách hàng</p>
              <h3 className="mt-3 text-xl font-black text-white">{form.name || "Tên chương trình"}</h3>
              <p className="mt-2 text-2xl font-black text-emerald-400">{discountPreview}</p>
              <p className="mt-3 text-sm leading-6 text-zinc-300">{form.description || choice.customerOutcome}</p>
              <p className="mt-4 rounded-lg bg-black/25 px-3 py-2 text-xs leading-5 text-zinc-400">{choice.customerOutcome}</p>
            </div>
            <p className="text-xs text-zinc-500">Nếu câu chữ chưa rõ với khách hàng, hãy quay lại sửa tên hoặc mô tả trước khi gửi duyệt.</p>
          </div>
        )}
        {step === 5 && choice && (
          <div className="space-y-4">
            <div className="rounded-xl border border-zinc-800 bg-zinc-950/35 p-4">
              <h3 className="font-black text-white">{form.name}</h3>
              <div className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
                <DetailItem label="Cách khách nhận">{choice.title}</DetailItem>
                <DetailItem label="Quyền lợi">{discountPreview}</DetailItem>
                <DetailItem label="Phạm vi">{selectedCinemaNames.length ? selectedCinemaNames.join(", ") : "Toàn hệ thống"}</DetailItem>
                <DetailItem label="Ngân sách">{money(form.budgetAmount)}</DetailItem>
                <DetailItem label="Hiệu lực">{dateTime(fromLocalInput(form.startAt))} → {dateTime(fromLocalInput(form.endAt))}</DetailItem>
                <DetailItem label="Sau khi gửi">Chờ một người khác phê duyệt</DetailItem>
              </div>
            </div>
            <details className="rounded-xl border border-zinc-800 p-4">
              <summary className="cursor-pointer text-xs font-black text-zinc-400">Thiết lập nâng cao</summary>
              <div className="mt-4 space-y-3">
                <Toggle checked={form.stackable} onChange={(value) => update("stackable", value)} label="Cho phép dùng chung với một ưu đãi khác khi hệ thống xác nhận tương thích" />
                <p className="text-[11px] leading-5 text-zinc-600">Mã nội bộ, thứ tự ưu tiên, tự kích hoạt, tự hoàn tất và dừng khi hết ngân sách sẽ được hệ thống thiết lập tự động.</p>
              </div>
            </details>
          </div>
        )}
        {error && <p role="alert" className="mt-4 rounded-lg border border-red-500/25 bg-red-500/[0.06] px-3 py-2 text-xs text-red-300">{error}</p>}
        <div className="mt-6 flex items-center justify-between border-t border-zinc-800 pt-4">
          <button type="button" onClick={step === 0 ? onClose : () => { setError(""); setStep((current) => current - 1); }} className={`${buttonClass} border border-zinc-700 text-zinc-300`}>
            {step === 0 ? "Hủy" : "Quay lại"}
          </button>
          {step < steps.length - 1 ? (
            <button type="button" onClick={next} className={`${buttonClass} h-10 bg-orange-500 text-white`}>
              Tiếp tục <ChevronRight className="h-4 w-4" />
            </button>
          ) : (
            <button type="button" disabled={busy} onClick={submit} className={`${buttonClass} h-10 bg-orange-500 text-white`}>
              {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              {busy ? "Đang tạo chương trình..." : "Tạo và gửi duyệt"}
            </button>
          )}
        </div>
      </div>
    </ModalShell>
  );
}

function CampaignModal({
  record,
  template,
  isManager = false,
  managerCinemaPublicIds = [],
  busy,
  onClose,
  onSave,
}) {
  const editing = Boolean(record);
  const source = record || template;
  const [form, setForm] = useState(() => ({
    code: template
      ? (source?.code || generatedCode("PROGRAM")) + "-COPY"
      : source?.code || generatedCode("PROGRAM"),
    name: template ? "Bản sao " + (source?.name || "") : source?.name || "",
    description: source?.description || "",
    priority: source?.priority ?? 100,
    stackable: source?.stackable ?? false,
    exclusiveCampaign: source?.exclusiveCampaign ?? false,
    autoActivate: source?.autoActivate ?? true,
    autoComplete: source?.autoComplete ?? true,
    autoPauseWhenBudgetExceeded: source?.autoPauseWhenBudgetExceeded ?? true,
    timezone: source?.timezone || "Asia/Ho_Chi_Minh",
    startAt: toLocalInput(template ? null : source?.startAt),
    endAt: toLocalInput(template ? Date.now() + 30 * 86400_000 : source?.endAt || Date.now() + 30 * 86400_000),
    budgetAmount: source?.budgetAmount ?? 100000000,
    maxRedemptions: source?.maxRedemptions ?? "",
    maxRedemptionsPerUser: source?.maxRedemptionsPerUser ?? 1,
    legalNotificationRef: source?.legalNotificationRef || "",
    remarks: source?.remarks || "",
    cinemaPublicIds:
      isManager && Array.isArray(source?.cinemaScope)
        ? source.cinemaScope.filter((id) => managerCinemaPublicIds.includes(id))
        : [],
    cinemaScopeOptions: [],
  }));
  const [formError, setFormError] = useState("");
  const update = (key, value) =>
    setForm((current) => ({ ...current, [key]: value }));
  const loadManagerCinemas = useCallback(
    async (query) => {
      const result = nestedPageData(
        await adminCinemaService.getCinemas({
          keyword: query || undefined,
          page: 0,
          size: 100,
        }),
      );
      const rows = Array.isArray(result) ? result : result?.content || [];
      const assigned = new Set(managerCinemaPublicIds.map(String));
      return rows.filter((item) =>
        assigned.has(String(item.publicId || item.id || "")),
      );
    },
    [managerCinemaPublicIds],
  );
  const normaliseCinema = useCallback(
    (item) => ({
      value: String(item.publicId || item.id || ""),
      label: item.name || item.cinemaName || item.publicId || item.id,
    }),
    [],
  );
  const submit = (event) => {
    event.preventDefault();
    setFormError("");
    if (!editing && isManager && form.cinemaPublicIds.length === 0) {
      setFormError(
        "Hãy chọn ít nhất một rạp được phân công. Quản lý rạp không thể tạo chiến dịch toàn hệ thống.",
      );
      return;
    }
    const payload = {
      ...form,
      priority: Number(form.priority),
      budgetAmount: Number(form.budgetAmount),
      maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
      maxRedemptionsPerUser: Number(form.maxRedemptionsPerUser),
      startAt: fromLocalInput(form.startAt),
      endAt: fromLocalInput(form.endAt),
      legalNotificationRef: form.legalNotificationRef || null,
      remarks: form.remarks || null,
      ...(!editing
        ? {
            scopeType: isManager ? "ASSIGNED_CINEMAS" : "GLOBAL",
            cinemaPublicIds: isManager ? form.cinemaPublicIds : [],
          }
        : {}),
    };
    delete payload.cinemaScopeOptions;
    if (editing) {
      delete payload.code;
      delete payload.cinemaPublicIds;
    }
    onSave(payload, editing);
  };
  return (
    <ModalShell
      title={editing ? "Sửa chương trình" : template ? "Nhân bản chương trình" : "Tạo chương trình"}
      icon={CalendarClock}
      onClose={onClose}
    >
      <form onSubmit={submit} className="grid gap-4 p-5 md:grid-cols-2">
        <Field label="Tên chương trình" required wide>
          <input
            required
            value={form.name}
            onChange={(e) => update("name", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Mô tả" wide>
          <textarea
            value={form.description}
            onChange={(e) => update("description", e.target.value)}
            className={`${fieldClass} h-20 py-2`}
          />
        </Field>
        <Field label="Bắt đầu" required>
          <input
            required
            type="datetime-local"
            value={form.startAt}
            onChange={(e) => update("startAt", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Kết thúc" required>
          <input
            required
            type="datetime-local"
            value={form.endAt}
            onChange={(e) => update("endAt", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Ngân sách" required>
          <input
            required
            min="1"
            type="number"
            value={form.budgetAmount}
            onChange={(e) => update("budgetAmount", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Số đơn tối đa">
          <input
            min="1"
            type="number"
            value={form.maxRedemptions}
            onChange={(e) => update("maxRedemptions", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Tối đa mỗi khách" required>
          <input
            required
            min="1"
            type="number"
            value={form.maxRedemptionsPerUser}
            onChange={(e) => update("maxRedemptionsPerUser", e.target.value)}
            className={fieldClass}
          />
        </Field>
        {!editing && isManager && (
          <div className="md:col-span-2">
            <EntityScopePicker
              icon={Building2}
              title="Rạp áp dụng chiến dịch"
              values={form.cinemaPublicIds}
              selectedOptions={form.cinemaScopeOptions}
              onChange={(cinemaPublicIds, cinemaScopeOptions) => {
                setFormError("");
                setForm((current) => ({
                  ...current,
                  cinemaPublicIds,
                  cinemaScopeOptions,
                }));
              }}
              loadOptions={loadManagerCinemas}
              normalise={normaliseCinema}
              emptyHint="Bắt buộc chọn ít nhất một rạp trong phạm vi được phân công."
            />
            <p className="mt-2 rounded-lg border border-sky-500/20 bg-sky-500/[0.06] px-3 py-2 text-xs leading-5 text-sky-100">
              Chiến dịch áp dụng tại: {form.cinemaPublicIds.length
                ? form.cinemaPublicIds
                    .map(
                      (id) =>
                        form.cinemaScopeOptions.find(
                          (option) => option.value === id,
                        )?.label || id,
                    )
                    .join(", ")
                : "Chưa chọn rạp"}
            </p>
          </div>
        )}
        <details className="rounded-xl border border-zinc-800 p-4 md:col-span-2">
          <summary className="cursor-pointer text-xs font-black text-zinc-400">Thiết lập nâng cao</summary>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field label="Mã nội bộ">
              <input disabled={editing} value={form.code} onChange={(e) => update("code", e.target.value.toUpperCase())} className={`${fieldClass} font-mono text-xs`} />
            </Field>
            <Field label="Thứ tự ưu tiên">
              <input min="0" type="number" value={form.priority} onChange={(e) => update("priority", e.target.value)} className={fieldClass} />
            </Field>
            <Toggle checked={form.autoActivate} onChange={(v) => update("autoActivate", v)} label="Tự bắt đầu khi đến thời gian" />
            <Toggle checked={form.autoComplete} onChange={(v) => update("autoComplete", v)} label="Tự kết thúc khi hết thời gian" />
            <Toggle checked={form.autoPauseWhenBudgetExceeded} onChange={(v) => update("autoPauseWhenBudgetExceeded", v)} label="Dừng khi hết ngân sách" />
            <Toggle checked={form.exclusiveCampaign} onChange={(v) => update("exclusiveCampaign", v)} label="Không dùng chung với chương trình khác" />
            <Toggle checked={form.stackable} onChange={(v) => update("stackable", v)} label="Cho phép dùng chung với ưu đãi khác khi tương thích" />
          </div>
        </details>
        {formError && (
          <p className="rounded-lg border border-red-500/30 bg-red-500/[0.07] px-3 py-2 text-xs text-red-200 md:col-span-2">
            {formError}
          </p>
        )}
        <div className="flex justify-end gap-2 border-t border-zinc-800 pt-4 md:col-span-2">
          <button
            type="button"
            onClick={onClose}
            className={`${buttonClass} border border-zinc-700 text-zinc-300`}
          >
            Hủy
          </button>
          <button
            disabled={busy}
            className={`${buttonClass} bg-orange-500 text-white`}
          >
            {busy && <Loader2 className="h-4 w-4 animate-spin" />} Lưu
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

const dayOptions = [
  ["MONDAY", "T2"],
  ["TUESDAY", "T3"],
  ["WEDNESDAY", "T4"],
  ["THURSDAY", "T5"],
  ["FRIDAY", "T6"],
  ["SATURDAY", "T7"],
  ["SUNDAY", "CN"],
];

const parseJsonObject = (value) => {
  if (!value) return {};
  if (typeof value === "object") return value;
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
};

const conditionLabelFromSource = (source, id, index) => {
  if (!source) return "";
  if (Array.isArray(source)) {
    const matched = source.find((item) => {
      if (item == null || typeof item !== "object") return false;
      return [item.value, item.publicId, item.id, item.key]
        .filter(Boolean)
        .map(String)
        .includes(String(id));
    });
    if (matched) {
      return (
        matched.label ||
        matched.title ||
        matched.movieTitle ||
        matched.name ||
        matched.cinemaName ||
        ""
      );
    }
    return typeof source[index] === "string" ? source[index] : "";
  }
  if (typeof source === "object") {
    const value = source[id] ?? source[String(id)];
    if (typeof value === "string") return value;
    if (value && typeof value === "object") {
      return (
        value.label ||
        value.title ||
        value.movieTitle ||
        value.name ||
        value.cinemaName ||
        ""
      );
    }
  }
  return "";
};

const conditionScopeOptionsFromMetadata = (
  ids,
  metadata,
  labelKey,
  fallbackKeys = [],
) => {
  const conditionLabels = parseJsonObject(metadata?.conditionLabels);
  const sources = [
    conditionLabels?.[labelKey],
    metadata?.[labelKey],
    ...fallbackKeys.map((key) => conditionLabels?.[key] ?? metadata?.[key]),
  ];
  return ids
    .map((id, index) => {
      const label = sources
        .map((source) => conditionLabelFromSource(source, id, index))
        .find(Boolean);
      return label ? { value: String(id), label } : null;
    })
    .filter(Boolean);
};

const conditionLabelMapFromOptions = (ids, options = []) => {
  const optionMap = new Map(
    options
      .filter((item) => item?.value && item?.label)
      .map((item) => [String(item.value), item.label]),
  );
  return Object.fromEntries(
    ids
      .map((id) => [String(id), optionMap.get(String(id))])
      .filter(([id, label]) => label && label !== id),
  );
};

const generatedCode = (type) => {
  const prefix = type === "COUPON" ? "CPN" : "EVT";
  const date = new Date().toISOString().slice(2, 10).replaceAll("-", "");
  const token = Math.random().toString(36).slice(2, 7).toUpperCase();
  return `${prefix}-${date}-${token}`;
};

const isEditableCampaignOption = (item) =>
  item.item?.status === "DRAFT" &&
  ["DRAFT", "REJECTED"].includes(item.item?.approvalStatus);

function CampaignPicker({ value, campaigns, onChange, onRefresh }) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const eligible = campaigns.filter(
    (item) => isEditableCampaignOption(item) || item.value === value,
  );
  const matches = eligible.filter((item) =>
    item.label.toLowerCase().includes(query.trim().toLowerCase()),
  );
  const selected = campaigns.find((item) => item.value === value);
  return (
    <div className="relative">
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setOpen((current) => !current)}
          className={`${fieldClass} flex items-center justify-between gap-3 text-left`}
        >
          <span className={selected ? "truncate text-white" : "text-zinc-500"}>
            {selected?.label || "Chọn chiến dịch bản nháp"}
          </span>
          <ChevronDown
            className={`h-4 w-4 shrink-0 text-zinc-400 transition-transform ${open ? "rotate-180" : ""}`}
          />
        </button>
        <IconButton title="Làm mới danh sách chiến dịch" onClick={onRefresh}>
          <RefreshCw className="h-4 w-4" />
        </IconButton>
      </div>
      {open && (
        <div className="absolute z-20 mt-1 w-full overflow-hidden rounded-lg border border-zinc-700 bg-zinc-950 shadow-2xl">
          <label className="relative block border-b border-zinc-800">
            <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-600" />
            <input
              autoFocus
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo tên hoặc mã chiến dịch"
              className={`${fieldClass} border-0 pl-10`}
            />
          </label>
          <div className="max-h-52 overflow-y-auto">
            {matches.length === 0 ? (
              <p className="px-3 py-4 text-xs text-zinc-500">
                Không có chiến dịch bản nháp phù hợp.
              </p>
            ) : (
              matches.map((item) => (
                <button
                  type="button"
                  key={item.value}
                  onClick={() => {
                    onChange(item.value);
                    setOpen(false);
                    setQuery("");
                  }}
                  className={`block w-full px-3 py-3 text-left text-xs transition-colors ${item.value === value ? "bg-orange-500/10 text-orange-200" : "text-zinc-300 hover:bg-zinc-800"}`}
                >
                  <span className="block font-bold">
                    {item.item?.name || item.label}
                  </span>
                  <span className="mt-1 block text-[10px] text-zinc-500">
                    {item.item?.code} · {dateTime(item.item?.startAt)} đến{" "}
                    {dateTime(item.item?.endAt)}
                  </span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function EntityScopePicker({
  icon: Icon,
  title,
  values,
  selectedOptions = [],
  onChange,
  loadOptions,
  normalise,
  emptyHint = "Để trống nếu áp dụng cho tất cả.",
}) {
  const [query, setQuery] = useState("");
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(async () => {
      setLoading(true);
      setError("");
      try {
        const result = await loadOptions(query);
        if (active)
          setOptions(result.map(normalise).filter((item) => item.value));
      } catch {
        if (active) setError("Không thể tải dữ liệu lúc này.");
      } finally {
        if (active) setLoading(false);
      }
    }, 200);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [loadOptions, normalise, query]);
  const known = new Map(
    [...selectedOptions, ...options].map((item) => [item.value, item]),
  );
  const add = (item) => {
    if (!values.includes(item.value)) {
      onChange(
        [...values, item.value],
        [
          ...selectedOptions.filter((option) => option.value !== item.value),
          item,
        ],
      );
    }
    setQuery("");
  };
  const remove = (value) => {
    const nextValues = values.filter((item) => item !== value);
    onChange(
      nextValues,
      selectedOptions.filter((item) => nextValues.includes(item.value)),
    );
  };
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-950/50 p-3">
      <div className="flex items-center gap-2">
        <Icon className="h-4 w-4 text-orange-300" />
        <p className="text-xs font-black text-white">{title}</p>
      </div>
      <p className="mt-1 text-[10px] leading-4 text-zinc-500">
        {emptyHint}
      </p>
      <label className="relative mt-3 block">
        <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-600" />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={`Tìm ${title.toLowerCase()}`}
          className={`${fieldClass} pl-10`}
        />
      </label>
      {values.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {values.map((value) => (
            <span
              key={value}
              className="inline-flex max-w-full items-center gap-1 rounded-full border border-orange-500/30 bg-orange-500/10 px-2 py-1 text-[10px] text-orange-100"
            >
              <span className="truncate">
                {known.get(value)?.label || value}
              </span>
              <button
                type="button"
                title="Bỏ chọn"
                onClick={() => remove(value)}
                className="text-orange-300 hover:text-white"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>
      )}
      <div className="mt-2 max-h-36 overflow-y-auto rounded-lg border border-zinc-800">
        {loading ? (
          <p className="px-3 py-3 text-xs text-zinc-500">Đang tìm...</p>
        ) : error ? (
          <p className="px-3 py-3 text-xs text-red-400">{error}</p>
        ) : options.filter((item) => !values.includes(item.value)).length ===
          0 ? (
          <p className="px-3 py-3 text-xs text-zinc-600">
            Không còn kết quả để chọn.
          </p>
        ) : (
          options
            .filter((item) => !values.includes(item.value))
            .map((item) => (
              <button
                type="button"
                key={item.value}
                onClick={() => add(item)}
                className="flex w-full items-center justify-between gap-3 border-b border-zinc-800 px-3 py-2 text-left text-xs text-zinc-300 last:border-0 hover:bg-zinc-800"
              >
                <span className="truncate font-bold">{item.label}</span>
                <Plus className="h-3.5 w-3.5 shrink-0 text-orange-300" />
              </button>
            ))
        )}
      </div>
    </div>
  );
}

function PromotionModal({
  record,
  mode = "create",
  cloneWarning = false,
  promotionType,
  campaigns,
  busy,
  onClose,
  onSave,
  onRefreshCampaigns,
  onCreateCampaign,
}) {
  const editing = mode === "edit";
  const cloning = mode === "clone";
  const type = record?.promotionType || promotionType || "AUTO";
  const initialRecord = cloning
    ? {
        ...record,
        campaignPublicId: record?.suggestedCampaignPublicId || "",
        code: record?.suggestedCode || "",
        name: record?.suggestedName || "",
        validFrom: record?.suggestedValidFrom,
        validTo: record?.suggestedValidTo,
      }
    : record;
  const rawConditions = parseJsonObject(initialRecord?.conditionsJson);
  const rawActions = parseJsonObject(initialRecord?.actionsJson);
  const rawMetadata = parseJsonObject(initialRecord?.metadataJson);
  const existingAction = Array.isArray(rawActions)
    ? rawActions[0] || {}
    : rawActions;
  const initialMoviePublicIds = Array.isArray(rawConditions.moviePublicIds)
    ? rawConditions.moviePublicIds
    : [];
  const initialCinemaPublicIds = Array.isArray(rawConditions.cinemaPublicIds)
    ? rawConditions.cinemaPublicIds
    : [];
  const knownConditionKeys = new Set([
    "minimumOrderAmount",
    "minOrderAmount",
    "requiredTierCode",
    "requiresVerification",
    "dayOfWeek",
    "moviePublicIds",
    "cinemaPublicIds",
  ]);
  const preservedConditions = Object.fromEntries(
    Object.entries(rawConditions).filter(
      ([key]) => !knownConditionKeys.has(key),
    ),
  );
  const defaultCampaign =
    campaigns.find((item) => item.value === initialRecord?.campaignPublicId) ||
    (!cloning && !editing
      ? campaigns.find(isEditableCampaignOption)
      : undefined);
  const [step, setStep] = useState(0);
  const [form, setForm] = useState(() => ({
    campaignPublicId:
      initialRecord?.campaignPublicId || defaultCampaign?.value || "",
    code:
      type === "VOUCHER"
        ? cloning
          ? initialRecord?.code || ""
          : initialRecord?.code || generatedCode(type)
        : initialRecord?.code || "",
    name: initialRecord?.name || "",
    description: initialRecord?.description || "",
    publicVisible:
      type === "VOUCHER"
        ? (initialRecord?.publicVisible ?? !cloning)
        : false,
    priority: initialRecord?.priority ?? 100,
    maxRedemptions: initialRecord?.maxRedemptions ?? "",
    maxRedemptionsPerUser: initialRecord?.maxRedemptionsPerUser ?? 1,
    validFrom: toLocalInput(
      initialRecord?.validFrom || defaultCampaign?.item?.startAt,
    ),
    validTo: toLocalInput(
      initialRecord?.validTo ||
        defaultCampaign?.item?.endAt ||
        Date.now() + 30 * 86400_000,
    ),
    minimumOrderAmount:
      rawConditions.minimumOrderAmount ?? rawConditions.minOrderAmount ?? "",
    requiredTierCode: rawConditions.requiredTierCode || "",
    requiresVerification: Boolean(rawConditions.requiresVerification),
    stackable: Boolean(initialRecord?.stackable),
    dayOfWeek: Array.isArray(rawConditions.dayOfWeek)
      ? rawConditions.dayOfWeek
      : [],
    moviePublicIds: initialMoviePublicIds,
    movieScopeOptions: conditionScopeOptionsFromMetadata(
      initialMoviePublicIds,
      rawMetadata,
      "moviePublicIds",
      ["movieIds", "movieNames", "movieTitles"],
    ),
    cinemaPublicIds: initialCinemaPublicIds,
    cinemaScopeOptions: conditionScopeOptionsFromMetadata(
      initialCinemaPublicIds,
      rawMetadata,
      "cinemaPublicIds",
      ["cinemaIds", "cinemaNames"],
    ),
    actionType:
      existingAction.discountType || existingAction.type || "PERCENTAGE",
    actionValue:
      existingAction.discountValue ??
      existingAction.value ??
      existingAction.amount ??
      existingAction.percentage ??
      10,
    maxDiscountAmount: existingAction.maxDiscountAmount ?? "",
  }));
  const [formError, setFormError] = useState("");
  const [serverFieldErrors, setServerFieldErrors] = useState({});
  const [saveConfirm, setSaveConfirm] = useState(null);
  const model = promotionModelFor(type);
  const update = (key, value) => {
    setSaveConfirm(null);
    setServerFieldErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
    setForm((current) => ({ ...current, [key]: value }));
  };
  const updateScope = (idsKey, optionsKey, values, selectedOptions = []) => {
    setSaveConfirm(null);
    setServerFieldErrors((current) => {
      if (!current.campaignPublicId) return current;
      const next = { ...current };
      delete next.campaignPublicId;
      return next;
    });
    setForm((current) => ({
      ...current,
      [idsKey]: values,
      [optionsKey]: selectedOptions,
    }));
  };
  const currentCampaign = campaigns.find(
    (item) => item.value === form.campaignPublicId,
  )?.item;
  const stackingBlockedByCampaign =
    Boolean(currentCampaign) && !currentCampaign.stackable;
  const stackingToggleDisabled = !currentCampaign || stackingBlockedByCampaign;
  const hasEditableCampaign = campaigns.some(isEditableCampaignOption);
  const steps = [
    "Chương trình",
    "Quyền lợi",
    "Phạm vi áp dụng",
    "Thời gian & hạn mức",
  ];
  const chooseCampaign = (campaignPublicId) => {
    const campaign = campaigns.find(
      (item) => item.value === campaignPublicId,
    )?.item;
    setSaveConfirm(null);
    setForm((current) => ({
      ...current,
      campaignPublicId,
      validFrom:
        editing || !campaign?.startAt
          ? current.validFrom
          : toLocalInput(campaign.startAt),
      validTo:
        editing || !campaign?.endAt
          ? current.validTo
          : toLocalInput(campaign.endAt),
    }));
  };
  const toggleDay = (value) => {
    setSaveConfirm(null);
    update(
      "dayOfWeek",
      form.dayOfWeek.includes(value)
        ? form.dayOfWeek.filter((item) => item !== value)
        : [...form.dayOfWeek, value],
    );
  };
  const loadMovies = useCallback(async (query) => {
    const result = nestedPageData(
      await adminMovieService.getMovies({
        keyword: query || undefined,
        page: 0,
        size: 20,
        sort: "title,asc",
      }),
    );
    return Array.isArray(result) ? result : result?.content || [];
  }, []);
  const loadCinemas = useCallback(async (query) => {
    const result = nestedPageData(
      await adminCinemaService.getCinemas({
        keyword: query || undefined,
        page: 0,
        size: 20,
      }),
    );
    return Array.isArray(result) ? result : result?.content || [];
  }, []);
  const movieOption = useCallback(
    (item) => ({
      value: String(item.publicId || item.id || ""),
      label: item.title || item.movieTitle || item.name || item.publicId,
    }),
    [],
  );
  const cinemaOption = useCallback(
    (item) => ({
      value: String(item.publicId || item.id || ""),
      label: item.name || item.cinemaName || item.publicId,
    }),
    [],
  );
  const validateStep = (target) => {
    setFormError("");
    if (target === 0 && (!form.campaignPublicId || !form.name.trim())) {
      setFormError("Hãy chọn chiến dịch và nhập tên hiển thị.");
      return false;
    }
    if (target === 0 && type === "VOUCHER" && !form.code.trim()) {
      setFormError("Hãy nhập mã phân phối cho voucher.");
      return false;
    }
    if (
      target === 1 &&
      form.actionType !== "FULL_DISCOUNT" &&
      Number(form.actionValue) <= 0
    ) {
      setFormError("Giá trị giảm phải lớn hơn 0.");
      return false;
    }
    if (target === 3) {
      if (
        new Date(form.validTo).getTime() <= new Date(form.validFrom).getTime()
      ) {
        setFormError("Thời điểm kết thúc phải sau thời điểm bắt đầu.");
        return false;
      }
      if (
        form.maxRedemptions &&
        Number(form.maxRedemptionsPerUser) > Number(form.maxRedemptions)
      ) {
        setFormError(
          "Lượt tối đa mỗi khách không thể lớn hơn tổng lượt phát hành.",
        );
        return false;
      }
    }
    return true;
  };
  const buildPayload = () => {
    const conditionsJson = {
      ...preservedConditions,
      ...(Number(form.minimumOrderAmount) > 0
        ? { minimumOrderAmount: Number(form.minimumOrderAmount) }
        : {}),
      ...(form.requiredTierCode.trim()
        ? { requiredTierCode: form.requiredTierCode.trim().toUpperCase() }
        : {}),
      ...(form.requiresVerification ? { requiresVerification: true } : {}),
      ...(form.dayOfWeek.length ? { dayOfWeek: form.dayOfWeek } : {}),
      ...(form.moviePublicIds.length
        ? { moviePublicIds: form.moviePublicIds }
        : {}),
      ...(form.cinemaPublicIds.length
        ? { cinemaPublicIds: form.cinemaPublicIds }
        : {}),
    };
    const actionsJson =
      form.actionType === "FULL_DISCOUNT"
        ? { discountType: "FULL_DISCOUNT" }
        : {
            discountType: form.actionType,
            discountValue: Number(form.actionValue),
            ...(form.actionType === "PERCENTAGE" &&
            Number(form.maxDiscountAmount) > 0
              ? { maxDiscountAmount: Number(form.maxDiscountAmount) }
              : {}),
          };
    const movieConditionLabels = conditionLabelMapFromOptions(
      form.moviePublicIds,
      form.movieScopeOptions,
    );
    const cinemaConditionLabels = conditionLabelMapFromOptions(
      form.cinemaPublicIds,
      form.cinemaScopeOptions,
    );
    const conditionLabels = {
      ...parseJsonObject(rawMetadata.conditionLabels),
      ...(Object.keys(movieConditionLabels).length
        ? { moviePublicIds: movieConditionLabels }
        : {}),
      ...(Object.keys(cinemaConditionLabels).length
        ? { cinemaPublicIds: cinemaConditionLabels }
        : {}),
    };
    if (!form.moviePublicIds.length) delete conditionLabels.moviePublicIds;
    if (!form.cinemaPublicIds.length) delete conditionLabels.cinemaPublicIds;
    const metadataJson = {
      ...rawMetadata,
      distributionModel: model.key,
    };
    if (Object.keys(conditionLabels).length) {
      metadataJson.conditionLabels = conditionLabels;
    } else {
      delete metadataJson.conditionLabels;
    }
    return {
      campaignPublicId: form.campaignPublicId,
      promotionType: type,
      code:
        type === "VOUCHER" || (cloning && type === "COUPON")
          ? form.code.trim() || null
          : null,
      name: form.name.trim(),
      description: form.description.trim(),
      publicVisible: type === "VOUCHER" ? form.publicVisible : false,
      priority: Number(form.priority),
      stackable: form.stackable,
      conditionsJson,
      actionsJson,
      metadataJson,
      maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
      maxRedemptionsPerUser:
        type === "AUTO" ? 1 : Number(form.maxRedemptionsPerUser),
      validFrom: fromLocalInput(form.validFrom),
      validTo: fromLocalInput(form.validTo),
      ...(cloning ? { clonedFromPublicId: record.sourcePublicId } : {}),
    };
  };
  const goNext = () => {
    setSaveConfirm(null);
    if (validateStep(step)) {
      setStep((current) => Math.min(current + 1, steps.length - 1));
    }
  };
  const requestSave = () => {
    setSaveConfirm(null);
    if (!validateStep(0) || !validateStep(1) || !validateStep(3)) return;
    setSaveConfirm(buildPayload());
  };
  const confirmSave = async () => {
    try {
      await onSave(saveConfirm, mode);
    } catch (error) {
      setSaveConfirm(null);
      const friendlyError = errorText(error);
      const backendMessage = String(
        error?.response?.data?.message ||
          error?.response?.data?.error?.message ||
          "",
      );
      const campaignLocked =
        cloning &&
        error?.response?.status === 409 &&
        /campaign.*locked|campaign configuration/i.test(backendMessage);
      const duplicateCode =
        error?.response?.status === 409 && /code/i.test(backendMessage);
      if (campaignLocked || duplicateCode) {
        setStep(0);
        setServerFieldErrors((current) => ({
          ...current,
          ...(campaignLocked ? { campaignPublicId: friendlyError } : {}),
          ...(duplicateCode ? { code: friendlyError } : {}),
        }));
      } else {
        setFormError(friendlyError);
      }
      if (campaignLocked) {
        setForm((current) => ({ ...current, campaignPublicId: "" }));
        await onRefreshCampaigns();
      }
    }
  };
  const submit = (event) => {
    event.preventDefault();
    if (step < steps.length - 1) {
      goNext();
      return;
    }
    requestSave();
  };
  const previewPromotion = {
    name: form.name,
    conditionsJson: {
      ...preservedConditions,
      minimumOrderAmount: Number(form.minimumOrderAmount) || undefined,
      requiredTierCode: form.requiredTierCode || undefined,
      dayOfWeek: form.dayOfWeek,
      moviePublicIds: form.moviePublicIds,
      cinemaPublicIds: form.cinemaPublicIds,
    },
    actionsJson:
      form.actionType === "FULL_DISCOUNT"
        ? { discountType: "FULL_DISCOUNT" }
        : {
            discountType: form.actionType,
            discountValue: form.actionValue,
            maxDiscountAmount: form.maxDiscountAmount,
          },
  };
  const modalAction = editing ? "Sửa" : cloning ? "Nhân bản" : "Tạo";
  return (
    <ModalShell
      title={`${modalAction} ${model.label.toLowerCase()}`}
      icon={BadgePercent}
      onClose={onClose}
    >
      <form onSubmit={submit} className="p-5">
        <div className="flex items-start gap-3 rounded-lg border border-orange-500/20 bg-orange-500/[0.06] px-3 py-2">
          {type === "AUTO" ? (
            <Globe2 className="mt-0.5 h-4 w-4 shrink-0 text-orange-300" />
          ) : type === "VOUCHER" ? (
            <Gift className="mt-0.5 h-4 w-4 shrink-0 text-orange-300" />
          ) : (
            <Tag className="mt-0.5 h-4 w-4 shrink-0 text-orange-300" />
          )}
          <div>
            <p className="text-xs font-black text-white">{model.label}</p>
            <p className="mt-1 text-[11px] leading-5 text-zinc-400">
              {model.description}
            </p>
          </div>
        </div>
        {cloning && (
          <div
            className={`mt-3 flex items-start gap-2 border-l-2 px-3 py-2 text-xs leading-5 ${cloneWarning ? "border-amber-500 bg-amber-500/[0.07] text-amber-200" : "border-sky-500 bg-sky-500/[0.07] text-sky-200"}`}
          >
            {cloneWarning ? (
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            ) : (
              <Copy className="mt-0.5 h-4 w-4 shrink-0" />
            )}
            <span>
              <strong>Nhân bản từ: {record.sourceName}.</strong>{" "}
              {cloneWarning
                ? "Chiến dịch gốc đã khóa cấu hình. Vui lòng chọn một chiến dịch khác."
                : "Hãy kiểm tra lại dữ liệu trước khi tạo bản sao."}
            </span>
          </div>
        )}
        <div className="mt-5 grid grid-cols-4 gap-1 rounded-lg bg-zinc-950 p-1">
          {steps.map((label, index) => (
            <button
              type="button"
              key={label}
              onClick={() => {
                if (index <= step || validateStep(step)) {
                  setSaveConfirm(null);
                  setStep(index);
                }
              }}
              className={`min-h-10 rounded-md px-2 text-[10px] font-black transition-colors ${index === step ? "bg-orange-500 text-white" : index < step ? "text-orange-300 hover:bg-zinc-800" : "text-zinc-600 hover:bg-zinc-800"}`}
            >
              <span className="mr-1">{index + 1}.</span>
              {label}
            </button>
          ))}
        </div>
        <div className="mt-5">
          {step === 0 && (
            <div className="grid gap-4 md:grid-cols-2">
              <Field
                label="Chương trình"
                required
                wide
                hint={
                  currentCampaign
                    ? `Hiệu lực: ${dateTime(currentCampaign.startAt)} đến ${dateTime(currentCampaign.endAt)}`
                    : "Chỉ chương trình đang soạn mới có thể thêm ưu đãi."
                }
              >
                <CampaignPicker
                  value={form.campaignPublicId}
                  campaigns={campaigns}
                  onChange={chooseCampaign}
                  onRefresh={onRefreshCampaigns}
                />
                {serverFieldErrors.campaignPublicId && (
                  <p role="alert" className="mt-1 text-[11px] text-red-400">
                    {serverFieldErrors.campaignPublicId}
                  </p>
                )}
              </Field>
              {cloning && !form.campaignPublicId && !hasEditableCampaign && (
                <div className="border-l-2 border-amber-500 bg-amber-500/[0.05] px-3 py-3 text-xs leading-5 text-amber-200 md:col-span-2">
                  <p>
                    Không có chiến dịch nào đang ở trạng thái nháp để nhận bản
                    sao này.
                  </p>
                  <button
                    type="button"
                    onClick={onCreateCampaign}
                    className="mt-2 font-black text-orange-300 hover:text-orange-200"
                  >
                    Tạo chương trình mới
                  </button>
                </div>
              )}
              <Field label="Tên hiển thị" required>
                <input
                  required
                  maxLength="255"
                  value={form.name}
                  onChange={(e) => update("name", e.target.value)}
                  placeholder={
                    type === "AUTO"
                      ? "Giảm giá thành viên tháng 8"
                      : type === "VOUCHER"
                        ? "Voucher lễ hội điện ảnh"
                        : "Coupon tri ân khách hàng"
                  }
                  className={fieldClass}
                />
              </Field>
              <Field label="Mô tả cho khách hàng" wide>
                <textarea
                  value={form.description}
                  onChange={(e) => update("description", e.target.value)}
                  placeholder="Mô tả ngắn gọn quyền lợi và phạm vi áp dụng"
                  className={`${fieldClass} h-24 py-2`}
                />
              </Field>
              {type !== "AUTO" && (
                <details className="rounded-xl border border-zinc-800 p-4 md:col-span-2">
                  <summary className="cursor-pointer text-xs font-black text-zinc-400">
                    Thiết lập nâng cao
                  </summary>
                  <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <Field label={type === "VOUCHER" ? "Mã voucher" : "Mã nội bộ"}>
                      {type === "COUPON" ? (
                        <div className="flex h-10 items-center rounded-lg border border-zinc-800 bg-zinc-950 px-3 text-xs text-zinc-500">
                          Hệ thống tự tạo mã riêng cho từng khách
                        </div>
                      ) : (
                        <div className="flex gap-2">
                          <input
                            value={form.code}
                            onChange={(event) => update("code", event.target.value.toUpperCase())}
                            maxLength="100"
                            className={`${fieldClass} font-mono text-xs`}
                          />
                          <IconButton
                            title="Gợi ý mã mới"
                            onClick={() => update("code", generatedCode("VOUCHER"))}
                          >
                            <RotateCw className="h-4 w-4" />
                          </IconButton>
                        </div>
                      )}
                      {serverFieldErrors.code && (
                        <p role="alert" className="mt-1 text-[11px] text-red-400">
                          {serverFieldErrors.code}
                        </p>
                      )}
                    </Field>
                    {type === "VOUCHER" && (
                      <div className="flex items-center pt-5">
                        <Toggle
                          checked={form.publicVisible}
                          onChange={(value) => update("publicVisible", value)}
                          label="Cho phép khách tự nhận voucher này"
                        />
                      </div>
                    )}
                  </div>
                </details>
              )}
            </div>
          )}
          {step === 1 && (
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Kiểu giảm" required>
                <select
                  value={form.actionType}
                  onChange={(e) => update("actionType", e.target.value)}
                  className={fieldClass}
                >
                  <option value="PERCENTAGE">Giảm phần trăm</option>
                  <option value="FIXED_AMOUNT">Giảm số tiền</option>
                  <option value="FULL_DISCOUNT">Miễn phí toàn bộ</option>
                </select>
              </Field>
              <Field
                label={
                  form.actionType === "PERCENTAGE"
                    ? "Phần trăm giảm (tối đa 100%)"
                    : "Số tiền giảm"
                }
                required
              >
                {form.actionType === "FULL_DISCOUNT" ? (
                  <div className="flex h-10 items-center gap-2 text-xs font-bold text-emerald-300">
                    <Gift className="h-4 w-4" /> Giảm toàn bộ đơn đủ điều kiện
                  </div>
                ) : (
                  <input
                    required
                    min="1"
                    max={form.actionType === "PERCENTAGE" ? 100 : undefined}
                    type="number"
                    value={form.actionValue}
                    onChange={(e) => update("actionValue", e.target.value)}
                    className={fieldClass}
                  />
                )}
              </Field>
              {form.actionType === "PERCENTAGE" && (
                <Field label="Mức giảm tối đa">
                  <input
                    min="1"
                    type="number"
                    value={form.maxDiscountAmount}
                    onChange={(e) =>
                      update("maxDiscountAmount", e.target.value)
                    }
                    placeholder="Không giới hạn"
                    className={fieldClass}
                  />
                </Field>
              )}
              <Field label="Đơn hàng tối thiểu">
                <input
                  min="0"
                  type="number"
                  value={form.minimumOrderAmount}
                  onChange={(e) => update("minimumOrderAmount", e.target.value)}
                  placeholder="0"
                  className={fieldClass}
                />
              </Field>
              <details className="rounded-xl border border-zinc-800 p-4 md:col-span-2">
                <summary className="cursor-pointer text-xs font-black text-zinc-400">
                  Thiết lập nâng cao
                </summary>
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  <Field label="Thứ tự hệ thống xét ưu đãi">
                    <input
                      min="0"
                      type="number"
                      value={form.priority}
                      onChange={(e) => update("priority", e.target.value)}
                      className={fieldClass}
                    />
                  </Field>
                  <div className="flex items-center pt-5">
                    <Toggle
                      checked={form.stackable}
                      onChange={(value) => update("stackable", value)}
                      label="Cho phép dùng chung với một ưu đãi khác"
                      disabled={stackingToggleDisabled}
                    />
                  </div>
                  {stackingBlockedByCampaign && (
                    <p role="alert" className="rounded-lg border border-amber-500/25 bg-amber-500/[0.08] px-3 py-2 text-[11px] font-bold leading-5 text-amber-200 md:col-span-2">
                      Chương trình này đang không cho phép dùng chung nhiều ưu đãi.
                    </p>
                  )}
                  {form.stackable && !stackingBlockedByCampaign && (
                    <p className="text-[10px] leading-4 text-amber-300 md:col-span-2">
                      Hệ thống chỉ kết hợp khi tất cả chương trình và ưu đãi liên quan đều cho phép.
                    </p>
                  )}
                </div>
              </details>
            </div>
          )}
          {step === 2 && (
            <div className="grid gap-4">
              <p className="text-xs leading-5 text-zinc-400">
                Thiết lập phạm vi chỉ áp dụng cho phim hoặc rạp cụ thể. Các lựa
                chọn sẽ được hệ thống kiểm tra lại khi khách thanh toán.
              </p>
              <div className="grid gap-4 md:grid-cols-2">
                <EntityScopePicker
                  icon={Film}
                  title="Phim áp dụng"
                  values={form.moviePublicIds}
                  selectedOptions={form.movieScopeOptions}
                  onChange={(value, selectedOptions) =>
                    updateScope(
                      "moviePublicIds",
                      "movieScopeOptions",
                      value,
                      selectedOptions,
                    )
                  }
                  loadOptions={loadMovies}
                  normalise={movieOption}
                />
                <EntityScopePicker
                  icon={Building2}
                  title="Rạp áp dụng"
                  values={form.cinemaPublicIds}
                  selectedOptions={form.cinemaScopeOptions}
                  onChange={(value, selectedOptions) =>
                    updateScope(
                      "cinemaPublicIds",
                      "cinemaScopeOptions",
                      value,
                      selectedOptions,
                    )
                  }
                  loadOptions={loadCinemas}
                  normalise={cinemaOption}
                />
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <Field label="Hạng thành viên">
                  <input
                    value={form.requiredTierCode}
                    onChange={(e) =>
                      update("requiredTierCode", e.target.value.toUpperCase())
                    }
                    placeholder="VD: GOLD"
                    className={fieldClass}
                  />
                </Field>
                <div className="flex items-center pt-5">
                  <Toggle
                    checked={form.requiresVerification}
                    onChange={(v) => update("requiresVerification", v)}
                    label="Yêu cầu tài khoản đã xác thực"
                  />
                </div>
              </div>
              <Field label="Ngày áp dụng">
                <div className="grid grid-cols-7 gap-1">
                  {dayOptions.map(([value, label]) => (
                    <button
                      type="button"
                      key={value}
                      onClick={() => toggleDay(value)}
                      className={`h-9 rounded-lg border text-[10px] font-black ${form.dayOfWeek.includes(value) ? "border-orange-500 bg-orange-500/10 text-orange-300" : "border-zinc-700 text-zinc-500 hover:text-white"}`}
                    >
                      {label}
                    </button>
                  ))}
                </div>
                <span className="mt-1 block text-[10px] text-zinc-600">
                  Không chọn nghĩa là áp dụng mọi ngày.
                </span>
              </Field>
              {Object.keys(preservedConditions).length > 0 && (
                <div className="flex items-start gap-2 rounded-lg border border-amber-500/20 bg-amber-500/[0.05] px-3 py-2 text-[11px] leading-5 text-amber-200">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                  <span>
                    Các điều kiện cấu hình trước đó vẫn được giữ nguyên khi lưu.
                  </span>
                </div>
              )}
            </div>
          )}
          {step === 3 && (
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Bắt đầu" required>
                <input
                  required
                  type="datetime-local"
                  value={form.validFrom}
                  onChange={(e) => update("validFrom", e.target.value)}
                  className={fieldClass}
                />
              </Field>
              <Field label="Kết thúc" required>
                <input
                  required
                  type="datetime-local"
                  value={form.validTo}
                  onChange={(e) => update("validTo", e.target.value)}
                  className={fieldClass}
                />
              </Field>
              {cloning && record.validityWindowShifted && (
                <div className="flex items-start gap-2 border-l-2 border-sky-500 bg-sky-500/[0.05] px-3 py-2 text-[11px] leading-5 text-sky-200 md:col-span-2">
                  <CalendarClock className="mt-0.5 h-4 w-4 shrink-0" />
                  <span>
                    Khoảng thời gian gốc đã hết hạn, hệ thống đã tự đề xuất
                    khoảng mới.
                  </span>
                </div>
              )}
              <Field label="Tổng lượt tối đa">
                <input
                  min="1"
                  type="number"
                  value={form.maxRedemptions}
                  onChange={(e) => update("maxRedemptions", e.target.value)}
                  placeholder="Không giới hạn"
                  className={fieldClass}
                />
              </Field>
              {type !== "AUTO" && (
                <Field label="Lượt tối đa mỗi khách" required>
                  <input
                    required
                    min="1"
                    type="number"
                    value={form.maxRedemptionsPerUser}
                    onChange={(e) =>
                      update("maxRedemptionsPerUser", e.target.value)
                    }
                    className={fieldClass}
                  />
                </Field>
              )}
              <div className="rounded-lg border border-sky-500/20 bg-sky-500/[0.05] p-3 md:col-span-2">
                <p className="text-[10px] font-black uppercase text-sky-300">
                  Tự động theo chiến dịch
                </p>
                <p className="mt-1 text-xs leading-5 text-zinc-400">
                  Ưu đãi được lưu cùng chương trình. Sau khi chương trình được
                  duyệt và đến thời gian chạy, hệ thống sẽ tự bật các ưu đãi
                  hợp lệ; người vận hành không cần bật từng ưu đãi.
                </p>
              </div>
              <div className="rounded-lg border border-zinc-800 bg-zinc-950/50 p-3 md:col-span-2">
                <p className="text-[10px] font-black uppercase text-zinc-500">
                  Xem trước
                </p>
                <div className="mt-2 grid gap-2 text-xs sm:grid-cols-2">
                  <p>
                    <span className="text-zinc-500">Quyền lợi:</span>{" "}
                    <span className="font-bold text-emerald-400">
                      {voucherDiscountSummary(previewPromotion)}
                    </span>
                  </p>
                  <p>
                    <span className="text-zinc-500">Điều kiện:</span>{" "}
                    <span className="text-zinc-300">
                      {conditionSummary(previewPromotion.conditionsJson)}
                    </span>
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
        {formError && (
          <p role="alert" className="mt-4 text-xs font-bold text-red-400">
            {formError}
          </p>
        )}
        {saveConfirm && (
          <div
            role="alertdialog"
            aria-modal="true"
            className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
          >
            <section className="w-full max-w-md rounded-xl border border-amber-500/25 bg-zinc-900 p-5 text-zinc-100 shadow-2xl shadow-black/70">
              <div className="flex items-start gap-3">
                <span className="rounded-lg bg-amber-500/10 p-2 text-amber-300">
                  <AlertTriangle className="h-5 w-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-base font-black text-white">
                    {editing
                      ? "Xác nhận lưu thay đổi ưu đãi?"
                      : cloning
                        ? "Xác nhận tạo bản sao ưu đãi?"
                        : "Xác nhận tạo ưu đãi mới?"}
                  </p>
                  <p className="mt-2 text-xs leading-5 text-zinc-400">
                    Ưu đãi “{saveConfirm.name}” sẽ được lưu cùng chương trình và
                    chỉ bắt đầu sau khi chương trình được duyệt.
                  </p>
                  <div className="mt-3 rounded-lg border border-zinc-800 bg-zinc-950/60 px-3 py-2 text-[11px] leading-5 text-zinc-400">
                    <p>
                      <span className="font-bold text-zinc-200">
                        Quyền lợi:
                      </span>{" "}
                      {voucherDiscountSummary(saveConfirm)}
                    </p>
                    <p>
                      <span className="font-bold text-zinc-200">
                        Điều kiện:
                      </span>{" "}
                      {conditionSummary(saveConfirm.conditionsJson)}
                    </p>
                  </div>
                </div>
              </div>
              <div className="mt-5 flex justify-end gap-2">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setSaveConfirm(null)}
                  className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
                >
                  Kiểm tra lại
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void confirmSave()}
                  className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
                >
                  {busy && <Loader2 className="h-4 w-4 animate-spin" />}
                  {editing
                    ? "Xác nhận lưu"
                    : cloning
                      ? "Tạo bản sao"
                      : "Xác nhận tạo"}
                </button>
              </div>
            </section>
          </div>
        )}
        <div className="mt-5 flex items-center justify-between gap-3 border-t border-zinc-800 pt-4">
          <button
            type="button"
            onClick={onClose}
            className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
          >
            Hủy
          </button>
          <div className="flex gap-2">
            {step > 0 && (
              <button
                type="button"
                onClick={() => {
                  setFormError("");
                  setSaveConfirm(null);
                  setStep((current) => current - 1);
                }}
                className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
              >
                Quay lại
              </button>
            )}
            {step < steps.length - 1 ? (
              <button
                type="button"
                onClick={goNext}
                className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
              >
                Tiếp tục
              </button>
            ) : (
              <button
                type="submit"
                disabled={busy || !form.campaignPublicId}
                className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
              >
                {busy && <Loader2 className="h-4 w-4 animate-spin" />}{" "}
                {editing
                  ? "Lưu thay đổi"
                  : cloning
                    ? "Tạo bản sao"
                    : "Tạo ưu đãi"}
              </button>
            )}
          </div>
        </div>
      </form>
    </ModalShell>
  );
}

const customerRecipientId = (customer) =>
  String(customer?.accountId ?? customer?.id ?? "");

function IssueModal({ promotion, busy, onClose, onIssue }) {
  const [query, setQuery] = useState("");
  const [customers, setCustomers] = useState([]);
  const [selected, setSelected] = useState(() => new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [step, setStep] = useState("select");
  const [result, setResult] = useState(null);
  const isCoupon = promotion.promotionType === "COUPON";
  const issueVerb = isCoupon ? "Gửi mã" : "Cấp voucher";

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(async () => {
      setLoading(true);
      setError("");
      try {
        const response = await getCustomers({
          keyword: query || undefined,
          status: "ACTIVE",
          page: 0,
          size: 20,
          sort: "joinedAt,desc",
        });
        if (active)
          setCustomers(
            Array.isArray(response?.content) ? response.content : [],
          );
      } catch (requestError) {
        if (active)
          setError(
            requestError?.message || "Không thể tải danh sách khách hàng.",
          );
      } finally {
        if (active) setLoading(false);
      }
    }, 250);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [query]);

  const toggleCustomer = (customer) => {
    const id = customerRecipientId(customer);
    setSelected((current) => {
      const next = new Map(current);
      if (next.has(id)) next.delete(id);
      else next.set(id, customer);
      return next;
    });
  };

  const selectVisible = () =>
    setSelected((current) => {
      const next = new Map(current);
      customers.forEach((customer) =>
        next.set(customerRecipientId(customer), customer),
      );
      return next;
    });

  const submit = async () => {
    setError("");
    try {
      setResult(await onIssue([...selected.keys()]));
      setStep("done");
    } catch (requestError) {
      setError(errorText(requestError));
      setStep("select");
    }
  };

  return (
    <ModalShell
      title={`${issueVerb}: ${promotion.name}`}
      icon={Users}
      onClose={onClose}
    >
      {step === "done" ? (
        <div className="p-6 text-center">
          <CheckCircle2 className="mx-auto h-10 w-10 text-emerald-400" />
          <h3 className="mt-3 text-base font-black text-white">
            Hoàn tất cấp ưu đãi
          </h3>
          <p className="mt-2 text-sm text-zinc-400">
            {isCoupon
              ? `Đã gửi mã riêng cho ${result?.issuedCount || 0} khách hàng.`
              : `Đã thêm voucher vào ví của ${result?.issuedCount || 0} khách hàng.`}{" "}
            {result?.alreadyOwnedCount || 0} khách đã có ưu đãi trước đó.
          </p>
          <button
            type="button"
            onClick={onClose}
            className={`${buttonClass} mt-5 bg-orange-500 text-white`}
          >
            Đóng
          </button>
        </div>
      ) : (
        <div className="p-5">
          <div className="mb-4 flex items-start gap-2 border-l-2 border-amber-500 bg-amber-500/[0.06] px-3 py-2 text-[11px] leading-5 text-amber-200">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>
              Mỗi đợt có thể chọn tối đa 1.000 khách hàng. {isCoupon
                ? "Mỗi khách sẽ nhận một mã riêng qua thông báo trong tài khoản."
                : "Voucher sẽ được thêm trực tiếp vào ví ưu đãi của từng khách."}
            </span>
          </div>
          {step === "confirm" ? (
            <div>
              <h3 className="text-sm font-black text-white">
                Xác nhận danh sách nhận
              </h3>
              <p className="mt-1 text-xs text-zinc-500">
                {isCoupon ? "Mã ưu đãi riêng" : `Voucher “${promotion.name}”`} sẽ
                được {isCoupon ? "gửi" : "cấp"} cho {selected.size} khách hàng.
              </p>
              <div className="mt-4 grid gap-2 rounded-lg border border-zinc-800 bg-zinc-950/50 p-3 text-xs sm:grid-cols-2">
                <DetailItem label="Số người nhận">
                  {selected.size} khách hàng
                </DetailItem>
                <DetailItem label="Kênh nhận">
                  {isCoupon ? "Thông báo trong tài khoản" : "Ví ưu đãi"}
                </DetailItem>
                <DetailItem label="Sử dụng đến">
                  {dateTime(promotion.validTo)}
                </DetailItem>
                <DetailItem label="Ưu đãi">
                  {voucherDiscountSummary(promotion)}
                </DetailItem>
              </div>
              <div className="mt-4 max-h-64 divide-y divide-zinc-800 overflow-y-auto border-y border-zinc-800">
                {[...selected.values()].map((customer) => (
                  <div
                    key={customerRecipientId(customer)}
                    className="flex items-center justify-between gap-3 py-3 text-xs"
                  >
                    <div className="min-w-0">
                      <p className="truncate font-bold text-white">
                        {customer.fullName || "Khách hàng"}
                      </p>
                      <p className="truncate text-zinc-500">
                        {customer.email ||
                          customer.phoneNumber ||
                          customer.customerCode}
                      </p>
                    </div>
                    <span className="shrink-0 font-mono text-[10px] text-zinc-600">
                      {customer.customerCode}
                    </span>
                  </div>
                ))}
              </div>
              {error && (
                <p role="alert" className="mt-3 text-xs text-red-400">
                  {error}
                </p>
              )}
              <div className="mt-5 flex justify-end gap-2">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setStep("select")}
                  className={`${buttonClass} border border-zinc-700 text-zinc-300`}
                >
                  Quay lại
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void submit()}
                  className={`${buttonClass} bg-orange-500 text-white`}
                >
                  {busy ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Send className="h-4 w-4" />
                  )}{" "}
                  {busy
                    ? `Đang xử lý ${selected.size} khách...`
                    : `${issueVerb} cho ${selected.size} khách`}
                </button>
              </div>
            </div>
          ) : (
            <>
              <label className="relative block">
                <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-600" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Tìm theo tên, email, số điện thoại hoặc mã khách"
                  className={`${fieldClass} pl-10`}
                />
              </label>
              <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs">
                <span className="font-bold text-zinc-400">
                  Đã chọn {selected.size} / 1.000 khách
                </span>
                <div className="flex gap-3">
                  <button
                    type="button"
                    disabled
                    title="Chức năng chọn toàn bộ chưa được hỗ trợ"
                    className="cursor-not-allowed font-bold text-zinc-700"
                  >
                    Tất cả người dùng
                  </button>
                  <button
                    type="button"
                    onClick={selectVisible}
                    className="font-bold text-orange-300 hover:text-orange-200"
                  >
                    Chọn trang này
                  </button>
                  <button
                    type="button"
                    disabled={selected.size === 0}
                    onClick={() => setSelected(new Map())}
                    className="font-bold text-zinc-500 hover:text-white disabled:opacity-40"
                  >
                    Bỏ chọn
                  </button>
                </div>
              </div>
              <div className="mt-3 min-h-56 max-h-80 overflow-y-auto border-y border-zinc-800">
                {loading ? (
                  <div className="flex h-56 items-center justify-center gap-2 text-xs text-zinc-500">
                    <Loader2 className="h-4 w-4 animate-spin" /> Đang tìm khách
                    hàng...
                  </div>
                ) : error ? (
                  <div className="flex h-56 items-center justify-center px-5 text-center text-xs text-red-400">
                    {error}
                  </div>
                ) : customers.length === 0 ? (
                  <div className="flex h-56 items-center justify-center text-xs text-zinc-600">
                    Không tìm thấy khách hàng hoạt động.
                  </div>
                ) : (
                  customers.map((customer) => {
                    const id = customerRecipientId(customer);
                    const checked = selected.has(id);
                    return (
                      <label
                        key={id}
                        className={`flex cursor-pointer items-center gap-3 border-b border-zinc-800/70 px-2 py-3 last:border-0 ${checked ? "bg-orange-500/[0.06]" : "hover:bg-zinc-800/40"}`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleCustomer(customer)}
                          className="h-4 w-4 accent-orange-500"
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-xs font-bold text-white">
                            {customer.fullName || "Khách hàng"}
                          </p>
                          <p className="mt-0.5 truncate text-[10px] text-zinc-500">
                            {customer.email ||
                              customer.phoneNumber ||
                              "Chưa có thông tin liên hệ"}
                          </p>
                        </div>
                        <span className="shrink-0 text-[10px] font-bold text-zinc-600">
                          {customer.customerCode || `#${customer.id}`}
                        </span>
                      </label>
                    );
                  })
                )}
              </div>
              <div className="mt-4 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  className={`${buttonClass} border border-zinc-700 text-zinc-300`}
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={selected.size === 0 || selected.size > 1000}
                  onClick={() => setStep("confirm")}
                  className={`${buttonClass} bg-orange-500 text-white`}
                >
                  <Check className="h-4 w-4" /> Kiểm tra danh sách
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </ModalShell>
  );
}
