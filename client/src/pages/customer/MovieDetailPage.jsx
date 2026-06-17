// TODO: Connect to Gateway API: GET /api/v1/movies/{movieId}
import { ArrowLeft, AlertCircle } from 'lucide-react';
import Header from '../../components/layout/Header';
import Footer from '../../components/layout/Footer';

export default function MovieDetailView({ movieId, onBack }) {
  return (
    <div className="flex flex-col min-h-screen bg-brand-dark text-zinc-100 selection:bg-[#ff7a1a] selection:text-zinc-950 font-sans font-medium">
      <Header />
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-7xl mx-auto w-full flex flex-col justify-center">
        
        {/* Back Button */}
        <div className="pb-6 border-b border-zinc-900 mb-8 flex justify-between items-center">
          <button
            onClick={onBack}
            className="flex items-center gap-2 bg-black/40 hover:bg-brand-coral/25 text-white border border-white/10 hover:border-brand-coral font-bold px-4 py-2 rounded-full transition-all duration-300 cursor-pointer text-xs"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Quay lại</span>
          </button>
          <span className="text-[10px] text-zinc-550 font-mono">MOVIE ID: {movieId}</span>
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
