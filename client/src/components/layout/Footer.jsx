import { Film, Mail, Phone } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="border-t border-zinc-900 bg-zinc-950 text-sm text-zinc-400">
      <div className="mx-auto grid w-full max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-3 lg:px-8">
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-lg bg-brand-orange/10 p-2"><Film className="h-5 w-5 text-brand-orange" /></span>
            <span className="text-lg font-black text-white"><span className="text-brand-orange">Lora</span>Film</span>
          </div>
          <p className="mt-4 max-w-sm text-xs leading-6 text-zinc-500">
            Đặt vé xem phim, quản lý đơn và nhận ưu đãi thành viên trong một trải nghiệm thống nhất.
          </p>
        </div>

        <nav aria-label="Liên kết LoraFilm">
          <h2 className="text-xs font-black uppercase tracking-wider text-zinc-200">Khám phá</h2>
          <ul className="mt-4 space-y-3 text-xs">
            <li><Link to="/movies" className="hover:text-white">Danh sách phim</Link></li>
            <li><Link to="/booking" className="hover:text-white">Chọn suất chiếu</Link></li>
            <li><Link to="/promotions" className="hover:text-white">Ưu đãi của tôi</Link></li>
            <li><Link to="/bookings" className="hover:text-white">Lịch sử đặt vé</Link></li>
          </ul>
        </nav>

        <div>
          <h2 className="text-xs font-black uppercase tracking-wider text-zinc-200">Hỗ trợ khách hàng</h2>
          <ul className="mt-4 space-y-3 text-xs">
            <li><a href="tel:19001000" className="flex items-center gap-2 hover:text-white"><Phone className="h-4 w-4" />1900 1000 (10:00–22:00)</a></li>
            <li><a href="mailto:support@lorafilm.vn" className="flex items-center gap-2 hover:text-white"><Mail className="h-4 w-4" />support@lorafilm.vn</a></li>
          </ul>
        </div>
      </div>
      <div className="border-t border-zinc-900/60 px-4 py-5 text-center text-xs text-zinc-600">
        © 2026 LoraFilm. Movie Tickets, Your Way.
      </div>
    </footer>
  );
}
