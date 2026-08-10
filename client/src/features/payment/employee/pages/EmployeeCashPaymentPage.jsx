import { useEffect, useMemo, useRef, useState } from 'react';
import { Banknote, CheckCircle2, Clock3, Search, Ticket, XCircle } from 'lucide-react';
import PaymentNoticeModal from '../../components/PaymentNoticeModal';
import {
  cancelCashPayment,
  collectCashPayment,
  createCashPayment,
  lookupCashBooking,
  paymentErrorMessage,
} from '../../services/paymentService';

const money = (value, currency = 'VND') =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(Number(value || 0));

const operationKey = (operation, reference) => {
  const key = `lorafilm.cash:${operation}:${reference}`;
  const existing = sessionStorage.getItem(key);
  if (existing) return existing;
  const generated = crypto.randomUUID();
  sessionStorage.setItem(key, generated);
  return generated;
};

export default function EmployeeCashPaymentPage() {
  const initialReference = useMemo(
    () => new URLSearchParams(window.location.search).get('reference') || '',
    [],
  );
  const autoLookupDone = useRef(false);
  const [reference, setReference] = useState(initialReference);
  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [receivedAmount, setReceivedAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const change = useMemo(() => Math.max(
    0,
    Number(receivedAmount || 0) - Number(booking?.amount || 0),
  ), [booking?.amount, receivedAmount]);

  const run = async action => {
    setLoading(true);
    try {
      await action();
    } catch (error) {
      setNotice({ tone: 'danger', title: 'Không thể xử lý giao dịch', message: paymentErrorMessage(error) });
    } finally {
      setLoading(false);
    }
  };

  const lookupReference = candidate => {
    if (!candidate.trim()) {
      setNotice({ tone: 'info', title: 'Thiếu mã đơn', message: 'Vui lòng nhập mã đặt vé hoặc UUID của đơn.' });
      return;
    }
    run(async () => {
      const result = await lookupCashBooking(candidate.trim());
      setBooking(result);
      setPayment(null);
      setReceivedAmount(String(result.amount || ''));
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
    // Chỉ tự tra cứu một lần khi đi từ màn hình Đơn tại quầy.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialReference]);

  const create = () => run(async () => {
    const result = await createCashPayment(
      { bookingPublicId: booking.bookingPublicId },
      operationKey('create', booking.bookingPublicId),
    );
    setPayment(result);
    setNotice({
      tone: 'success',
      title: 'Đã mở giao dịch tiền mặt',
      message: 'Hãy kiểm đếm tiền khách đưa trước khi xác nhận đã thu.',
    });
  });

  const collect = () => run(async () => {
    const result = await collectCashPayment(
      payment.paymentPublicId,
      Number(receivedAmount),
      operationKey('collect', payment.paymentPublicId),
    );
    setPayment(current => ({ ...current, status: 'SUCCESS', ...result }));
    setConfirm(null);
    setNotice({
      tone: 'success',
      title: 'Đã ghi nhận thu tiền',
      message: `Tiền thừa cần trả khách: ${money(result.changeAmount, booking.currency)}. Đơn đang được chuyển sang Booking để phát vé.`,
    });
  });

  const cancel = () => run(async () => {
    await cancelCashPayment(
      payment.paymentPublicId,
      operationKey('cancel', payment.paymentPublicId),
    );
    setPayment(current => ({ ...current, status: 'CANCELLED' }));
    setConfirm(null);
    setNotice({ tone: 'success', title: 'Đã hủy giao dịch', message: 'Chưa ghi nhận khoản thu nào cho giao dịch này.' });
  });

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 text-white">
      <header className="border-b border-zinc-800 pb-6">
        <p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Thanh toán tại quầy</p>
        <h1 className="mt-2 text-3xl font-black uppercase">Thu tiền tại quầy</h1>
        <p className="mt-2 text-sm text-zinc-400">Tra cứu đơn đã chốt số tiền, kiểm tiền khách đưa và xác nhận thu tiền mặt.</p>
      </header>

      <form onSubmit={lookup} className="flex flex-col gap-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-5 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-500" />
          <input value={reference} onChange={event => setReference(event.target.value)}
            placeholder="Nhập mã đơn LORAFILM-... hoặc UUID"
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-3 pl-12 pr-4 text-sm outline-none focus:border-amber-500" />
        </div>
        <button disabled={loading} className="rounded-xl bg-amber-500 px-7 py-3 text-sm font-black text-black disabled:opacity-50">
          {loading ? 'Đang tra cứu...' : 'Tra cứu đơn'}
        </button>
      </form>

      {!booking ? (
        <div className="rounded-3xl border border-dashed border-zinc-800 py-20 text-center text-zinc-500">
          <Ticket className="mx-auto mb-4 h-12 w-12" />
          <p>Thông tin đơn chính xác từ hệ thống đặt vé sẽ hiển thị tại đây.</p>
        </div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
          <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
            <div className="flex items-start justify-between gap-4 border-b border-zinc-800 pb-5">
              <div>
                <p className="text-xs font-bold uppercase text-zinc-500">Đơn đặt vé</p>
                <h2 className="mt-2 text-xl font-black">{booking.movieTitle || 'Đơn xem phim'}</h2>
                <p className="mt-1 break-all font-mono text-xs text-zinc-500">{booking.bookingPublicId}</p>
              </div>
              <span className="rounded-full bg-amber-500/10 px-3 py-1 text-xs font-black text-amber-400">
                {booking.bookingStatus}
              </span>
            </div>
            <dl className="mt-6 grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl bg-zinc-950 p-4">
                <dt className="text-xs text-zinc-500">Khách hàng</dt>
                <dd className="mt-1 font-bold">Tài khoản #{booking.accountId}</dd>
              </div>
              <div className="rounded-2xl bg-zinc-950 p-4">
                <dt className="text-xs text-zinc-500">Số vé</dt>
                <dd className="mt-1 font-bold">{booking.ticketCount || 0} vé</dd>
              </div>
              <div className="rounded-2xl bg-zinc-950 p-4 sm:col-span-2">
                <dt className="flex items-center gap-2 text-xs text-zinc-500"><Clock3 className="h-4 w-4" /> Hạn thanh toán</dt>
                <dd className="mt-1 font-bold">{new Date(booking.expiresAt).toLocaleString('vi-VN')}</dd>
              </div>
            </dl>
          </section>

          <aside className="rounded-3xl border border-amber-500/30 bg-zinc-900 p-6">
            <p className="text-xs font-black uppercase tracking-widest text-zinc-500">Số tiền phải thu</p>
            <p className="mt-2 text-3xl font-black text-amber-500">{money(booking.amount, booking.currency)}</p>
            <label htmlFor="cash-received-amount"
              className="mt-6 block text-xs font-bold uppercase text-zinc-400">Tiền khách đưa</label>
            <input id="cash-received-amount" type="number" min="0" step="1000" value={receivedAmount}
              onChange={event => setReceivedAmount(event.target.value)}
              disabled={payment?.status === 'SUCCESS' || payment?.status === 'CANCELLED'}
              className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-lg font-black outline-none focus:border-amber-500" />
            <div className="mt-4 flex justify-between border-t border-zinc-800 pt-4">
              <span className="text-sm text-zinc-400">Tiền thừa</span>
              <strong className="text-emerald-400">{money(change, booking.currency)}</strong>
            </div>

            {!payment ? (
              <button onClick={create} disabled={loading}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-amber-500 py-3 font-black text-black disabled:opacity-50">
                <Banknote className="h-5 w-5" /> Tạo giao dịch tiền mặt
              </button>
            ) : payment.status === 'SUCCESS' ? (
              <div className="mt-6 flex items-center gap-3 rounded-xl bg-emerald-500/10 p-4 text-emerald-400">
                <CheckCircle2 className="h-5 w-5" /> <strong>Đã thu tiền</strong>
              </div>
            ) : payment.status === 'CANCELLED' ? (
              <div className="mt-6 flex items-center gap-3 rounded-xl bg-red-500/10 p-4 text-red-400">
                <XCircle className="h-5 w-5" /> <strong>Đã hủy giao dịch</strong>
              </div>
            ) : (
              <div className="mt-6 grid gap-3">
                <button disabled={loading || Number(receivedAmount) < Number(booking.amount)}
                  onClick={() => setConfirm('collect')}
                  className="rounded-xl bg-emerald-600 py-3 font-black disabled:opacity-40">
                  Xác nhận đã thu
                </button>
                <button disabled={loading} onClick={() => setConfirm('cancel')}
                  className="rounded-xl border border-red-500/40 py-3 font-black text-red-400">
                  Hủy giao dịch tiền mặt
                </button>
              </div>
            )}
          </aside>
        </div>
      )}

      <PaymentNoticeModal open={Boolean(notice)} {...notice} onClose={() => setNotice(null)} />
      <PaymentNoticeModal
        open={Boolean(confirm)}
        title={confirm === 'collect' ? 'Xác nhận đã nhận đủ tiền?' : 'Hủy giao dịch tiền mặt?'}
        message={confirm === 'collect'
          ? `Xác nhận đã nhận ${money(receivedAmount, booking?.currency)} và trả khách ${money(change, booking?.currency)} tiền thừa.`
          : 'Thao tác này chỉ hủy giao dịch tiền mặt chưa thu, không hủy đơn Booking.'}
        tone={confirm === 'collect' ? 'success' : 'danger'}
        confirmLabel={confirm === 'collect' ? 'Xác nhận đã thu' : 'Xác nhận hủy'}
        cancelLabel="Quay lại"
        busy={loading}
        onConfirm={confirm === 'collect' ? collect : cancel}
        onClose={() => setConfirm(null)}
      />
    </div>
  );
}
