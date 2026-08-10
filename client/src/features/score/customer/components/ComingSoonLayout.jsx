import { Sparkles, Gift, Tag, Clock } from 'lucide-react';

export default function ComingSoonLayout() {
  const perks = [
    {
      icon: <Gift className="h-6 w-6 text-brand-orange" />,
      title: "Đổi điểm nhận ưu đãi",
      desc: "Sử dụng điểm tích lũy để thanh toán trực tiếp hoặc đổi lấy các voucher giảm giá độc quyền cho tour tiếp theo.",
      badge: "Sắp ra mắt trong Giai đoạn 2"
    },
    {
      icon: <Tag className="h-6 w-6 text-amber-400" />,
      title: "Đặc quyền sinh nhật & Quà tặng",
      desc: "Nhận gấp đôi điểm thưởng vào tháng sinh nhật cùng hàng loạt quà tặng hấp dẫn từ đối tác khách sạn 5 sao.",
      badge: "Sắp ra mắt trong Giai đoạn 2"
    },
    {
      icon: <Clock className="h-6 w-6 text-emerald-400" />,
      title: "Gia hạn & Bảo lưu điểm",
      desc: "Chính sách linh hoạt cho phép bảo lưu và chuyển nhượng điểm thưởng giữa các thành viên trong gia đình.",
      badge: "Sắp ra mắt trong Giai đoạn 2"
    }
  ];

  return (
    <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 shadow-2xl relative overflow-hidden">
      {/* Decorative Blur */}
      <div className="absolute -bottom-20 -right-20 w-72 h-72 bg-brand-orange/5 rounded-full filter blur-3xl pointer-events-none" />

      <div className="max-w-xl mb-10 relative z-10">
        <span className="inline-flex items-center gap-1.5 rounded-md bg-white/5 px-2.5 py-1 text-[9px] font-black text-brand-orange border border-white/10 uppercase tracking-widest mb-4 shadow-sm">
          <Sparkles className="h-3 w-3" />
          Hệ sinh thái điểm thưởng mới
        </span>
        <h3 className="text-2xl md:text-3xl font-black text-white tracking-tight mb-2">
          Khám phá những đặc quyền sắp ra mắt
        </h3>
        <p className="text-xs text-zinc-400 leading-relaxed tracking-wide">
          Chúng tôi đang tiếp tục nâng cấp Trung tâm Khách hàng Thành viên với nhiều tính năng mới giúp tối ưu hóa giá trị mỗi chuyến đi của bạn.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 relative z-10">
        {perks.map((perk, index) => (
          <div
            key={index}
            className="group relative rounded-3xl bg-white/5 border border-white/5 p-6 hover:bg-white/10 hover:border-white/10 transition-all duration-300 flex flex-col justify-between shadow-xl shadow-black/10"
          >
            <div>
              <div className="h-14 w-14 rounded-2xl bg-zinc-950/50 flex items-center justify-center mb-5 shadow-inner border border-zinc-800/50 group-hover:scale-110 transition-transform">
                {perk.icon}
              </div>
              <h4 className="text-sm font-black text-white mb-2 tracking-wide group-hover:text-brand-orange transition-colors">
                {perk.title}
              </h4>
              <p className="text-[11px] text-zinc-400 leading-relaxed mb-6 font-medium">
                {perk.desc}
              </p>
            </div>
            <div className="pt-4 border-t border-zinc-800/40 flex items-center justify-between">
              <span className="text-[9px] font-black uppercase tracking-widest text-zinc-500 bg-black/20 px-2.5 py-1 rounded-md border border-white/5 shadow-inner">
                {perk.badge}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
