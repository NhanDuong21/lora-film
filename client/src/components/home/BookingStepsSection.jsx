import { Ticket, Armchair, CreditCard } from "lucide-react";

export default function BookingStepsSection() {
    const steps = [
        {
            id: 1,
            icon: Ticket,
            title: "1. CHỌN PHIM & SUẤT CHIẾU",
            description: "Tìm kiếm bộ phim yêu thích của bạn và chọn suất chiếu phù hợp nhất tại cụm rạp Lora."
        },
        {
            id: 2,
            icon: Armchair,
            title: "2. CHỌN GHẾ NGỒI & THỨC ĂN",
            description: "Lựa chọn vị trí ngồi đẹp nhất trong rạp cùng danh mục bắp nước, combo ưu đãi đi kèm."
        },
        {
            id: 3,
            icon: CreditCard,
            title: "3. THANH TOÁN AN TOÀN",
            description: "Thực hiện thanh toán trực tuyến bảo mật cao và nhận vé điện tử tức thì qua Email/SMS."
        }
    ];

    return (
        <section id="booking-steps" className="px-6 md:px-12 py-16 bg-zinc-950 border-t border-zinc-900">
            {/* Section Header */}
            <div className="max-w-4xl mx-auto text-center mb-12">
                <span className="text-brand-coral font-bold tracking-widest text-xs uppercase block mb-2">
                    Quy Trình Đơn Giản
                </span>
                <h3 className="text-xl md:text-2xl font-black text-white uppercase tracking-wide">
                    Mua vé chỉ với 3 bước nhanh chóng
                </h3>
            </div>

            {/* Steps Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto mt-12 relative px-4">
                {/* Flow Connectors */}
                <div className="absolute top-12 left-[25%] w-[16%] h-[2px] bg-gradient-to-r from-brand-coral/50 to-orange-500/50 hidden md:block z-0" />
                <div className="absolute top-12 left-[59%] w-[16%] h-[2px] bg-gradient-to-r from-brand-coral/50 to-orange-500/50 hidden md:block z-0" />

                {steps.map((step) => {
                    const Icon = step.icon;
                    return (
                        <div
                            key={step.id}
                            className="w-full bg-zinc-900/30 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-6 flex flex-col items-center text-center transition-all duration-500 relative overflow-hidden group hover:border-brand-coral/50 hover:shadow-[0_0_30px_rgba(216,129,116,0.05)] hover:bg-zinc-900/50 z-10"
                        >
                            {/* Glow effect */}
                            <div className="absolute -top-12 -left-12 w-24 h-24 bg-brand-coral/5 rounded-full filter blur-xl pointer-events-none group-hover:bg-brand-coral/10 transition-all duration-500"></div>

                            {/* Icon Box */}
                            <div className="w-16 h-16 bg-zinc-950 border border-zinc-800 rounded-2xl flex items-center justify-center mb-5 text-zinc-400 group-hover:text-brand-coral group-hover:border-brand-coral group-hover:shadow-[0_0_20px_rgba(216,129,116,0.2)] transition-all duration-500">
                                <Icon className="w-8 h-8 transition-transform duration-500 group-hover:scale-110" />
                            </div>

                            {/* Step Title */}
                            <h4 className="text-zinc-250 group-hover:text-brand-coral font-black text-sm md:text-base tracking-wider uppercase mb-3 transition-colors duration-300">
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
