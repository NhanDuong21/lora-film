import { useState, useEffect, useMemo } from 'react';
import { MapPin, Phone, Clock, Star, Film, ChevronLeft, ChevronRight, HelpCircle } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

const THEATER_DATA = {
  1: {
    id: 1,
    name: "Lora Nguyen Du",
    address: "116 Nguyễn Du, Bến Thành, Quận 1, TP. Hồ Chí Minh",
    hotline: "1900 6017",
    hours: "08:00 - 24:00",
    mapUrl: "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3919.4851772635955!2d106.69342777573617!3d10.774105359235887!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3919485177263595%3A0x1c8b368beeeae3df!2zMTE2IE5ndXnhu4VuIER1LCBC4bq_biBUaMOgbmgsIFF14bqtbiAxLCBUaMOgbmggcGjhu5EgSOG7kyBDaMOtIE1pbmgsIFZpZXRuYW0!5e0!3m2!1svi!2s!4v1717000000000!5m2!1svi!2s",
    banners: [
      "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1513106580091-1d82408b8cd6?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=1200&auto=format&fit=crop&q=80"
    ]
  },
  2: {
    id: 2,
    name: "Lora Thao Dien",
    address: "Tầng 5, Vincom Mega Mall Thảo Điền, 161 Xa Lộ Hà Nội, Quận 2, TP. Hồ Chí Minh",
    hotline: "1900 6018",
    hours: "08:30 - 24:00",
    mapUrl: "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3919.203799650058!2d106.75019057573653!3d10.795764058836566!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x31752613d5089c25%3A0xfcf2d3fb89faef!2zVmluY29tIE1lZ2EgTWFsbCBUaOG6o28gxJBp4buBbg!5e0!3m2!1svi!2s!4v1717000000001!5m2!1svi!2s",
    banners: [
      "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1513106580091-1d82408b8cd6?w=1200&auto=format&fit=crop&q=80"
    ]
  },
  3: {
    id: 3,
    name: "Lora Royal City",
    address: "Tầng B2, Vincom Mega Mall Royal City, 72A Nguyễn Trãi, Thanh Xuân, Hà Nội",
    hotline: "1900 6019",
    hours: "09:00 - 24:00",
    mapUrl: "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3724.8967431526435!2d105.81299907602517!3d21.000780280642954!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3135ac9ad89b6b7d%3A0xbcfad5ffb0f49b14!2zVmluY29tIE1lZ2EgTWFsbCBUaOG6o28gxJBp4buBbg!5e0!3m2!1svi!2s!4v1717000000002!5m2!1svi!2s",
    banners: [
      "https://images.unsplash.com/photo-1513106580091-1d82408b8cd6?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80"
    ]
  }
};

const TICKET_PRICES = [
  { type: "Ghế Thường (2D Digital)", price: "80,000đ" },
  { type: "Ghế VIP", price: "110,000đ" },
  { type: "Ghế Đôi (Sweetheart Couple)", price: "220,000đ" }
];

const ADDONS = [
  { name: "Phụ thu suất chiếu Cuối Tuần (T6 - CN)", price: "+10,000đ / vé" },
  { name: "Phụ thu công nghệ chiếu 3D/IMAX", price: "+30,000đ / vé" }
];

const FIXED_SHOWTIMES = ["09:30", "13:15", "16:45", "19:30", "22:15"];

export default function CinemaDetailView({ cinemaId, onBookTicket }) {
  const { movies, cinemas } = useData();
  const currentCinemaId = cinemaId || 'c1';

  const cinema = useMemo(() => {
    let found = cinemas.find(c => String(c.id) === String(currentCinemaId));
    if (!found) {
      if (currentCinemaId === 1 || currentCinemaId === '1' || currentCinemaId === 'c1') found = cinemas.find(c => c.name.includes("Nguyễn Du") || c.name.includes("Nguyen Du") || c.id === 'c1');
      else if (currentCinemaId === 2 || currentCinemaId === '2' || currentCinemaId === 'c2') found = cinemas.find(c => c.name.includes("Thảo Điền") || c.name.includes("Thao Dien") || c.id === 'c2');
      else if (currentCinemaId === 3 || currentCinemaId === '3' || currentCinemaId === 'c3') found = cinemas.find(c => c.name.includes("Royal City") || c.id === 'c3');
    }
    const resolved = found || cinemas[0];
    
    // Merge standard properties (hotline, hours, mapUrl, banners) from static THEATER_DATA
    const staticId = resolved.id === 'c1' ? 1 : resolved.id === 'c2' ? 2 : resolved.id === 'c3' ? 3 : 1;
    const staticDetails = THEATER_DATA[staticId] || THEATER_DATA[1];
    
    return {
      ...resolved,
      hotline: staticDetails.hotline,
      hours: staticDetails.hours,
      mapUrl: staticDetails.mapUrl,
      banners: staticDetails.banners
    };
  }, [cinemas, currentCinemaId]);

  // Carousel Active index state
  const [activeSlide, setActiveSlide] = useState(0);

  // 4-Day Calendar dynamic states
  const days = useMemo(() => {
    const weekdays = ["Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
    const list = [];
    for (let i = 0; i < 4; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      const dayName = i === 0 ? "Hôm Nay" : weekdays[d.getDay()];
      const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      const fullDateStr = d.toLocaleDateString('vi-VN');
      list.push({
        label: dayName,
        date: dateStr,
        fullDate: fullDateStr
      });
    }
    return list;
  }, []);

  const [activeDateIndex, setActiveDateIndex] = useState(0);
  const activeDate = days[activeDateIndex];

  // 1. Infinite 3-Second Automatic Banner Carousel Effect
  useEffect(() => {
    const timer = setInterval(() => {
      setActiveSlide(prev => (prev + 1) % cinema.banners.length);
    }, 3000);
    return () => clearInterval(timer);
  }, [cinema.banners.length]);

  const handlePrevSlide = () => {
    setActiveSlide(prev => (prev - 1 + cinema.banners.length) % cinema.banners.length);
  };

  const handleNextSlide = () => {
    setActiveSlide(prev => (prev + 1) % cinema.banners.length);
  };

  // Traverses mock movies and returns only NOW_SHOWING list
  const activeMovies = useMemo(() => {
    return movies.filter(m => m.status === 'NOW_SHOWING' || m.status === 'DANG_CHIEU');
  }, [movies]);

  // Direct trigger booking flow
  const handleSelectShowtime = (movie, time) => {
    const bookingPayload = {
      movieId: movie.id,
      movieTitle: movie.title,
      cinema: cinema.name,
      time: time,
      format: '2D DIGITAL',
      date: activeDate.date,
      fullDate: activeDate.fullDate,
      selectedSeats: []
    };
    onBookTicket(bookingPayload);
  };

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen">
      
      {/* ❖ TOP LAYER: Infinite 3-Second Automatic Banner Carousel */}
      <div className="w-full h-[400px] md:h-[500px] relative overflow-hidden group">
        
        {/* Banner Images Track */}
        <div className="w-full h-full flex transition-transform duration-700 ease-in-out" style={{ transform: `translateX(-${activeSlide * 100}%)` }}>
          {cinema.banners.map((url, idx) => (
            <div key={idx} className="w-full h-full shrink-0 relative">
              <img src={url} alt={`Slide ${idx + 1}`} className="w-full h-full object-cover" />
              {/* Dark overlay strip */}
              <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/40 to-transparent" />
            </div>
          ))}
        </div>

        {/* Manual Slideshow Controls */}
        <button
          onClick={handlePrevSlide}
          className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/60 hover:bg-orange-500 border border-zinc-800 text-white p-3 rounded-full opacity-0 group-hover:opacity-100 transition-all focus:outline-none"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <button
          onClick={handleNextSlide}
          className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/60 hover:bg-orange-500 border border-zinc-800 text-white p-3 rounded-full opacity-0 group-hover:opacity-100 transition-all focus:outline-none"
        >
          <ChevronRight className="w-5 h-5" />
        </button>

        {/* Carousel Indicators */}
        <div className="absolute bottom-32 left-1/2 -translate-y-1/2 -translate-x-1/2 flex items-center gap-2 z-20">
          {cinema.banners.map((_, idx) => (
            <button
              key={idx}
              onClick={() => setActiveSlide(idx)}
              className={`h-1.5 rounded-full transition-all duration-350 ${
                activeSlide === idx ? 'w-6 bg-orange-500' : 'w-1.5 bg-white/40'
              }`}
            />
          ))}
        </div>

        {/* Theater overlay details headline panel */}
        <div className="absolute bottom-6 left-0 w-full z-15 px-6 md:px-12 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="space-y-2">
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-wider text-white drop-shadow-lg">
              {cinema.name}
            </h1>
            <div className="flex items-center gap-2 text-zinc-300 text-xs md:text-sm drop-shadow-md">
              <MapPin className="w-4 h-4 text-orange-500 shrink-0" />
              <span>{cinema.address}</span>
            </div>
          </div>
          <div className="flex items-center gap-6 text-xs md:text-sm text-zinc-300 bg-zinc-950/80 backdrop-blur-md px-5 py-3 rounded-2xl border border-zinc-800 self-start md:self-auto">
            <div className="flex items-center gap-2">
              <Phone className="w-4 h-4 text-orange-500" />
              <span>Hotline: <strong>{cinema.hotline}</strong></span>
            </div>
            <div className="h-4 w-[1px] bg-zinc-800" />
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-orange-500" />
              <span>{cinema.hours}</span>
            </div>
          </div>
        </div>

      </div>

      <div className="max-w-7xl mx-auto px-6 md:px-12 py-12 space-y-12">
        
        {/* ❖ MIDDLE LAYER: Interactive 4-Day Movie Showtime Scheduler System */}
        <div className="space-y-6">
          
          <div className="border-b border-zinc-900 pb-4">
            <h2 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Lịch Chiếu Phim</h2>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
              Chọn suất chiếu và ngày chiếu để bắt đầu đặt vé trực tiếp
            </p>
          </div>

          {/* Interactive Date Tabs modeled after image_b279e2.jpg */}
          <div className="flex items-center gap-3 overflow-x-auto pb-2 scrollbar-none">
            {days.map((item, idx) => {
              const isActive = activeDateIndex === idx;
              return (
                <button
                  key={idx}
                  onClick={() => setActiveDateIndex(idx)}
                  className={`flex flex-col items-center justify-center min-w-[110px] py-3.5 px-4 rounded-xl border transition-all select-none focus:outline-none cursor-pointer ${
                    isActive 
                      ? 'bg-blue-600 border-blue-500 text-white shadow-lg shadow-blue-600/25 scale-[1.02]' 
                      : 'bg-zinc-900 border-zinc-850 text-zinc-400 hover:text-white hover:bg-zinc-800'
                  }`}
                >
                  <span className="text-[10px] font-black uppercase tracking-wider">{item.label}</span>
                  <span className="text-sm font-black mt-0.5">{item.date}</span>
                </button>
              );
            })}
          </div>

          {/* Dynamic Grid Traversal mapping catalogs */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-4">
            {activeMovies.map((movie) => (
              <div key={movie.id} className="bg-zinc-900 border border-zinc-850 hover:border-zinc-800 rounded-3xl p-5 shadow-2xl flex gap-5 group">
                
                {/* Poster Slot */}
                <div 
                  onClick={() => handleSelectShowtime(movie, "19:30")}
                  className="w-24 md:w-28 aspect-[2/3] rounded-2xl overflow-hidden shrink-0 border border-zinc-800 cursor-pointer bg-zinc-950"
                >
                  <img src={movie.posterUrl || movie.image} alt={movie.title} className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-300" />
                </div>

                {/* Showtime Details */}
                <div className="flex-grow space-y-3.5">
                  <div className="space-y-1">
                    <h3 
                      onClick={() => handleSelectShowtime(movie, "19:30")}
                      className="text-xs font-black text-white hover:text-orange-500 transition-colors uppercase tracking-wider cursor-pointer line-clamp-1"
                    >
                      {movie.title}
                    </h3>
                    <div className="flex items-center gap-2">
                      <span className="bg-orange-500/10 border border-orange-500/30 text-orange-500 text-[8px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded">
                        {movie.ageRating || 'T16'}
                      </span>
                      <div className="flex items-center gap-0.5">
                        <Star className="w-3 h-3 fill-brand-yellow text-brand-yellow" />
                        <span className="text-[9px] font-bold text-zinc-400">{movie.rating}</span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <span className="text-[9px] text-zinc-555 font-black uppercase tracking-wider block">Suất Chiếu 2D Digital:</span>
                    <div className="flex flex-wrap gap-2">
                      {FIXED_SHOWTIMES.map((time, tIdx) => (
                        <button
                          key={tIdx}
                          onClick={() => handleSelectShowtime(movie, time)}
                          className="bg-zinc-950 border border-zinc-800 hover:bg-orange-500 hover:border-orange-500 hover:text-white transition-all text-[10px] font-black tracking-wider py-2 px-3 rounded-lg focus:outline-none"
                        >
                          {time}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

              </div>
            ))}
          </div>

        </div>

        {/* ❖ BOTTOM LAYER: Asymmetric Split Footer Information Matrix */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 py-10 border-t border-zinc-850">
          
          {/* Column 1: Ticket Pricing Ledger */}
          <div className="space-y-5">
            <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
              <Film className="w-5 h-5 text-orange-500" />
              <h2 className="text-base md:text-lg font-black uppercase tracking-wider text-white">Bảng Giá Vé Lora</h2>
            </div>

            {/* Standard Ticket Pricing Table */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-xl">
              <table className="w-full border-collapse text-xs font-semibold">
                <thead>
                  <tr className="bg-zinc-950 text-zinc-400 border-b border-zinc-850">
                    <th className="text-left py-3.5 px-4 font-black uppercase tracking-wider">Loại Ghế (2D Digital)</th>
                    <th className="text-right py-3.5 px-4 font-black uppercase tracking-wider">Đơn Giá</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-850 text-zinc-200">
                  {TICKET_PRICES.map((item, idx) => (
                    <tr key={idx} className="hover:bg-zinc-950/20 transition-colors">
                      <td className="py-3.5 px-4 font-bold text-zinc-300">{item.type}</td>
                      <td className="text-right py-3.5 px-4 font-black text-amber-500">{item.price}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Addons and Surcharges List */}
            <div className="bg-zinc-900/40 border border-zinc-900 p-4.5 rounded-2xl space-y-2.5">
              <div className="flex items-center gap-1.5 text-zinc-400 text-[10px] font-black uppercase tracking-wider">
                <HelpCircle className="w-3.5 h-3.5 text-orange-500" />
                <span>Quy định phụ thu vé</span>
              </div>
              <ul className="space-y-2 text-xs text-zinc-400">
                {ADDONS.map((addon, aIdx) => (
                  <li key={aIdx} className="flex justify-between">
                    <span>{addon.name}</span>
                    <strong className="text-zinc-300 font-bold">{addon.price}</strong>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Column 2: Cinema Profile Dossier & Embedded Maps */}
          <div className="space-y-5">
            <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
              <MapPin className="w-5 h-5 text-orange-500" />
              <h2 className="text-base md:text-lg font-black uppercase tracking-wider text-white">Vị Trí Rạp Hợp Tác</h2>
            </div>

            <div className="space-y-4">
              <div className="text-xs text-zinc-400 space-y-1 bg-zinc-900/30 border border-zinc-900 p-4 rounded-xl">
                <p>Địa chỉ: <strong className="text-zinc-200 font-bold">{cinema.address}</strong></p>
                <p>Điện thoại liên hệ: <strong className="text-zinc-200 font-bold">{cinema.hotline}</strong></p>
                <p>Khung giờ phục vụ: <strong className="text-zinc-200 font-bold">{cinema.hours}</strong></p>
              </div>

              {/* Embedded Location Map iframe pointing directly to actual coordinates */}
              <div className="w-full h-64 rounded-2xl overflow-hidden shadow-2xl bg-zinc-900 border border-zinc-800">
                <iframe
                  title={`Bản đồ ${cinema.name}`}
                  src={cinema.mapUrl}
                  className="w-full h-full border-0"
                  allowFullScreen=""
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>
  );
}
