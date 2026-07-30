import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertTriangle, CheckCircle2, Clock3, LoaderCircle } from 'lucide-react';
import { getPaymentStatus, paymentErrorMessage } from '../../services/paymentService';
import { resetPaymentAttemptKey } from '@/features/booking/customer/services/paymentHandoffService';
import { BOOKING_CHANGED_EVENT } from '@/features/booking/customer/services/bookingService';

const TERMINAL = new Set(['FAILED', 'CANCELLED', 'EXPIRED']);

export default function PaymentReturnPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const paymentPublicId = params.get('paymentPublicId');
  const bookingPublicId = params.get('bookingPublicId');
  const provider = params.get('provider');
  const verified = params.get('verified');
  const [status, setStatus] = useState(null);
  const [error, setError] = useState('');
  const [pollingTimedOut, setPollingTimedOut] = useState(false);
  const [retryNonce, setRetryNonce] = useState(0);

  const view = useMemo(() => {
    if (!paymentPublicId || error || verified === 'false') return 'error';
    if (status?.reconciliationStatus === 'REQUIRED' || status?.reconciliationStatus === 'IN_REVIEW') return 'review';
    if (status?.status === 'SUCCESS' && status?.bookingDeliveryStatus === 'DELIVERED') return 'success';
    if (TERMINAL.has(status?.status)) return 'failed';
    if (pollingTimedOut) return 'delayed';
    return 'waiting';
  }, [error, paymentPublicId, pollingTimedOut, status, verified]);

  useEffect(() => {
    if (!paymentPublicId) {
      return undefined;
    }

    let active = true;
    let timer;
    let attempts = 0;

    const poll = async () => {
      try {
        const current = await getPaymentStatus(paymentPublicId);
        if (!active) return;
        setStatus(current);
        if (TERMINAL.has(current.status)) {
          resetPaymentAttemptKey(current.bookingPublicId || bookingPublicId, provider);
        }
        attempts += 1;
        const done = TERMINAL.has(current.status)
          || current.reconciliationStatus === 'REQUIRED'
          || current.reconciliationStatus === 'IN_REVIEW'
          || (current.status === 'SUCCESS' && current.bookingDeliveryStatus === 'DELIVERED');
        if (done) {
          window.dispatchEvent(new CustomEvent(BOOKING_CHANGED_EVENT, {
            detail: {
              action: current.status,
              publicId: current.bookingPublicId || bookingPublicId,
            },
          }));
        }
        if (!done && attempts < 60) {
          timer = window.setTimeout(poll, 2000);
        } else if (!done) {
          setPollingTimedOut(true);
        }
      } catch (requestError) {
        if (active) setError(paymentErrorMessage(requestError));
      }
    };

    poll();
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [bookingPublicId, paymentPublicId, provider, retryNonce]);

  const content = {
    waiting: {
      icon: <LoaderCircle className="h-12 w-12 animate-spin text-brand-orange" />,
      title: 'Đang xác nhận thanh toán',
      text: 'LoraFilm đang chờ kết quả có thẩm quyền từ nhà cung cấp. Bạn có thể giữ nguyên trang này.',
    },
    success: {
      icon: <CheckCircle2 className="h-12 w-12 text-emerald-400" />,
      title: 'Thanh toán thành công',
      text: 'Đơn đã được Booking xác nhận và vé đang sẵn sàng.',
    },
    failed: {
      icon: <AlertTriangle className="h-12 w-12 text-red-400" />,
      title: 'Thanh toán chưa thành công',
      text: 'Giao dịch đã kết thúc nhưng chưa thu tiền. Bạn có thể quay lại đơn để thử phương thức khác nếu còn hạn.',
    },
    review: {
      icon: <Clock3 className="h-12 w-12 text-amber-400" />,
      title: 'Giao dịch đang được đối soát',
      text: 'Nhà cung cấp đã ghi nhận kết quả nhưng đơn cần được kiểm tra thêm. Vui lòng không thanh toán lại.',
    },
    delayed: {
      icon: <Clock3 className="h-12 w-12 text-amber-400" />,
      title: 'Việc xác nhận đang lâu hơn dự kiến',
      text: 'Giao dịch chưa có kết luận cuối cùng. Vui lòng không thanh toán lại; bạn có thể kiểm tra lại ngay hoặc xem trạng thái trong chi tiết đơn.',
    },
    error: {
      icon: <AlertTriangle className="h-12 w-12 text-red-400" />,
      title: 'Không thể xác minh lượt quay lại',
      text: error || (!paymentPublicId
        ? 'Không xác định được giao dịch cần kiểm tra.'
        : 'Chữ ký từ trang quay lại không hợp lệ. Trạng thái đơn chỉ được cập nhật qua callback an toàn.'),
    },
  }[view];

  const targetBooking = status?.bookingPublicId || bookingPublicId;

  return (
    <section className="mx-auto flex min-h-[70vh] max-w-3xl items-center justify-center px-4 py-16">
      <div className="w-full rounded-3xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-2xl md:p-12">
        <div className="mx-auto mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-zinc-950">
          {content.icon}
        </div>
        <p className="text-xs font-black uppercase tracking-[0.25em] text-brand-orange">LoraFilm Payment</p>
        <h1 className="mt-3 text-3xl font-black text-white">{content.title}</h1>
        <p className="mx-auto mt-4 max-w-xl leading-7 text-zinc-400">{content.text}</p>
        {paymentPublicId && (
          <p className="mt-5 break-all font-mono text-xs text-zinc-600">Mã giao dịch: {paymentPublicId}</p>
        )}
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          {view === 'delayed' && (
            <button type="button" onClick={() => {
              setPollingTimedOut(false);
              setRetryNonce((value) => value + 1);
            }}
              className="rounded-xl bg-brand-orange px-6 py-3 text-sm font-black text-white">
              Kiểm tra lại
            </button>
          )}
          {targetBooking && (
            <button type="button" onClick={() => navigate(`/bookings/${targetBooking}`)}
              className="rounded-xl bg-brand-orange px-6 py-3 text-sm font-black text-white">
              Xem chi tiết đơn
            </button>
          )}
          <button type="button" onClick={() => navigate('/bookings')}
            className="rounded-xl border border-zinc-700 px-6 py-3 text-sm font-bold text-zinc-300 hover:bg-zinc-800">
            Lịch sử đặt vé
          </button>
        </div>
      </div>
    </section>
  );
}
