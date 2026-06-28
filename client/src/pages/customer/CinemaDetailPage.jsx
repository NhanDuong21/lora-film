// TODO: Connect to Gateway API: GET /api/v1/cinemas/{cinemaId}
import { MapPin, Phone, Clock, AlertCircle } from 'lucide-react';
import Header from '../../components/layout/Header';
import Footer from '../../components/layout/Footer';

export default function CinemaDetailView() {
  return (
    <div className="flex flex-col min-h-screen bg-zinc-950 text-white selection:bg-[#ff7a1a] selection:text-zinc-950 font-sans font-medium">
      <Header />
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-7xl mx-auto w-full flex flex-col justify-center">
        
        {/* Header Breadcrumbs block */}
        <div className="pb-5 border-b border-zinc-900 mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="space-y-2">
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-wider text-white">
              Thông Tin Cụm Rạp
            </h1>
            <div className="flex items-center gap-2 text-zinc-300 text-xs md:text-sm">
              <MapPin className="w-4 h-4 text-orange-500 shrink-0" />
              <span>Hệ thống rạp chiếu phim LoraFilm toàn quốc</span>
            </div>
          </div>
          <div className="flex items-center gap-6 text-xs md:text-sm text-zinc-300 bg-zinc-900/60 backdrop-blur-md px-5 py-3 rounded-2xl border border-zinc-800 self-start md:self-auto">
            <div className="flex items-center gap-2">
              <Phone className="w-4 h-4 text-orange-500" />
              <span>Hotline: <strong>1900 6017</strong></span>
            </div>
            <div className="h-4 w-[1px] bg-zinc-850" />
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-orange-500" />
              <span>08:00 - 24:00</span>
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
