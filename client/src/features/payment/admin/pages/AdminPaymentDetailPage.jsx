import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { ArrowLeft, Banknote, CalendarClock, Film, ReceiptText, ShieldAlert, Webhook } from 'lucide-react';
import { getAdminPayment, paymentErrorMessage } from '../../services/paymentService';

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

  if (loading) return <div className="py-24 text-center text-zinc-500">Đang tải giao dịch...</div>;
  if (!detail) return null;

  const payment = detail.payment || {};
  const snapshot = detail.analyticsSnapshot || {};
  const cash = detail.cashDetail || {};

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 text-white">
      <button onClick={() => navigate('/admin/payments')} className="flex items-center gap-2 text-sm font-bold text-zinc-400 hover:text-white">
        <ArrowLeft className="h-4 w-4" /> Quay lại danh sách giao dịch
      </button>
      <header className="flex flex-col justify-between gap-5 rounded-3xl border border-zinc-800 bg-zinc-900 p-7 lg:flex-row lg:items-center">
        <div>
          <p className="text-xs font-black uppercase tracking-wider text-zinc-500">Mã giao dịch</p>
          <h1 className="mt-2 text-2xl font-black">{payment.paymentTransactionCode}</h1>
          <p className="mt-2 break-all font-mono text-xs text-zinc-600">{payment.paymentPublicId}</p>
        </div>
        <div className="text-left lg:text-right">
          <p className="text-xs font-black uppercase text-zinc-500">Giá trị thanh toán</p>
          <p className="mt-2 text-3xl font-black text-brand-orange">{money(payment.amount, payment.currency)}</p>
          <p className="mt-2 text-xs text-zinc-400">{payment.provider} · {payment.status} · lần {payment.attemptNumber}</p>
        </div>
      </header>

      <div className="grid gap-6 xl:grid-cols-3">
        <InfoCard icon={ReceiptText} title="Định danh & trạng thái" rows={[
          ['Booking UUID', payment.bookingPublicId],
          ['Trạng thái Payment', payment.status],
          ['Giao kết Booking', payment.bookingDeliveryStatus],
          ['Trạng thái đối soát', payment.reconciliationStatus],
          ['Mã giao dịch provider', payment.externalTransactionId || 'Chưa ghi nhận'],
        ]} />
        <InfoCard icon={Film} title="Snapshot đơn hàng" rows={[
          ['Phim', snapshot.movieTitle || 'Chưa có dữ liệu'],
          ['Số vé', `${snapshot.ticketCount || 0} vé`],
          ['Tiền vé', money(snapshot.ticketAmount, snapshot.currency)],
          ['Bắp nước', money(snapshot.foodAmount, snapshot.currency)],
          ['Giảm giá', money(snapshot.discountAmount, snapshot.currency)],
        ]} />
        <InfoCard icon={CalendarClock} title="Mốc thời gian" rows={[
          ['Tạo lúc', formatTime(payment.createdAt)],
          ['Cập nhật lúc', formatTime(payment.updatedAt)],
          ['Hạn thanh toán', formatTime(payment.expiresAt)],
          ['Kênh thanh toán', payment.paymentMethod],
          ['Đơn vị tiền tệ', payment.currency],
        ]} />
      </div>

      {Object.keys(cash).length > 0 && (
        <InfoCard icon={Banknote} title="Thu tiền tại quầy" rows={[
          ['Tiền khách đưa', money(cash.receivedAmount, payment.currency)],
          ['Tiền thừa', money(cash.changeAmount, payment.currency)],
          ['Nhân viên thu', cash.collectedByAccountId ? `Tài khoản #${cash.collectedByAccountId}` : 'Chưa thu'],
          ['Thu lúc', formatTime(cash.collectedAt)],
        ]} />
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        <Timeline title="Nhật ký vòng đời" icon={ReceiptText} items={detail.logs} />
        <Timeline title="Webhook đã nhận" icon={Webhook} items={detail.webhooks} />
        <Timeline title="Giao nhận Outbox" icon={CalendarClock} items={detail.outboxEvents} />
        <Timeline title="Hồ sơ đối soát" icon={ShieldAlert} items={detail.reconciliationCases} />
      </div>
    </div>
  );
}

function InfoCard({ icon: Icon, title, rows }) {
  return (
    <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
      <h2 className="flex items-center gap-2 border-b border-zinc-800 pb-4 text-sm font-black uppercase">
        <Icon className="h-5 w-5 text-brand-orange" /> {title}
      </h2>
      <dl className="mt-4 space-y-3">
        {rows.map(([label, value]) => (
          <div key={label} className="flex items-start justify-between gap-5 text-sm">
            <dt className="text-zinc-500">{label}</dt><dd className="max-w-[65%] break-all text-right font-bold text-zinc-200">{value ?? '—'}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function Timeline({ title, icon: Icon, items = [] }) {
  return (
    <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
      <h2 className="flex items-center gap-2 text-sm font-black uppercase"><Icon className="h-5 w-5 text-brand-orange" /> {title}</h2>
      {items.length === 0 ? <p className="mt-5 text-sm text-zinc-600">Chưa có bản ghi.</p> : (
        <div className="mt-5 space-y-3">
          {items.map((item, index) => (
            <article key={item.eventId || item.publicId || item.id || index} className="rounded-2xl bg-zinc-950 p-4">
              <div className="flex justify-between gap-3"><strong className="text-sm">{item.eventType || item.reasonCode || item.processingStatus || item.status}</strong><span className="text-[10px] text-zinc-600">{formatTime(item.createdAt || item.receivedAt || item.openedAt)}</span></div>
              <p className="mt-2 text-xs leading-5 text-zinc-500">{item.message || item.lastError || item.resolutionNote || item.destination || 'Không có ghi chú.'}</p>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—';
}
