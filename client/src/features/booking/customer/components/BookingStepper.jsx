import React from 'react';
import { Check } from 'lucide-react';

export default function BookingStepper({ currentStep = 1, completed = false }) {
  const steps = [
    { label: 'Chọn suất', desc: 'Rạp, phim và giờ chiếu' },
    { label: 'Chọn ghế', desc: 'Vị trí yêu thích' },
    { label: 'Bắp nước', desc: 'Không bắt buộc' },
    { label: 'Thanh toán', desc: 'Xác nhận và thanh toán' }
  ];

  return (
    <nav
      aria-label="Tiến trình đặt vé"
      className="mx-auto mb-5 w-full max-w-7xl rounded-2xl border border-zinc-800/80 bg-zinc-900/80 px-4 py-3 shadow-xl backdrop-blur-md sm:px-5"
    >
      <div className="mb-2 flex items-center justify-between sm:hidden">
        <span className="text-xs font-black text-white">
          Bước {Math.min(currentStep, steps.length)}/{steps.length}
        </span>
        <span className="text-xs font-bold text-brand-orange">
          {completed ? 'Hoàn tất đặt vé' : steps[Math.min(currentStep, steps.length) - 1]?.label}
        </span>
      </div>

      <div className="flex items-center justify-between gap-2">
        {steps.map((step, idx) => {
          const stepNum = idx + 1;
          const isCompleted = completed || stepNum < currentStep;
          const isActive = !completed && stepNum === currentStep;
          const isPending = !completed && stepNum > currentStep;

          return (
            <React.Fragment key={idx}>
              <div
                aria-current={isActive ? 'step' : undefined}
                className="relative flex shrink-0 items-center gap-2 sm:min-w-[118px]"
              >
                <div
                  className={`relative z-10 flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-black transition-all duration-300 sm:h-8 sm:w-8 ${
                    isCompleted
                      ? 'bg-emerald-500 text-black'
                      : isActive
                      ? 'bg-brand-orange text-white shadow-[0_0_18px_rgba(255,122,0,0.35)]'
                      : 'bg-zinc-950 text-zinc-600 border border-zinc-800'
                  }`}
                >
                  {isCompleted ? <Check className="h-4 w-4 stroke-[3]" /> : stepNum}
                </div>

                <div className="hidden min-w-0 flex-col sm:flex">
                  <span
                    className={`whitespace-nowrap text-[10px] font-black uppercase tracking-wider ${
                      isActive ? 'text-brand-orange' : isCompleted ? 'text-emerald-400' : 'text-zinc-500'
                    }`}
                  >
                    {step.label}
                  </span>
                  <span className={`mt-0.5 whitespace-nowrap text-[9px] font-bold ${
                    isPending ? 'text-zinc-600' : 'text-zinc-400'
                  }`}>
                    {step.desc}
                  </span>
                </div>
              </div>

              {idx < steps.length - 1 && (
                <div className="relative h-px flex-grow overflow-hidden rounded bg-zinc-800/80">
                  <div
                    className={`absolute inset-y-0 left-0 rounded transition-all duration-500 ${
                      isCompleted ? 'bg-emerald-500 w-full' : 'bg-brand-orange w-0'
                    }`}
                  />
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>
    </nav>
  );
}
