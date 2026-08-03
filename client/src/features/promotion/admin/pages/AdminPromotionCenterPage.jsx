import { useCallback, useEffect, useState } from "react";
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  BadgePercent,
  Banknote,
  CalendarClock,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Building2,
  CirclePause,
  Clock3,
  Copy,
  Edit3,
  Eye,
  Film,
  Gift,
  Globe2,
  Loader2,
  Play,
  Plus,
  RefreshCw,
  RotateCw,
  Search,
  Send,
  Tag,
  Trash2,
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
  DISABLED: "Đã tắt",
  EXPIRED: "Hết hạn",
  PASSED: "Đạt pháp lý",
  FAILED: "Không đạt",
  AUTO: "Voucher hệ thống",
  VOUCHER: "Voucher sự kiện",
  COUPON: "Coupon theo khách",
  PERCENTAGE: "Giảm phần trăm",
  FIXED_AMOUNT: "Giảm số tiền",
  FULL_DISCOUNT: "Miễn phí toàn bộ",
};

const promotionTabs = [
  { key: "system", type: "AUTO", label: "Voucher hệ thống", icon: Globe2 },
  { key: "event", type: "VOUCHER", label: "Voucher sự kiện", icon: Gift },
  { key: "coupon", type: "COUPON", label: "Coupon theo khách", icon: Tag },
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
const dateTime = (value) =>
  value ? new Date(value).toLocaleString("vi-VN") : "-";
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

function StatusBadge({ value }) {
  if (!value) return null;
  return (
    <span
      className={`inline-flex rounded border px-2 py-1 text-[10px] font-bold ${badge(value)}`}
    >
      {labels[value] || value}
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
    KILL_SWITCH: "Dừng khẩn cấp chiến dịch?",
  })[action] || "Cập nhật chiến dịch?";

const campaignActionDescription = (campaign, action) => {
  const name = `“${campaign.name}”`;
  if (action === "PUBLISH")
    return `${name} sẽ tự chạy theo thời gian hiệu lực. Các promotion thuộc chiến dịch sẽ không được bật thủ công.`;
  if (action === "ACTIVATE")
    return `${name} cùng các promotion hợp lệ sẽ được mở để áp dụng tại checkout.`;
  if (action === "PAUSE" || action === "KILL_SWITCH")
    return `${name} sẽ ngừng áp dụng ngay cả khi promotion vẫn còn hiệu lực.`;
  return `Bạn đang cập nhật bước ${campaignActionTitle(action).replace("?", "").toLowerCase()} cho ${name}.`;
};

export default function AdminPromotionCenterPage() {
  const { userRole } = useAuth();
  const normalizedRole = String(userRole || "").replace(/^ROLE_/, "");
  const canViewOperations = [
    "ADMIN",
    "OPERATIONS_MANAGER",
    "FINANCE_DIRECTOR",
  ].includes(normalizedRole);
  const [view, setView] = useState("promotions");
  const [tab, setTab] = useState("system");
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
        setPromotions(pageData(promotionResult));
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
      : action === "LEGAL"
        ? adminPromotionService.reviewCampaignLegal(
            campaign.publicId,
            "PASSED",
            "Legal review passed",
          )
        : adminPromotionService.transitionCampaign(
            campaign.publicId,
            action,
            action === "KILL_SWITCH" ? "Stopped by operator" : undefined,
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
        text: `Đã cấp ${result?.issuedCount || 0} ưu đãi; ${result?.alreadyOwnedCount || 0} khách đã sở hữu.`,
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

  return (
    <div className="min-h-screen bg-[#09090b] text-zinc-100">
      <header className="border-b border-zinc-800 bg-zinc-950/80 px-5 py-5 lg:px-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-black text-white">Promotion Center</h1>
            <p className="mt-1 text-xs text-zinc-500">
              {view === "operations"
                ? "Giám sát sức khỏe ưu đãi và đối soát phân tán"
                : view === "campaigns"
                ? "Quản trị vòng đời chiến dịch"
                : promotionModelFor(selectedTab.type).description}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {view !== "operations" && (
              <button
                type="button"
                onClick={() => {
                  setView((value) =>
                    value === "promotions" ? "campaigns" : "promotions",
                  );
                  setQuery("");
                  setStatus("");
                  setPage(0);
                }}
                className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
              >
                {view !== "promotions" ? (
                  <ArrowLeft className="h-4 w-4" />
                ) : (
                  <CalendarClock className="h-4 w-4" />
                )}
                {view !== "promotions" ? "Promotion Center" : "Chiến dịch"}
              </button>
            )}
            {canViewOperations && (
              <button
                type="button"
                onClick={() => {
                  setView((value) =>
                    value === "operations" ? "promotions" : "operations",
                  );
                  setPage(0);
                }}
                className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}
              >
                {view === "operations" ? (
                  <ArrowLeft className="h-4 w-4" />
                ) : (
                  <Activity className="h-4 w-4" />
                )}
                {view === "operations" ? "Promotion Center" : "Vận hành"}
              </button>
            )}
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
            {view !== "operations" && (
              <button
                type="button"
                onClick={() =>
                  setModal({
                    type: view === "campaigns" ? "campaign" : "promotion",
                    promotionType: selectedTab.type,
                    mode: view === "campaigns" ? undefined : "create",
                  })
                }
                className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
              >
                <Plus className="h-4 w-4" />{" "}
                {view === "campaigns"
                  ? "Chiến dịch"
                  : promotionModelFor(selectedTab.type).shortLabel}
              </button>
            )}
          </div>
        </div>
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
          <div className="mb-4 grid grid-cols-1 border-b border-zinc-800 sm:grid-cols-3">
            {promotionTabs.map(
              ({ key, icon: Icon, label, type: promotionType }) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => {
                    setTab(key);
                    setStatus("");
                    setPage(0);
                  }}
                  className={`flex min-h-14 items-center gap-3 border-b-2 px-4 py-3 text-left text-xs font-bold ${tab === key ? "border-orange-500 bg-orange-500/[0.05] text-white" : "border-transparent text-zinc-500 hover:bg-zinc-900 hover:text-zinc-200"}`}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span>
                    <span className="block">{label}</span>
                    <span className="mt-0.5 block text-[10px] font-medium text-zinc-600">
                      {promotionModelFor(promotionType).shortLabel}
                    </span>
                  </span>
                </button>
              ),
            )}
          </div>
        )}

        {view !== "operations" && (
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
              placeholder="Tìm theo tên hoặc mã"
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
              <option value="">Mọi chiến dịch</option>
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

        {view === "promotions" && selectedTab.type === "AUTO" && (
          <div className="mb-4 flex items-start gap-2 border-l-2 border-sky-500 bg-sky-500/[0.06] px-3 py-2 text-xs leading-5 text-sky-200">
            <Globe2 className="mt-0.5 h-4 w-4 shrink-0" />
            <span>
              Loại này không được lưu vào ví cá nhân. Promotion Engine tự xét
              tất cả ưu đãi đang hoạt động và trả kết quả tốt nhất qua API
              preview checkout.
            </span>
          </div>
        )}

        {view === "operations" ? (
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
              onEdit={(item) => setModal({ type: "campaign", record: item })}
              onDelete={(item) =>
                runConfirmed({
                  title: "Xóa chiến dịch?",
                  description: `Chiến dịch “${item.name}” sẽ bị hủy và không thể sử dụng lại.`,
                  action: () =>
                    adminPromotionService.deleteCampaign(item.publicId),
                  success: "Đã xóa chiến dịch.",
                  danger: true,
                  refreshCampaigns: true,
                })
              }
              onAction={(item, action) =>
                runConfirmed({
                  title: campaignActionTitle(action),
                  description: campaignActionDescription(item, action),
                  action: () => campaignAction(item, action),
                  success: "Đã cập nhật vòng đời chiến dịch.",
                  refreshCampaigns: true,
                })
              }
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
                  description: `Ưu đãi “${item.name}” sẽ bị xóa khỏi chiến dịch.`,
                  action: () =>
                    adminPromotionService.deletePromotion(item.publicId),
                  success: "Đã xóa promotion.",
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

      {modal?.type === "campaign" && (
        <CampaignModal
          record={modal.record}
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
                ? "Đã cập nhật promotion."
                : mode === "clone"
                  ? "Đã tạo bản sao promotion."
                  : "Đã tạo promotion.",
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
  const model = promotionModelFor(type);
  return (
    <div className="flex min-h-52 flex-col items-center justify-center px-5 text-center">
      <BadgePercent className="h-8 w-8 text-zinc-700" />
      <p className="mt-3 text-sm font-black text-zinc-300">
        Chưa có {model.label.toLowerCase()}
      </p>
      <p className="mt-1 max-w-md text-xs leading-5 text-zinc-600">
        {model.description}
      </p>
    </div>
  );
}

const alertLabels = {
  EXPIRATION_BACKLOG_HIGH: "Tồn đọng reservation quá hạn vượt ngưỡng",
  EXPIRATION_OLDEST_AGE_HIGH: "Reservation quá hạn lâu nhất vượt SLA",
  REVERSAL_RATE_HIGH: "Tần suất reversal trong một giờ tăng cao",
  CAMPAIGN_BUDGET_EXPOSURE_HIGH: "Campaign chạm ngưỡng exposure ngân sách",
};

const durationText = (seconds) => {
  const value = Number(seconds || 0);
  if (value < 60) return `${value} giây`;
  if (value < 3600) return `${Math.floor(value / 60)} phút`;
  return `${Math.floor(value / 3600)} giờ`;
};

function OperationsDashboard({ data, loading }) {
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
      detail: "Campaign đang hoạt động",
      icon: Banknote,
      tone: "text-emerald-300 border-emerald-500/30",
    },
    {
      label: "Budget exposure",
      value: money(promotion.activeBudgetExposure),
      detail: `${Number(promotion.campaignsAtExposureThreshold || 0)} campaign chạm ngưỡng`,
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
    <section aria-label="Giám sát vận hành promotion">
      {alerts.length > 0 && (
        <div className="mb-4 border-l-2 border-red-500 bg-red-500/[0.07] px-4 py-3 text-sm text-red-200">
          <div className="flex items-center gap-2 font-bold">
            <AlertTriangle className="h-4 w-4" />
            {alerts.length} cảnh báo đang hoạt động
          </div>
          <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-xs text-red-300">
            {alerts.map((item) => (
              <span key={item}>
                {item === "RECONCILIATION_MISMATCH"
                  ? `${mismatch} booking lệch promotion`
                  : alertLabels[item] || item}
              </span>
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
      <div className="mt-4 text-right text-[11px] text-zinc-600">
        Cập nhật {dateTime(promotion.observedAt)}
      </div>
    </section>
  );
}

function CampaignTable({ rows, busy, onEdit, onDelete, onAction }) {
  return (
    <table className="w-full min-w-[980px] text-left text-sm">
      <thead className="bg-zinc-950 text-[10px] font-bold uppercase text-zinc-500">
        <tr>
          {[
            "Chiến dịch",
            "Trạng thái",
            "Hiệu lực",
            "Ngân sách",
            "Lượt dùng",
            "Thao tác",
          ].map((value) => (
            <th key={value} className="px-4 py-3">
              {value}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {rows.map((row) => (
          <tr key={row.publicId} className="hover:bg-zinc-900/70">
            <td className="px-4 py-4">
              <p className="font-bold text-white">{row.name}</p>
              <p className="mt-1 font-mono text-xs text-zinc-500">{row.code}</p>
            </td>
            <td className="px-4 py-4">
              <div className="flex flex-wrap gap-1">
                <StatusBadge value={row.status} />
                <StatusBadge value={row.approvalStatus} />
                <StatusBadge value={row.legalStatus} />
              </div>
            </td>
            <td className="px-4 py-4 text-xs text-zinc-400">
              {dateTime(row.startAt)}
              <br />
              {dateTime(row.endAt)}
            </td>
            <td className="px-4 py-4 text-xs">
              <p className="font-bold text-white">
                {money(row.budgetUsed)} / {money(row.budgetAmount)}
              </p>
              <p className="mt-1 text-zinc-500">
                Đang giữ {money(row.budgetReserved)}
              </p>
            </td>
            <td className="px-4 py-4 text-zinc-300">
              {row.redemptionCount || 0}
              {row.maxRedemptions ? ` / ${row.maxRedemptions}` : ""}
            </td>
            <td className="px-4 py-4">
              <div className="flex flex-wrap gap-1">
                <IconButton
                  title="Sửa"
                  onClick={() => onEdit(row)}
                  disabled={busy || row.status !== "DRAFT"}
                >
                  <Edit3 className="h-4 w-4" />
                </IconButton>
                {row.approvalStatus === "DRAFT" && (
                  <IconButton
                    title="Gửi duyệt"
                    onClick={() => onAction(row, "SUBMIT")}
                    disabled={busy}
                  >
                    <Send className="h-4 w-4" />
                  </IconButton>
                )}
                {row.approvalStatus === "PENDING" && (
                  <IconButton
                    title="Phê duyệt"
                    onClick={() => onAction(row, "APPROVE")}
                    disabled={busy}
                  >
                    <Check className="h-4 w-4" />
                  </IconButton>
                )}
                {row.legalStatus !== "PASSED" && (
                  <IconButton
                    title="Duyệt pháp lý"
                    onClick={() => onAction(row, "LEGAL")}
                    disabled={busy}
                  >
                    <Check className="h-4 w-4" />
                  </IconButton>
                )}
                {row.status === "DRAFT" &&
                  row.approvalStatus === "APPROVED" &&
                  row.legalStatus === "PASSED" && (
                    <IconButton
                      title="Xuất bản"
                      onClick={() => onAction(row, "PUBLISH")}
                      disabled={busy}
                    >
                      <CalendarClock className="h-4 w-4" />
                    </IconButton>
                  )}
                {["SCHEDULED", "PAUSED"].includes(row.status) && (
                  <IconButton
                    title="Kích hoạt"
                    onClick={() => onAction(row, "ACTIVATE")}
                    disabled={busy}
                  >
                    <Play className="h-4 w-4" />
                  </IconButton>
                )}
                {row.status === "ACTIVE" && (
                  <IconButton
                    title="Tạm dừng"
                    onClick={() => onAction(row, "PAUSE")}
                    disabled={busy}
                  >
                    <CirclePause className="h-4 w-4" />
                  </IconButton>
                )}
                <IconButton
                  title="Xóa"
                  danger
                  onClick={() => onDelete(row)}
                  disabled={busy || row.status !== "DRAFT"}
                >
                  <Trash2 className="h-4 w-4" />
                </IconButton>
              </div>
            </td>
          </tr>
        ))}
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
    <table className="w-full min-w-[1120px] text-left text-sm">
      <thead className="bg-zinc-950 text-[10px] font-bold uppercase text-zinc-500">
        <tr>
          {[
            "Ưu đãi",
            "Chiến dịch & phạm vi",
            "Trạng thái",
            "Hiệu lực",
            "Hạn mức",
            "Thao tác",
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
              <p className="mt-1 font-mono text-xs text-zinc-500">
                {row.code || "Không cần mã checkout"}
              </p>
              <p className="mt-2 text-xs font-black text-emerald-400">
                {voucherDiscountSummary(row)}
              </p>
            </td>
            <td className="max-w-xs px-4 py-4">
              <p className="truncate text-xs font-bold text-zinc-300">
                {campaignDetails.get(row.campaignPublicId)?.name ||
                  "Chiến dịch chưa tải"}
              </p>
              <div className="mt-1 flex flex-wrap items-center gap-1">
                <StatusBadge
                  value={campaignDetails.get(row.campaignPublicId)?.status}
                />
                <span className="text-[10px] leading-4 text-zinc-500">
                  {row.promotionType === "AUTO"
                    ? "Tự áp dụng tại checkout"
                    : row.publicVisible
                      ? "Khách nhận vào ví"
                      : "Cấp cho khách được chọn"}
                </span>
              </div>
            </td>
            <td className="px-4 py-4">
              <StatusBadge
                value={effectivePromotionStatus(
                  row,
                  campaignDetails.get(row.campaignPublicId),
                )}
              />
              <p className="mt-2 text-[10px] leading-4 text-zinc-500">
                Tự chạy theo trạng thái chiến dịch.
                <br />
                Ưu tiên {row.priority ?? 0}
              </p>
            </td>
            <td className="px-4 py-4 text-xs leading-5 text-zinc-400">
              <span className="text-zinc-600">Từ</span>{" "}
              {dateTime(row.validFrom)}
              <br />
              <span className="text-zinc-600">Đến</span> {dateTime(row.validTo)}
            </td>
            <td className="px-4 py-4 text-zinc-300">
              <p>
                {row.redemptionCount || 0}
                {row.maxRedemptions
                  ? ` / ${row.maxRedemptions}`
                  : " / Không giới hạn"}
              </p>
              <p className="mt-1 text-[10px] text-zinc-500">
                {row.maxRedemptionsPerUser || 1} lượt / khách
              </p>
            </td>
            <td className="px-4 py-4">
              <div
                className="flex gap-1"
                onClick={(event) => event.stopPropagation()}
              >
                <IconButton
                  title="Xem chi tiết"
                  onClick={() => onDetail(row)}
                  disabled={busy}
                >
                  <Eye className="h-4 w-4" />
                </IconButton>
                <IconButton
                  title="Sửa"
                  onClick={() => onEdit(row)}
                  disabled={busy || row.status !== "DRAFT"}
                >
                  <Edit3 className="h-4 w-4" />
                </IconButton>
                <IconButton
                  title="Nhân bản"
                  onClick={() => onClone(row)}
                  disabled={busy}
                >
                  <Copy className="h-4 w-4" />
                </IconButton>
                {row.promotionType !== "AUTO" && (
                  <IconButton
                    title="Phát hành"
                    onClick={() => onIssue(row)}
                    disabled={busy}
                  >
                    <Users className="h-4 w-4" />
                  </IconButton>
                )}
                <IconButton
                  title="Xóa"
                  danger
                  onClick={() => onDelete(row)}
                  disabled={busy || row.status === "ACTIVE"}
                >
                  <Trash2 className="h-4 w-4" />
                </IconButton>
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
      title={`Chi tiết ${model.shortLabel || "promotion"}`}
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
            Không thể tải lại detail mới nhất. Đang hiển thị dữ liệu trên danh
            sách.
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
              <p className="mt-2 font-mono text-xs font-bold text-amber-200">
                {promotion.code || "Không cần mã checkout"}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <StatusBadge value={promotion.status} />
              <StatusBadge value={actualStatus} />
            </div>
          </div>
          {promotion.description && (
            <p className="mt-3 text-sm leading-6 text-zinc-300">
              {promotion.description}
            </p>
          )}
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <DetailItem label="Chiến dịch">
            <span>{campaign?.name || "Chưa tải tên chiến dịch"}</span>
            <span className="mt-1 block text-xs text-zinc-500">
              {campaignStatus}
            </span>
          </DetailItem>
          <DetailItem label="Quyền lợi">
            <span className="text-emerald-300">
              {voucherDiscountSummary(promotion)}
            </span>
          </DetailItem>
          <DetailItem label="Ưu tiên">{promotion.priority ?? 0}</DetailItem>
          <DetailItem label="Cộng dồn">
            {promotion.stackable
              ? "Cho phép khi chiến dịch và ưu đãi đi kèm cũng cho phép"
              : "Không cho phép"}
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
          <DetailItem label="Cơ chế">
            <span>
              {promotion.promotionType === "AUTO"
                ? "Khách chọn tại checkout"
                : promotion.promotionType === "COUPON"
                  ? "Cấp mã qua thông báo"
                  : "Khách nhận vào ví"}
            </span>
            {metadata.distributionModel && (
              <span className="mt-1 block text-xs text-zinc-500">
                {metadata.distributionModel}
              </span>
            )}
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
          <DetailItem label="Điều kiện nâng cao">
            {advancedConditions.length === 0 ? (
              "Không có"
            ) : (
              <div className="space-y-1 text-xs font-semibold text-zinc-300">
                {advancedConditions.map((item) => (
                  <p key={item.key}>
                    <span className="text-zinc-500">{item.key}:</span>{" "}
                    {item.value}
                  </p>
                ))}
              </div>
            )}
          </DetailItem>
        </div>

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
          {promotion.promotionType !== "AUTO" && (
            <button
              type="button"
              onClick={() => onIssue(promotion)}
              className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}
            >
              <Users className="h-4 w-4" /> Phát hành
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

function Toggle({ checked, onChange, label }) {
  return (
    <label className="flex cursor-pointer items-center gap-2 text-xs font-bold text-zinc-300">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 accent-orange-500"
      />
      {label}
    </label>
  );
}

function CampaignModal({ record, busy, onClose, onSave }) {
  const editing = Boolean(record);
  const [form, setForm] = useState(() => ({
    code: record?.code || "",
    name: record?.name || "",
    description: record?.description || "",
    priority: record?.priority ?? 100,
    stackable: record?.stackable ?? false,
    exclusiveCampaign: record?.exclusiveCampaign ?? false,
    autoActivate: record?.autoActivate ?? true,
    autoComplete: record?.autoComplete ?? true,
    autoPauseWhenBudgetExceeded: record?.autoPauseWhenBudgetExceeded ?? true,
    timezone: record?.timezone || "Asia/Ho_Chi_Minh",
    startAt: toLocalInput(record?.startAt),
    endAt: toLocalInput(record?.endAt || Date.now() + 30 * 86400_000),
    budgetAmount: record?.budgetAmount ?? 100000000,
    maxRedemptions: record?.maxRedemptions ?? "",
    maxRedemptionsPerUser: record?.maxRedemptionsPerUser ?? 1,
    legalNotificationRef: record?.legalNotificationRef || "",
    remarks: record?.remarks || "",
  }));
  const update = (key, value) =>
    setForm((current) => ({ ...current, [key]: value }));
  const submit = (event) => {
    event.preventDefault();
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
    };
    if (editing) delete payload.code;
    onSave(payload, editing);
  };
  return (
    <ModalShell
      title={editing ? "Sửa chiến dịch" : "Tạo chiến dịch"}
      icon={CalendarClock}
      onClose={onClose}
    >
      <form onSubmit={submit} className="grid gap-4 p-5 md:grid-cols-2">
        <Field label="Mã chiến dịch" required>
          <input
            required
            disabled={editing}
            value={form.code}
            onChange={(e) => update("code", e.target.value.toUpperCase())}
            className={fieldClass}
          />
        </Field>
        <Field label="Tên chiến dịch" required>
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
        <Field label="Ưu tiên" required>
          <input
            required
            min="0"
            type="number"
            value={form.priority}
            onChange={(e) => update("priority", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Tổng lượt tối đa">
          <input
            min="1"
            type="number"
            value={form.maxRedemptions}
            onChange={(e) => update("maxRedemptions", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Trần lượt mỗi khách trên từng ưu đãi" required>
          <input
            required
            min="1"
            type="number"
            value={form.maxRedemptionsPerUser}
            onChange={(e) => update("maxRedemptionsPerUser", e.target.value)}
            className={fieldClass}
          />
        </Field>
        <div className="grid gap-3 md:col-span-2 sm:grid-cols-2">
          <Toggle
            checked={form.autoActivate}
            onChange={(v) => update("autoActivate", v)}
            label="Tự kích hoạt"
          />
          <Toggle
            checked={form.autoComplete}
            onChange={(v) => update("autoComplete", v)}
            label="Tự hoàn tất"
          />
          <Toggle
            checked={form.autoPauseWhenBudgetExceeded}
            onChange={(v) => update("autoPauseWhenBudgetExceeded", v)}
            label="Dừng khi hết ngân sách"
          />
          <Toggle
            checked={form.exclusiveCampaign}
            onChange={(v) => update("exclusiveCampaign", v)}
            label="Chiến dịch độc quyền"
          />
          <Toggle
            checked={form.stackable}
            onChange={(v) => update("stackable", v)}
            label="Cho phép các ưu đãi trong chiến dịch được cộng dồn"
          />
        </div>
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
        Để trống nếu áp dụng cho tất cả.
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
  const hasEditableCampaign = campaigns.some(isEditableCampaignOption);
  const steps = ["Thông tin", "Quyền lợi", "Phạm vi", "Hạn mức"];
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
                label="Chiến dịch"
                required
                wide
                hint={
                  currentCampaign
                    ? `Hiệu lực: ${dateTime(currentCampaign.startAt)} đến ${dateTime(currentCampaign.endAt)}`
                    : "Chỉ campaign bản nháp có thể cấu hình promotion."
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
                    Tạo chiến dịch mới
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
              <Field label="Mã phân phối" required={type === "VOUCHER"}>
                {type === "AUTO" ? (
                  <div className="flex h-10 items-center text-xs font-bold text-zinc-500">
                    Không cần mã, khách chọn tại checkout
                  </div>
                ) : type === "COUPON" ? (
                  <div className="flex h-10 items-center rounded-lg border border-zinc-800 bg-zinc-950 px-3 font-mono text-xs font-bold text-amber-200">
                    {form.code || "Sẽ tự sinh sau khi lưu"}
                  </div>
                ) : (
                  <div className="flex gap-2">
                    <input
                      required
                      value={form.code}
                      onChange={(event) =>
                        update("code", event.target.value.toUpperCase())
                      }
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
                    label="Cho phép khách hàng tự nhận vào ví"
                  />
                </div>
              )}
              <Field label="Mô tả cho khách hàng" wide>
                <textarea
                  value={form.description}
                  onChange={(e) => update("description", e.target.value)}
                  placeholder="Mô tả ngắn gọn quyền lợi và phạm vi áp dụng"
                  className={`${fieldClass} h-24 py-2`}
                />
              </Field>
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
              <Field label="Ưu tiên xét (số nhỏ xét trước)" required>
                <input
                  required
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
                  label="Cho phép cộng dồn với 1 ưu đãi AUTO"
                />
              </div>
              {form.stackable && (
                <p className="text-[10px] leading-4 text-amber-300 md:col-span-2">
                  Chỉ cộng dồn khi cả hai ưu đãi và cả hai chiến dịch đều cho
                  phép. Chiến dịch độc quyền không được ghép với chiến dịch
                  khác.
                </p>
              )}
            </div>
          )}
          {step === 2 && (
            <div className="grid gap-4">
              <p className="text-xs leading-5 text-zinc-400">
                Thiết lập phạm vi chỉ áp dụng cho phim hoặc rạp cụ thể. Các lựa
                chọn dùng mã công khai và được kiểm tra lại bởi backend khi
                checkout.
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
                  Promotion được tạo ở trạng thái bản nháp. Khi campaign được
                  duyệt và đến thời gian chạy, hệ thống tự kích hoạt các
                  promotion hợp lệ. Không có nút bật/tắt từng promotion.
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
                      ? "Xác nhận lưu thay đổi promotion?"
                      : cloning
                        ? "Xác nhận tạo bản sao promotion?"
                        : "Xác nhận tạo promotion mới?"}
                  </p>
                  <p className="mt-2 text-xs leading-5 text-zinc-400">
                    Promotion “{saveConfirm.name}” sẽ được lưu ở trạng thái bản
                    nháp và chỉ chạy theo lifecycle của chiến dịch.
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
                    : "Tạo promotion"}
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
    <ModalShell title={`Cấp ${promotion.name}`} icon={Users} onClose={onClose}>
      {step === "done" ? (
        <div className="p-6 text-center">
          <CheckCircle2 className="mx-auto h-10 w-10 text-emerald-400" />
          <h3 className="mt-3 text-base font-black text-white">
            Hoàn tất cấp ưu đãi
          </h3>
          <p className="mt-2 text-sm text-zinc-400">
            {promotion.promotionType === "COUPON"
              ? `Đã cấp coupon và gửi thông báo cho ${result?.issuedCount || 0} khách hàng.`
              : `Đã thêm voucher vào ví của ${result?.issuedCount || 0} khách hàng.`}{" "}
            {result?.alreadyOwnedCount || 0} khách đã sở hữu trước đó.
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
              API hiện chỉ hỗ trợ tối đa 1.000 tài khoản mỗi lần và chưa có thao
              tác “tất cả người dùng”. Coupon được cấp riêng, không xuất hiện
              trong ví và mã sẽ được gửi vào thông báo hệ thống của khách hàng.
            </span>
          </div>
          {step === "confirm" ? (
            <div>
              <h3 className="text-sm font-black text-white">
                Xác nhận danh sách nhận
              </h3>
              <p className="mt-1 text-xs text-zinc-500">
                Coupon{" "}
                <span className="font-mono text-amber-300">
                  {promotion.code}
                </span>{" "}
                sẽ được cấp cho {selected.size} khách hàng.
              </p>
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
                  {busy ? `Đang cấp ${selected.size} khách...` : "Xác nhận cấp"}
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
                    title="Backend chưa hỗ trợ cấp cho toàn bộ người dùng"
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
