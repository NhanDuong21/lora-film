import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  CalendarClock,
  Check,
  CheckCircle2,
  ExternalLink,
  Film,
  History,
  ReceiptText,
  RotateCcw,
  ShieldCheck,
  WalletCards,
  Wrench,
  X,
  XCircle,
} from 'lucide-react';
import { EmptyWorkspace } from '../../admin/components/HrWorkspace';
import { ActionModal } from '../../admin/components/OperationsConsole';
import managerOperationsService from '../services/managerOperationsService';

const PAYMENT_STATUS = {
  CREATED: 'Đã tạo giao dịch',
  PENDING: 'Chờ thanh toán',
  PROCESSING: 'Đang xử lý',
  SUCCESS: 'Thanh toán thành công',
  FAILED: 'Thanh toán thất bại',
  EXPIRED: 'Đã hết hạn',
  CANCELLED: 'Đã hủy',
  REFUNDED: 'Đã hoàn tiền',
  PARTIALLY_REFUNDED: 'Hoàn tiền một phần',
};

const REFUND_STATUS = {
  PENDING_APPROVAL: 'Chờ quản lý rạp duyệt',
  REQUESTED: 'Đã chuyển hệ thống xử lý',
  PROCESSING: 'Đang hoàn tiền',
  REQUIRES_ACTION: 'Cần trả tiền tại quầy',
  SUCCESS: 'Đã hoàn tiền',
  FAILED: 'Hoàn tiền lỗi',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
};

const PROVIDERS = {
  CASH: 'Tiền mặt tại quầy',
  MOMO: 'MoMo',
  VNPAY: 'VNPay',
  STRIPE: 'Thẻ quốc tế',
  MOCK: 'Mô phỏng nội bộ',
};

const PAYMENT_METHOD = { ONLINE: 'Thanh toán trực tuyến', CASH: 'Tiền mặt tại quầy' };
const DELIVERY_STATUS = {
  PENDING: 'Chưa gửi kết quả sang hệ thống đặt vé',
  PROCESSING: 'Đang gửi kết quả sang hệ thống đặt vé',
  DELIVERED: 'Hệ thống đặt vé đã nhận kết quả',
  PUBLISHED: 'Đã gửi kết quả thành công',
  FAILED: 'Gửi kết quả chưa thành công',
  DEAD_LETTER: 'Cần bộ phận kỹ thuật kiểm tra',
  NOT_REQUIRED: 'Không cần gửi kết quả',
};

const RECONCILIATION_STATUS = {
  NONE: 'Không cần đối soát',
  NOT_REQUIRED: 'Không cần đối soát',
  REQUIRED: 'Cần kế toán kiểm tra',
  IN_REVIEW: 'Kế toán đang kiểm tra',
  RESOLVED: 'Đã xử lý đối soát',
};

const COMPONENTS = {
  FULL_ORDER: 'Toàn bộ đơn',
  CONCESSION: 'Đồ ăn và thức uống',
  PRICE_DIFFERENCE: 'Chênh lệch giá',
  OPERATIONAL_ADJUSTMENT: 'Điều chỉnh vận hành',
};

const REASONS = {
  CUSTOMER_REQUEST: 'Khách hàng yêu cầu và đủ điều kiện',
  SHOWTIME_CANCELLED: 'Suất chiếu bị hủy',
  SERVICE_INCIDENT: 'Sự cố dịch vụ tại rạp',
  DUPLICATE_CHARGE: 'Nghi ngờ thu tiền trùng',
  WRONG_ITEM: 'Sai hoặc thiếu sản phẩm',
  PRICE_ADJUSTMENT: 'Điều chỉnh chênh lệch giá',
};

const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: currency || 'VND', maximumFractionDigits: 0,
}).format(Number(value || 0));

const dateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa ghi nhận';

const statusTone = status => {
  if (['SUCCESS', 'DELIVERED', 'PUBLISHED', 'RESOLVED'].includes(status)) return 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300';
  if (['FAILED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'DEAD_LETTER'].includes(status)) return 'border-red-500/25 bg-red-500/10 text-red-300';
  if (['PENDING', 'PENDING_APPROVAL', 'REQUIRES_ACTION', 'REQUIRED', 'IN_REVIEW'].includes(status)) return 'border-amber-500/25 bg-amber-500/10 text-amber-300';
  return 'border-sky-500/20 bg-sky-500/10 text-sky-300';
};

function StatusPill({ status, labels = PAYMENT_STATUS }) {
  return <span className={`inline-flex rounded-full border px-3 py-1 text-[10px] font-black ${statusTone(status)}`}>{labels[status] || 'Chưa xác định'}</span>;
}

function InfoCard({ icon: Icon, title, rows, children, className = '' }) {
  return (
    <section className={`rounded-3xl border border-zinc-800 bg-zinc-900/80 p-5 md:p-6 ${className}`}>
      <h2 className="flex items-center gap-2 border-b border-zinc-800 pb-4 text-sm font-black uppercase tracking-wide"><Icon size={18} className="text-brand-orange" /> {title}</h2>
      {rows ? <dl className="mt-5 space-y-3">{rows.map(([label, value]) => <div key={label} className="flex items-start justify-between gap-5 text-sm"><dt className="text-zinc-500">{label}</dt><dd className="max-w-[65%] break-words text-right font-bold text-zinc-200">{value}</dd></div>)}</dl> : <div className="mt-5">{children}</div>}
    </section>
  );
}

const paymentConclusion = payment => {
  if (['REQUIRED', 'IN_REVIEW'].includes(payment.reconciliationStatus)) return {
    tone: 'warning',
    title: 'Giao dịch cần quản trị viên hoặc kế toán kiểm tra',
    detail: 'Quản lý rạp chỉ theo dõi và cung cấp thông tin vận hành. Không tự sửa trạng thái hoặc kết luận chênh lệch tiền.',
  };
  if (payment.status === 'SUCCESS' && ['DELIVERED', 'PUBLISHED'].includes(payment.bookingDeliveryStatus)) return {
    tone: 'success',
    title: 'Đã thu tiền và đơn đặt vé đã nhận kết quả',
    detail: 'Giao dịch đã hoàn tất bình thường. Vé, ghế và phục vụ khách được theo dõi trong hồ sơ đơn đặt vé liên quan.',
  };
  if (payment.status === 'SUCCESS') return {
    tone: 'warning',
    title: 'Đã thu tiền, đang cập nhật đơn đặt vé',
    detail: 'Không thu lại tiền. Nếu trạng thái kéo dài, chuyển quản trị viên hoặc kế toán kiểm tra thay vì tự sửa giao dịch.',
  };
  if (['FAILED', 'CANCELLED', 'EXPIRED'].includes(payment.status)) return {
    tone: 'neutral',
    title: 'Giao dịch không phát sinh thu tiền thành công',
    detail: 'Kiểm tra hồ sơ đơn đặt vé để xác nhận ghế đã được trả. Không cần tạo yêu cầu hoàn tiền nếu khách chưa bị trừ tiền.',
  };
  return {
    tone: 'info',
    title: 'Giao dịch đang chờ kết quả',
    detail: 'Không tạo giao dịch thay thế hoặc thu lại tiền khi hệ thống vẫn đang xử lý kết quả hiện tại.',
  };
};

const conclusionClass = tone => ({
  success: 'border-emerald-500/30 bg-emerald-500/[0.06]',
  warning: 'border-amber-500/30 bg-amber-500/[0.06]',
  info: 'border-sky-500/30 bg-sky-500/[0.06]',
  neutral: 'border-zinc-800 bg-zinc-900',
}[tone] || 'border-zinc-800 bg-zinc-900');

export default function ManagerPaymentDetailPage() {
  const { paymentPublicId } = useParams();
  const navigate = useNavigate();
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [detail, setDetail] = useState(null);
  const [state, setState] = useState({ loading: true, error: '', success: '' });
  const [decision, setDecision] = useState(null);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(value => ({ ...value, loading: true, error: '' }));
    try {
      const result = await managerOperationsService.getPaymentDetail(selectedCinemaId, paymentPublicId);
      setDetail(result);
      setState(value => ({ ...value, loading: false, error: '' }));
    } catch (error) {
      setDetail(null);
      setState({ loading: false, success: '', error: error?.response?.data?.message || 'Không thể tải hồ sơ giao dịch này.' });
    }
  }, [paymentPublicId, selectedCinemaId]);

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const openDecision = (refund, type) => {
    setDecision({ refund, type });
    setNote('');
    setState(value => ({ ...value, error: '' }));
  };

  const submitDecision = async event => {
    event.preventDefault();
    if (note.trim().length < 5) {
      setState(value => ({ ...value, error: 'Vui lòng ghi rõ căn cứ xử lý, ít nhất 5 ký tự.' }));
      return;
    }
    setSubmitting(true);
    try {
      if (decision.type === 'approve') await managerOperationsService.approveRefund(selectedCinemaId, decision.refund.refundPublicId, note.trim());
      else await managerOperationsService.rejectRefund(selectedCinemaId, decision.refund.refundPublicId, note.trim());
      const success = decision.type === 'approve'
        ? 'Đã duyệt yêu cầu hoàn tiền. Hệ thống hoặc nhân viên quầy sẽ tiếp tục xử lý.'
        : 'Đã từ chối yêu cầu và lưu lý do để nhân viên tra cứu.';
      setDecision(null);
      setNote('');
      setState({ loading: false, error: '', success });
      await load();
    } catch (error) {
      setState(value => ({ ...value, error: error?.response?.data?.message || 'Không thể xử lý yêu cầu hoàn tiền.' }));
    } finally {
      setSubmitting(false);
    }
  };

  const timeline = useMemo(() => {
    if (!detail?.payment) return [];
    const payment = detail.payment;
    const items = [
      { at: payment.createdAt, title: 'Khởi tạo giao dịch', detail: `Lần thanh toán thứ ${payment.attemptNumber || 1} qua ${PROVIDERS[payment.provider] || 'phương thức đã chọn'}` },
      { at: payment.updatedAt, title: PAYMENT_STATUS[payment.status] || 'Cập nhật giao dịch', detail: DELIVERY_STATUS[payment.bookingDeliveryStatus] || 'Chưa có thông tin giao nhận kết quả' },
      ...(detail.refundRequests || []).map(refund => ({ at: refund.requestedAt, title: `Yêu cầu hoàn tiền ${refund.refundCode}`, detail: `${REFUND_STATUS[refund.status] || 'Đang xử lý'} · ${money(refund.amount, refund.currency)}` })),
    ];
    return items.filter(item => item.at).sort((a, b) => new Date(a.at) - new Date(b.at));
  }, [detail]);

  if (cinemaState.loading || (state.loading && !detail)) return <div className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải toàn bộ hồ sơ giao dịch…</div>;
  if (!selectedCinema) return <EmptyWorkspace title="Chưa được phân công rạp" description="Quản trị viên cần phân công rạp trước khi bạn có thể xem giao dịch." />;
  if (state.error && !detail) return <EmptyWorkspace title="Không thể mở hồ sơ giao dịch" description={state.error} action={<button type="button" onClick={() => navigate('/manager/payments')} className="rounded-xl bg-white px-4 py-2 text-sm font-black text-black">Quay lại danh sách</button>} />;
  if (!detail) return null;

  const payment = detail.payment || {};
  const snapshot = detail.bookingSnapshot || {};
  const refunds = detail.refundRequests || [];
  const conclusion = paymentConclusion(payment);
  const needsFinance = ['REQUIRED', 'IN_REVIEW'].includes(payment.reconciliationStatus);

  return (
    <main className="mx-auto w-full max-w-[1500px] space-y-6 pb-10 text-white">
      <button type="button" onClick={() => navigate('/manager/payments')} className="inline-flex items-center gap-2 text-sm font-bold text-zinc-400 hover:text-white"><ArrowLeft size={17} /> Quay lại danh sách giao dịch</button>

      <header className="flex flex-col justify-between gap-6 rounded-3xl border border-zinc-800 bg-zinc-900 p-6 md:p-8 lg:flex-row lg:items-center">
        <div><p className="text-xs font-black uppercase tracking-[0.18em] text-zinc-500">Hồ sơ giao dịch tại rạp</p><div className="mt-3 flex flex-wrap items-center gap-3"><h1 className="text-2xl font-black tracking-wide">{payment.paymentTransactionCode || 'Chưa có mã giao dịch'}</h1><StatusPill status={payment.status} /></div><p className="mt-3 text-sm text-zinc-500">Lần thanh toán thứ {payment.attemptNumber || 1} · {PROVIDERS[payment.provider] || payment.provider || 'Chưa rõ phương thức'}</p></div>
        <div className="lg:text-right"><p className="text-xs font-black uppercase text-zinc-500">Số tiền giao dịch</p><p className="mt-2 text-3xl font-black text-brand-orange">{money(payment.amount, payment.currency)}</p><p className="mt-2 text-sm font-bold text-zinc-300">{selectedCinema.name}</p></div>
      </header>

      {state.success ? <div role="status" className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] p-4 text-sm font-bold text-emerald-200">{state.success}</div> : null}
      {state.error ? <div role="alert" className="rounded-2xl border border-red-500/25 bg-red-500/[0.06] p-4 text-sm font-bold text-red-200">{state.error}</div> : null}

      <section className={`rounded-3xl border p-6 ${conclusionClass(conclusion.tone)}`}>
        <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center"><div><p className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-zinc-500"><CheckCircle2 size={17} className="text-brand-orange" /> Kết luận vận hành</p><h2 className="mt-3 text-xl font-black">{conclusion.title}</h2><p className="mt-2 max-w-4xl text-sm leading-6 text-zinc-400">{conclusion.detail}</p></div><button type="button" onClick={() => navigate(`/manager/bookings/${payment.bookingPublicId}`)} className="inline-flex shrink-0 items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-xs font-black text-black"><ExternalLink size={16} /> Mở đơn đặt vé liên quan</button></div>
      </section>

      {needsFinance ? <section className="rounded-3xl border border-amber-500/30 bg-amber-500/[0.06] p-6"><h2 className="flex items-center gap-2 font-black text-amber-200"><AlertTriangle size={19} /> Cần chuyển bộ phận đối soát</h2><p className="mt-2 text-sm leading-6 text-zinc-400">Giao dịch đang ở trạng thái “{RECONCILIATION_STATUS[payment.reconciliationStatus]}”. Quản lý rạp không tự sửa số tiền, trạng thái hoặc mã từ cổng thanh toán; hãy cung cấp thông tin vận hành cho quản trị viên/kế toán.</p></section> : null}

      <div className="grid gap-6 xl:grid-cols-3">
        <InfoCard icon={Film} title="Đơn đặt vé liên quan" rows={[["Phim", snapshot.movieTitle || payment.movieTitle || 'Chưa có dữ liệu'], ['Số vé', `${snapshot.ticketCount ?? payment.ticketCount ?? 0} vé`], ['Tình trạng cập nhật đơn', DELIVERY_STATUS[payment.bookingDeliveryStatus] || 'Chưa ghi nhận'], ['Mã đơn đặt vé', payment.bookingPublicId || 'Chưa ghi nhận']]} />
        <InfoCard icon={ReceiptText} title="Cơ cấu số tiền" rows={[["Tiền vé", money(snapshot.ticketAmount ?? payment.ticketAmount, payment.currency)], ['Bắp nước', money(snapshot.foodAmount ?? payment.foodAmount, payment.currency)], ['Giảm giá', `-${money(snapshot.discountAmount ?? payment.discountAmount, payment.currency)}`], ['Đã hoàn', money(payment.refundedAmount, payment.currency)], ['Còn có thể hoàn', money(payment.refundableAmount, payment.currency)], ['Tổng thanh toán', money(payment.amount, payment.currency)]]} />
        <InfoCard icon={CalendarClock} title="Thời gian và phương thức" rows={[["Tạo giao dịch", dateTime(payment.createdAt)], ['Cập nhật gần nhất', dateTime(payment.updatedAt)], ['Hạn thanh toán', dateTime(payment.expiresAt)], ['Phương thức', PAYMENT_METHOD[payment.paymentMethod] || payment.paymentMethod || 'Chưa ghi nhận'], ['Nhà cung cấp', PROVIDERS[payment.provider] || payment.provider || 'Chưa ghi nhận'], ['Đối soát', RECONCILIATION_STATUS[payment.reconciliationStatus] || 'Không cần đối soát']]} />
      </div>

      <InfoCard icon={RotateCcw} title={`Yêu cầu hoàn tiền (${refunds.length})`}>
        {refunds.length ? <div className="space-y-3">{refunds.map(refund => <article key={refund.refundPublicId} className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4 md:p-5"><div className="flex flex-col justify-between gap-4 md:flex-row md:items-start"><div><div className="flex flex-wrap items-center gap-2"><p className="font-black text-zinc-100">{refund.refundCode}</p><StatusPill status={refund.status} labels={REFUND_STATUS} /></div><p className="mt-2 text-sm font-bold text-zinc-300">{COMPONENTS[refund.refundComponent] || 'Phạm vi hoàn tiền'} · {money(refund.amount, refund.currency)}</p><p className="mt-2 text-sm leading-6 text-zinc-500">{REASONS[refund.reasonCode] || 'Lý do nghiệp vụ'}: {refund.reasonDetail || 'Không có mô tả bổ sung'}</p>{refund.reviewNote ? <p className="mt-2 text-xs leading-5 text-sky-300/70">Ghi chú duyệt: {refund.reviewNote}</p> : null}</div>{refund.status === 'PENDING_APPROVAL' ? <div className="flex shrink-0 gap-2"><button type="button" onClick={() => openDecision(refund, 'approve')} className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-500/10 px-4 py-2.5 text-xs font-black text-emerald-300"><Check size={15} /> Duyệt</button><button type="button" onClick={() => openDecision(refund, 'reject')} className="inline-flex items-center gap-1.5 rounded-xl bg-red-500/10 px-4 py-2.5 text-xs font-black text-red-300"><X size={15} /> Từ chối</button></div> : null}</div><p className="mt-3 border-t border-zinc-800 pt-3 text-xs text-zinc-600">Tạo lúc {dateTime(refund.requestedAt)}{refund.reviewedAt ? ` · Duyệt lúc ${dateTime(refund.reviewedAt)}` : ''}</p></article>)}</div> : <div className="py-10 text-center"><RotateCcw className="mx-auto text-zinc-700" /><p className="mt-3 text-sm font-bold text-zinc-400">Giao dịch chưa có yêu cầu hoàn tiền</p></div>}
      </InfoCard>

      <InfoCard icon={History} title="Diễn biến giao dịch">
        {timeline.length ? <ol>{timeline.map((item, index) => <li key={`${item.at}-${item.title}`} className="relative grid grid-cols-[24px_1fr] gap-3 pb-6 last:pb-0"><div className="relative flex justify-center"><span className="mt-1.5 h-2.5 w-2.5 rounded-full bg-brand-orange" />{index < timeline.length - 1 ? <span className="absolute bottom-0 top-4 w-px bg-zinc-800" /> : null}</div><div><div className="flex flex-wrap items-center gap-2"><p className="font-black text-zinc-200">{item.title}</p><span className="text-xs text-zinc-600">{dateTime(item.at)}</span></div><p className="mt-1 text-sm text-zinc-500">{item.detail}</p></div></li>)}</ol> : <p className="py-8 text-center text-sm text-zinc-500">Chưa có mốc giao dịch.</p>}
      </InfoCard>

      <details className="group rounded-3xl border border-zinc-800 bg-zinc-900/80"><summary className="flex cursor-pointer list-none items-center justify-between gap-4 p-6"><div><h2 className="flex items-center gap-2 text-sm font-black uppercase tracking-wide"><Wrench size={18} className="text-zinc-500" /> Định danh kỹ thuật</h2><p className="mt-2 text-xs text-zinc-500">Chỉ mở khi cần cung cấp mã cho quản trị viên hoặc kế toán.</p></div><span className="text-xs font-black text-zinc-500 group-open:hidden">Mở xem</span><span className="hidden text-xs font-black text-zinc-500 group-open:inline">Thu gọn</span></summary><div className="grid gap-4 border-t border-zinc-800 p-6 md:grid-cols-2"><InfoCard icon={WalletCards} title="Mã hệ thống" rows={[["Payment UUID", payment.paymentPublicId], ['Booking UUID', payment.bookingPublicId], ['Mã từ cổng thanh toán', payment.externalTransactionId || 'Chưa ghi nhận']]} /><InfoCard icon={ShieldCheck} title="Trạng thái hệ thống" rows={[["Trạng thái giao dịch", PAYMENT_STATUS[payment.status] || 'Chưa xác định'], ['Giao nhận kết quả', DELIVERY_STATUS[payment.bookingDeliveryStatus] || 'Chưa ghi nhận'], ['Đối soát', RECONCILIATION_STATUS[payment.reconciliationStatus] || 'Không cần đối soát']]} /></div></details>

      <div className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.05] p-5 text-sm leading-6 text-sky-100/70"><p className="flex items-center gap-2 font-black text-sky-300"><ShieldCheck size={17} /> Ranh giới trách nhiệm</p><p className="mt-2">Quản lý rạp xác minh tình huống và duyệt yêu cầu hoàn tiền của đúng rạp. Lỗi cổng thanh toán, chênh lệch tiền và thay đổi trạng thái kỹ thuật thuộc quản trị viên hoặc kế toán.</p></div>

      <ActionModal open={Boolean(decision)} onClose={() => { setDecision(null); setState(value => ({ ...value, error: '' })); }} title={decision?.type === 'approve' ? 'Duyệt yêu cầu hoàn tiền' : 'Từ chối yêu cầu hoàn tiền'} description={decision ? `${decision.refund.refundCode} · ${money(decision.refund.amount, decision.refund.currency)}` : ''} onSubmit={submitDecision} submitLabel={decision?.type === 'approve' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'} submitting={submitting} tone={decision?.type === 'reject' ? 'danger' : 'orange'}>
        {decision ? <div className="rounded-xl border border-white/10 bg-white/[0.025] p-4"><p className="text-xs font-black uppercase tracking-wider text-zinc-600">Nhân viên đề nghị</p><p className="mt-2 text-sm font-black text-zinc-200">{COMPONENTS[decision.refund.refundComponent] || 'Hoàn tiền'} · {money(decision.refund.amount, decision.refund.currency)}</p><p className="mt-2 text-sm leading-6 text-zinc-400">{decision.refund.reasonDetail}</p></div> : null}
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Ghi chú quyết định *<textarea required minLength={5} maxLength={1000} value={note} onChange={event => setNote(event.target.value)} placeholder={decision?.type === 'approve' ? 'Nêu căn cứ đã kiểm tra trước khi duyệt…' : 'Nêu rõ lý do từ chối để nhân viên biết cách xử lý…'} className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-orange-500" /></label>
        {decision?.type === 'approve' ? <div className="flex items-start gap-3 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-xs leading-5 text-emerald-100/75"><CheckCircle2 size={17} className="mt-0.5 shrink-0 text-emerald-300" /> Hoàn tiền online được hệ thống xử lý; tiền mặt được chuyển cho nhân viên quầy trả khách.</div> : <div className="flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-xs leading-5 text-red-100/75"><XCircle size={17} className="mt-0.5 shrink-0 text-red-300" /> Yêu cầu sẽ kết thúc và không phát sinh hoàn tiền.</div>}
      </ActionModal>
    </main>
  );
}
