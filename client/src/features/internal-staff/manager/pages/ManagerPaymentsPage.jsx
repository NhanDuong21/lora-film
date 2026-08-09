import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  AlertTriangle,
  Check,
  CheckCircle2,
  CircleDollarSign,
  Clock3,
  Eye,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-react';
import { EmptyWorkspace, HrHero, WorkflowSteps } from '../../admin/components/HrWorkspace';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
  MetricStrip,
} from '../../admin/components/OperationsConsole';
import managerOperationsService from '../services/managerOperationsService';

const PAYMENT_STATUS = {
  CREATED: 'Đã tạo',
  PENDING: 'Chờ thanh toán',
  PROCESSING: 'Đang xử lý',
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  EXPIRED: 'Hết hạn',
  CANCELLED: 'Đã hủy',
  REFUNDED: 'Đã hoàn tiền',
  PARTIALLY_REFUNDED: 'Hoàn tiền một phần',
};

const REFUND_STATUS = {
  PENDING_APPROVAL: 'Chờ quản lý rạp duyệt',
  REQUESTED: 'Đã chuyển hệ thống xử lý',
  PROCESSING: 'Đang hoàn tiền',
  REQUIRES_ACTION: 'Cần trả tiền tại quầy',
  SUCCEEDED: 'Đã hoàn tiền',
  FAILED: 'Hoàn tiền lỗi',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
};

const PROVIDERS = { CASH: 'Tiền mặt', MOMO: 'MoMo', VNPAY: 'VNPay', STRIPE: 'Thẻ quốc tế', MOCK: 'Mô phỏng' };
const PAYMENT_FILTER_STATUSES = ['PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED'];
const PROVIDER_FILTERS = ['CASH', 'MOMO', 'VNPAY', 'MOCK'];
const COMPONENTS = {
  FULL_ORDER: 'Toàn bộ đơn',
  CONCESSION: 'Đồ ăn & thức uống',
  PRICE_DIFFERENCE: 'Chênh lệch giá',
  OPERATIONAL_ADJUSTMENT: 'Điều chỉnh vận hành',
};

const formatMoney = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: currency || 'VND', maximumFractionDigits: 0,
}).format(Number(value || 0));

const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa có';

const pillTone = status => {
  if (['SUCCESS', 'SUCCEEDED'].includes(status)) return 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300';
  if (['FAILED', 'REJECTED', 'CANCELLED', 'EXPIRED'].includes(status)) return 'border-red-500/20 bg-red-500/10 text-red-300';
  if (['PENDING_APPROVAL', 'REQUIRES_ACTION'].includes(status)) return 'border-amber-500/20 bg-amber-500/10 text-amber-300';
  return 'border-sky-500/20 bg-sky-500/10 text-sky-300';
};

const StatusPill = ({ status, refund = false }) => (
  <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black ${pillTone(status)}`}>
    {(refund ? REFUND_STATUS : PAYMENT_STATUS)[status] || status || 'Chưa xác định'}
  </span>
);

const emptyPage = { content: [], number: 0, totalPages: 0, totalElements: 0 };

export default function ManagerPaymentsPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [tab, setTab] = useState('transactions');
  const [filters, setFilters] = useState({ query: '', status: '', provider: '', page: 0, size: 20 });
  const [refundFilters, setRefundFilters] = useState({ status: 'PENDING_APPROVAL', page: 0, size: 20 });
  const [draftQuery, setDraftQuery] = useState('');
  const [payments, setPayments] = useState(emptyPage);
  const [refunds, setRefunds] = useState(emptyPage);
  const [summary, setSummary] = useState({});
  const [state, setState] = useState({ loading: false, error: '' });
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [decision, setDecision] = useState(null);
  const [note, setNote] = useState('');
  const [actionState, setActionState] = useState({ loading: false, error: '', success: '' });

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState({ loading: true, error: '' });
    try {
      const paymentParams = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''));
      const refundParams = Object.fromEntries(Object.entries(refundFilters).filter(([, value]) => value !== ''));
      const [paymentData, refundData, counts] = await Promise.all([
        managerOperationsService.getPayments(selectedCinemaId, paymentParams),
        managerOperationsService.getRefundRequests(selectedCinemaId, refundParams),
        managerOperationsService.getPaymentSummary(selectedCinemaId),
      ]);
      setPayments(paymentData || emptyPage);
      setRefunds(refundData || emptyPage);
      setSummary(counts || {});
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.response?.data?.message || 'Không thể tải giao dịch của rạp lúc này.' });
    }
  }, [filters, refundFilters, selectedCinemaId]);

  useEffect(() => {
    // Fetch whenever the selected cinema or operator filters change.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const paymentRows = useMemo(() => payments?.content || payments?.data || [], [payments]);
  const refundRows = useMemo(() => refunds?.content || refunds?.data || [], [refunds]);

  const submitSearch = event => {
    event.preventDefault();
    setFilters(value => ({ ...value, query: draftQuery.trim(), page: 0 }));
  };

  const openDetail = async payment => {
    setDetail({ payment, bookingSnapshot: null, refundRequests: [] });
    setDetailLoading(true);
    try {
      setDetail(await managerOperationsService.getPaymentDetail(selectedCinemaId, payment.paymentPublicId));
    } catch (error) {
      setActionState({ loading: false, success: '', error: error?.response?.data?.message || 'Không thể tải chi tiết giao dịch.' });
    } finally {
      setDetailLoading(false);
    }
  };

  const openDecision = (refund, type) => {
    setDecision({ refund, type });
    setNote('');
    setActionState(value => ({ ...value, error: '' }));
  };

  const submitDecision = async event => {
    event.preventDefault();
    if (note.trim().length < 5) {
      setActionState({ loading: false, success: '', error: 'Vui lòng ghi rõ lý do hoặc căn cứ xử lý, ít nhất 5 ký tự.' });
      return;
    }
    setActionState({ loading: true, error: '', success: '' });
    try {
      if (decision.type === 'approve') await managerOperationsService.approveRefund(selectedCinemaId, decision.refund.refundPublicId, note.trim());
      else await managerOperationsService.rejectRefund(selectedCinemaId, decision.refund.refundPublicId, note.trim());
      const success = decision.type === 'approve'
        ? 'Đã duyệt yêu cầu. Hệ thống thanh toán sẽ tiếp tục xử lý theo phương thức gốc.'
        : 'Đã từ chối yêu cầu và lưu lý do để nhân viên tra cứu.';
      setDecision(null);
      setNote('');
      setActionState({ loading: false, error: '', success });
      await load();
    } catch (error) {
      setActionState({ loading: false, success: '', error: error?.response?.data?.message || 'Không thể xử lý yêu cầu hoàn tiền.' });
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp được phân công…</p>;
  if (!selectedCinema) return <EmptyWorkspace title="Chưa được phân công rạp" description="Quản trị viên cần phân công rạp trước khi bạn có thể xem giao dịch." />;

  return (
    <main className="space-y-5 pb-8 text-white">
      <HrHero
        context={`Kiểm soát giao dịch · ${selectedCinema.name}`}
        title="Giao dịch tại rạp"
        description="Theo dõi giao dịch phát sinh tại rạp và duyệt yêu cầu hoàn tiền do nhân viên tạo. Đối soát tài chính, lỗi cổng thanh toán và xử lý ngoại lệ thuộc Quản trị viên hoặc kế toán."
        actions={<button type="button" onClick={load} disabled={state.loading} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-black text-black disabled:opacity-40"><RefreshCw size={17} className={state.loading ? 'animate-spin' : ''} /> Làm mới dữ liệu</button>}
      />

      <WorkflowSteps steps={[
        { label: 'Nhân viên tiếp nhận', hint: 'Tạo yêu cầu và ghi rõ lý do', state: 'done' },
        { label: 'Quản lý rạp kiểm tra', hint: 'Duyệt trong đúng phạm vi rạp', state: 'active' },
        { label: 'Hệ thống hoàn tiền', hint: 'Tự động hoặc trả tại quầy', state: 'waiting' },
        { label: 'Quản trị viên/kế toán đối soát', hint: 'Chỉ xử lý lỗi tài chính', state: 'waiting' },
      ]} />

      <MetricStrip items={[
        { icon: CircleDollarSign, label: 'Tổng giao dịch', value: summary.totalTransactions || 0, hint: 'Tất cả giao dịch tại rạp', tone: 'blue' },
        { icon: CheckCircle2, label: 'Thành công', value: summary.successful || 0, hint: 'Đã ghi nhận thanh toán', tone: 'green' },
        { icon: Clock3, label: 'Đang xử lý', value: summary.processing || 0, hint: 'Chờ kết quả thanh toán', tone: 'amber' },
        { icon: AlertTriangle, label: 'Cần kế toán kiểm tra', value: summary.needsFinanceReview || 0, hint: 'Quản lý rạp chỉ theo dõi, không tự đối soát', tone: 'red' },
      ]} />

      <div className="flex flex-col gap-3 rounded-2xl border border-white/10 bg-[#0b0b0e] p-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-2">
          <button type="button" onClick={() => setTab('transactions')} className={`rounded-xl px-4 py-2.5 text-sm font-black ${tab === 'transactions' ? 'bg-brand-orange text-black' : 'bg-white/5 text-zinc-400'}`}>Giao dịch</button>
          <button type="button" onClick={() => setTab('refunds')} className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-black ${tab === 'refunds' ? 'bg-brand-orange text-black' : 'bg-white/5 text-zinc-400'}`}><RotateCcw size={16} /> Yêu cầu hoàn tiền {refunds?.totalElements ? `(${refunds.totalElements})` : ''}</button>
        </div>
        <span className="inline-flex items-center gap-2 px-2 text-xs text-zinc-600"><ShieldCheck size={15} /> Chỉ hiển thị dữ liệu của {selectedCinema.name}</span>
      </div>

      {actionState.success ? <div role="status" className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-sm font-bold text-emerald-200">{actionState.success}</div> : null}
      {actionState.error && !decision ? <div role="alert" className="rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-sm font-bold text-red-200">{actionState.error}</div> : null}

      {tab === 'transactions' ? (
        <ConsolePanel className="overflow-hidden">
          <form onSubmit={submitSearch} className="grid gap-3 border-b border-white/10 p-4 xl:grid-cols-[minmax(280px,1fr)_210px_180px_auto]">
            <label className="relative"><Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} /><input value={draftQuery} onChange={event => setDraftQuery(event.target.value)} placeholder="Mã giao dịch hoặc mã đơn" aria-label="Tìm giao dịch" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-orange-500" /></label>
            <select value={filters.status} onChange={event => setFilters(value => ({ ...value, status: event.target.value, page: 0 }))} aria-label="Lọc trạng thái giao dịch" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-orange-500"><option value="">Tất cả trạng thái</option>{PAYMENT_FILTER_STATUSES.map(value => <option key={value} value={value}>{PAYMENT_STATUS[value]}</option>)}</select>
            <select value={filters.provider} onChange={event => setFilters(value => ({ ...value, provider: event.target.value, page: 0 }))} aria-label="Lọc phương thức thanh toán" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-orange-500"><option value="">Mọi phương thức</option>{PROVIDER_FILTERS.map(value => <option key={value} value={value}>{PROVIDERS[value]}</option>)}</select>
            <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-black"><Search size={17} /> Tra cứu</button>
          </form>
          {state.loading ? <p className="py-20 text-center text-sm font-bold text-zinc-500">Đang tải giao dịch…</p> : state.error ? <EmptyWorkspace title="Không thể tải dữ liệu" description={state.error} /> : paymentRows.length ? (
            <div className="overflow-x-auto"><table className="min-w-[960px] w-full text-left text-sm"><thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-wider text-zinc-600"><tr><th className="p-4">Giao dịch</th><th className="p-4">Đơn / phim</th><th className="p-4">Phương thức</th><th className="p-4">Giá trị</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Thao tác</th></tr></thead><tbody className="divide-y divide-white/5">{paymentRows.map(payment => <tr key={payment.paymentPublicId} className="hover:bg-white/[0.02]"><td className="p-4"><p className="font-black text-zinc-100">{payment.paymentTransactionCode || 'Chưa có mã'}</p><p className="mt-1 text-xs text-zinc-600">{formatDateTime(payment.createdAt)}</p></td><td className="p-4"><p className="max-w-60 truncate font-bold text-zinc-300">{payment.movieTitle || 'Đơn đặt vé'}</p><p className="mt-1 max-w-48 truncate font-mono text-[10px] text-zinc-600">{payment.bookingPublicId}</p></td><td className="p-4"><p className="font-bold text-zinc-300">{PROVIDERS[payment.provider] || payment.provider || payment.paymentMethod}</p><p className="mt-1 text-xs text-zinc-600">{payment.ticketCount || 0} vé</p></td><td className="p-4"><p className="font-black text-zinc-100">{formatMoney(payment.amount, payment.currency)}</p>{Number(payment.refundedAmount || 0) > 0 ? <p className="mt-1 text-xs text-amber-300">Đã hoàn {formatMoney(payment.refundedAmount, payment.currency)}</p> : null}</td><td className="p-4"><StatusPill status={payment.status} />{payment.reconciliationStatus && payment.reconciliationStatus !== 'NOT_REQUIRED' ? <p className="mt-2 text-[10px] font-black text-red-300">Kế toán cần kiểm tra</p> : null}</td><td className="p-4 text-right"><button type="button" onClick={() => openDetail(payment)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:bg-white/5"><Eye size={15} /> Chi tiết</button></td></tr>)}</tbody></table></div>
          ) : <EmptyWorkspace title="Không có giao dịch phù hợp" description="Thử đổi từ khóa, trạng thái hoặc phương thức thanh toán." />}
          <ConsolePagination page={payments.number || 0} totalPages={payments.totalPages || 0} totalElements={payments.totalElements || 0} onPage={page => setFilters(value => ({ ...value, page }))} />
        </ConsolePanel>
      ) : (
        <ConsolePanel className="overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-white/10 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-black text-zinc-100">Yêu cầu do nhân viên tại rạp gửi</p><p className="mt-1 text-xs text-zinc-500">Kiểm tra lý do, số tiền và phạm vi hoàn trước khi quyết định.</p></div><select value={refundFilters.status} onChange={event => setRefundFilters(value => ({ ...value, status: event.target.value, page: 0 }))} aria-label="Lọc trạng thái yêu cầu hoàn tiền" className="h-10 rounded-xl border border-white/10 bg-black/30 px-3 text-xs font-bold text-white outline-none"><option value="">Tất cả yêu cầu</option>{Object.entries(REFUND_STATUS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
          {state.loading ? <p className="py-20 text-center text-sm font-bold text-zinc-500">Đang tải yêu cầu hoàn tiền…</p> : state.error ? <EmptyWorkspace title="Không thể tải dữ liệu" description={state.error} /> : refundRows.length ? <div className="divide-y divide-white/5">{refundRows.map(refund => <article key={refund.refundPublicId} className="grid gap-4 p-5 xl:grid-cols-[1.05fr_.8fr_1.3fr_auto] xl:items-center"><div><p className="font-black text-zinc-100">{refund.refundCode}</p><p className="mt-1 text-xs text-zinc-600">Giao dịch {refund.paymentPublicId?.slice(0, 8)}… · {formatDateTime(refund.requestedAt)}</p></div><div><p className="text-lg font-black text-zinc-100">{formatMoney(refund.amount, refund.currency)}</p><p className="mt-1 text-xs text-zinc-500">{COMPONENTS[refund.refundComponent] || refund.refundComponent}</p></div><div><p className="text-sm font-bold text-zinc-300">{refund.reasonCode?.replaceAll('_', ' ')}</p><p className="mt-1 line-clamp-2 text-xs leading-5 text-zinc-500">{refund.reasonDetail}</p><div className="mt-2"><StatusPill status={refund.status} refund /></div></div>{refund.status === 'PENDING_APPROVAL' ? <div className="flex gap-2"><button type="button" onClick={() => openDecision(refund, 'approve')} className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-500/10 px-3 py-2 text-xs font-black text-emerald-300"><Check size={15} /> Duyệt</button><button type="button" onClick={() => openDecision(refund, 'reject')} className="inline-flex items-center gap-1.5 rounded-lg bg-red-500/10 px-3 py-2 text-xs font-black text-red-300"><X size={15} /> Từ chối</button></div> : <div className="xl:text-right"><StatusPill status={refund.status} refund /></div>}</article>)}</div> : <EmptyWorkspace title="Không có yêu cầu hoàn tiền" description={refundFilters.status === 'PENDING_APPROVAL' ? 'Hiện không có yêu cầu nào đang chờ quản lý rạp duyệt.' : 'Không có yêu cầu phù hợp với trạng thái đã chọn.'} />}
          <ConsolePagination page={refunds.number || 0} totalPages={refunds.totalPages || 0} totalElements={refunds.totalElements || 0} onPage={page => setRefundFilters(value => ({ ...value, page }))} />
        </ConsolePanel>
      )}

      <div className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.05] p-4 text-xs leading-5 text-sky-100/70"><p className="flex items-center gap-2 font-black text-sky-300"><ShieldCheck size={15} /> Ranh giới trách nhiệm</p><p className="mt-2">Quản lý rạp xác minh nghiệp vụ tại rạp và duyệt yêu cầu. Nếu có lệch tiền, giao dịch treo, lỗi cổng thanh toán hoặc cần đối soát sổ sách, hãy chuyển Quản trị viên/kế toán; không tự sửa trạng thái giao dịch.</p></div>

      <DetailDrawer open={Boolean(detail)} onClose={() => setDetail(null)} title={`Chi tiết giao dịch ${detail?.payment?.paymentTransactionCode || ''}`} subtitle={detailLoading ? 'Đang tải dữ liệu đầy đủ…' : selectedCinema.name}>
        <div className="space-y-5"><div className="flex flex-wrap gap-2"><StatusPill status={detail?.payment?.status} />{detail?.payment?.reconciliationStatus && detail.payment.reconciliationStatus !== 'NOT_REQUIRED' ? <span className="inline-flex rounded-full border border-red-500/20 bg-red-500/10 px-2.5 py-1 text-[10px] font-black text-red-300">Cần kế toán kiểm tra</span> : null}</div><DetailGrid items={[
          { label: 'Phim', value: detail?.payment?.movieTitle || detail?.bookingSnapshot?.movieTitle || 'Đơn đặt vé' },
          { label: 'Phương thức', value: PROVIDERS[detail?.payment?.provider] || detail?.payment?.provider || detail?.payment?.paymentMethod },
          { label: 'Số tiền', value: formatMoney(detail?.payment?.amount, detail?.payment?.currency) },
          { label: 'Có thể hoàn', value: formatMoney(detail?.payment?.refundableAmount, detail?.payment?.currency) },
          { label: 'Mã đơn đặt vé', value: detail?.payment?.bookingPublicId },
          { label: 'Mã từ cổng thanh toán', value: detail?.payment?.externalTransactionId || 'Chưa có' },
          { label: 'Tình trạng phát vé', value: detail?.payment?.bookingDeliveryStatus || 'Chưa xác định' },
          { label: 'Tạo lúc', value: formatDateTime(detail?.payment?.createdAt) },
        ]} />{detail?.refundRequests?.length ? <div><p className="mb-3 text-xs font-black uppercase tracking-wider text-zinc-500">Lịch sử yêu cầu hoàn tiền</p><div className="space-y-2">{detail.refundRequests.map(refund => <div key={refund.refundPublicId} className="flex items-center justify-between gap-3 rounded-xl border border-white/10 bg-white/[0.025] p-3"><div><p className="text-sm font-black text-zinc-200">{refund.refundCode}</p><p className="mt-1 text-xs text-zinc-600">{formatMoney(refund.amount, refund.currency)}</p></div><StatusPill status={refund.status} refund /></div>)}</div></div> : null}</div>
      </DetailDrawer>

      <ActionModal open={Boolean(decision)} onClose={() => { setDecision(null); setActionState(value => ({ ...value, error: '' })); }} title={decision?.type === 'approve' ? 'Duyệt yêu cầu hoàn tiền' : 'Từ chối yêu cầu hoàn tiền'} description={decision ? `${decision.refund.refundCode} · ${formatMoney(decision.refund.amount, decision.refund.currency)}` : ''} onSubmit={submitDecision} submitLabel={decision?.type === 'approve' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'} submitting={actionState.loading} tone={decision?.type === 'reject' ? 'danger' : 'orange'}>
        {actionState.error ? <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-xs font-bold text-red-200">{actionState.error}</p> : null}
        {decision ? <div className="rounded-xl border border-white/10 bg-white/[0.025] p-4"><p className="text-xs font-black uppercase tracking-wider text-zinc-600">Nhân viên đề nghị</p><p className="mt-2 text-sm font-black text-zinc-200">{COMPONENTS[decision.refund.refundComponent] || decision.refund.refundComponent} · {formatMoney(decision.refund.amount, decision.refund.currency)}</p><p className="mt-2 text-sm leading-6 text-zinc-400">{decision.refund.reasonDetail}</p></div> : null}
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Ghi chú quyết định *<textarea required minLength={5} maxLength={1000} value={note} onChange={event => setNote(event.target.value)} placeholder={decision?.type === 'approve' ? 'Nêu căn cứ đã kiểm tra trước khi duyệt…' : 'Nêu rõ lý do từ chối để nhân viên biết cách xử lý…'} className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-orange-500" /></label>
        {decision?.type === 'approve' ? <div className="flex items-start gap-3 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-xs leading-5 text-emerald-100/75"><CheckCircle2 size={17} className="mt-0.5 shrink-0 text-emerald-300" /> Sau khi duyệt, hệ thống sẽ hoàn qua phương thức gốc. Với tiền mặt, nhân viên quầy cần trả tiền và ghi nhận theo hướng dẫn.</div> : <div className="flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-xs leading-5 text-red-100/75"><XCircle size={17} className="mt-0.5 shrink-0 text-red-300" /> Yêu cầu sẽ kết thúc ở trạng thái từ chối và không phát sinh hoàn tiền.</div>}
      </ActionModal>
    </main>
  );
}
