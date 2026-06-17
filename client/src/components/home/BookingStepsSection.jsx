import { Ticket, Armchair, CreditCard } from 'lucide-react';

export default function BookingSteps() {
  const steps = [
    {
      id: 1,
      icon: Ticket,
      title: '1. CHỌN PHIM & SUẤT CHIẾU',
      description: 'Tìm kiếm bộ phim yêu thích của bạn và chọn suất chiếu phù hợp nhất tại cụm rạp Lora.'
    },
    {
      id: 2,
      icon: Armchair,
      title: '2. CHỌN GHẾ NGỒI & THỨC ĂN',
      description: 'Lựa chọn vị trí ngồi đẹp nhất trong rạp cùng danh mục bắp nước, combo ưu đãi đi kèm.'
    },
    {
      id: 3,
      icon: CreditCard,
      title: '3. THANH TOÁN AN TOÀN',
      description: 'Thực hiện thanh toán trực tuyến bảo mật cao và nhận vé điện tử tức thì qua Email/SMS.'
    }
  ];

  return (
    <section id="steps" className="px-6 md:px-12 py-16 bg-zinc-950 border-t border-zinc-900">
      {/* Section Header */}
      <div className="max-w-4xl mx-auto text-center mb-12">
        <span className="text-amber-500 font-bold tracking-widest text-xs uppercase block mb-2">
          QUY TRÌNH ĐƠN GIẢN
        </span>
        <h3 className="text-xl md:text-2xl font-black text-white uppercase tracking-wide">
          MUA VÉ CHỈ VỚI 3 BƯỚC NHANH CHÓNG
        </h3>
      </div>

      {/* Glassmorphism Steps Grid Wrapper */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto mt-12 relative px-4 perspective-1500 transform-style-3d">
        
        {/* Vector Flow Connector 1 (Desktop only progress line connecting Step 1 to 2) */}
        <div className="absolute top-[4.5rem] left-[25%] w-[16%] h-[2px] bg-gradient-to-r from-amber-500/50 to-orange-500/50 hidden md:block z-0" />
        
        {/* Vector Flow Connector 2 (Desktop only progress line connecting Step 2 to 3) */}
        <div className="absolute top-[4.5rem] left-[59%] w-[16%] h-[2px] bg-gradient-to-r from-amber-500/50 to-orange-500/50 hidden md:block z-0" />

        {steps.map((step) => {
          const Icon = step.icon;
          const cardClass = step.id === 1 ? 'booking-step-card-1' : step.id === 2 ? 'booking-step-card-2' : 'booking-step-card-3';
          return (
            <div
              key={step.id}
              className={`w-full bg-zinc-900/30 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-6 flex flex-col items-center text-center transition-all duration-500 relative overflow-hidden group hover:border-amber-500/50 hover:shadow-[0_0_30px_rgba(245,158,11,0.05)] hover:bg-zinc-900/50 z-10 ${cardClass}`}
            >
              {/* Decorative internal card light glow */}
              <div className="absolute -top-12 -left-12 w-24 h-24 bg-amber-500/5 rounded-full filter blur-xl pointer-events-none group-hover:bg-amber-500/10 transition-all duration-500"></div>

              {/* Elevated Vector Icon Box */}
              <div className="w-16 h-16 bg-zinc-950 border border-zinc-800 rounded-2xl flex items-center justify-center mb-5 text-zinc-400 group-hover:text-amber-400 group-hover:border-amber-500 group-hover:shadow-[0_0_20px_rgba(245,158,11,0.2)] transition-all duration-500">
                <Icon className="w-8 h-8 transition-transform duration-500 group-hover:scale-110" />
              </div>

              {/* Step Title */}
              <h4 className="text-zinc-200 group-hover:text-amber-500 font-extrabold text-sm md:text-base tracking-wider uppercase mb-3 transition-colors duration-300">
                {step.title}
              </h4>

              {/* Step Description */}
              <p className="text-xs text-zinc-400 group-hover:text-zinc-300 leading-relaxed max-w-[260px] transition-colors duration-300">
                {step.description}
              </p>
            </div>
          );
        })}
      </div>
    </section>
  );
}
