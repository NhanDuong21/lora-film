import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  AlertCircle,
  CheckCircle2,
  Gift,
  Loader2,
  RefreshCw,
  Search,
  Sparkles,
  X,
} from 'lucide-react';
import {
  badgeClass,
  conditionSummary,
  currency,
  formatDateTime,
  labelFor,
  safeJsonParse,
  voucherDiscountSummary,
} from '../../shared/promotionPresentation';

const voucherId = voucher => voucher?.publicId || voucher?.voucherPublicId || voucher?.id || voucher?.code;

const jsonValue = (value, fallback = {}) => (
  typeof value === 'string' ? safeJsonParse(value, fallback).value : (value ?? fallback)
);

const firstAction = voucher => {
  const actions = jsonValue(voucher?.actionsJson);
  return Array.isArray(actions) ? actions[0] || {} : actions;
};

const estimatedDiscount = (voucher, bookingAmount) => {
  const amount = Math.max(0, Number(bookingAmount || 0));
  const action = firstAction(voucher);
  const type = action?.discountType || action?.type || action?.actionType || voucher?.voucherType;
  const value = Number(
    action?.discountValue
    ?? action?.value
    ?? action?.amount
    ?? action?.percentage
    ?? voucher?.faceValue
    ?? 0
  );

  if (type === 'PERCENTAGE' || type === 'PERCENT') {
    const cap = Number(
      action?.maxDiscountAmount
      ?? action?.maximumDiscountAmount
      ?? action?.maxAmount
      ?? Number.POSITIVE_INFINITY
    );
    return Math.min(amount, amount * Math.max(0, value) / 100, cap);
  }
  if (['FIXED_AMOUNT', 'AMOUNT', 'CASHBACK'].includes(type)) {
    return Math.min(amount, Math.max(0, value));
  }
  if (type === 'FREE' || type === 'FULL_DISCOUNT') return amount;
  return 0;
};

const evaluateVoucher = (voucher, bookingAmount) => {
  const status = String(voucher?.status || '').toUpperCase();
  const now = Date.now();
  const validFrom = voucher?.validFrom ? new Date(voucher.validFrom).getTime() : null;
  const validTo = voucher?.validTo ? new Date(voucher.validTo).getTime() : null;
  const conditions = jsonValue(voucher?.conditionsJson);
  const minimum = Math.max(
    Number(voucher?.minimumOrderAmount || 0),
    Number(conditions?.minimumOrderAmount ?? conditions?.minOrderAmount ?? 0)
  );

  if (!['ACTIVE', 'ISSUED', 'AVAILABLE'].includes(status)) {
    return { eligible: false, reason: labelFor(status), estimate: 0 };
  }
  if (Number.isFinite(validFrom) && validFrom > now) {
    return { eligible: false, reason: `Có hiệu lực từ ${formatDateTime(voucher.validFrom)}`, estimate: 0 };
  }
  if (Number.isFinite(validTo) && validTo <= now) {
    return { eligible: false, reason: 'Voucher đã hết hạn', estimate: 0 };
  }
  if (
    voucher?.maxUsage !== null
    && voucher?.maxUsage !== undefined
    && Number(voucher?.usageCount || 0) >= Number(voucher.maxUsage)
  ) {
    return { eligible: false, reason: 'Voucher đã hết lượt sử dụng', estimate: 0 };
  }
  if (Number(bookingAmount || 0) < minimum) {
    return {
      eligible: false,
      reason: `Cần đơn tối thiểu ${currency(minimum)}`,
      estimate: 0,
    };
  }
  return {
    eligible: true,
    reason: 'Đủ điều kiện cơ bản',
    estimate: estimatedDiscount(voucher, bookingAmount),
  };
};

export default function PromotionChooser({
  open,
  vouchers = [],
  loading = false,
  error = '',
  selectedPromotionId = '',
  bookingAmount = 0,
  onSelect,
  onClear,
  onClose,
  onRefresh,
}) {
  const [query, setQuery] = useState('');
  const searchRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    searchRef.current?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, open]);

  const evaluatedVouchers = useMemo(() => vouchers.map(voucher => ({
    voucher,
    id: voucherId(voucher),
    evaluation: evaluateVoucher(voucher, bookingAmount),
  })), [bookingAmount, vouchers]);

  const recommendedId = useMemo(() => evaluatedVouchers
    .filter(item => item.evaluation.eligible)
    .sort((left, right) => right.evaluation.estimate - left.evaluation.estimate)[0]?.id, [evaluatedVouchers]);

  const filteredVouchers = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase('vi-VN');
    return evaluatedVouchers
      .filter(({ voucher }) => !normalizedQuery || [voucher.name, voucher.code, voucher.description]
        .some(value => String(value || '').toLocaleLowerCase('vi-VN').includes(normalizedQuery)))
      .sort((left, right) => {
        if (left.evaluation.eligible !== right.evaluation.eligible) {
          return left.evaluation.eligible ? -1 : 1;
        }
        return right.evaluation.estimate - left.evaluation.estimate;
      });
  }, [evaluatedVouchers, query]);

  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="promotion-chooser-title"
        className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4">
          <div className="flex min-w-0 items-start gap-3">
            <span className="rounded-xl bg-emerald-500/10 p-2.5 text-emerald-400">
              <Gift className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <h2 id="promotion-chooser-title" className="text-base font-black text-white">Ví voucher</h2>
              <p className="mt-1 text-xs text-zinc-500">{vouchers.length} voucher trong tài khoản</p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            <button
              type="button"
              title="Tải lại ví voucher"
              aria-label="Tải lại ví voucher"
              disabled={loading}
              onClick={onRefresh}
              className="rounded-lg p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
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
          <label className="relative block">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
            <input
              ref={searchRef}
              type="search"
              value={query}
              onChange={event => setQuery(event.target.value)}
              placeholder="Tìm theo tên hoặc mã voucher"
              aria-label="Tìm voucher"
              className="h-10 w-full rounded-xl border border-zinc-800 bg-zinc-950 pl-10 pr-3 text-xs font-semibold text-white outline-none transition-colors placeholder:text-zinc-600 focus:border-emerald-500"
            />
          </label>
        </div>

        <div className="min-h-48 flex-1 overflow-y-auto p-5">
          {error && (
            <div role="alert" className="mb-4 flex gap-2 rounded-xl border border-red-500/25 bg-red-500/10 p-3 text-xs leading-5 text-red-300">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {loading && vouchers.length === 0 ? (
            <div className="flex min-h-48 items-center justify-center gap-2 text-xs font-bold text-zinc-500">
              <Loader2 className="h-4 w-4 animate-spin" />
              Đang tải ví voucher...
            </div>
          ) : filteredVouchers.length === 0 ? (
            <div className="flex min-h-48 flex-col items-center justify-center px-6 text-center">
              <Gift className="h-8 w-8 text-zinc-700" />
              <p className="mt-3 text-sm font-black text-zinc-300">
                {query ? 'Không tìm thấy voucher phù hợp' : 'Ví voucher đang trống'}
              </p>
              <p className="mt-1 text-xs leading-5 text-zinc-600">
                {query ? 'Thử tìm bằng tên hoặc mã khác.' : 'Voucher được phát hành cho bạn sẽ xuất hiện tại đây.'}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredVouchers.map(({ voucher, id, evaluation }) => {
                const selected = String(id) === String(selectedPromotionId);
                const recommended = id === recommendedId && evaluation.eligible;
                return (
                  <article
                    key={id}
                    className={`border p-4 transition-colors ${
                      selected
                        ? 'border-emerald-500/50 bg-emerald-500/[0.07]'
                        : 'border-zinc-800 bg-zinc-950/60 hover:border-zinc-700'
                    } rounded-xl ${evaluation.eligible ? '' : 'opacity-65'}`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="break-words text-sm font-black text-white">{voucher.name || 'Voucher LoraFilm'}</h3>
                          {recommended && (
                            <span className="inline-flex items-center gap-1 rounded border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[9px] font-black uppercase text-amber-300">
                              <Sparkles className="h-3 w-3" /> Gợi ý
                            </span>
                          )}
                        </div>
                        <p className="mt-1 break-all font-mono text-[10px] font-bold text-zinc-500">{voucher.code}</p>
                      </div>
                      <span className={`shrink-0 rounded border px-2 py-1 text-[9px] font-black ${badgeClass(voucher.status)}`}>
                        {labelFor(voucher.status)}
                      </span>
                    </div>

                    <div className="mt-3 grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
                      <div className="min-w-0 space-y-1.5">
                        <p className="text-sm font-black text-emerald-400">{voucherDiscountSummary(voucher)}</p>
                        {evaluation.estimate > 0 && (
                          <p className="text-[10px] font-bold text-emerald-300">
                            Ước tính giảm {currency(evaluation.estimate)} theo giá trị đơn hiện tại
                          </p>
                        )}
                        <p className="break-words text-[11px] leading-5 text-zinc-400">{conditionSummary(voucher.conditionsJson)}</p>
                        <p className={`flex items-start gap-1.5 text-[10px] font-bold ${evaluation.eligible ? 'text-emerald-400' : 'text-amber-400'}`}>
                          {evaluation.eligible
                            ? <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                            : <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" />}
                          <span>{evaluation.reason} · Hết hạn {formatDateTime(voucher.validTo)}</span>
                        </p>
                      </div>
                      <button
                        type="button"
                        disabled={!evaluation.eligible}
                        onClick={() => {
                          onSelect(voucher);
                          onClose();
                        }}
                        className={`h-9 min-w-28 rounded-lg px-3 text-[10px] font-black uppercase transition-colors ${
                          selected
                            ? 'border border-emerald-500/40 bg-emerald-500/10 text-emerald-300'
                            : 'bg-emerald-500 text-zinc-950 hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-600'
                        }`}
                      >
                        {selected ? 'Đã chọn' : 'Chọn voucher'}
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>

        <footer className="border-t border-zinc-800 bg-zinc-950/40 px-5 py-3">
          <div className="flex items-start justify-between gap-3">
            <p className="flex min-w-0 gap-2 text-[10px] leading-4 text-zinc-500">
              <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
              <span>Việc chọn voucher chưa làm thay đổi tổng tiền. Checkout chỉ hiển thị giảm giá khi Booking backend xác nhận.</span>
            </p>
            {selectedPromotionId && (
              <button
                type="button"
                onClick={onClear}
                className="shrink-0 text-[10px] font-black uppercase text-zinc-400 hover:text-white"
              >
                Bỏ chọn
              </button>
            )}
          </div>
        </footer>
      </section>
    </div>,
    document.body
  );
}
