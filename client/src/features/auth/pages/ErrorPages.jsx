const STATUS_CONTENT = {
  401: {
    title: 'Cần đăng nhập',
    message: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn.',
    action: 'Đăng nhập',
    href: '/login'
  },
  403: {
    title: 'Không có quyền truy cập',
    message: 'Tài khoản của bạn không có quyền mở trang này.',
    action: 'Về trang chủ',
    href: '/'
  },
  404: {
    title: 'Không tìm thấy trang',
    message: 'Đường dẫn này không tồn tại hoặc đã được di chuyển.',
    action: 'Về trang chủ',
    href: '/'
  },
  500: {
    title: 'Đã xảy ra lỗi',
    message: 'Ứng dụng gặp lỗi ngoài dự kiến. Vui lòng thử tải lại.',
    action: 'Tải lại',
    href: window.location.pathname
  }
};

export function ErrorPage({ status, onRetry }) {
  const content = STATUS_CONTENT[status] || STATUS_CONTENT[500];

  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-950 px-5 text-white">
      <section className="w-full max-w-lg rounded-3xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-2xl">
        <p className="text-7xl font-black text-orange-500">{status}</p>
        <h1 className="mt-5 text-2xl font-black uppercase tracking-wide">{content.title}</h1>
        <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-zinc-400">{content.message}</p>
        {onRetry ? (
          <button type="button" onClick={onRetry}
            className="mt-7 rounded-xl bg-orange-500 px-5 py-3 text-sm font-bold text-zinc-950">
            {content.action}
          </button>
        ) : (
          <a href={content.href}
            className="mt-7 inline-block rounded-xl bg-orange-500 px-5 py-3 text-sm font-bold text-zinc-950">
            {content.action}
          </a>
        )}
      </section>
    </main>
  );
}

export const UnauthorizedPage = () => <ErrorPage status={401} />;
export const ForbiddenPage = () => <ErrorPage status={403} />;
export const NotFoundPage = () => <ErrorPage status={404} />;
export const ServerErrorPage = ({ onRetry }) => <ErrorPage status={500} onRetry={onRetry} />;
