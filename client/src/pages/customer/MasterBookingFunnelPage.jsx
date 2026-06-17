// TODO: Connect to Gateway API: GET /api/v1/booking-funnel
import { AlertCircle } from 'lucide-react';
import Header from '../../components/layout/Header';
import Footer from '../../components/layout/Footer';

export default function MasterBookingFunnel({ onBackHome }) {
  return (
    <div className="flex flex-col min-h-screen bg-zinc-950 text-white selection:bg-[#ff7a1a] selection:text-zinc-950 font-sans font-medium">
      <Header />
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-7xl mx-auto w-full flex flex-col justify-center">
        
        {/* Funnel Title */}
        <div className="pb-5 border-b border-zinc-900 mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Lập Lịch Vé Xem Phim</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
              Đặt lịch xem phim nhanh chóng trong 3 bước accordion tiện lợi
            </p>
          </div>
          <button
            onClick={onBackHome}
            className="text-xs font-bold text-zinc-500 hover:text-orange-500 transition-colors self-start sm:self-center cursor-pointer"
          >
            Quay lại trang chủ
          </button>
        </div>

        {/* Beautiful, stylized, dark-themed Empty State notification panel */}
        <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-900/40 border border-zinc-800 rounded-2xl max-w-xl mx-auto my-12 space-y-4 shadow-2xl">
          <div className="w-12 h-12 rounded-full bg-orange-500/10 flex items-center justify-center text-orange-500">
            <AlertCircle className="w-6 h-6 animate-pulse" />
          </div>
          <h2 className="text-lg font-bold text-zinc-100">Hệ thống đang được cập nhật</h2>
          <p className="text-xs text-zinc-400">
            No real data available yet. This module is waiting for backend API integration.
          </p>
        </div>

      </main>
      <Footer />
    </div>
  );
}
