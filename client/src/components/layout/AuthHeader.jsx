import { Link, useLocation } from 'react-router-dom';

export default function AuthHeader() {
  const location = useLocation();
  const isRegister = location.pathname === '/register';
  const isLogin = location.pathname === '/login';
  const destination = isLogin ? '/register' : '/login';
  const prompt = isLogin ? 'Chưa có tài khoản?' : 'Đã có tài khoản?';
  const action = isLogin ? 'Đăng ký' : 'Đăng nhập';

  return (
    <header className="fixed inset-x-0 top-0 z-50 h-20 border-b border-white/[0.07] bg-[#09090b]/90 backdrop-blur-xl">
      <div className="mx-auto flex h-full max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link
          to="/"
          aria-label="LoraFilm - Trang chủ"
          className="flex items-center gap-2.5 rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
        >
          <img src="/images/main-logo.png" alt="" className="h-11 w-11 rounded-xl object-contain" />
          <span className="text-xl font-black tracking-tight text-white sm:text-2xl">
            Lora<span className="text-brand-orange">Film</span>
          </span>
        </Link>

        {(isLogin || isRegister) ? (
          <div className="flex items-center gap-2 text-xs sm:gap-3 sm:text-sm">
            <span className="hidden text-zinc-500 sm:inline">{prompt}</span>
            <Link
              to={destination}
              state={{ from: location.state?.from }}
              className="rounded-full border border-brand-orange/50 px-4 py-2 font-black text-brand-orange transition hover:border-brand-orange hover:bg-brand-orange/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
            >
              {action}
            </Link>
          </div>
        ) : (
          <Link
            to="/"
            className="text-sm font-bold text-zinc-400 transition hover:text-brand-orange focus:outline-none focus-visible:text-brand-orange"
          >
            Về trang chủ
          </Link>
        )}
      </div>
    </header>
  );
}
