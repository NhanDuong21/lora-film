import React from 'react';
import { Check } from 'lucide-react';

export default function BookingStepper({ currentStep = 1 }) {
  const steps = [
    { label: 'Chọn Suất Chiếu', desc: 'Phim, rạp & giờ chiếu' },
    { label: 'Chọn Ghế', desc: 'Chọn vị trí ngồi' },
    { label: 'Bắp Nước', desc: 'Chọn đồ ăn thức uống' },
    { label: 'Thanh Toán', desc: 'Thanh toán vé' },
    { label: 'Xác Nhận', desc: 'Nhận vé & thông tin' }
  ];

  return (
    <div className="w-full bg-zinc-900/60 border border-zinc-800 rounded-3xl p-6 mb-8 shadow-lg max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 md:gap-4">
        {steps.map((step, idx) => {
          const stepNum = idx + 1;
          const isCompleted = stepNum < currentStep;
          const isActive = stepNum === currentStep;

          return (
            <div key={idx} className="flex-1 flex items-center gap-3 group">
              {/* Step Circle Indicator */}
              <div
                className={`w-9 h-9 rounded-full flex items-center justify-center font-black text-xs transition-all duration-300 ${
                  isCompleted
                    ? 'bg-emerald-500 text-white shadow-[0_0_15px_rgba(16,185,129,0.3)]'
                    : isActive
                    ? 'bg-brand-orange text-white shadow-[0_0_20px_rgba(255,122,0,0.5)] scale-110 border-2 border-orange-400'
                    : 'bg-zinc-800 text-zinc-500 border border-zinc-700'
                }`}
              >
                {isCompleted ? <Check className="w-4 h-4 stroke-[3]" /> : stepNum}
              </div>

              {/* Step Details */}
              <div className="flex flex-col">
                <span
                  className={`text-xs font-black uppercase tracking-wider transition-colors duration-300 ${
                    isActive ? 'text-brand-orange' : isCompleted ? 'text-emerald-400' : 'text-zinc-400'
                  }`}
                >
                  {step.label}
                </span>
                <span className="text-[10px] text-zinc-500 font-bold mt-0.5 line-clamp-1">{step.desc}</span>
              </div>

              {/* Connecting line for desktop view */}
              {idx < steps.length - 1 && (
                <div className="hidden md:block flex-grow h-0.5 mx-4 bg-zinc-800 rounded">
                  <div
                    className={`h-full rounded transition-all duration-500 ${
                      isCompleted ? 'bg-emerald-500 w-full' : 'bg-zinc-800 w-0'
                    }`}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
