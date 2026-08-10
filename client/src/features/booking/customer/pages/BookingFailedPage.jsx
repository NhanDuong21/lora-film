import { useMemo } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { AlertCircle, RefreshCw, Home } from 'lucide-react';
import BookingStepper from '../components/BookingStepper';

export default function BookingFailedPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const bookingId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('bookingId');
  }, [location.search]);

  return (
    <div className="min-h-screen bg-zinc-950 px-4 pb-16 pt-6 font-sans font-medium text-zinc-100 selection:bg-brand-orange md:px-12">
      <div className="max-w-4xl mx-auto w-full space-y-8">
        {/* Booking Stepper - Highlight step 5 in Red or alert state */}
        <BookingStepper currentStep={4} />

        <div className="bg-zinc-900 border border-red-500/25 rounded-3xl p-8 md:p-12 text-center space-y-6 max-w-2xl mx-auto shadow-2xl">
          <div className="w-20 h-20 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 mx-auto border border-red-500/20 shadow-[0_0_30px_rgba(239,68,68,0.15)]">
            <AlertCircle className="w-10 h-10" />
          </div>

          <div className="space-y-2">
            <h1 className="text-2xl md:text-3xl font-black uppercase tracking-wider text-white">THANH TOÁN THẤT BẠI</h1>
            <p className="text-sm text-zinc-400 max-w-md mx-auto leading-relaxed">
              Giao dịch của bạn đã bị từ chối hoặc không thể hoàn tất. Đừng lo lắng, số tiền trong tài khoản của bạn chưa bị khấu trừ.
            </p>
          </div>

          {bookingId && (
            <div className="inline-block bg-zinc-950/80 border border-zinc-800 rounded-2xl px-5 py-3 text-xs font-bold text-zinc-500">
              Mã giao dịch: <span className="text-zinc-300 font-mono tracking-wider">{bookingId}</span>
            </div>
          )}

          <div className="pt-4 flex flex-col sm:flex-row gap-4 justify-center items-center">
            {bookingId ? (
              <button
                onClick={() => navigate(`/bookings/checkout?bookingId=${bookingId}`)}
                className="w-full sm:w-auto bg-brand-orange hover:bg-opacity-95 text-white font-black px-6 py-3.5 rounded-2xl text-xs uppercase tracking-widest flex items-center justify-center gap-2 shadow-lg shadow-brand-orange/20 cursor-pointer"
              >
                <RefreshCw className="w-4 h-4" />
                <span>Thử thanh toán lại</span>
              </button>
            ) : (
              <button
                onClick={() => navigate('/movies')}
                className="w-full sm:w-auto bg-brand-orange hover:bg-opacity-95 text-white font-black px-6 py-3.5 rounded-2xl text-xs uppercase tracking-widest flex items-center justify-center gap-2 shadow-lg shadow-brand-orange/20 cursor-pointer"
              >
                <RefreshCw className="w-4 h-4" />
                <span>Quay lại chọn phim</span>
              </button>
            )}

            <Link
              to="/"
              className="w-full sm:w-auto bg-zinc-950 hover:bg-zinc-800 text-zinc-300 font-bold px-6 py-3.5 rounded-2xl border border-zinc-800 text-xs uppercase tracking-widest flex items-center justify-center gap-2 transition-colors"
            >
              <Home className="w-4 h-4" />
              <span>Về trang chủ</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
