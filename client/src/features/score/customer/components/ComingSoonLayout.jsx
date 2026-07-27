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
    <div className="rounded-3xl bg-gradient-to-br from-zinc-900 via-zinc-900/90 to-zinc-950 border border-zinc-800 p-8 shadow-2xl relative overflow-hidden">
      {/* Decorative Blur */}
      <div className="absolute -bottom-10 -right-10 w-48 h-48 bg-brand-orange/10 rounded-full filter blur-3xl pointer-events-none" />

      <div className="max-w-xl mb-8 relative z-10">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-orange/10 px-3 py-1 text-xs font-black text-brand-orange border border-brand-orange/20 uppercase tracking-wider mb-3">
          <Sparkles className="h-3.5 w-3.5" />
          Hệ sinh thái điểm thưởng mới
        </span>
        <h3 className="text-2xl font-black text-white tracking-tight">
          Khám phá những đặc quyền sắp ra mắt
        </h3>
        <p className="text-xs text-zinc-400 mt-1 leading-relaxed">
          Chúng tôi đang tiếp tục nâng cấp Trung tâm Khách hàng Thành viên với nhiều tính năng mới giúp tối ưu hóa giá trị mỗi chuyến đi của bạn.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 relative z-10">
        {perks.map((perk, index) => (
          <div
            key={index}
            className="group relative rounded-2xl bg-zinc-800/40 border border-zinc-800/80 p-6 hover:bg-zinc-800/70 hover:border-zinc-700 transition-all duration-300 flex flex-col justify-between"
          >
            <div>
              <div className="h-12 w-12 rounded-2xl bg-zinc-900 flex items-center justify-center mb-4 shadow-inner border border-zinc-800 group-hover:scale-110 transition-transform">
                {perk.icon}
              </div>
              <h4 className="text-base font-black text-white mb-2 group-hover:text-brand-orange transition-colors">
                {perk.title}
              </h4>
              <p className="text-xs text-zinc-400 leading-relaxed mb-6">
                {perk.desc}
              </p>
            </div>
            <div className="pt-4 border-t border-zinc-800/60 flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-500 bg-zinc-900 px-2.5 py-1 rounded-md border border-zinc-800">
                {perk.badge}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
