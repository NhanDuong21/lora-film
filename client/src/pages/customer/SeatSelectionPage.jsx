// TODO: Connect to Gateway API: GET /api/v1/seats
import { ArrowLeft, AlertCircle } from 'lucide-react';
import Header from '../../components/layout/Header';
import Footer from '../../components/layout/Footer';

export default function SeatSelectionView({ bookingData, onBack }) {
  const { cinema, time, date } = bookingData || {};

  return (
    <div className="flex flex-col min-h-screen bg-brand-dark text-zinc-100 selection:bg-[#ff7a1a] selection:text-zinc-950 font-sans font-medium">
      <Header />
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-7xl mx-auto w-full flex flex-col justify-center">
        
        {/* Header Strip with Back link */}
        <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
          <button
            onClick={onBack}
            className="flex items-center gap-2 text-zinc-400 hover:text-brand-coral transition-colors text-xs font-bold cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Quay lại</span>
          </button>
          
          <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-zinc-400">
            <div>
              <span className="text-zinc-650 font-bold mr-1">Rạp:</span>
              <span className="text-zinc-200 font-semibold">{cinema || 'N/A'}</span>
            </div>
            <div>
              <span className="text-zinc-650 font-bold mr-1">Suất chiếu:</span>
              <span className="text-brand-coral font-black">{time || 'N/A'}</span>
            </div>
            <div>
              <span className="text-zinc-650 font-bold mr-1">Ngày:</span>
              <span className="text-zinc-200 font-semibold">{date || 'N/A'}</span>
            </div>
          </div>
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
