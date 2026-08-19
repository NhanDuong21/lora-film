import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  AlertCircle,
  BadgeCheck,
  CalendarDays,
  CheckCircle2,
  Gift,
  Globe2,
  History,
  Loader2,
  RefreshCw,
  ReceiptText,
  ShoppingCart,
  TicketPercent,
  WalletCards,
  X,
} from "lucide-react";
import customerPromotionService from "../services/customerPromotionService";
import {
  badgeClass,
  conditionSummary,
  currency,
  formatDateTime,
  friendlyPromotionError,
  isWalletPromotionUsable,
  labelFor,
  estimatedDiscountAmount,
  promotionSourceLabel,
  safeJsonParse,
  walletPromotionUnavailableReason,
  walletUsageRemaining,
  voucherDiscountSummary,
} from "../../shared/promotionPresentation";

const contentOf = (payload) =>
  Array.isArray(payload) ? payload : payload?.content || [];

const tabItems = [
  { key: "wallet", label: "Có thể sử dụng", icon: WalletCards },
  { key: "event", label: "Có thể nhận", icon: Gift },
  { key: "system", label: "Tự động áp dụng", icon: Globe2 },
  { key: "history", label: "Lịch sử", icon: History },
];

const walletIdOf = (promotion) =>
  promotion?.walletPublicId || promotion?.selectionPublicId || promotion?.publicId;

const jsonValue = (value, fallback = {}) =>
  typeof value === "string"
    ? safeJsonParse(value, fallback).value
    : (value ?? fallback);

const firstAction = (promotion) => {
  const actions = jsonValue(promotion?.actionsJson);
  return Array.isArray(actions) ? actions[0] || {} : actions || {};
};

const discountTypeLabel = (promotion) => {
  const action = firstAction(promotion);
  return labelFor(
    action.discountType ||
      action.type ||
      action.actionType ||
      promotion?.voucherType,
  );
};

const maxDiscountOf = (promotion) => {
  const action = firstAction(promotion);
  return (
    action.maxDiscountAmount ??
    action.maximumDiscountAmount ??
    action.maxAmount ??
    null
  );
};

const minimumOrderOf = (promotion) => {
  const conditions = jsonValue(promotion?.conditionsJson);
  return conditions.minimumOrderAmount ?? conditions.minOrderAmount ?? null;
};

const looksLikeEnglishSeedCopy = (value) =>
  /\b(welcome|save|discount|automatic|promotion|applied|order|instant|off)\b/i.test(String(value || ""));

const customerPromotionTitle = (promotion) => {
  const original = promotion?.name || "";
  if (/welcome/i.test(original) && /10\s*%/i.test(original)) return "Chào thành viên mới – giảm 10%";
  if (/(50\s*k|50[.,]?000)/i.test(original) && /discount|save/i.test(original)) return "Giảm ngay 50.000đ";
  if (/combo/i.test(original) && /20\s*%/i.test(original)) return "Combo tối – giảm 20%";
  if (!looksLikeEnglishSeedCopy(original)) return original || "Ưu đãi LoraFilm";
  return promotion?.promotionType === "AUTO"
    ? `Ưu đãi tự động ${voucherDiscountSummary(promotion)}`
    : `Ưu đãi giảm ${voucherDiscountSummary(promotion)}`;
};

const customerPromotionDescription = (promotion) => {
  const original = promotion?.description || "";
  if (!looksLikeEnglishSeedCopy(original)) return original;
  const minimum = minimumOrderOf(promotion);
  if (promotion?.promotionType === "AUTO") {
    return "Tự động áp dụng khi đơn hàng đáp ứng đủ điều kiện.";
  }
  return Number(minimum) > 0
    ? `Giảm ${voucherDiscountSummary(promotion)} cho đơn từ ${currency(minimum)}.`
    : "Ưu đãi dành riêng cho thành viên LoraFilm.";
};

const dayLabels = {
  MONDAY: "T2",
  TUESDAY: "T3",
  WEDNESDAY: "T4",
  THURSDAY: "T5",
  FRIDAY: "T6",
  SATURDAY: "T7",
  SUNDAY: "CN",
};

const configuredValues = (conditions, primaryKey, legacyKey) => {
  if (Array.isArray(conditions?.[primaryKey])) return conditions[primaryKey];
  if (Array.isArray(conditions?.[legacyKey])) return conditions[legacyKey];
  return [];
};

const labelList = (source, ids = []) => {
  if (!source) return [];
  if (Array.isArray(source)) {
    return source
      .map((item) => {
        if (typeof item === "string" && !ids.includes(item)) return item;
        if (item && typeof item === "object") {
          return (
            item.label ||
            item.title ||
            item.movieTitle ||
            item.name ||
            item.cinemaName ||
            ""
          );
        }
        return "";
      })
      .filter(Boolean);
  }
  if (typeof source === "object") {
    return ids
      .map((id) => {
        const value = source[id] ?? source[String(id)];
        if (typeof value === "string" && value !== String(id)) return value;
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
        return "";
      })
      .filter(Boolean);
  }
  return [];
};

const customerConditionItems = (promotion) => {
  const conditions = jsonValue(promotion?.conditionsJson);
  const metadata = jsonValue(promotion?.metadataJson);
  const conditionLabels = jsonValue(metadata?.conditionLabels);
  const items = [];
  const minimum = minimumOrderOf(promotion);
  const maximum = maxDiscountOf(promotion);
  if (Number(minimum) > 0) {
    items.push(`Áp dụng cho đơn hàng từ ${currency(minimum)}.`);
  }
  if (Number(maximum) > 0) {
    items.push(`Giảm tối đa ${currency(maximum)}.`);
  }
  const movieIds = configuredValues(conditions, "moviePublicIds", "movieIds");
  if (movieIds.length) {
    const names = labelList(
      conditionLabels?.moviePublicIds ||
        metadata?.moviePublicIds ||
        conditionLabels?.movieNames ||
        metadata?.movieNames ||
        conditionLabels?.movieTitles ||
        metadata?.movieTitles,
      movieIds,
    );
    items.push(
      names.length
        ? `Áp dụng cho phim ${names.join(", ")}.`
        : "Áp dụng cho một số phim được cấu hình.",
    );
  }
  const cinemaIds = configuredValues(
    conditions,
    "cinemaPublicIds",
    "cinemaIds",
  );
  if (cinemaIds.length) {
    const names = labelList(
      conditionLabels?.cinemaPublicIds ||
        metadata?.cinemaPublicIds ||
        conditionLabels?.cinemaNames ||
        metadata?.cinemaNames,
      cinemaIds,
    );
    items.push(
      names.length
        ? `Áp dụng tại rạp ${names.join(", ")}.`
        : "Áp dụng tại một số rạp được cấu hình.",
    );
  }
  if (Array.isArray(conditions.dayOfWeek) && conditions.dayOfWeek.length) {
    items.push(
      `Áp dụng vào ${conditions.dayOfWeek
        .map((day) => dayLabels[day] || day)
        .join(", ")}.`,
    );
  }
  if (conditions.requiredTierCode) {
    items.push(`Dành cho hạng thành viên ${conditions.requiredTierCode}.`);
  }
  if (conditions.requiresVerification) {
    items.push("Yêu cầu tài khoản đã xác thực.");
  }
  if (!items.length) {
    items.push(
      conditions && Object.keys(conditions).length > 0
        ? "Có điều kiện áp dụng theo chương trình."
        : "Áp dụng cho mọi đơn hàng hợp lệ.",
    );
  }
  return items;
};

function DetailMetric({ label, value }) {
  if (value === null || value === undefined || value === "") return null;
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-950/60 p-3">
      <p className="text-[10px] font-black uppercase text-zinc-500">{label}</p>
      <p className="mt-1 break-words text-sm font-black text-white">{value}</p>
    </div>
  );
}

function PromotionCard({
  promotion,
  claimable = false,
  system = false,
  recommended = false,
  busy,
  onClaim,
  onOpenDetail,
}) {
  const available = ["ACTIVE", "AVAILABLE"].includes(
    String(promotion.status || "").toUpperCase(),
  );
  const remaining =
    promotion.maxRedemptions == null
      ? null
      : Math.max(
          0,
          Number(promotion.maxRedemptions) -
            Number(promotion.redemptionCount || 0),
        );
  const usageRemaining =
    promotion.maxUsage == null
      ? null
      : Math.max(
          0,
          Number(promotion.maxUsage) - Number(promotion.usageCount || 0),
        );

  return (
    <article
      role={onOpenDetail ? "button" : undefined}
      tabIndex={onOpenDetail ? 0 : undefined}
      onClick={onOpenDetail}
      onKeyDown={(event) => {
        if (!onOpenDetail) return;
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onOpenDetail();
        }
      }}
      className={`grid gap-4 border-b border-zinc-800 py-5 last:border-b-0 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center ${
        onOpenDetail
          ? "cursor-pointer rounded-lg px-3 transition-colors hover:bg-zinc-900/70 focus:outline-none focus:ring-2 focus:ring-brand-orange/60"
          : ""
      }`}
    >
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <TicketPercent className="h-4 w-4 text-brand-orange" />
          <h3 className="break-words text-sm font-black text-white">
            {customerPromotionTitle(promotion)}
          </h3>
          {recommended && (
            <span className="rounded border border-amber-400/30 bg-amber-400/10 px-2 py-0.5 text-[10px] font-black text-amber-200">
              Đề xuất
            </span>
          )}
          <span
            className={`rounded border px-2 py-0.5 text-[10px] font-bold ${badgeClass(promotion.status)}`}
          >
            {claimable
              ? "Có thể nhận"
              : system
                ? "Tự động"
                : labelFor(promotion.status)}
          </span>
        </div>
        <p className="mt-2 text-[10px] font-black uppercase text-sky-300">
          {promotionSourceLabel(promotion)}
        </p>
        {promotion.code && (
          <p className="mt-2 break-all font-mono text-xs font-bold text-amber-300">
            {promotion.code}
          </p>
        )}
        <p className="mt-2 text-base font-black text-brand-orange">
          {voucherDiscountSummary(promotion)}
        </p>
        {customerPromotionDescription(promotion) && (
          <p className="mt-1 text-xs leading-5 text-zinc-400">
            {customerPromotionDescription(promotion)}
          </p>
        )}
        <p className="mt-2 text-[11px] leading-5 text-zinc-500">
          {conditionSummary(promotion.conditionsJson)}
        </p>
        <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-[10px] font-semibold text-zinc-600">
          <span>Đến {formatDateTime(promotion.validTo)}</span>
          {remaining !== null && <span>Còn {remaining} lượt phát hành</span>}
          {usageRemaining !== null && (
            <span>Còn {usageRemaining} lượt dùng</span>
          )}
        </div>
      </div>
      {claimable ? (
        <button
          type="button"
          disabled={busy || !available || remaining === 0}
          onClick={(event) => {
            event.stopPropagation();
            onClaim(promotion.promotionPublicId || promotion.publicId);
          }}
          className="inline-flex h-9 min-w-32 items-center justify-center gap-2 rounded-lg bg-brand-orange px-3 text-xs font-black text-white transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
        >
          {busy ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Gift className="h-4 w-4" />
          )}
          {remaining === 0 ? "Đã hết lượt" : "Nhận vào ví"}
        </button>
      ) : system ? (
        <span className="inline-flex h-9 items-center gap-2 self-center rounded-lg border border-sky-500/25 bg-sky-500/10 px-3 text-xs font-bold text-sky-200">
          <Globe2 className="h-4 w-4" /> Tự động khi đủ điều kiện
        </span>
      ) : (
        <span className="inline-flex h-9 items-center gap-2 self-center rounded-lg border border-emerald-500/25 bg-emerald-500/10 px-3 text-xs font-bold text-emerald-300">
          <BadgeCheck className="h-4 w-4" /> Đã thuộc ví
        </span>
      )}
    </article>
  );
}

function PromotionHistoryRow({ item }) {
  const labels = {
    USED: "Đã dùng",
    EXPIRED: "Hết hạn",
    REVOKED: "Đã thu hồi",
    RESTORED: "Đã hoàn lại",
  };
  return (
    <article className="grid gap-3 border-b border-zinc-800 px-3 py-5 last:border-b-0 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <History className="h-4 w-4 text-zinc-500" />
          <h3 className="break-words text-sm font-black text-white">
            {item.promotionName || "Ưu đãi LoraFilm"}
          </h3>
          <span className={`rounded border px-2 py-0.5 text-[10px] font-black ${badgeClass(item.eventType)}`}>
            {labels[item.eventType] || labelFor(item.eventType)}
          </span>
        </div>
        {item.promotionCode && (
          <p className="mt-2 font-mono text-xs font-bold text-amber-300">
            {item.promotionCode}
          </p>
        )}
        <p className="mt-2 text-xs leading-5 text-zinc-400">{item.detail}</p>
        {item.bookingPublicId && (
          <p className="mt-1 break-all text-[10px] text-zinc-600">
            Đơn hàng: {item.bookingPublicId}
          </p>
        )}
      </div>
      <time className="text-xs font-bold text-zinc-500">
        {formatDateTime(item.eventAt)}
      </time>
    </article>
  );
}

function VoucherDetailModal({
  walletId,
  detail,
  loading,
  error,
  onClose,
  onRetry,
  onUseNow,
}) {
  useEffect(() => {
    if (!walletId) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose, walletId]);

  if (!walletId) return null;

  const usable = detail ? isWalletPromotionUsable(detail) : false;
  const usageRemaining = detail ? walletUsageRemaining(detail) : null;
  const minimum = detail ? minimumOrderOf(detail) : null;
  const maximum = detail ? maxDiscountOf(detail) : null;
  const conditionItems = detail ? customerConditionItems(detail) : [];
  const unavailableReason = detail
    ? walletPromotionUnavailableReason(detail)
    : "";

  return (
    <div
      className="fixed inset-0 z-[90] flex items-end justify-center bg-black/80 p-0 backdrop-blur-sm sm:items-center sm:p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="voucher-detail-title"
        className="flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-t-lg border border-zinc-800 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70 sm:rounded-lg"
      >
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4">
          <div className="flex min-w-0 items-center gap-3">
            <span className="rounded-lg bg-brand-orange/10 p-2.5 text-brand-orange">
              <TicketPercent className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="text-[10px] font-black uppercase text-brand-orange">
                Chi tiết voucher
              </p>
              <h2
                id="voucher-detail-title"
                className="mt-1 break-words text-lg font-black text-white"
              >
                {customerPromotionTitle(detail)}
              </h2>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className="rounded-lg border border-zinc-700 p-2 text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-5 py-5">
          {loading ? (
            <div className="flex min-h-56 items-center justify-center gap-2 text-sm font-bold text-zinc-500">
              <Loader2 className="h-5 w-5 animate-spin" /> Đang tải chi tiết...
            </div>
          ) : error ? (
            <div className="flex min-h-56 flex-col items-center justify-center text-center">
              <AlertCircle className="h-8 w-8 text-amber-400" />
              <p className="mt-3 text-sm font-black text-white">
                Không thể tải chi tiết voucher
              </p>
              <p className="mt-2 max-w-sm text-xs leading-5 text-zinc-500">
                {error}
              </p>
              <button
                type="button"
                onClick={onRetry}
                className="mt-4 inline-flex h-9 items-center gap-2 rounded-lg bg-brand-orange px-4 text-xs font-black text-white hover:bg-orange-600"
              >
                <RefreshCw className="h-4 w-4" /> Thử lại
              </button>
            </div>
          ) : !detail ? (
            <div className="flex min-h-56 flex-col items-center justify-center text-center">
              <Gift className="h-8 w-8 text-zinc-700" />
              <p className="mt-3 text-sm font-black text-white">
                Không tìm thấy voucher
              </p>
              <p className="mt-2 max-w-sm text-xs leading-5 text-zinc-500">
                Voucher có thể đã hết quyền sử dụng hoặc không còn trong ví.
              </p>
            </div>
          ) : (
            <div className="space-y-5">
              <div className="rounded-lg border border-brand-orange/20 bg-brand-orange/[0.06] p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-2xl font-black text-brand-orange">
                    {voucherDiscountSummary(detail)}
                  </p>
                  <span
                    className={`rounded border px-2 py-1 text-[10px] font-black ${badgeClass(detail.status)}`}
                  >
                    {labelFor(detail.status)}
                  </span>
                </div>
                <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs font-semibold text-zinc-400">
                  <span className="inline-flex items-center gap-1.5">
                    <CalendarDays className="h-3.5 w-3.5 text-zinc-500" />
                    Từ {formatDateTime(detail.validFrom)}
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    <CalendarDays className="h-3.5 w-3.5 text-zinc-500" />
                    Đến {formatDateTime(detail.validTo)}
                  </span>
                </div>
                {!usable && unavailableReason && (
                  <p className="mt-3 flex items-start gap-2 rounded-lg border border-amber-500/20 bg-amber-500/10 px-3 py-2 text-xs font-bold leading-5 text-amber-200">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                    {unavailableReason}
                  </p>
                )}
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                <DetailMetric
                  label="Loại giảm"
                  value={discountTypeLabel(detail)}
                />
                <DetailMetric
                  label="Đơn tối thiểu"
                  value={Number(minimum) > 0 ? currency(minimum) : null}
                />
                <DetailMetric
                  label="Mức giảm tối đa"
                  value={Number(maximum) > 0 ? currency(maximum) : null}
                />
                <DetailMetric
                  label="Lượt còn lại"
                  value={
                    usageRemaining === null
                      ? null
                      : `${usageRemaining} lượt`
                  }
                />
              </div>

              {customerPromotionDescription(detail) && (
                <section className="rounded-lg border border-zinc-800 bg-zinc-950/40 p-4">
                  <h3 className="flex items-center gap-2 text-xs font-black uppercase text-zinc-400">
                    <ReceiptText className="h-4 w-4" /> Hướng dẫn
                  </h3>
                  <p className="mt-2 text-sm leading-6 text-zinc-300">
                    {customerPromotionDescription(detail)}
                  </p>
                </section>
              )}

              <section className="rounded-lg border border-zinc-800 bg-zinc-950/40 p-4">
                <h3 className="flex items-center gap-2 text-xs font-black uppercase text-zinc-400">
                  <CheckCircle2 className="h-4 w-4" /> Điều kiện áp dụng
                </h3>
                <ul className="mt-3 space-y-2 text-sm leading-6 text-zinc-300">
                  {conditionItems.map((item) => (
                    <li key={item} className="flex gap-2">
                      <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </section>
            </div>
          )}
        </div>

        <footer className="flex flex-wrap justify-end gap-2 border-t border-zinc-800 bg-zinc-950/50 px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="h-10 rounded-lg border border-zinc-700 px-4 text-xs font-black text-zinc-300 hover:bg-zinc-800 hover:text-white"
          >
            Đóng
          </button>
          {detail && usable && (
            <button
              type="button"
              onClick={onUseNow}
              className="inline-flex h-10 items-center gap-2 rounded-lg bg-brand-orange px-4 text-xs font-black text-white hover:bg-orange-600"
            >
              <ShoppingCart className="h-4 w-4" /> Dùng ngay
            </button>
          )}
        </footer>
      </section>
    </div>
  );
}

export default function CustomerPromotionCenterPage({ embedded = false }) {
  const navigate = useNavigate();
  const [tab, setTab] = useState(embedded ? "wallet" : "event");
  const [publicPromotions, setPublicPromotions] = useState([]);
  const [systemPromotions, setSystemPromotions] = useState([]);
  const [wallet, setWallet] = useState([]);
  const [history, setHistory] = useState([]);
  const [walletOwnershipIds, setWalletOwnershipIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [message, setMessage] = useState(null);
  const [detailWalletId, setDetailWalletId] = useState("");
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [publicPage, walletPage, systemPage, historyItems] = await Promise.all([
        customerPromotionService.getPublicPromotions({
          page: 0,
          size: 100,
          sort: "priority,asc",
        }),
        customerPromotionService.getMyPromotions({
          page: 0,
          size: 100,
          sort: "validTo,asc",
          status: "ALL",
        }),
        customerPromotionService.getSystemPromotions({
          page: 0,
          size: 100,
          sort: "priority,asc",
        }),
        customerPromotionService.getMyPromotionHistory(),
      ]);
      const walletItems = contentOf(walletPage);
      setPublicPromotions(contentOf(publicPage));
      setWallet(walletItems.filter((item) => isWalletPromotionUsable(item)));
      setWalletOwnershipIds(
        new Set(
          walletItems
            .map((item) => item.promotionPublicId || item.promotion?.publicId)
            .filter(Boolean),
        ),
      );
      setSystemPromotions(contentOf(systemPage));
      setHistory(Array.isArray(historyItems) ? historyItems : []);
      setMessage(null);
    } catch (error) {
      setMessage({ kind: "error", text: friendlyPromotionError(error) });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const eventPromotions = useMemo(
    () =>
      publicPromotions.filter(
        (item) =>
          item.promotionType === "VOUCHER" &&
          !walletOwnershipIds.has(item.promotionPublicId || item.publicId),
      ),
    [publicPromotions, walletOwnershipIds],
  );
  const personalVouchers = useMemo(
    () =>
      wallet
        .filter((item) => item.promotionType === "VOUCHER")
        .sort(
          (first, second) =>
            estimatedDiscountAmount(second, 1_000_000) -
            estimatedDiscountAmount(first, 1_000_000),
        ),
    [wallet],
  );
  const activeSystemPromotions = useMemo(
    () =>
      systemPromotions
        .filter((item) => item.promotionType === "AUTO")
        .sort(
          (first, second) =>
            estimatedDiscountAmount(second, 1_000_000) -
            estimatedDiscountAmount(first, 1_000_000),
        ),
    [systemPromotions],
  );

  const counts = {
    system: activeSystemPromotions.length,
    event: eventPromotions.length,
    wallet: personalVouchers.length,
    history: history.length,
  };
  const rows =
    tab === "history"
      ? history
      : tab === "system"
      ? activeSystemPromotions
      : tab === "event"
        ? eventPromotions
        : personalVouchers;

  const claim = async (publicId) => {
    setBusyId(publicId);
    setMessage(null);
    try {
      await customerPromotionService.claimVoucher(publicId);
      await load();
      setMessage({
        kind: "success",
        text: "Voucher đã được thêm vào ví cá nhân.",
      });
      setTab("wallet");
    } catch (error) {
      setMessage({ kind: "error", text: friendlyPromotionError(error) });
    } finally {
      setBusyId("");
    }
  };

  const loadVoucherDetail = useCallback(
    async (walletPublicId) => {
      if (!walletPublicId) return;
      setDetailLoading(true);
      setDetailError("");
      try {
        const result =
          await customerPromotionService.getMyPromotionDetail(walletPublicId);
        setDetail(result);
        if (!isWalletPromotionUsable(result)) {
          await load();
        }
      } catch (error) {
        setDetail(null);
        setDetailError(
          friendlyPromotionError(error) ||
            "Voucher không còn trong ví hoặc đã hết quyền sử dụng.",
        );
      } finally {
        setDetailLoading(false);
      }
    },
    [load],
  );

  const openVoucherDetail = (promotion) => {
    const walletPublicId = walletIdOf(promotion);
    if (!walletPublicId) return;
    setDetailWalletId(walletPublicId);
    setDetail(null);
    setDetailError("");
    void loadVoucherDetail(walletPublicId);
  };

  const closeVoucherDetail = useCallback(() => {
    setDetailWalletId("");
    setDetail(null);
    setDetailError("");
    setDetailLoading(false);
  }, []);

  const useVoucherNow = () => {
    closeVoucherDetail();
    navigate("/movies");
  };

  const content = (
    <div className={embedded ? "w-full" : "mx-auto max-w-6xl"}>
      {!embedded && (
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-6">
          <div>
            <div className="flex items-center gap-2 text-xs font-black uppercase text-brand-orange">
              <WalletCards className="h-4 w-4" /> Ví ưu đãi
            </div>
            <h1 className="mt-2 text-2xl font-black text-white sm:text-3xl">
              Ưu đãi của tôi
            </h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
              Nhận voucher sự kiện, quản lý ưu đãi cá nhân và kiểm tra nguồn
              phân phối trước khi đặt vé.
            </p>
          </div>
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading}
            title="Tải lại khuyến mãi"
            aria-label="Tải lại khuyến mãi"
            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-zinc-700 text-zinc-400 hover:bg-zinc-800 hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </header>
      )}

      <div
        className={`${embedded ? "" : "mt-6"} grid grid-cols-2 border-b border-zinc-800 sm:grid-cols-4`}
      >
        {tabItems.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={`flex min-h-12 items-center justify-center gap-2 border-b-2 px-2 py-3 text-xs font-bold ${tab === key ? "border-brand-orange text-brand-orange" : "border-transparent text-zinc-500 hover:bg-zinc-900/60 hover:text-zinc-200"}`}
          >
            <Icon className="h-4 w-4" /> {label}
            {counts[key] !== undefined ? ` (${counts[key]})` : ""}
          </button>
        ))}
      </div>

      {message && (
        <div
          role="alert"
          className={`mt-4 flex items-start gap-2 border-l-2 px-3 py-2 text-sm ${message.kind === "error" ? "border-red-500 bg-red-500/10 text-red-300" : "border-emerald-500 bg-emerald-500/10 text-emerald-300"}`}
        >
          {message.kind === "error" ? (
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          ) : (
            <BadgeCheck className="mt-0.5 h-4 w-4 shrink-0" />
          )}
          <span>{message.text}</span>
          {message.kind === "error" && (
            <button
              type="button"
              onClick={() => void load()}
              className="ml-auto shrink-0 text-xs font-black"
            >
              Thử lại
            </button>
          )}
        </div>
      )}

      {tab === "system" && (
        <div className="mt-4 flex items-start gap-2 rounded-lg border border-sky-500/20 bg-sky-500/[0.05] px-3 py-2 text-xs leading-5 text-sky-100">
          <Globe2 className="mt-0.5 h-4 w-4 shrink-0 text-sky-300" />
          <span>
            Hệ thống tự áp dụng ưu đãi tốt nhất khi đơn hàng đủ điều kiện. Bạn
            không cần chọn hoặc nhập mã.
          </span>
        </div>
      )}

      <section className="mt-2" aria-live="polite">
        {loading ? (
          <div className="flex min-h-48 items-center justify-center gap-2 text-sm font-semibold text-zinc-500">
            <Loader2 className="h-5 w-5 animate-spin" /> Đang tải khuyến mãi...
          </div>
        ) : rows.length === 0 ? (
          <div className="flex min-h-48 flex-col items-center justify-center border-y border-zinc-800 text-center">
            <Gift className="h-8 w-8 text-zinc-700" />
            <p className="mt-3 text-sm font-black text-zinc-300">
              {tab === "event"
                ? "Chưa có ưu đãi mới để nhận"
                : tab === "system"
                  ? "Chưa có ưu đãi tự động đang áp dụng"
                  : tab === "history"
                    ? "Chưa có lịch sử sử dụng voucher"
                    : "Bạn chưa có voucher có thể sử dụng"}
            </p>
            <p className="mt-1 text-xs text-zinc-600">
              Các ưu đãi hợp lệ sẽ xuất hiện tại đây.
            </p>
          </div>
        ) : (
          rows.map((item, index) =>
            tab === "history" ? (
              <PromotionHistoryRow
                key={`${item.eventType}-${item.eventAt}-${item.promotionPublicId}-${index}`}
                item={item}
              />
            ) : (
              <PromotionCard
                key={item.publicId}
                promotion={item}
                claimable={tab === "event"}
                system={tab === "system"}
                recommended={tab === "wallet" && index === 0}
                busy={busyId === (item.promotionPublicId || item.publicId)}
                onClaim={claim}
                onOpenDetail={
                  tab === "wallet" ? () => openVoucherDetail(item) : undefined
                }
              />
            ),
          )
        )}
      </section>
      <VoucherDetailModal
        walletId={detailWalletId}
        detail={detail}
        loading={detailLoading}
        error={detailError}
        onClose={closeVoucherDetail}
        onRetry={() => void loadVoucherDetail(detailWalletId)}
        onUseNow={useVoucherNow}
      />
    </div>
  );

  return embedded ? (
    content
  ) : (
    <main className="min-h-screen bg-zinc-950 px-4 py-10 text-zinc-100 sm:px-6 lg:px-8">
      {content}
    </main>
  );
}
