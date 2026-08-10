import { useCallback, useEffect, useState } from 'react';
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
import { useAuth } from '@/contexts/AuthContext';
import {
  completeCashRefund,
  createAdminRefund,
  getAdminPayment,
  paymentErrorMessage,
  retryAdminRefund,
} from '../../services/paymentService';
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
  const { userRole } = useAuth();
  const { triggerAlert, triggerConfirm, triggerToast } = useOutletContext() || {};
  const isAdmin = (userRole || '').replace(/^ROLE_/, '') === 'ADMIN';
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [refundAction, setRefundAction] = useState(null);
  const [refundForm, setRefundForm] = useState({
    scope: 'FULL_ORDER',
    amount: '',
    reasonCode: 'CUSTOMER_SERVICE_APPROVED',
    note: '',
    providerReference: '',
  });
  const [refundRequestKey, setRefundRequestKey] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const result = await getAdminPayment(paymentPublicId);
      setDetail(result);
    } catch (error) {
      setDetail(null);
      setLoadError(paymentErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [paymentPublicId]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      load();
    }, 0);
    return () => {
      window.clearTimeout(timer);
    };
  }, [load]);

  if (loading) {
    return <div className="py-24 text-center text-zinc-500">Đang tải giao dịch...</div>;
  }
  if (loadError) {
    return (
      <div className="mx-auto max-w-2xl rounded-3xl border border-red-500/30 bg-red-500/5 p-8 text-center text-white">
        <AlertTriangle className="mx-auto h-10 w-10 text-red-400" />
        <h1 className="mt-4 text-xl font-black">Không thể tải chi tiết giao dịch</h1>
        <p className="mt-3 text-sm leading-6 text-zinc-400">{loadError}</p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <button
            type="button"
            onClick={load}
            className="rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase"
          >
            Thử lại
          </button>
          <button
            type="button"
            onClick={() => navigate('/admin/payments')}
            className="rounded-xl border border-zinc-700 px-5 py-2.5 text-xs font-black uppercase"
          >
            Quay lại danh sách
          </button>
        </div>
      </div>
    );
  }
  if (!detail) return null;

  const payment = detail.payment || {};
  const snapshot = detail.analyticsSnapshot || {};
  const cash = detail.cashDetail || {};
  const conclusion = paymentConclusion(payment);
  const reconciliationCases = detail.reconciliationCases || [];
  const refunds = detail.refunds || [];
  const openReconciliation = reconciliationCases.find(item =>
    !['RESOLVED', 'IGNORED'].includes(item.status));
  const canCreateRefund = isAdmin
    && payment.status === 'SUCCESS'
    && Number(payment.refundableAmount || 0) > 0;

  const openCreateRefund = () => {
    setRefundForm({
      scope: 'FULL_ORDER',
      amount: '',
      reasonCode: 'CUSTOMER_SERVICE_APPROVED',
      note: '',
      providerReference: '',
    });
    setRefundRequestKey(crypto.randomUUID());
    setRefundAction({ mode: 'create' });
  };

  const submitRefund = async event => {
    event.preventDefault();
    if (!isAdmin || !refundAction) return;
    setSubmitting(true);
    try {
      if (refundAction.mode === 'cash') {
        await completeCashRefund(refundAction.refund.refundPublicId, {
          providerReference: refundForm.providerReference.trim(),
          note: refundForm.note.trim(),
        });
        triggerToast?.('Đã ghi nhận tiền mặt được trả lại cho khách.');
      } else {
        const isFull = refundForm.scope === 'FULL_ORDER';
        await createAdminRefund(
          payment.paymentPublicId,
          {
            refundType: isFull ? 'FULL' : 'PARTIAL',
            refundComponent: refundForm.scope,
            amount: isFull ? null : Number(refundForm.amount),
            reasonCode: refundForm.reasonCode,
            note: refundForm.note.trim(),
          },
          refundRequestKey,
        );
        triggerToast?.('Đã tiếp nhận yêu cầu hoàn tiền.');
      }
      setRefundAction(null);
      await load();
    } catch (error) {
      triggerAlert?.(paymentErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  const retryRefund = async refund => {
    if (!isAdmin) return;
    const accepted = triggerConfirm
      ? await triggerConfirm(
          `Thử lại yêu cầu ${refund.refundCode}? Hệ thống sẽ không tạo thêm khoản hoàn trùng.`,
        )
      : true;
    if (!accepted) return;
    try {
      await retryAdminRefund(refund.refundPublicId);
      triggerToast?.('Đã đưa yêu cầu hoàn tiền vào hàng đợi xử lý lại.');
      await load();
    } catch (error) {
      triggerAlert?.(paymentErrorMessage(error));
    }
  };

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
        {canCreateRefund && (
          <button
            type="button"
            onClick={openCreateRefund}
            className="flex items-center gap-2 rounded-xl border border-red-500/40 px-4 py-2.5 text-xs font-black uppercase text-red-300 hover:bg-red-500/10"
          >
            <Banknote className="h-4 w-4" /> Tạo yêu cầu hoàn tiền
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

      <RefundSection
        refunds={refunds}
        payment={payment}
        isAdmin={isAdmin}
        onRetry={retryRefund}
        onCompleteCash={refund => {
          setRefundForm({
            scope: 'FULL_ORDER',
            amount: '',
            reasonCode: refund.reasonCode || 'CASH_REFUND',
            note: '',
            providerReference: '',
          });
          setRefundAction({ mode: 'cash', refund });
        }}
      />

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

      {refundAction && (
        <RefundModal
          mode={refundAction.mode}
          payment={payment}
          refund={refundAction.refund}
          form={refundForm}
          setForm={setRefundForm}
          submitting={submitting}
          onClose={() => setRefundAction(null)}
          onSubmit={submitRefund}
        />
      )}
    </div>
  );
}

const REFUND_STATUS_LABELS = {
  PENDING_APPROVAL: 'Chờ quản lý rạp duyệt',
  REQUESTED: 'Đã tiếp nhận',
  PROCESSING: 'Đang hoàn qua nhà cung cấp',
  SUCCESS: 'Đã hoàn cho khách',
  FAILED: 'Hoàn tiền thất bại',
  REQUIRES_ACTION: 'Cần nhân viên xử lý',
  REJECTED: 'Quản lý rạp đã từ chối',
  CANCELLED: 'Đã hủy yêu cầu',
};

const REFUND_COMPONENT_LABELS = {
  FULL_ORDER: 'Toàn bộ phần tiền còn lại',
  CONCESSION: 'Bắp nước',
  PRICE_DIFFERENCE: 'Chênh lệch giá',
  OPERATIONAL_ADJUSTMENT: 'Điều chỉnh nghiệp vụ',
};

function RefundSection({
  refunds,
  payment,
  isAdmin,
  onRetry,
  onCompleteCash,
}) {
  const refundedAmount = Number(payment.refundedAmount || 0);
  const refundableAmount = Number(payment.refundableAmount || 0);
  return (
    <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
      <div className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-4 md:flex-row md:items-center">
        <div>
          <h2 className="flex items-center gap-2 text-sm font-black uppercase">
            <Banknote className="h-5 w-5 text-brand-orange" /> Tiền đã trả lại khách
          </h2>
          <p className="mt-2 text-xs text-zinc-500">
            Không hỗ trợ hoàn riêng từng vé; hoàn một phần chỉ áp dụng cho bắp nước,
            chênh lệch giá hoặc điều chỉnh nghiệp vụ.
          </p>
        </div>
        <div className="flex gap-6 text-right text-xs">
          <div>
            <span className="block text-zinc-500">Đã hoàn thành</span>
            <strong className="mt-1 block text-emerald-400">
              {money(refundedAmount, payment.currency)}
            </strong>
          </div>
          <div>
            <span className="block text-zinc-500">Còn có thể hoàn</span>
            <strong className="mt-1 block text-brand-orange">
              {money(refundableAmount, payment.currency)}
            </strong>
          </div>
        </div>
      </div>
      {refunds.length === 0 ? (
        <p className="py-10 text-center text-sm text-zinc-600">
          Giao dịch này chưa phát sinh hoàn tiền.
        </p>
      ) : (
        <div className="mt-4 space-y-3">
          {refunds.map(refund => (
            <article
              key={refund.refundPublicId}
              className="flex flex-col justify-between gap-4 rounded-2xl bg-zinc-950 p-4 lg:flex-row lg:items-center"
            >
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <strong>{refund.refundCode}</strong>
                  <span className={`rounded-full border px-2 py-0.5 text-[10px] font-black ${
                    refund.status === 'SUCCESS'
                      ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400'
                      : ['FAILED', 'CANCELLED'].includes(refund.status)
                        ? 'border-red-500/30 bg-red-500/10 text-red-400'
                        : 'border-amber-500/30 bg-amber-500/10 text-amber-300'
                  }`}>
                    {REFUND_STATUS_LABELS[refund.status] || refund.status}
                  </span>
                  {refund.automatic && (
                    <span className="rounded-full bg-sky-500/10 px-2 py-1 text-[10px] font-bold text-sky-300">
                      Hệ thống tự động
                    </span>
                  )}
                </div>
                <p className="mt-2 text-sm font-bold text-zinc-300">
                  {REFUND_COMPONENT_LABELS[refund.refundComponent] || refund.refundComponent}
                  {' · '}
                  <span className="text-brand-orange">
                    {money(refund.amount, refund.currency)}
                  </span>
                </p>
                <p className="mt-1 text-xs text-zinc-500">
                  {reasonLabel(refund.reasonCode)} · {formatTime(refund.requestedAt)}
                </p>
                {refund.failureMessage && (
                  <p className="mt-2 text-xs text-red-300">
                    {humanizeSystemMessage(refund.failureMessage)}
                  </p>
                )}
              </div>
              {isAdmin && (
                <div className="flex shrink-0 gap-2">
                  {refund.provider === 'CASH' && refund.status === 'REQUIRES_ACTION' && (
                    <button
                      type="button"
                      onClick={() => onCompleteCash(refund)}
                      className="rounded-xl bg-brand-orange px-4 py-2 text-xs font-black uppercase"
                    >
                      Xác nhận đã trả tại quầy
                    </button>
                  )}
                  {refund.provider !== 'CASH'
                    && ['FAILED', 'REQUIRES_ACTION'].includes(refund.status) && (
                      <button
                        type="button"
                        onClick={() => onRetry(refund)}
                        className="rounded-xl border border-zinc-700 px-4 py-2 text-xs font-black uppercase hover:bg-zinc-800"
                      >
                        Thử lại
                      </button>
                  )}
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function RefundModal({
  mode,
  payment,
  refund,
  form,
  setForm,
  submitting,
  onClose,
  onSubmit,
}) {
  const isCash = mode === 'cash';
  const scopeHelp = {
    FULL_ORDER: 'Hoàn toàn bộ số tiền còn lại của giao dịch. Ghế/vé chỉ chuyển sang đã hoàn khi tổng tiền hoàn bằng toàn bộ giá trị đơn.',
    CONCESSION: 'Chỉ hoàn phần bắp nước và không thể vượt quá tiền bắp nước đã lưu trong đơn.',
    PRICE_DIFFERENCE: 'Dùng khi cần trả lại phần chênh lệch giá đã được nghiệp vụ xác minh.',
    OPERATIONAL_ADJUSTMENT: 'Dùng cho khoản điều chỉnh dịch vụ có căn cứ, không phải hoàn riêng từng vé.',
  }[form.scope];
  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-xl rounded-3xl border border-zinc-700 bg-zinc-900 p-6 shadow-2xl"
      >
        <h2 className="text-xl font-black">
          {isCash ? 'Xác nhận đã hoàn tiền mặt' : 'Tạo yêu cầu hoàn tiền'}
        </h2>
        <p className="mt-2 text-sm leading-6 text-zinc-400">
          {isCash
            ? `Ghi nhận biên nhận sau khi đã trả ${money(refund.amount, refund.currency)} cho khách tại quầy.`
            : `Số tiền còn có thể hoàn: ${money(payment.refundableAmount, payment.currency)}.`}
        </p>
        {isCash ? (
          <label className="mt-6 block text-xs font-black uppercase text-zinc-400">
            Mã biên nhận tại quầy
            <input
              required
              maxLength={150}
              value={form.providerReference}
              onChange={event => setForm(current => ({
                ...current,
                providerReference: event.target.value,
              }))}
              placeholder="Ví dụ: CASH-RFD-20260729-001"
              className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange"
            />
          </label>
        ) : (
          <div className="mt-6 space-y-4">
            <label className="block text-xs font-black uppercase text-zinc-400">
              Phạm vi hoàn tiền
              <select
                value={form.scope}
                onChange={event => setForm(current => ({
                  ...current,
                  scope: event.target.value,
                  amount: '',
                  reasonCode: event.target.value === 'CONCESSION'
                    ? 'CONCESSION_ISSUE'
                    : event.target.value === 'PRICE_DIFFERENCE'
                      ? 'PRICE_CORRECTION'
                      : event.target.value === 'OPERATIONAL_ADJUSTMENT'
                        ? 'OPERATIONAL_ADJUSTMENT'
                        : 'CUSTOMER_SERVICE_APPROVED',
                }))}
                className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white"
              >
                <option value="FULL_ORDER">Hoàn toàn bộ số tiền còn lại</option>
                <option value="CONCESSION">Hoàn tiền bắp nước</option>
                <option value="PRICE_DIFFERENCE">Hoàn chênh lệch giá</option>
                <option value="OPERATIONAL_ADJUSTMENT">Điều chỉnh nghiệp vụ</option>
              </select>
            </label>
            <p className="rounded-xl bg-zinc-950 p-3 text-xs leading-5 text-zinc-400">
              {scopeHelp}
            </p>
            {form.scope !== 'FULL_ORDER' && (
              <label className="block text-xs font-black uppercase text-zinc-400">
                Số tiền hoàn
                <input
                  required
                  type="number"
                  min="1"
                  max={Number(payment.refundableAmount || 0)}
                  step="1"
                  value={form.amount}
                  onChange={event => setForm(current => ({
                    ...current,
                    amount: event.target.value,
                  }))}
                  className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange"
                />
              </label>
            )}
          </div>
        )}
        <label className="mt-4 block text-xs font-black uppercase text-zinc-400">
          Ghi chú và căn cứ
          <textarea
            required
            maxLength={isCash ? 1000 : 2000}
            rows={4}
            value={form.note}
            onChange={event => setForm(current => ({
              ...current,
              note: event.target.value,
            }))}
            placeholder={isCash
              ? 'Ghi rõ hình thức trả tiền, người nhận và chứng từ liên quan...'
              : 'Ghi rõ lý do, căn cứ phê duyệt và thông tin đã kiểm tra...'}
            className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange"
          />
        </label>
        {!isCash && (
          <p className="mt-4 text-xs leading-5 text-amber-300">
            Không chọn từng vé tại đây. Việc hoàn riêng từng vé chỉ được mở sau khi có nghiệp vụ
            hủy vé, tính lại giá và quyết định mở bán lại ghế.
          </p>
        )}
        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-zinc-700 px-5 py-2.5 text-xs font-black uppercase"
          >
            Quay lại
          </button>
          <button
            disabled={submitting}
            className="rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase disabled:opacity-50"
          >
            {submitting ? 'Đang xử lý...' : isCash ? 'Xác nhận đã trả' : 'Tạo yêu cầu'}
          </button>
        </div>
      </form>
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
