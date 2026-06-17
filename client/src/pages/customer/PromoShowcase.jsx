import { ArrowRight, Clock, Tag } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function PromoShowcase({ onNavigate }) {
  const { events } = useData();

  const featuredEvent = events && events.length > 0 ? events[0] : {
    id: "e1",
    title: "Thứ Hai Vui Vẻ - Đồng Giá Vé 60K",
    dateRange: "Đến 31/12/2026",
    rewardDetails: "Cơ hội thưởng thức phim bom tấn với giá cực hời mỗi ngày thứ ba hàng tuần tại hệ thống LoraFilm."
  };

  const miniEvent1 = events && events.length > 1 ? events[1] : {
    id: "e2",
    title: "Thành Viên Vàng LoraFilm - Nhân Đôi Điểm Tích Lũy",
    dateRange: "Đến 30/06/2026",
    rewardDetails: "Nhận x2 điểm thưởng khi mua vé online trong suốt tháng 6."
  };

  const miniEvent2 = events && events.length > 2 ? events[2] : {
    id: "e3",
    title: "Combo Bắp Nước Siêu Anh Hùng - Tặng Bình Nước",
    dateRange: "Đến 31/12/2026",
    rewardDetails: "Nhận bình nước độc quyền khi mua Combo phim bom tấn."
  };

  const handleFeatureClick = () => {
    if (onNavigate) {
      onNavigate('event-detail', { eventId: featuredEvent.id });
    }
  };

  const handleMiniClick = (eventId) => {
    if (onNavigate) {
      onNavigate('event-detail', { eventId });
    }
  };

  const handleSeeAllClick = (e) => {
    e.preventDefault();
    if (onNavigate) {
      onNavigate('events', null);
    }
  };

  return (
    <section className="w-full bg-zinc-950 text-zinc-100 py-16 border-t border-b border-zinc-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header Layout */}
        <div className="flex justify-between items-center pb-4 border-b border-zinc-900/80 mb-6">
          <h2 className="text-lg md:text-xl font-black uppercase tracking-wider text-white">
            SỰ KIỆN & ƯU ĐÃI HOT
          </h2>
          <button
            onClick={handleSeeAllClick}
            className="text-xs font-bold text-zinc-400 hover:text-amber-500 transition-colors flex items-center gap-1 group"
          >
            <span>Xem tất cả</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>

        {/* Asymmetric Promotional Matrix Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
          
          {/* LEFT CONTENT ZONE: The Feature Event Block (2/3 Width Space) */}
          <div
            onClick={handleFeatureClick}
            className="lg:col-span-2 relative overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 aspect-[16/9] md:aspect-[21/9] cursor-pointer group"
          >
            {/* Featured Image */}
            <img
              src="https://images.unsplash.com/photo-1513151233558-d860c5398176?w=800&auto=format&fit=crop&q=80"
              alt={featuredEvent.title}
              className="w-full h-full object-cover group-hover:scale-[1.02] transition-transform duration-500"
            />
            
            {/* Deep bottom gradient overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-black via-black/45 to-transparent z-10 flex flex-col justify-end p-6 md:p-8">
              <div className="flex flex-wrap items-center gap-3 mb-2">
                <span className="bg-brand-coral text-white text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full flex items-center gap-1">
                  <Tag className="w-3 h-3" />
                  Khuyến mãi mới
                </span>
                <span className="text-[10px] text-zinc-300 font-bold flex items-center gap-1 bg-black/40 px-2 py-0.5 rounded">
                  <Clock className="w-3 h-3 text-brand-coral" />
                  {featuredEvent.dateRange}
                </span>
              </div>
              
              <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
                <div>
                  <h3 className="text-lg md:text-2xl font-black text-white uppercase tracking-tight group-hover:text-amber-500 transition-colors">
                    {featuredEvent.title}
                  </h3>
                  <p className="text-xs text-zinc-300 mt-1 max-w-xl line-clamp-1">
                    {featuredEvent.rewardDetails}
                  </p>
                </div>
                
                {/* Orange floating capsule pill button */}
                <span className="bg-orange-500 text-white text-xs font-black uppercase tracking-wider px-5 py-2.5 rounded-full shadow-lg shadow-orange-500/20 group-hover:bg-orange-600 transition-colors whitespace-nowrap self-start md:self-auto">
                  Nhận Ưu Đãi
                </span>
              </div>
            </div>
          </div>

          {/* RIGHT CONTENT ZONE: The Stacked Mini-Promos Column (1/3 Width Space) */}
          <div className="lg:col-span-1 flex flex-col gap-4 h-full justify-between min-h-[300px] lg:min-h-0">
            
            {/* Mini Card 1 - Member Promo (ID: e2) */}
            <div
              onClick={() => handleMiniClick(miniEvent1.id)}
              className="w-full h-[47%] bg-zinc-900/50 hover:bg-zinc-900 border border-zinc-800/60 hover:border-amber-500/40 rounded-xl p-3 flex gap-4 cursor-pointer transition-all group"
            >
              {/* Left Thumbnail */}
              <div className="w-24 h-full shrink-0 overflow-hidden rounded-lg bg-zinc-850">
                <img
                  src="https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400&auto=format&fit=crop&q=80"
                  alt={miniEvent1.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-350"
                />
              </div>
              {/* Right content panel */}
              <div className="flex-grow flex flex-col justify-between min-w-0">
                <div>
                  <h4 className="text-xs md:text-sm font-black text-white line-clamp-2 uppercase tracking-tight group-hover:text-amber-500 transition-colors">
                    {miniEvent1.title}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-1 line-clamp-1">
                    {miniEvent1.rewardDetails}
                  </p>
                </div>
                
                <div className="flex justify-between items-center pt-2">
                  <span className="text-[9px] text-zinc-500 font-bold">{miniEvent1.dateRange}</span>
                  <span className="text-[10px] font-black text-orange-500 uppercase flex items-center gap-0.5 group-hover:text-orange-400">
                    <span>Chi tiết</span>
                    <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
                  </span>
                </div>
              </div>
            </div>

            {/* Mini Card 2 - Combo Promo (ID: e3) */}
            <div
              onClick={() => handleMiniClick(miniEvent2.id)}
              className="w-full h-[47%] bg-zinc-900/50 hover:bg-zinc-900 border border-zinc-800/60 hover:border-amber-500/40 rounded-xl p-3 flex gap-4 cursor-pointer transition-all group"
            >
              {/* Left Thumbnail */}
              <div className="w-24 h-full shrink-0 overflow-hidden rounded-lg bg-zinc-850">
                <img
                  src="https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400&auto=format&fit=crop&q=80"
                  alt={miniEvent2.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-350"
                />
              </div>
              {/* Right content panel */}
              <div className="flex-grow flex flex-col justify-between min-w-0">
                <div>
                  <h4 className="text-xs md:text-sm font-black text-white line-clamp-2 uppercase tracking-tight group-hover:text-amber-500 transition-colors">
                    {miniEvent2.title}
                  </h4>
                  <p className="text-[10px] text-zinc-400 mt-1 line-clamp-1">
                    {miniEvent2.rewardDetails}
                  </p>
                </div>
                
                <div className="flex justify-between items-center pt-2">
                  <span className="text-[9px] text-zinc-500 font-bold">{miniEvent2.dateRange}</span>
                  <span className="text-[10px] font-black text-orange-500 uppercase flex items-center gap-0.5 group-hover:text-orange-400">
                    <span>Chi tiết</span>
                    <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
                  </span>
                </div>
              </div>
            </div>

          </div>

        </div>

      </div>
    </section>
  );
}
