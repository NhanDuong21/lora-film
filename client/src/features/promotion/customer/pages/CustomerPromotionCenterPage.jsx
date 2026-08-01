import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  BadgeCheck,
  Gift,
  Globe2,
  Loader2,
  RefreshCw,
  TicketPercent,
  WalletCards,
} from "lucide-react";
import customerPromotionService from "../services/customerPromotionService";
import {
  badgeClass,
  conditionSummary,
  formatDateTime,
  friendlyPromotionError,
  labelFor,
  estimatedDiscountAmount,
  promotionSourceLabel,
  voucherDiscountSummary,
} from "../../shared/promotionPresentation";

const contentOf = (payload) =>
  Array.isArray(payload) ? payload : payload?.content || [];

const tabItems = [
  { key: "system", label: "Hệ thống", icon: Globe2 },
  { key: "event", label: "Sự kiện", icon: Gift },
  { key: "wallet", label: "Ví voucher", icon: WalletCards },
];

function PromotionCard({
  promotion,
  claimable = false,
  system = false,
  recommended = false,
  busy,
  onClaim,
}) {
  const available = ["ACTIVE", "AVAILABLE"].includes(promotion.status);
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
    <article className="grid gap-4 border-b border-zinc-800 py-5 last:border-b-0 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <TicketPercent className="h-4 w-4 text-emerald-400" />
          <h3 className="break-words text-sm font-black text-white">
            {promotion.name || "Ưu đãi LoraFilm"}
          </h3>
          {recommended && (
            <span className="rounded border border-amber-400/30 bg-amber-400/10 px-2 py-0.5 text-[10px] font-black text-amber-200">
              Đề xuất
            </span>
          )}
          <span
            className={`rounded border px-2 py-0.5 text-[10px] font-bold ${badgeClass(promotion.status)}`}
          >
            {labelFor(promotion.status)}
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
        <p className="mt-2 text-base font-black text-emerald-400">
          {voucherDiscountSummary(promotion)}
        </p>
        {promotion.description && (
          <p className="mt-1 text-xs leading-5 text-zinc-400">
            {promotion.description}
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
          onClick={() =>
            onClaim(promotion.promotionPublicId || promotion.publicId)
          }
          className="inline-flex h-9 min-w-32 items-center justify-center gap-2 rounded-lg bg-emerald-500 px-3 text-xs font-black text-zinc-950 transition-colors hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
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
          <Globe2 className="h-4 w-4" /> Chọn tại checkout
        </span>
      ) : (
        <span className="inline-flex h-9 items-center gap-2 self-center rounded-lg border border-emerald-500/25 bg-emerald-500/10 px-3 text-xs font-bold text-emerald-300">
          <BadgeCheck className="h-4 w-4" /> Đã thuộc ví
        </span>
      )}
    </article>
  );
}

export default function CustomerPromotionCenterPage({ embedded = false }) {
  const [tab, setTab] = useState(embedded ? "wallet" : "event");
  const [publicPromotions, setPublicPromotions] = useState([]);
  const [systemPromotions, setSystemPromotions] = useState([]);
  const [wallet, setWallet] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [message, setMessage] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [publicPage, walletPage, systemPage] = await Promise.all([
        customerPromotionService.getPublicPromotions({
          page: 0,
          size: 100,
          sort: "priority,asc",
        }),
        customerPromotionService.getMyPromotions({
          page: 0,
          size: 100,
          sort: "validTo,asc",
        }),
        customerPromotionService.getSystemPromotions({
          page: 0,
          size: 100,
          sort: "priority,asc",
        }),
      ]);
      setPublicPromotions(contentOf(publicPage));
      setWallet(contentOf(walletPage));
      setSystemPromotions(contentOf(systemPage));
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

  const claimedPromotionIds = useMemo(
    () => new Set(wallet.map((item) => item.promotionPublicId).filter(Boolean)),
    [wallet],
  );
  const eventPromotions = useMemo(
    () =>
      publicPromotions.filter(
        (item) =>
          item.promotionType === "VOUCHER" &&
          !claimedPromotionIds.has(item.promotionPublicId || item.publicId),
      ),
    [claimedPromotionIds, publicPromotions],
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
  };
  const rows =
    tab === "system"
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

  const content = (
    <div className={embedded ? "w-full" : "mx-auto max-w-6xl"}>
      {!embedded && (
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-6">
          <div>
            <div className="flex items-center gap-2 text-xs font-black uppercase text-emerald-400">
              <WalletCards className="h-4 w-4" /> Ví ưu đãi
            </div>
            <h1 className="mt-2 text-2xl font-black text-white sm:text-3xl">
              Khuyến mãi của bạn
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
        className={`${embedded ? "" : "mt-6"} grid grid-cols-2 border-b border-zinc-800 sm:grid-cols-3`}
      >
        {tabItems.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={`flex min-h-12 items-center justify-center gap-2 border-b-2 px-2 py-3 text-xs font-bold ${tab === key ? "border-emerald-500 text-white" : "border-transparent text-zinc-500 hover:bg-zinc-900/60 hover:text-zinc-200"}`}
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
            Voucher hệ thống không thuộc ví cá nhân. Bạn có thể chọn voucher phù
            hợp ở bước checkout, hệ thống sẽ kiểm tra điều kiện trước khi giảm
            giá.
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
                ? "Chưa có voucher sự kiện mới"
                : tab === "system"
                  ? "Chưa có ưu đãi hệ thống đang chạy"
                  : "Ví voucher cá nhân đang trống"}
            </p>
            <p className="mt-1 text-xs text-zinc-600">
              Các ưu đãi hợp lệ sẽ xuất hiện tại đây.
            </p>
          </div>
        ) : (
          rows.map((item, index) => (
            <PromotionCard
              key={item.publicId}
              promotion={item}
              claimable={tab === "event"}
              system={tab === "system"}
              recommended={tab === "wallet" && index === 0}
              busy={busyId === (item.promotionPublicId || item.publicId)}
              onClaim={claim}
            />
          ))
        )}
      </section>
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
