import { Check } from 'lucide-react';

const steps = ['Tài khoản', 'Hồ sơ', 'Xác minh'];

export function AuthStepper({ currentStep }) {
  return (
    <ol aria-label="Tiến trình đăng ký" className="mb-7 grid grid-cols-3 gap-2">
      {steps.map((label, index) => {
        const step = index + 1;
        const isComplete = step < currentStep;
        const isCurrent = step === currentStep;

        return (
          <li key={label} className="relative flex min-w-0 flex-col items-center gap-2 text-center">
            {index > 0 && (
              <span
                aria-hidden="true"
                className={`absolute right-1/2 top-4 h-px w-[calc(100%-2rem)] -translate-x-4 ${
                  step <= currentStep ? 'bg-brand-orange' : 'bg-zinc-800'
                }`}
              />
            )}
            <span
              aria-current={isCurrent ? 'step' : undefined}
              className={`relative z-10 flex h-8 w-8 items-center justify-center rounded-full border text-xs font-black transition-colors ${
                isComplete
                  ? 'border-brand-orange bg-brand-orange text-zinc-950'
                  : isCurrent
                    ? 'border-brand-orange bg-brand-orange/15 text-brand-orange shadow-[0_0_0_4px_rgba(255,122,0,0.08)]'
                    : 'border-zinc-700 bg-zinc-950 text-zinc-500'
              }`}
            >
              {isComplete ? <Check aria-hidden="true" className="h-4 w-4" /> : step}
            </span>
            <span className={`truncate text-[10px] font-black uppercase tracking-wider sm:text-xs ${
              isCurrent || isComplete ? 'text-zinc-200' : 'text-zinc-600'
            }`}>
              {label}
            </span>
          </li>
        );
      })}
    </ol>
  );
}

export function GoogleButton({ onStart }) {
  return (
    <a
      href={`${import.meta.env.VITE_API_BASE_URL || ''}/oauth2/authorization/google`}
      onClick={onStart}
      className="flex min-h-12 w-full items-center justify-center gap-3 rounded-xl border border-zinc-200 bg-white px-4 py-3 text-sm font-black text-zinc-900 transition hover:bg-zinc-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-900"
    >
      <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24">
        <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
        <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
        <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
        <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
      </svg>
      Tiếp tục với Google
    </a>
  );
}

export function AuthDivider({ children = 'Hoặc' }) {
  return (
    <div className="relative my-5" aria-hidden="true">
      <div className="absolute inset-0 flex items-center">
        <div className="w-full border-t border-zinc-800" />
      </div>
      <div className="relative flex justify-center">
        <span className="bg-[#141417] px-3 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-600">
          {children}
        </span>
      </div>
    </div>
  );
}

export default function AuthShell({ children, maxWidth = 'max-w-lg' }) {
  return (
    <div className="relative flex min-h-[calc(100vh-5rem)] w-full items-center justify-center overflow-hidden bg-[#070708] px-4 py-8 text-white sm:px-6 sm:py-12">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0">
        <div className="absolute left-1/2 top-1/2 h-[34rem] w-[34rem] -translate-x-1/2 -translate-y-1/2 rounded-full bg-brand-orange/[0.07] blur-[110px]" />
        <div className="absolute -right-48 top-10 h-96 w-96 rounded-full bg-amber-800/[0.07] blur-[110px]" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(0,0,0,0.28)_72%,rgba(0,0,0,0.72)_100%)]" />
        <div className="absolute inset-0 opacity-[0.035] [background-image:linear-gradient(rgba(255,255,255,.35)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,.35)_1px,transparent_1px)] [background-size:48px_48px]" />
      </div>

      <section className={`relative z-10 w-full ${maxWidth} rounded-3xl border border-white/[0.09] bg-[#141417]/95 p-5 shadow-[0_30px_90px_-30px_rgba(0,0,0,0.95)] backdrop-blur-xl sm:p-8`}>
        {children}
      </section>
    </div>
  );
}
