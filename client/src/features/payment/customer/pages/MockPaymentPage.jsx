import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { BadgeCheck, BadgeX } from 'lucide-react';
import PaymentNoticeModal from '../../components/PaymentNoticeModal';
import { completeMockPayment, paymentErrorMessage } from '../../services/paymentService';

export default function MockPaymentPage() {
  const { paymentPublicId } = useParams();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);

  const complete = async simulatedStatus => {
    setBusy(true);
    try {
      await completeMockPayment(paymentPublicId, simulatedStatus);
      navigate(`/payments/return?paymentPublicId=${paymentPublicId}&verified=true`);
    } catch (error) {
      setNotice(paymentErrorMessage(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mx-auto max-w-xl px-4 py-20">
      <div className="rounded-3xl border border-amber-500/30 bg-zinc-900 p-8 text-center">
        <p className="text-xs font-black uppercase tracking-[0.25em] text-amber-400">Chỉ dùng local/test</p>
        <h1 className="mt-3 text-2xl font-black text-white">Giả lập kết quả thanh toán</h1>
        <p className="mt-3 break-all font-mono text-xs text-zinc-500">{paymentPublicId}</p>
        <div className="mt-8 grid gap-3 sm:grid-cols-2">
          <button disabled={busy} onClick={() => complete('SUCCESS')}
            className="flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 py-3 font-black text-white disabled:opacity-50">
            <BadgeCheck className="h-5 w-5" /> Thành công
          </button>
          <button disabled={busy} onClick={() => complete('FAILED')}
            className="flex items-center justify-center gap-2 rounded-xl bg-red-600 px-5 py-3 font-black text-white disabled:opacity-50">
            <BadgeX className="h-5 w-5" /> Thất bại
          </button>
        </div>
      </div>
      <PaymentNoticeModal open={Boolean(notice)} title="Không thể giả lập" message={notice} tone="danger" onClose={() => setNotice(null)} />
    </section>
  );
}
