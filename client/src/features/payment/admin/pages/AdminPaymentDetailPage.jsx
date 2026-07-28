import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  Banknote,
  CalendarClock,
  CheckCircle2,
  CircleDashed,
  ExternalLink,
  Film,
  ReceiptText,
  ShieldAlert,
  Wrench,
} from 'lucide-react';
import { getAdminPayment, paymentErrorMessage } from '../../services/paymentService';
import {
  DELIVERY_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  destinationLabel,
  eventLabel,
  humanizeSystemMessage,
  paymentConclusion,
  providerLabel,
  reasonLabel,
  statusLabel,
} from '../paymentAdminPresentation';

const money = (value, currency = 'VND') =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(Number(value || 0));

export default function AdminPaymentDetailPage() {
  const { paymentPublicId } = useParams();
  const navigate = useNavigate();
  const { triggerAlert } = useOutletContext() || {};
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    getAdminPayment(paymentPublicId)
      .then(result => active && setDetail(result))
      .catch(error => {
        triggerAlert?.(paymentErrorMessage(error));
        navigate('/admin/payments');
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [navigate, paymentPublicId, triggerAlert]);

  if (loading) {
    return <div className="py-24 text-center text-zinc-500">Đang tải giao dịch...</div>;
  }
  if (!detail) return null;

  const payment = detail.payment || {};
  const snapshot = detail.analyticsSnapshot || {};
  const cash = detail.cashDetail || {};
  const conclusion = paymentConclusion(payment);
  const reconciliationCases = detail.reconciliationCases || [];
  const openReconciliation = reconciliationCases.find(item =>
    !['RESOLVED', 'IGNORED'].includes(item.status));

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 text-white">
      <button
        onClick={() => navigate('/admin/payments')}
        className="flex items-center gap-2 text-sm font-bold text-zinc-400 hover:text-white"
      >
        <ArrowLeft className="h-4 w-4" /> Quay lại danh sách giao dịch
      </button>

      <header className="flex flex-col justify-between gap-5 rounded-3xl border border-zinc-800 bg-zinc-900 p-7 lg:flex-row lg:items-center">
        <div>
          <p className="text-xs font-black uppercase tracking-wider text-zinc-500">Mã giao dịch</p>
          <h1 className="mt-2 text-2xl font-black">{payment.paymentTransactionCode}</h1>
          <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
            <span className="rounded-full border border-zinc-700 bg-zinc-800 px-3 py-1 font-bold">
              {providerLabel(payment.provider)}
            </span>
            <span className="text-zinc-400">Lần thanh toán thứ {payment.attemptNumber}</span>
          </div>
        </div>
        <div className="text-left lg:text-right">
          <p className="text-xs font-black uppercase text-zinc-500">Số tiền giao dịch</p>
          <p className="mt-2 text-3xl font-black text-brand-orange">
            {money(payment.amount, payment.currency)}
          </p>
          <p className="mt-2 text-sm font-bold text-zinc-300">{statusLabel(payment.status)}</p>
        </div>
      </header>

      <ConclusionBanner
        conclusion={conclusion}
        onPrimaryAction={() => {
          if (conclusion.action) navigate('/admin/payments');
          else navigate(`/admin/bookings/${payment.bookingPublicId}`);
        }}
      />

      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => navigate(`/admin/bookings/${payment.bookingPublicId}`)}
          className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-xs font-black uppercase text-white"
        >
          <ExternalLink className="h-4 w-4" /> Mở đơn đặt vé liên quan
        </button>
        {openReconciliation && (
          <button
            type="button"
            onClick={() => navigate('/admin/payments')}
            className="flex items-center gap-2 rounded-xl border border-amber-500/30 px-4 py-2.5 text-xs font-black uppercase text-amber-300"
          >
            <ShieldAlert className="h-4 w-4" /> Mở hồ sơ cần xử lý
          </button>
        )}
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <InfoCard
          icon={Film}
          title="Đơn đặt vé liên quan"
          rows={[
            ['Phim', snapshot.movieTitle || payment.movieTitle || 'Chưa có dữ liệu'],
            ['Số vé', `${snapshot.ticketCount ?? payment.ticketCount ?? 0} vé`],
            ['Trạng thái cập nhật đơn',
              DELIVERY_STATUS_LABELS[payment.bookingDeliveryStatus]
                || payment.bookingDeliveryStatus
                || 'Chưa ghi nhận'],
            ['Mã đơn', 'Mở đơn để xem mã và thông tin khách hàng'],
          ]}
        />
        <InfoCard
          icon={ReceiptText}
          title="Cơ cấu số tiền"
          rows={[
            ['Tiền vé', money(snapshot.ticketAmount ?? payment.ticketAmount, payment.currency)],
            ['Bắp nước', money(snapshot.foodAmount ?? payment.foodAmount, payment.currency)],
            ['Giảm giá', `-${money(snapshot.discountAmount ?? payment.discountAmount, payment.currency)}`],
            ['Tổng thanh toán', money(payment.amount, payment.currency)],
          ]}
        />
        <InfoCard
          icon={CalendarClock}
          title="Thời gian và phương thức"
          rows={[
            ['Tạo giao dịch', formatTime(payment.createdAt)],
            ['Cập nhật gần nhất', formatTime(payment.updatedAt)],
            ['Hạn thanh toán', formatTime(payment.expiresAt)],
            ['Phương thức', PAYMENT_METHOD_LABELS[payment.paymentMethod] || payment.paymentMethod],
            ['Nhà cung cấp', providerLabel(payment.provider)],
          ]}
        />
      </div>

      {Object.keys(cash).length > 0 && (
        <InfoCard
          icon={Banknote}
          title="Thu tiền tại quầy"
          rows={[
            ['Tiền khách đưa', money(cash.receivedAmount, payment.currency)],
            ['Tiền thừa', money(cash.changeAmount, payment.currency)],
            ['Nhân viên thu', cash.collectedByAccountId ? 'Đã ghi nhận nhân viên thu' : 'Chưa thu'],
            ['Thời điểm thu', formatTime(cash.collectedAt)],
          ]}
        />
      )}

      {openReconciliation && (
        <section className="rounded-3xl border border-amber-500/30 bg-amber-500/5 p-6">
          <h2 className="flex items-center gap-2 text-sm font-black uppercase text-amber-300">
            <AlertTriangle className="h-5 w-5" /> Nội dung cần kiểm tra
          </h2>
          <p className="mt-4 text-lg font-bold">{reasonLabel(openReconciliation.reasonCode)}</p>
          <p className="mt-2 text-sm leading-6 text-zinc-400">
            {openReconciliation.resolutionNote
              ? humanizeSystemMessage(openReconciliation.resolutionNote)
              : 'Đối chiếu giao dịch trên cổng thanh toán và trạng thái đơn trước khi kết luận.'}
          </p>
          <p className="mt-3 text-xs text-zinc-500">
            Trạng thái: {statusLabel(openReconciliation.status)}
          </p>
        </section>
      )}

      <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
        <h2 className="flex items-center gap-2 text-sm font-black uppercase">
          <ReceiptText className="h-5 w-5 text-brand-orange" /> Diễn biến giao dịch
        </h2>
        <p className="mt-2 text-xs text-zinc-500">
          Các mốc chính được sắp theo thời gian để nhân viên biết giao dịch đã đi đến đâu.
        </p>
        <BusinessTimeline items={detail.logs || []} />
      </section>

      <details className="group rounded-3xl border border-zinc-800 bg-zinc-900">
        <summary className="flex cursor-pointer list-none items-center justify-between gap-3 p-6">
          <div>
            <h2 className="flex items-center gap-2 text-sm font-black uppercase">
              <Wrench className="h-5 w-5 text-zinc-400" /> Dữ liệu kỹ thuật
            </h2>
            <p className="mt-2 text-xs text-zinc-500">
              UUID, webhook và hàng đợi chỉ cần mở khi điều tra sự cố.
            </p>
          </div>
          <span className="text-xs font-bold text-zinc-500 group-open:hidden">Mở xem</span>
          <span className="hidden text-xs font-bold text-zinc-500 group-open:inline">Thu gọn</span>
        </summary>
        <div className="space-y-6 border-t border-zinc-800 p-6">
          <InfoCard
            compact
            icon={Wrench}
            title="Định danh kỹ thuật"
            rows={[
              ['Payment UUID', payment.paymentPublicId],
              ['Booking UUID', payment.bookingPublicId],
              ['Mã giao dịch nhà cung cấp', payment.externalTransactionId || 'Chưa ghi nhận'],
              ['Trạng thái nội bộ', payment.status],
              ['Trạng thái giao nhận', payment.bookingDeliveryStatus || 'Chưa ghi nhận'],
              ['Trạng thái đối soát', payment.reconciliationStatus || 'NONE'],
            ]}
          />
          <div className="grid gap-6 xl:grid-cols-3">
            <TechnicalTimeline title="Thông báo nhà cung cấp" items={detail.webhooks} />
            <TechnicalTimeline title="Hàng đợi hệ thống" items={detail.outboxEvents} />
            <TechnicalTimeline title="Lịch sử hồ sơ đối soát" items={reconciliationCases} />
          </div>
        </div>
      </details>
    </div>
  );
}

function ConclusionBanner({ conclusion, onPrimaryAction }) {
  const tone = {
    success: {
      box: 'border-emerald-500/30 bg-emerald-500/5',
      icon: CheckCircle2,
      iconClass: 'text-emerald-400',
    },
    warning: {
      box: 'border-amber-500/30 bg-amber-500/5',
      icon: AlertTriangle,
      iconClass: 'text-amber-400',
    },
    danger: {
      box: 'border-red-500/30 bg-red-500/5',
      icon: AlertTriangle,
      iconClass: 'text-red-400',
    },
    info: {
      box: 'border-sky-500/30 bg-sky-500/5',
      icon: CircleDashed,
      iconClass: 'text-sky-400',
    },
    neutral: {
      box: 'border-zinc-700 bg-zinc-900',
      icon: ShieldAlert,
      iconClass: 'text-zinc-400',
    },
  }[conclusion.tone];
  const Icon = tone.icon;

  return (
    <section className={`flex flex-col justify-between gap-4 rounded-3xl border p-6 lg:flex-row lg:items-center ${tone.box}`}>
      <div className="flex gap-4">
        <Icon className={`mt-0.5 h-6 w-6 shrink-0 ${tone.iconClass}`} />
        <div>
          <h2 className="text-lg font-black">{conclusion.title}</h2>
          <p className="mt-2 max-w-4xl text-sm leading-6 text-zinc-400">{conclusion.detail}</p>
        </div>
      </div>
      {conclusion.action && (
        <button
          type="button"
          onClick={onPrimaryAction}
          className="shrink-0 rounded-xl bg-brand-orange px-4 py-2.5 text-xs font-black uppercase"
        >
          {conclusion.action}
        </button>
      )}
    </section>
  );
}

function InfoCard({ icon: Icon, title, rows, compact = false }) {
  return (
    <section className={`rounded-3xl border border-zinc-800 bg-zinc-900 ${compact ? 'p-5' : 'p-6'}`}>
      <h2 className="flex items-center gap-2 border-b border-zinc-800 pb-4 text-sm font-black uppercase">
        <Icon className="h-5 w-5 text-brand-orange" /> {title}
      </h2>
      <dl className="mt-4 space-y-3">
        {rows.map(([label, value]) => (
          <div key={label} className="flex items-start justify-between gap-5 text-sm">
            <dt className="text-zinc-500">{label}</dt>
            <dd className="max-w-[65%] break-all text-right font-bold text-zinc-200">
              {value ?? '—'}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function BusinessTimeline({ items }) {
  if (items.length === 0) {
    return <p className="mt-5 text-sm text-zinc-600">Chưa có mốc cập nhật.</p>;
  }
  return (
    <ol className="mt-6 space-y-0">
      {items.map((item, index) => (
        <li key={`${item.eventType}-${item.createdAt}-${index}`} className="relative flex gap-4 pb-6 last:pb-0">
          {index < items.length - 1 && (
            <span className="absolute left-[7px] top-5 h-full w-px bg-zinc-800" />
          )}
          <span className="relative mt-1.5 h-4 w-4 shrink-0 rounded-full border-4 border-zinc-900 bg-brand-orange" />
          <div className="flex-1 rounded-2xl bg-zinc-950 p-4">
            <div className="flex flex-wrap justify-between gap-3">
              <strong className="text-sm">{eventLabel(item.eventType)}</strong>
              <span className="text-[10px] text-zinc-600">{formatTime(item.createdAt)}</span>
            </div>
            <p className="mt-2 text-xs leading-5 text-zinc-400">
              {humanizeSystemMessage(item.message)}
            </p>
            {item.currentStatus && (
              <p className="mt-2 text-[10px] font-bold text-zinc-500">
                Kết quả sau mốc này: {statusLabel(item.currentStatus)}
              </p>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}

function TechnicalTimeline({ title, items = [] }) {
  return (
    <section className="rounded-2xl bg-zinc-950 p-5">
      <h3 className="text-xs font-black uppercase text-zinc-300">{title}</h3>
      {items.length === 0 ? (
        <p className="mt-4 text-xs text-zinc-600">Chưa có bản ghi.</p>
      ) : (
        <div className="mt-4 space-y-3">
          {items.map((item, index) => (
            <article
              key={item.eventId || item.publicId || item.id || index}
              className="border-b border-zinc-800 pb-3 last:border-0 last:pb-0"
            >
              <strong className="block break-all text-xs">
                {item.eventType
                  ? eventLabel(item.eventType)
                  : item.reasonCode
                    ? reasonLabel(item.reasonCode)
                    : statusLabel(item.processingStatus || item.status)}
              </strong>
              <p className="mt-1 text-[10px] leading-4 text-zinc-500">
                {item.destination && destinationLabel(item.destination)}
                {item.lastError && humanizeSystemMessage(item.lastError)}
                {item.reasonCode && reasonLabel(item.reasonCode)}
                {!item.destination && !item.lastError && !item.reasonCode && 'Không có ghi chú lỗi.'}
              </p>
              <p className="mt-1 text-[10px] text-zinc-700">
                {formatTime(item.createdAt || item.receivedAt || item.openedAt)}
              </p>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : 'Chưa ghi nhận';
}
