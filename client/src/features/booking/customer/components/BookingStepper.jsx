import React from 'react';
import { Check } from 'lucide-react';

export default function BookingStepper({ currentStep = 1 }) {
  const steps = [
    { label: 'Rạp & Phim', desc: 'Chọn suất chiếu' },
    { label: 'Chỗ Ngồi', desc: 'Vị trí yêu thích' },
    { label: 'Bắp Nước', desc: 'Dịch vụ đi kèm' },
    { label: 'Thanh Toán', desc: 'Xác nhận đơn' },
    { label: 'Hoàn Tất', desc: 'Nhận vé xem phim' }
  ];

  return (
    <div className="w-full bg-zinc-900/80 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-4 sm:p-6 mb-8 shadow-2xl max-w-7xl mx-auto">
      <div className="flex items-center justify-between gap-2 sm:gap-4 overflow-x-auto scrollbar-none pb-2 sm:pb-0">
        {steps.map((step, idx) => {
          const stepNum = idx + 1;
          const isCompleted = stepNum < currentStep;
          const isActive = stepNum === currentStep;
          const isPending = stepNum > currentStep;

          return (
            <React.Fragment key={idx}>
              <div className="flex flex-col items-center gap-2 sm:gap-3 shrink-0 group relative min-w-[70px] sm:min-w-[90px]">
                {/* Step Circle Indicator */}
                <div
                  className={`w-10 h-10 sm:w-12 sm:h-12 rounded-full flex items-center justify-center font-black text-xs sm:text-sm transition-all duration-500 relative z-10 ${
                    isCompleted
                      ? 'bg-emerald-500 text-black shadow-[0_0_20px_rgba(16,185,129,0.3)]'
                      : isActive
                      ? 'bg-brand-orange text-white shadow-[0_0_25px_rgba(255,122,0,0.5)] scale-110 border-2 border-orange-400/50'
                      : 'bg-zinc-950 text-zinc-600 border border-zinc-800'
                  }`}
                >
                  {isCompleted ? <Check className="w-5 h-5 sm:w-6 sm:h-6 stroke-[3]" /> : stepNum}
                  
                  {/* Active Ripple Effect */}
                  {isActive && (
                    <div className="absolute inset-0 rounded-full border-2 border-brand-orange animate-ping opacity-20 pointer-events-none" />
                  )}
                </div>

                {/* Step Details */}
                <div className="flex flex-col items-center text-center">
                  <span
                    className={`text-[10px] sm:text-xs font-black uppercase tracking-wider transition-colors duration-300 whitespace-nowrap ${
                      isActive ? 'text-brand-orange' : isCompleted ? 'text-emerald-400' : 'text-zinc-500'
                    }`}
                  >
                    {step.label}
                  </span>
                  <span className={`hidden sm:block text-[9px] font-bold mt-0.5 whitespace-nowrap ${
                    isPending ? 'text-zinc-600' : 'text-zinc-400'
                  }`}>
                    {step.desc}
                  </span>
                </div>
              </div>

              {/* Connecting line */}
              {idx < steps.length - 1 && (
                <div className="hidden sm:block flex-grow h-0.5 mx-2 sm:mx-4 bg-zinc-800/60 rounded overflow-hidden relative -translate-y-4">
                  <div
                    className={`absolute left-0 top-0 bottom-0 rounded transition-all duration-700 ease-in-out ${
                      isCompleted ? 'bg-emerald-500 w-full' : 'bg-brand-orange w-0'
                    }`}
                  />
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
