import { Link } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';

export default function BookingFlowHeader() {
  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-zinc-800/80 bg-zinc-950/95 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
        <Link
          to="/"
          aria-label="Về trang chủ LoraFilm"
          className="flex items-center gap-2.5 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
        >
          <img
            src="/images/main-logo.png"
            alt=""
            className="h-9 w-auto object-contain"
          />
          <span className="text-xl font-black leading-none tracking-tight text-white sm:text-2xl">
            Lora<span className="text-amber-500">Film</span>
          </span>
        </Link>

        <p className="hidden text-xs font-black uppercase tracking-[.18em] text-zinc-400 sm:block">
          Đặt vé trực tuyến
        </p>

        <div className="flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/5 px-3 py-2 text-emerald-400">
          <ShieldCheck className="h-4 w-4" />
          <span className="hidden text-[10px] font-black uppercase tracking-wider sm:inline">
            Thanh toán an toàn
          </span>
          <span className="text-[10px] font-black uppercase tracking-wider sm:hidden">
            Bảo mật
          </span>
        </div>
      </div>
    </header>
  );
}
