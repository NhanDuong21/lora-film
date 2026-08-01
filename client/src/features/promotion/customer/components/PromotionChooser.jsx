import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import {
  AlertCircle,
  CheckCircle2,
  Gift,
  Globe2,
  Loader2,
  RefreshCw,
  Search,
  WalletCards,
  X,
} from "lucide-react";
import {
  conditionSummary,
  currency,
  formatDateTime,
  isWalletPromotionUsable,
  promotionSourceLabel,
  safeJsonParse,
  voucherDiscountSummary,
} from "../../shared/promotionPresentation";
import {
  getCinemaBySlug,
  getMovieById,
  getMovies,
} from "@/features/catalog/customer/services/movieService";

const promotionId = (promotion) =>
  promotion?.walletPublicId ||
  promotion?.selectionPublicId ||
  promotion?.publicId ||
  promotion?.promotionPublicId ||
  promotion?.id ||
  promotion?.code;

const jsonValue = (value, fallback = {}) =>
  typeof value === "string"
    ? safeJsonParse(value, fallback).value
    : (value ?? fallback);

const dayLabels = {
  MONDAY: "T2",
  TUESDAY: "T3",
  WEDNESDAY: "T4",
  THURSDAY: "T5",
  FRIDAY: "T6",
  SATURDAY: "T7",
  SUNDAY: "CN",
};

const configuredIds = (conditions, primaryKey, legacyKey) => {
  if (Array.isArray(conditions?.[primaryKey])) return conditions[primaryKey];
  if (Array.isArray(conditions?.[legacyKey])) return conditions[legacyKey];
  return [];
};

const labelFromSource = (source, id, index) => {
  if (!source) return "";
  if (Array.isArray(source)) {
    const matched = source.find((item) => {
      if (item == null || typeof item !== "object") return false;
      return [
        item.value,
        item.publicId,
        item.id,
        item.key,
        item.moviePublicId,
        item.cinemaPublicId,
      ]
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

const hasResolvedScope = (labels, id) =>
  Object.prototype.hasOwnProperty.call(labels || {}, String(id));

const readableScopeLabel = (value, id) => {
  const label =
    value?.label ||
    value?.title ||
    value?.movieTitle ||
    value?.name ||
    value?.cinemaName ||
    value?.publicId ||
    "";
  return label && label !== String(id) ? label : "";
};

const listContent = (page) =>
  Array.isArray(page) ? page : page?.content || page?.data || [];

const resolveMovieScopeLabel = async (id) => {
  try {
    const movie = await getMovieById(id);
    const label = readableScopeLabel(movie, id);
    if (label) return label;
  } catch {
    // The public detail endpoint can hide non-public movies; fall back below.
  }
  try {
    const page = await getMovies({ status: "ALL", page: 0, size: 300 });
    const movie = listContent(page).find((item) =>
      [item?.publicId, item?.id, item?.slug]
        .filter(Boolean)
        .map(String)
        .includes(String(id)),
    );
    return readableScopeLabel(movie, id) || null;
  } catch {
    return null;
  }
};

const resolveCinemaScopeLabel = async (id) => {
  try {
    const cinema = await getCinemaBySlug(id);
    return readableScopeLabel(cinema, id) || null;
  } catch {
    return null;
  }
};

const configuredNames = (
  promotion,
  ids,
  labelKey,
  resolvedLabels = {},
  fallbackKeys = [],
) => {
  const metadata = jsonValue(promotion?.metadataJson);
  const conditionLabels = jsonValue(metadata?.conditionLabels);
  const sources = [
    conditionLabels?.[labelKey],
    metadata?.[labelKey],
    ...fallbackKeys.map((key) => conditionLabels?.[key] ?? metadata?.[key]),
  ];
  return ids.map((id, index) => {
    const configuredLabel = sources
      .map((source) => labelFromSource(source, id, index))
      .find(Boolean);
    if (configuredLabel && configuredLabel !== String(id)) {
      return configuredLabel;
    }
    return (
      readableScopeLabel({ label: resolvedLabels[String(id)] }, id) ||
      configuredLabel ||
      null
    );
  });
};

const uniqueConditionScopeIds = (vouchers = []) => {
  const movieIds = new Set();
  const cinemaIds = new Set();
  vouchers.forEach((promotion) => {
    const conditions = jsonValue(promotion?.conditionsJson);
    configuredIds(conditions, "moviePublicIds", "movieIds").forEach((id) => {
      if (id) movieIds.add(String(id));
    });
    configuredIds(conditions, "cinemaPublicIds", "cinemaIds").forEach((id) => {
      if (id) cinemaIds.add(String(id));
    });
  });
  return {
    movieIds: Array.from(movieIds),
    cinemaIds: Array.from(cinemaIds),
  };
};

const conditionNotes = (promotion, scopeLabels = {}) => {
  const conditions = jsonValue(promotion?.conditionsJson);
  const notes = [];
  const minimum = conditions?.minimumOrderAmount ?? conditions?.minOrderAmount;
  if (Number(minimum) > 0) {
    notes.push(`Đơn tối thiểu ${currency(minimum)}`);
  }
  const movies = configuredIds(conditions, "moviePublicIds", "movieIds");
  if (movies.length) {
    const movieNames = configuredNames(
      promotion,
      movies,
      "moviePublicIds",
      scopeLabels.movies,
      ["movieIds", "movieNames", "movieTitles"],
    ).filter(Boolean);
    notes.push(
      movieNames.length
        ? `Chỉ áp dụng cho phim ${movieNames.join(", ")}`
        : "Chỉ áp dụng cho phim được cấu hình",
    );
  }
  const cinemas = configuredIds(conditions, "cinemaPublicIds", "cinemaIds");
  if (cinemas.length) {
    const cinemaNames = configuredNames(
      promotion,
      cinemas,
      "cinemaPublicIds",
      scopeLabels.cinemas,
      ["cinemaIds", "cinemaNames"],
    ).filter(Boolean);
    notes.push(
      cinemaNames.length
        ? `Chỉ áp dụng cho rạp ${cinemaNames.join(", ")}`
        : "Chỉ áp dụng cho rạp được cấu hình",
    );
  }
  if (Array.isArray(conditions?.dayOfWeek) && conditions.dayOfWeek.length) {
    notes.push(
      `Chỉ áp dụng ${conditions.dayOfWeek.map((day) => dayLabels[day] || day).join(", ")}`,
    );
  }
  if (conditions?.requiredTierCode) {
    notes.push(`Yêu cầu hạng ${conditions.requiredTierCode}`);
  }
  if (conditions?.requiresVerification) {
    notes.push("Yêu cầu tài khoản đã xác thực");
  }
  return notes;
};

const contextualReason = (evaluation, bookingContext = {}) => {
  if (evaluation?.reasonCode === "MOVIE_NOT_APPLICABLE") {
    return bookingContext.movieTitle
      ? `Không áp dụng cho phim ${bookingContext.movieTitle}`
      : evaluation.reason;
  }
  if (evaluation?.reasonCode === "CINEMA_NOT_APPLICABLE") {
    return bookingContext.cinemaName
      ? `Không áp dụng cho rạp ${bookingContext.cinemaName}`
      : evaluation.reason;
  }
  return evaluation?.reason;
};

const promotionTab = (promotion) => {
  if (promotion.promotionType === "COUPON") return "hidden";
  if (promotion.promotionType === "AUTO") return "system";
  return "wallet";
};

const walletGroupKey = (item) => {
  if (!item.evaluation.eligible) return "unavailable";
  if (
    item.promotion.source === "PUBLIC_EVENT" ||
    item.promotion.ownershipType === "CLAIMABLE"
  )
    return "claimable";
  return "wallet";
};

const groupMeta = {
  wallet: { label: "Voucher trong ví", icon: WalletCards },
  claimable: { label: "Có thể nhận vào ví", icon: Gift },
  unavailable: { label: "Không khả dụng", icon: AlertCircle },
};

export default function PromotionChooser({
  open,
  vouchers = [],
  loading = false,
  error = "",
  selectedPromotionIds = [],
  backendAppliedIds = [],
  promotionEvaluations = [],
  bookingContext = {},
  onSelect,
  onClaim,
  onClear,
  onClose,
  onRefresh,
}) {
  const [query, setQuery] = useState("");
  const [activeTab, setActiveTab] = useState("wallet");
  const [claimingId, setClaimingId] = useState("");
  const [scopeLabels, setScopeLabels] = useState({ movies: {}, cinemas: {} });
  const searchRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    searchRef.current?.focus();
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose, open]);

  const scopeIds = useMemo(() => uniqueConditionScopeIds(vouchers), [vouchers]);

  useEffect(() => {
    if (!open) return undefined;
    const missingMovieIds = scopeIds.movieIds.filter(
      (id) => !hasResolvedScope(scopeLabels.movies, id),
    );
    const missingCinemaIds = scopeIds.cinemaIds.filter(
      (id) => !hasResolvedScope(scopeLabels.cinemas, id),
    );
    if (missingMovieIds.length === 0 && missingCinemaIds.length === 0) {
      return undefined;
    }
    let active = true;
    const resolveLabels = async () => {
      const [movies, cinemas] = await Promise.all([
        Promise.all(
          missingMovieIds.map(async (id) => {
            const label = await resolveMovieScopeLabel(id);
            return [String(id), label];
          }),
        ),
        Promise.all(
          missingCinemaIds.map(async (id) => {
            const label = await resolveCinemaScopeLabel(id);
            return [String(id), label];
          }),
        ),
      ]);
      if (!active) return;
      setScopeLabels((current) => ({
        movies: { ...current.movies, ...Object.fromEntries(movies) },
        cinemas: { ...current.cinemas, ...Object.fromEntries(cinemas) },
      }));
    };
    void resolveLabels();
    return () => {
      active = false;
    };
  }, [
    open,
    scopeIds.movieIds.join("|"),
    scopeIds.cinemaIds.join("|"),
    scopeLabels.movies,
    scopeLabels.cinemas,
  ]);

  const appliedIds = useMemo(
    () => new Set(backendAppliedIds.map(String)),
    [backendAppliedIds],
  );
  const selectedIds = useMemo(
    () => new Set(selectedPromotionIds.map(String)),
    [selectedPromotionIds],
  );
  const evaluationMap = useMemo(() => {
    const result = new Map();
    promotionEvaluations.forEach((evaluation) => {
      if (evaluation?.userPromotionPublicId) {
        result.set(`wallet:${evaluation.userPromotionPublicId}`, evaluation);
      }
      if (evaluation?.promotionPublicId) {
        result.set(`promotion:${evaluation.promotionPublicId}`, evaluation);
      }
    });
    return result;
  }, [promotionEvaluations]);
  const catalog = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi-VN");
    return vouchers
      .filter((promotion) => isWalletPromotionUsable(promotion))
      .map((promotion) => {
        const id = promotionId(promotion);
        const walletId =
          promotion?.walletPublicId ||
          promotion?.selectionPublicId ||
          (promotion?.source === "CUSTOMER_WALLET"
            ? promotion?.publicId
            : null);
        const promotionPublicId =
          promotion?.promotionPublicId || promotion?.publicId;
        const backendEvaluation = walletId
          ? evaluationMap.get(`wallet:${walletId}`)
          : evaluationMap.get(`promotion:${promotionPublicId}`);
        const evaluation = backendEvaluation
          ? {
              ...backendEvaluation,
              reason: contextualReason(backendEvaluation, bookingContext),
              notes: conditionNotes(promotion, scopeLabels),
            }
          : {
              eligible: false,
              discountAmount: 0,
              reason: loading
                ? "Đang kiểm tra điều kiện với hệ thống"
                : "Chưa xác nhận được điều kiện áp dụng",
              reasonCode: "NOT_EVALUATED",
              notes: conditionNotes(promotion, scopeLabels),
            };
        return {
          promotion,
          id,
          evaluation,
          estimatedDiscount: Number(evaluation.discountAmount || 0),
        };
      })
      .filter(
        ({ promotion }) =>
          !normalizedQuery ||
          [promotion.name, promotion.code, promotion.description].some(
            (value) =>
              String(value || "")
                .toLocaleLowerCase("vi-VN")
                .includes(normalizedQuery),
          ),
      );
  }, [bookingContext, evaluationMap, loading, query, scopeLabels, vouchers]);

  const tabCounts = useMemo(
    () => ({
      wallet: catalog.filter(
        (item) => promotionTab(item.promotion) === "wallet",
      ).length,
      system: catalog.filter(
        (item) => promotionTab(item.promotion) === "system",
      ).length,
    }),
    [catalog],
  );
  const hasExclusiveSelection = useMemo(
    () =>
      catalog.some(
        ({ promotion, id }) =>
          selectedIds.has(String(id)) && !promotion?.stackable,
      ),
    [catalog, selectedIds],
  );

  const grouped = useMemo(() => {
    const visible = catalog
      .filter((item) => promotionTab(item.promotion) === activeTab)
      .sort(
        (first, second) =>
          Number(second.evaluation.eligible) -
            Number(first.evaluation.eligible) ||
          second.estimatedDiscount - first.estimatedDiscount ||
          String(first.promotion.validTo || "").localeCompare(
            String(second.promotion.validTo || ""),
          ),
      );
    if (activeTab === "system") {
      return [
        {
          key: "system",
          label: "Voucher hệ thống",
          icon: Globe2,
          items: visible,
        },
      ].filter((group) => group.items.length > 0);
    }
    return ["wallet", "claimable", "unavailable"]
      .map((key) => ({
        key,
        label: groupMeta[key].label,
        icon: groupMeta[key].icon,
        items: visible
          .filter((item) => walletGroupKey(item) === key)
          .sort(
            (first, second) =>
              second.estimatedDiscount - first.estimatedDiscount ||
              String(first.promotion.validTo || "").localeCompare(
                String(second.promotion.validTo || ""),
              ),
          ),
      }))
      .filter((group) => group.items.length > 0);
  }, [activeTab, catalog]);

  const claimAndSelect = async (promotion) => {
    if (!onClaim) return;
    const id = promotion.promotionPublicId || promotion.publicId;
    setClaimingId(String(id));
    try {
      await onClaim(promotion);
    } finally {
      setClaimingId("");
    }
  };

  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="promotion-chooser-title"
        className="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4">
          <div className="flex min-w-0 items-start gap-3">
            <span className="rounded-lg bg-emerald-500/10 p-2.5 text-emerald-400">
              <Gift className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <h2
                id="promotion-chooser-title"
                className="text-base font-black text-white"
              >
                Chọn ưu đãi
              </h2>
              <p className="mt-1 text-xs text-zinc-500">
                {vouchers.length} voucher và ưu đãi hệ thống
              </p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            <button
              type="button"
              title="Tải lại ưu đãi"
              aria-label="Tải lại ưu đãi"
              disabled={loading}
              onClick={onRefresh}
              className="rounded-lg p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white disabled:opacity-50"
            >
              <RefreshCw
                className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
              />
            </button>
            <button
              type="button"
              aria-label="Đóng"
              onClick={onClose}
              className="rounded-lg p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </header>

        <div className="border-b border-zinc-800 px-5 py-3">
          <div className="mb-3 grid grid-cols-2 gap-1 rounded-lg bg-zinc-950 p-1">
            {[
              { key: "wallet", label: "Voucher ví", icon: WalletCards },
              { key: "system", label: "Voucher hệ thống", icon: Globe2 },
            ].map(({ key, label, icon: Icon }) => (
              <button
                key={key}
                type="button"
                onClick={() => setActiveTab(key)}
                className={`flex min-h-10 items-center justify-center gap-2 rounded-md px-2 text-[10px] font-black transition-colors ${
                  activeTab === key
                    ? "bg-emerald-500 text-zinc-950"
                    : "text-zinc-500 hover:bg-zinc-800 hover:text-white"
                }`}
              >
                <Icon className="h-4 w-4" />
                {label} ({tabCounts[key] || 0})
              </button>
            ))}
          </div>
          <label className="relative block">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
            <input
              ref={searchRef}
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo tên hoặc mã"
              aria-label="Tìm ưu đãi"
              className="h-10 w-full rounded-lg border border-zinc-800 bg-zinc-950 pl-10 pr-3 text-xs font-semibold text-white outline-none transition-colors placeholder:text-zinc-600 focus:border-emerald-500"
            />
          </label>
        </div>

        <div className="min-h-48 flex-1 overflow-y-auto p-5">
          {error && (
            <div
              role="alert"
              className="mb-4 flex gap-2 border-l-2 border-red-500 bg-red-500/10 p-3 text-xs leading-5 text-red-300"
            >
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}
          {loading && vouchers.length === 0 ? (
            <div className="flex min-h-48 items-center justify-center gap-2 text-xs font-bold text-zinc-500">
              <Loader2 className="h-4 w-4 animate-spin" /> Đang tải ưu đãi...
            </div>
          ) : grouped.length === 0 ? (
            <div className="flex min-h-48 flex-col items-center justify-center px-6 text-center">
              <Gift className="h-8 w-8 text-zinc-700" />
              <p className="mt-3 text-sm font-black text-zinc-300">
                {query
                  ? "Không tìm thấy ưu đãi phù hợp"
                  : activeTab === "system"
                    ? "Chưa có voucher hệ thống phù hợp"
                    : "Chưa có voucher ví để lựa chọn"}
              </p>
              <p className="mt-1 text-xs leading-5 text-zinc-600">
                {query
                  ? "Thử tìm bằng tên hoặc mã khác."
                  : activeTab === "system"
                    ? "Voucher hệ thống đang chạy sẽ xuất hiện tại tab này."
                    : "Voucher đã nhận sẽ xuất hiện tại đây."}
              </p>
            </div>
          ) : (
            <div className="space-y-6">
              {grouped.map((group) => {
                const GroupIcon = group.icon;
                return (
                  <section key={group.key}>
                    <h3 className="mb-2 flex items-center gap-2 text-[10px] font-black uppercase text-zinc-500">
                      <GroupIcon className="h-3.5 w-3.5" /> {group.label} (
                      {group.items.length})
                    </h3>
                    <div className="space-y-2">
                      {group.items.map(
                        ({ promotion, id, evaluation }, itemIndex) => {
                          const selected = selectedIds.has(String(id));
                          const applied = [
                            promotion.walletPublicId,
                            promotion.publicId,
                            promotion.promotionPublicId,
                          ]
                            .filter(Boolean)
                            .some((value) => appliedIds.has(String(value)));
                          const active = selected || applied;
                          const blockedBySelection =
                            !selected &&
                            selectedIds.size > 0 &&
                            (hasExclusiveSelection || !promotion?.stackable);
                          const claimable = group.key === "claimable";
                          const recommended =
                            ["wallet", "system"].includes(group.key) &&
                            itemIndex === 0 &&
                            evaluation.eligible;
                          return (
                            <article
                              key={id}
                              aria-disabled={
                                !evaluation.eligible || blockedBySelection
                              }
                              className={`rounded-xl border px-4 py-4 transition-opacity ${
                                active
                                  ? "border-emerald-500/35 bg-emerald-500/[0.06]"
                                  : "border-zinc-800 bg-zinc-950/25"
                              } ${
                                evaluation.eligible && !blockedBySelection
                                  ? ""
                                  : "opacity-40"
                              }`}
                            >
                              <div className="flex items-start justify-between gap-3">
                                <div className="min-w-0">
                                  <div className="flex flex-wrap items-center gap-2">
                                    <h4 className="break-words text-sm font-black text-white">
                                      {promotion.name || "Ưu đãi LoraFilm"}
                                    </h4>
                                    {recommended && (
                                      <span className="rounded border border-amber-400/30 bg-amber-400/10 px-2 py-0.5 text-[9px] font-black text-amber-200">
                                        Đề xuất
                                      </span>
                                    )}
                                    {promotion?.stackable && (
                                      <span className="rounded border border-sky-400/25 bg-sky-400/10 px-2 py-0.5 text-[9px] font-black text-sky-200">
                                        Cộng dồn
                                      </span>
                                    )}
                                    {applied && (
                                      <span className="inline-flex items-center gap-1 rounded border border-emerald-500/30 bg-emerald-500/10 px-2 py-0.5 text-[9px] font-black text-emerald-300">
                                        <CheckCircle2 className="h-3 w-3" />{" "}
                                        Đang áp dụng
                                      </span>
                                    )}
                                  </div>
                                  {promotion.code && (
                                    <p className="mt-1 break-all font-mono text-[10px] font-bold text-zinc-500">
                                      {promotion.code}
                                    </p>
                                  )}
                                </div>
                                <span
                                  className={`shrink-0 rounded border px-2 py-1 text-[9px] font-black ${
                                    evaluation.eligible && !blockedBySelection
                                      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
                                      : "border-zinc-700 bg-zinc-800 text-zinc-400"
                                  }`}
                                >
                                  {evaluation.reasonCode === "NOT_EVALUATED" &&
                                  loading
                                    ? "Đang kiểm tra"
                                    : blockedBySelection
                                      ? "Không cộng dồn"
                                      : evaluation.eligible
                                      ? "Có thể sử dụng"
                                      : "Không khả dụng"}
                                </span>
                              </div>
                              <div className="mt-3 grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
                                <div className="min-w-0 space-y-1.5">
                                  <p className="text-sm font-black text-emerald-400">
                                    {voucherDiscountSummary(promotion)}
                                  </p>
                                  <p className="text-[10px] font-bold uppercase text-sky-300">
                                    {promotionSourceLabel(promotion)}
                                  </p>
                                  <p className="break-words text-[11px] leading-5 text-zinc-400">
                                    {conditionSummary(promotion.conditionsJson)}
                                  </p>
                                  {evaluation.notes.length > 0 && (
                                    <ul className="space-y-1 text-[10px] leading-4 text-zinc-500">
                                      {evaluation.notes.map((note) => (
                                        <li key={note} className="flex gap-1.5">
                                          <span className="mt-1 h-1 w-1 shrink-0 rounded-full bg-zinc-600" />
                                          <span>{note}</span>
                                        </li>
                                      ))}
                                    </ul>
                                  )}
                                  <p
                                    className={`flex items-start gap-1.5 text-[10px] font-bold ${evaluation.eligible ? "text-emerald-400" : "text-amber-400"}`}
                                  >
                                    {evaluation.eligible ? (
                                      <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                                    ) : (
                                      <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                                    )}
                                    <span>
                                      {evaluation.reason} · Hết hạn{" "}
                                      {formatDateTime(promotion.validTo)}
                                    </span>
                                  </p>
                                </div>
                                <button
                                  type="button"
                                  disabled={
                                    !evaluation.eligible ||
                                    claimingId === String(id) ||
                                    (applied && !selected) ||
                                    blockedBySelection ||
                                    loading
                                  }
                                  onClick={() => {
                                    if (claimable)
                                      void claimAndSelect(promotion);
                                    else onSelect(promotion);
                                  }}
                                  className={`h-9 min-w-28 rounded-lg px-3 text-[10px] font-black uppercase transition-colors ${
                                    active
                                      ? "border border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
                                      : "bg-emerald-500 text-zinc-950 hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-600"
                                  }`}
                                >
                                  {claimingId === String(id) ? (
                                    <Loader2 className="mx-auto h-4 w-4 animate-spin" />
                                  ) : !evaluation.eligible ? (
                                    "Không khả dụng"
                                  ) : blockedBySelection ? (
                                    "Không cộng dồn"
                                  ) : selected ? (
                                    "Bỏ chọn"
                                  ) : applied ? (
                                    "Tự động áp dụng"
                                  ) : claimable ? (
                                    "Nhận & dùng"
                                  ) : (
                                    "Chọn"
                                  )}
                                </button>
                              </div>
                            </article>
                          );
                        },
                      )}
                    </div>
                  </section>
                );
              })}
            </div>
          )}
        </div>

        <footer className="border-t border-zinc-800 bg-zinc-950/40 px-5 py-3">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="flex min-w-0 gap-2 text-[10px] leading-4 text-zinc-500">
              <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
              <span>
                {selectedIds.size > 0
                  ? `${selectedIds.size} voucher đã chọn`
                  : "Chưa chọn voucher thủ công"}
              </span>
            </p>
            <div className="flex shrink-0 items-center justify-end gap-2">
              {selectedIds.size > 0 && (
                <button
                  type="button"
                  onClick={onClear}
                  className="h-9 rounded-lg px-3 text-[10px] font-black uppercase text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-white"
                >
                  Bỏ chọn tất cả
                </button>
              )}
              <button
                type="button"
                onClick={onClose}
                className="h-9 rounded-lg bg-emerald-500 px-5 text-[10px] font-black uppercase text-zinc-950 transition-colors hover:bg-emerald-400"
              >
                Xong
              </button>
            </div>
          </div>
        </footer>
      </section>
    </div>,
    document.body,
  );
}
