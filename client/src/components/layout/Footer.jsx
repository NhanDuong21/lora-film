import { CreditCard, Mail, MapPin, Phone, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

const footerLinks = {
  discover: [
    { label: 'Danh sách phim', to: '/movies' },
    { label: 'Chọn suất chiếu', to: '/booking' },
    { label: 'Hệ thống rạp', to: '/#rap' },
    { label: 'Ưu đãi thành viên', to: '/promotions' },
    { label: 'Vé & đơn hàng', to: '/account/tickets' },
  ],
  support: [
    { label: 'Hướng dẫn thanh toán', to: '/support/payment' },
    { label: 'Chính sách đổi, hủy và hoàn vé', to: '/support/refunds' },
    { label: 'Câu hỏi thường gặp', to: '/support/faq' },
    { label: 'Điều khoản sử dụng', to: '/support/terms' },
    { label: 'Chính sách bảo mật', to: '/support/privacy' },
  ],
};

export default function Footer() {
  return (
    <footer className="border-t border-zinc-900 bg-[#08080a] text-sm text-zinc-400">
      <div className="mx-auto grid w-full max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-2 lg:grid-cols-[1.15fr_0.8fr_1fr_1fr] lg:px-8">
        <div>
          <div className="flex items-center gap-2">
            <img
              src="/images/main-logo.png"
              alt="LoraFilm"
              className="h-10 w-10 rounded-xl object-contain"
            />
            <span className="text-lg font-black text-white">
              <span className="text-brand-orange">Lora</span>Film
            </span>
          </div>
          <p className="mt-4 max-w-sm text-sm leading-6 text-zinc-500">
            Chọn suất, giữ ghế và thanh toán trong vài phút. Vé điện tử sẵn sàng ngay sau khi hoàn tất.
          </p>
          <p className="mt-5 text-sm font-black text-zinc-200">
            LoraFilm — Vé phim trong tầm tay.
          </p>
        </div>

        <nav aria-label="Khám phá LoraFilm">
          <h2 className="text-xs font-black uppercase tracking-wider text-zinc-200">Khám phá</h2>
          <ul className="mt-4 space-y-3 text-xs">
            {footerLinks.discover.map(link => (
              <li key={link.to}>
                <Link to={link.to} className="transition-colors hover:text-brand-orange">
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <nav aria-label="Thông tin và chính sách">
          <h2 className="text-xs font-black uppercase tracking-wider text-zinc-200">
            Thông tin & hỗ trợ
          </h2>
          <ul className="mt-4 space-y-3 text-xs">
            {footerLinks.support.map(link => (
              <li key={link.to}>
                <Link to={link.to} className="transition-colors hover:text-brand-orange">
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div>
          <h2 className="text-xs font-black uppercase tracking-wider text-zinc-200">
            Liên hệ hỗ trợ
          </h2>
          <ul className="mt-4 space-y-3 text-xs">
            <li>
              <a href="tel:19006868" className="flex items-center gap-2 transition-colors hover:text-white">
                <Phone className="h-4 w-4 text-brand-orange" /> 1900 6868 (10:00–22:00)
              </a>
            </li>
            <li>
              <a href="mailto:support@lorafilm.vn" className="flex items-center gap-2 transition-colors hover:text-white">
                <Mail className="h-4 w-4 text-brand-orange" /> support@lorafilm.vn
              </a>
            </li>
            <li className="flex items-start gap-2 text-zinc-500">
              <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-brand-orange" />
              Hỗ trợ tại tất cả rạp LoraFilm
            </li>
          </ul>

          <div className="mt-6 border-t border-zinc-800 pt-5">
            <div className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-zinc-200">
              <CreditCard className="h-4 w-4 text-brand-orange" /> Thanh toán hỗ trợ
            </div>
            <p className="mt-3 text-xs leading-5 text-zinc-500">
              VNPay · MoMo · Tiền mặt tại quầy
            </p>
          </div>
        </div>
      </div>

      <div className="border-t border-zinc-900/70 px-4 py-5">
        <div className="mx-auto flex w-full max-w-7xl flex-col items-center justify-between gap-3 text-center text-xs text-zinc-600 sm:flex-row sm:text-left">
          <span>© 2026 LoraFilm. Bản quyền thuộc về LoraFilm.</span>
          <span className="inline-flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-emerald-500" /> Thanh toán và dữ liệu được bảo vệ
          </span>
        </div>
      </div>
    </footer>
  );
}
