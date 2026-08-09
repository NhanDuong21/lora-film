import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowRight,
  Banknote,
  CheckCircle2,
  CircleDollarSign,
  Info,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
} from 'lucide-react';
import PaymentNoticeModal from '../../components/PaymentNoticeModal';
import {
  completeEmployeeCashRefund,
  createEmployeeRefundRequest,
  getEmployeeCashRefunds,
  lookupRefundCandidate,
  paymentErrorMessage,
} from '../../services/paymentService';

const COMPONENTS = [
  { value: 'FULL_ORDER', label: 'Toàn bộ đơn', hint: 'Hoàn toàn bộ số tiền còn có thể hoàn' },
  { value: 'CONCESSION', label: 'Đồ ăn & thức uống', hint: 'Sai hoặc thiếu sản phẩm tại quầy' },
  { value: 'PRICE_DIFFERENCE', label: 'Chênh lệch giá', hint: 'Điều chỉnh phần tiền thu chênh' },
  { value: 'OPERATIONAL_ADJUSTMENT', label: 'Sự cố vận hành', hint: 'Phòng chiếu, suất chiếu hoặc dịch vụ gặp sự cố' },
];

const REASONS = [
  ['CUSTOMER_REQUEST', 'Khách hàng yêu cầu và đủ điều kiện'],
  ['SHOWTIME_CANCELLED', 'Suất chiếu bị hủy'],
  ['SERVICE_INCIDENT', 'Sự cố dịch vụ tại rạp'],
  ['DUPLICATE_CHARGE', 'Nghi ngờ thu tiền trùng'],
  ['WRONG_ITEM', 'Sai hoặc thiếu sản phẩm'],
  ['PRICE_ADJUSTMENT', 'Điều chỉnh chênh lệch giá'],
];

const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: currency || 'VND', maximumFractionDigits: 0,
}).format(Number(value || 0));

const operationKey = paymentPublicId => {
  const storageKey = `lorafilm.refund-request:${paymentPublicId}`;
  const existing = sessionStorage.getItem(storageKey);
  if (existing) return existing;
  const generated = crypto.randomUUID();
  sessionStorage.setItem(storageKey, generated);
  return generated;
};

const initialForm = {
  refundType: 'FULL',
  refundComponent: 'FULL_ORDER',
  amount: '',
  reasonCode: 'CUSTOMER_REQUEST',
  note: '',
};

const reasonForComponent = {
  CONCESSION: 'WRONG_ITEM',
  PRICE_DIFFERENCE: 'PRICE_ADJUSTMENT',
  OPERATIONAL_ADJUSTMENT: 'SERVICE_INCIDENT',
};

const partialStep = currency => currency === 'VND' ? 1000 : 0.01;

const partialLimitFor = (payment, component) => {
  const remaining = Number(payment?.refundableAmount || 0);
  const componentBalance = component === 'CONCESSION'
    ? Number(payment?.concessionRefundableAmount || 0)
    : remaining;
  const rawLimit = Math.min(remaining, componentBalance);
  const step = partialStep(payment?.currency);
  const strictlyPartialLimit = rawLimit >= remaining ? rawLimit - step : rawLimit;
  return Math.max(0, Math.floor(strictlyPartialLimit / step) * step);
};

export default function EmployeeRefundRequestPage() {
  const initialReference = useMemo(
    () => new URLSearchParams(window.location.search).get('reference') || '',
    [],
  );
  const autoLookupDone = useRef(false);
  const [reference, setReference] = useState(initialReference);
  const [payment, setPayment] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);
  const [result, setResult] = useState(null);
  const [cashState, setCashState] = useState({ loading: true, error: '', items: [] });
  const [cashTarget, setCashTarget] = useState(null);
  const [cashForm, setCashForm] = useState({ providerReference: '', note: '' });
  const [cashSubmitting, setCashSubmitting] = useState(false);

  const selectedComponent = useMemo(
    () => COMPONENTS.find(item => item.value === form.refundComponent),
    [form.refundComponent],
  );
  const partialLimit = useMemo(
    () => partialLimitFor(payment, form.refundComponent),
    [form.refundComponent, payment],
  );

  const loadCashRefunds = useCallback(async () => {
    setCashState(value => ({ ...value, loading: true, error: '' }));
    try {
      const response = await getEmployeeCashRefunds();
      setCashState({ loading: false, error: '', items: response?.content || [] });
    } catch (error) {
      setCashState({ loading: false, error: paymentErrorMessage(error), items: [] });
    }
  }, []);

  useEffect(() => {
    let active = true;
    getEmployeeCashRefunds()
      .then(response => {
        if (active) setCashState({ loading: false, error: '', items: response?.content || [] });
      })
      .catch(error => {
        if (active) setCashState({ loading: false, error: paymentErrorMessage(error), items: [] });
      });
    return () => { active = false; };
  }, []);

  const run = async action => {
    setLoading(true);
    try {
      await action();
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Không thể xử lý yêu cầu',
        message: error?.response?.data?.message || paymentErrorMessage(error),
      });
    } finally {
      setLoading(false);
    }
  };

  const lookupReference = candidateReference => {
    if (!candidateReference.trim()) {
      setNotice({ tone: 'info', title: 'Thiếu thông tin tra cứu', message: 'Vui lòng nhập mã giao dịch hoặc mã đơn đặt vé.' });
      return;
    }
    run(async () => {
      const candidate = await lookupRefundCandidate(candidateReference.trim());
      setPayment(candidate);
      setResult(null);
      setForm(initialForm);
    });
  };

  const lookup = event => {
    event.preventDefault();
    lookupReference(reference);
  };

  useEffect(() => {
    if (!initialReference || autoLookupDone.current) return;
    autoLookupDone.current = true;
    lookupReference(initialReference);
    // Chỉ tự tra cứu một lần khi đi từ chi tiết đơn.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialReference]);

  const changeType = refundType => setForm(value => {
    const preferredPartialComponent = Number(payment?.concessionRefundableAmount || 0) > 0
      ? 'CONCESSION'
      : 'PRICE_DIFFERENCE';
    const refundComponent = refundType === 'FULL' ? 'FULL_ORDER' : preferredPartialComponent;
    return {
      ...value,
      refundType,
      refundComponent,
      reasonCode: refundType === 'FULL'
        ? 'CUSTOMER_REQUEST'
        : reasonForComponent[refundComponent],
      amount: '',
    };
  });

  const changeComponent = refundComponent => setForm(value => ({
    ...value,
    refundComponent,
    reasonCode: reasonForComponent[refundComponent] || value.reasonCode,
    amount: '',
  }));

  const submit = event => {
    event.preventDefault();
    if (form.note.trim().length < 10) {
      setNotice({ tone: 'info', title: 'Cần mô tả rõ hơn', message: 'Vui lòng mô tả sự việc ít nhất 10 ký tự để quản lý rạp có đủ thông tin duyệt.' });
      return;
    }
    if (form.refundType === 'PARTIAL' && (!Number(form.amount) || Number(form.amount) > partialLimit)) {
      setNotice({
        tone: 'info',
        title: 'Số tiền chưa hợp lệ',
        message: partialLimit > 0
          ? `Số tiền hoàn một phần phải lớn hơn 0 và không vượt quá ${money(partialLimit, payment.currency)}.`
          : 'Hạng mục này không còn số tiền đủ điều kiện để hoàn một phần.',
      });
      return;
    }
    run(async () => {
      const payload = {
        refundType: form.refundType,
        refundComponent: form.refundComponent,
        amount: form.refundType === 'PARTIAL' ? Number(form.amount) : null,
        reasonCode: form.reasonCode,
        note: form.note.trim(),
      };
      const created = await createEmployeeRefundRequest(
        payment.paymentPublicId,
        payload,
        operationKey(payment.paymentPublicId),
      );
      setResult(created);
      setNotice({
        tone: 'success',
        title: 'Đã gửi quản lý rạp duyệt',
        message: 'Yêu cầu đã được ghi nhận. Chưa trả tiền cho khách cho đến khi quản lý rạp phê duyệt.',
      });
    });
  };

  const confirmCashRefund = async event => {
    event.preventDefault();
    if (!cashForm.providerReference.trim() || cashForm.note.trim().length < 5) {
      setNotice({
        tone: 'info',
        title: 'Thiếu thông tin xác nhận',
        message: 'Vui lòng nhập mã biên nhận và mô tả cách đã trả tiền cho khách (ít nhất 5 ký tự).',
      });
      return;
    }
    setCashSubmitting(true);
    try {
      await completeEmployeeCashRefund(cashTarget.refundPublicId, {
        providerReference: cashForm.providerReference.trim(),
        note: cashForm.note.trim(),
      });
      setCashTarget(null);
      setCashForm({ providerReference: '', note: '' });
      setNotice({
        tone: 'success',
        title: 'Đã xác nhận hoàn tiền mặt',
        message: 'Hệ thống đã ghi nhận nhân viên trả tiền cho khách tại quầy.',
      });
      await loadCashRefunds();
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa thể xác nhận hoàn tiền',
        message: error?.response?.data?.message || paymentErrorMessage(error),
      });
    } finally {
      setCashSubmitting(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 pb-8 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8">
        <p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Hỗ trợ sau thanh toán</p>
        <h1 className="mt-2 text-3xl font-black">Tạo yêu cầu hoàn tiền</h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-zinc-400">Nhân viên tiếp nhận yêu cầu và ghi nhận đầy đủ thông tin. Quản lý rạp sẽ kiểm tra, duyệt hoặc từ chối; chỉ trả tiền cho khách sau khi yêu cầu được duyệt.</p>
      </header>

      <div className="grid gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 p-3 sm:grid-cols-3">
        {[['1', 'Nhân viên tạo yêu cầu', true], ['2', 'Quản lý rạp kiểm tra & duyệt', false], ['3', 'Hệ thống hoặc quầy hoàn tất', false]].map(([step, label, active]) => <div key={step} className={`flex items-center gap-3 rounded-xl border p-3 ${active ? 'border-amber-500/30 bg-amber-500/10 text-amber-300' : 'border-zinc-800 bg-zinc-950/40 text-zinc-500'}`}><span className="grid h-7 w-7 shrink-0 place-items-center rounded-full border border-current/30 text-xs font-black">{step}</span><span className="text-xs font-black">{label}</span></div>)}
      </div>

      <form onSubmit={lookup} className="flex flex-col gap-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-5 sm:flex-row">
        <div className="relative flex-1"><Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-500" /><input value={reference} onChange={event => setReference(event.target.value)} placeholder="Mã giao dịch, UUID thanh toán hoặc UUID đơn đặt vé" aria-label="Mã giao dịch cần hoàn tiền" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-3 pl-12 pr-4 text-sm outline-none focus:border-amber-500" /></div>
        <button disabled={loading} className="rounded-xl bg-amber-500 px-7 py-3 text-sm font-black text-black disabled:opacity-50">{loading ? 'Đang tra cứu…' : 'Tra cứu giao dịch'}</button>
      </form>

      {!payment ? (
        <div className="grid min-h-64 place-items-center rounded-3xl border border-dashed border-zinc-800 text-center text-zinc-500"><div><RotateCcw className="mx-auto mb-4 h-12 w-12" /><p className="font-bold text-zinc-400">Tra cứu giao dịch thành công cần hoàn tiền</p><p className="mt-2 text-sm">Hệ thống chỉ hiển thị giao dịch thuộc rạp bạn đang làm việc.</p></div></div>
      ) : result ? (
        <section className="rounded-3xl border border-emerald-500/30 bg-emerald-500/[0.06] p-8 text-center"><CheckCircle2 className="mx-auto h-12 w-12 text-emerald-400" /><h2 className="mt-4 text-2xl font-black text-emerald-100">Đã gửi yêu cầu cho quản lý rạp</h2><p className="mt-2 text-sm text-emerald-100/60">Mã yêu cầu: <strong className="text-emerald-200">{result.refundCode}</strong></p><p className="mx-auto mt-4 max-w-lg text-sm leading-6 text-zinc-400">Trạng thái hiện tại là “Chờ quản lý rạp duyệt”. Không tự ý hoàn tiền hoặc sửa giao dịch trong thời gian chờ.</p><button type="button" onClick={() => { setPayment(null); setResult(null); setReference(''); }} className="mt-6 rounded-xl bg-white px-5 py-3 text-sm font-black text-black">Tạo yêu cầu khác</button></section>
      ) : (
        <form onSubmit={submit} className="grid items-start gap-6 lg:grid-cols-[360px_1fr]">
          <aside className="space-y-4 rounded-3xl border border-zinc-800 bg-zinc-900 p-6 lg:sticky lg:top-6">
            <div className="flex items-start justify-between gap-3 border-b border-zinc-800 pb-5"><div><p className="text-xs font-black uppercase tracking-wider text-zinc-500">Giao dịch đã chọn</p><h2 className="mt-2 text-lg font-black">{payment.paymentTransactionCode}</h2></div><span className="rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-black text-emerald-400">Thành công</span></div>
            <dl className="space-y-3 text-sm"><div className="flex justify-between gap-3"><dt className="text-zinc-500">Phim</dt><dd className="max-w-48 text-right font-bold text-zinc-200">{payment.movieTitle || 'Đơn đặt vé'}</dd></div><div className="flex justify-between gap-3"><dt className="text-zinc-500">Tiền vé</dt><dd className="font-bold text-zinc-200">{money(payment.ticketAmount, payment.currency)}</dd></div><div className="flex justify-between gap-3"><dt className="text-zinc-500">Bắp nước</dt><dd className="font-bold text-zinc-200">{money(payment.foodAmount, payment.currency)}</dd></div><div className="flex justify-between gap-3 border-t border-zinc-800 pt-3"><dt className="text-zinc-500">Đã thanh toán</dt><dd className="font-black text-zinc-100">{money(payment.amount, payment.currency)}</dd></div><div className="flex justify-between gap-3"><dt className="text-zinc-500">Đã hoàn/đang chờ</dt><dd className="font-bold text-zinc-300">{money(Number(payment.amount || 0) - Number(payment.refundableAmount || 0), payment.currency)}</dd></div></dl>
            <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4"><p className="text-xs font-black uppercase text-amber-400">Còn có thể đề nghị hoàn</p><p className="mt-2 text-2xl font-black text-amber-300">{money(payment.refundableAmount, payment.currency)}</p></div>
            <p className="flex items-start gap-2 text-xs leading-5 text-zinc-500"><ShieldCheck size={15} className="mt-0.5 shrink-0" /> Dữ liệu đã được giới hạn theo rạp mà tài khoản nhân viên đang được phân công.</p>
          </aside>

          <section className="space-y-6 rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
            <div><p className="text-sm font-black text-zinc-100">1. Chọn phạm vi cần hoàn</p><div className="mt-3 grid gap-3 sm:grid-cols-2"><button type="button" onClick={() => changeType('FULL')} className={`rounded-2xl border p-4 text-left ${form.refundType === 'FULL' ? 'border-amber-500 bg-amber-500/10' : 'border-zinc-700 bg-zinc-950/40'}`}><span className="font-black text-zinc-100">Hoàn toàn bộ</span><span className="mt-1 block text-xs leading-5 text-zinc-500">Đề nghị hoàn toàn bộ số tiền còn lại của đơn.</span></button><button type="button" onClick={() => changeType('PARTIAL')} className={`rounded-2xl border p-4 text-left ${form.refundType === 'PARTIAL' ? 'border-amber-500 bg-amber-500/10' : 'border-zinc-700 bg-zinc-950/40'}`}><span className="font-black text-zinc-100">Hoàn một phần</span><span className="mt-1 block text-xs leading-5 text-zinc-500">Dành cho đồ ăn, chênh lệch giá hoặc điều chỉnh vận hành.</span></button></div></div>

            <div><label className="text-sm font-black text-zinc-100">2. Hạng mục hoàn tiền *</label><div className="mt-3 grid gap-3 sm:grid-cols-2">{COMPONENTS.filter(item => form.refundType === 'FULL' ? item.value === 'FULL_ORDER' : item.value !== 'FULL_ORDER').map(item => { const disabled = form.refundType === 'PARTIAL' && partialLimitFor(payment, item.value) <= 0; return <button key={item.value} type="button" disabled={disabled} onClick={() => changeComponent(item.value)} className={`rounded-xl border p-4 text-left disabled:cursor-not-allowed disabled:opacity-40 ${form.refundComponent === item.value ? 'border-amber-500/50 bg-amber-500/10' : 'border-zinc-700 bg-zinc-950/40'}`}><span className="text-sm font-black text-zinc-200">{item.label}</span><span className="mt-1 block text-xs leading-5 text-zinc-500">{item.hint}</span>{item.value === 'CONCESSION' && form.refundType === 'PARTIAL' ? <span className="mt-2 block text-xs font-bold text-amber-300">Còn đủ điều kiện: {money(payment.concessionRefundableAmount, payment.currency)}</span> : null}</button>; })}</div></div>

            {form.refundType === 'PARTIAL' ? <label className="block text-sm font-black text-zinc-100">3. Số tiền đề nghị hoàn *<div className="relative mt-2"><CircleDollarSign className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={19} /><input required type="number" min={partialStep(payment.currency)} max={partialLimit} step={partialStep(payment.currency)} value={form.amount} onChange={event => setForm(value => ({ ...value, amount: event.target.value }))} placeholder={partialLimit > 0 ? `Nhập số tiền, tối đa ${money(partialLimit, payment.currency)}` : 'Hạng mục này không có số tiền để hoàn'} disabled={partialLimit <= 0} className="h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 pl-12 pr-4 text-sm outline-none focus:border-amber-500 disabled:opacity-50" /></div><span className="mt-2 block text-xs font-normal leading-5 text-zinc-500">Hoàn một phần luôn phải thấp hơn số tiền còn lại. Nếu cần hoàn đủ {money(payment.refundableAmount, payment.currency)}, hãy chọn “Hoàn toàn bộ”.</span></label> : null}

            <label className="block text-sm font-black text-zinc-100">{form.refundType === 'PARTIAL' ? '4' : '3'}. Lý do chính *<select value={form.reasonCode} onChange={event => setForm(value => ({ ...value, reasonCode: event.target.value }))} className="mt-2 h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm outline-none focus:border-amber-500">{REASONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>

            <label className="block text-sm font-black text-zinc-100">{form.refundType === 'PARTIAL' ? '5' : '4'}. Mô tả sự việc *<textarea required minLength={10} maxLength={2000} value={form.note} onChange={event => setForm(value => ({ ...value, note: event.target.value }))} placeholder="Ghi rõ tình huống, nội dung đã kiểm tra với khách và căn cứ đề nghị hoàn…" className="mt-2 min-h-32 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-4 text-sm leading-6 outline-none focus:border-amber-500" /><span className="mt-1 block text-right text-xs font-normal text-zinc-600">{form.note.length}/2000 ký tự</span></label>

            <div className="flex items-start gap-3 rounded-xl border border-sky-500/20 bg-sky-500/5 p-4 text-xs leading-5 text-sky-100/70"><Info size={17} className="mt-0.5 shrink-0 text-sky-300" /><span>Đang đề nghị: <strong className="text-sky-200">{selectedComponent?.label}</strong>, số tiền <strong className="text-sky-200">{form.refundType === 'FULL' ? money(payment.refundableAmount, payment.currency) : form.amount ? money(form.amount, payment.currency) : 'chưa nhập'}</strong>. Quản lý rạp là người đưa ra quyết định cuối cùng.</span></div>
            <button disabled={loading} className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-amber-500 px-5 text-sm font-black text-black disabled:opacity-50">{loading ? 'Đang gửi yêu cầu…' : 'Gửi quản lý rạp duyệt'} <ArrowRight size={18} /></button>
          </section>
        </form>
      )}

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/70">
        <div className="flex flex-col gap-3 border-b border-zinc-800 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="flex items-center gap-2 font-black text-zinc-100"><Banknote size={18} className="text-emerald-400" /> Hoàn tiền mặt đã được duyệt</p>
            <p className="mt-1 text-xs leading-5 text-zinc-500">Chỉ trả tiền và xác nhận tại đây sau khi quản lý rạp đã duyệt yêu cầu.</p>
          </div>
          <button type="button" onClick={loadCashRefunds} disabled={cashState.loading} className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-2 text-xs font-black text-zinc-300 disabled:opacity-50"><RefreshCw size={15} className={cashState.loading ? 'animate-spin' : ''} /> Làm mới danh sách</button>
        </div>

        {cashState.loading ? <p className="py-12 text-center text-sm font-bold text-zinc-500">Đang kiểm tra yêu cầu đã duyệt…</p> : cashState.error ? <div className="p-6 text-center"><p className="text-sm font-bold text-red-300">{cashState.error}</p><button type="button" onClick={loadCashRefunds} className="mt-3 text-xs font-black text-amber-400">Thử tải lại</button></div> : cashState.items.length ? <div className="divide-y divide-zinc-800">{cashState.items.map(item => <article key={item.refundPublicId} className="grid gap-4 p-5 lg:grid-cols-[1fr_.7fr_auto] lg:items-center"><div><p className="font-black text-zinc-100">{item.refundCode}</p><p className="mt-1 text-xs text-zinc-500">Yêu cầu đã được quản lý rạp duyệt · {item.reasonDetail || 'Không có ghi chú bổ sung'}</p></div><div><p className="text-lg font-black text-emerald-300">{money(item.amount, item.currency)}</p><p className="mt-1 text-xs text-zinc-500">Hoàn bằng tiền mặt tại quầy</p></div><button type="button" onClick={() => { setCashTarget(item); setCashForm({ providerReference: '', note: '' }); }} className="rounded-xl bg-emerald-500 px-4 py-2.5 text-xs font-black text-black">Xác nhận đã trả tiền</button></article>)}</div> : <div className="px-6 py-12 text-center"><CheckCircle2 className="mx-auto h-9 w-9 text-emerald-500/50" /><p className="mt-3 text-sm font-black text-zinc-300">Không có khoản tiền mặt đang chờ trả</p><p className="mt-1 text-xs text-zinc-600">Các yêu cầu chưa được duyệt sẽ không xuất hiện tại đây.</p></div>}

        {cashTarget ? <form onSubmit={confirmCashRefund} className="border-t border-emerald-500/20 bg-emerald-500/[0.04] p-5"><div className="mb-4"><p className="font-black text-emerald-200">Xác nhận đã trả {money(cashTarget.amount, cashTarget.currency)} cho khách</p><p className="mt-1 text-xs text-zinc-500">Kiểm đếm tiền và giao cho khách trước khi bấm xác nhận. Thao tác này sẽ hoàn tất yêu cầu.</p></div><div className="grid gap-4 lg:grid-cols-2"><label className="text-xs font-black text-zinc-300">Mã biên nhận / mã phiếu chi *<input autoFocus value={cashForm.providerReference} onChange={event => setCashForm(value => ({ ...value, providerReference: event.target.value }))} maxLength={150} placeholder="Ví dụ: PC-LM81-20260809-001" className="mt-2 h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-normal outline-none focus:border-emerald-500" /></label><label className="text-xs font-black text-zinc-300">Ghi chú cách trả tiền *<input value={cashForm.note} onChange={event => setCashForm(value => ({ ...value, note: event.target.value }))} maxLength={1000} placeholder="Ví dụ: Đã kiểm đếm và giao trực tiếp cho khách" className="mt-2 h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-normal outline-none focus:border-emerald-500" /></label></div><div className="mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end"><button type="button" onClick={() => setCashTarget(null)} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black text-zinc-300">Hủy thao tác</button><button disabled={cashSubmitting} className="rounded-xl bg-emerald-500 px-5 py-2.5 text-xs font-black text-black disabled:opacity-50">{cashSubmitting ? 'Đang xác nhận…' : 'Xác nhận đã giao tiền cho khách'}</button></div></form> : null}
      </section>

      <PaymentNoticeModal open={Boolean(notice)} {...notice} onClose={() => setNotice(null)} />
    </div>
  );
}
