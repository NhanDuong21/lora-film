import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapPin, Phone, Clock, Star, Film, ChevronLeft, ChevronRight, HelpCircle } from 'lucide-react';
import { getCinemaBySlug, getShowtimes, getMovies } from '@/features/movies-genres/services/movieService';

const CINEMA_STATIC_DETAILS = {
  'lora-nguyen-du': {
    hotline: "1900 6017",
    hours: "08:00 - 24:00",
    mapUrl: "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3919.4851772635955!2d106.69342777573617!3d10.774105359235887!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3919485177263595%3A0x1c8b368beeeae3df!2zMTE2IE5ndXnhu4VuIER1LCBC4bq_biBUaMOgbmgsIFF14bqtbiAxLCBUaMOgbmggcGjhu5EgSOG7kyBDaMOtIE1pbmgsIFZpZXRuYW0!5e0!3m2!1svi!2s!4v1717000000000!5m2!1svi!2s",
    banners: [
      "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1513106580091-1d82408b8cd6?w=1200&auto=format&fit=crop&q=80"
    ]
  },
  'lora-thao-dien': {
    hotline: "1900 6018",
    hours: "08:30 - 24:00",
    mapUrl: "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3919.203799650058!2d106.75019057573653!3d10.795764058836566!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x31752613d5089c25%3A0xfcf2d3fb89faef!2zVmluY29tIE1lZ2EgTWFsbCBUaOG6o28gxJBp4buBbg!5e0!3m2!1svi!2s!4v1717000000001!5m2!1svi!2s",
    banners: [
      "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=1200&auto=format&fit=crop&q=80"
    ]
  },
  'lora-royal-city': {
    hotline: "1900 6019",
    hours: "09:00 - 24:00",
    mapUrl: "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3724.8967431526435!2d105.81299907602517!3d21.000780280642954!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3135ac9ad89b6b7d%3A0xbcfad5ffb0f49b14!2zVmluY29tIE1lZ2EgTWFsbCBUaOG6o28gxJBp4buBbg!5e0!3m2!1svi!2s!4v1717000000002!5m2!1svi!2s",
    banners: [
      "https://images.unsplash.com/photo-1513106580091-1d82408b8cd6?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=80"
    ]
  }
};

const TICKET_PRICES = [
  { type: "Ghế Thường (2D Digital)", price: "80,000đ" },
  { type: "Ghế VIP", price: "110,000đ" },
  { type: "Ghế Đôi (Couple Sweetheart)", price: "220,000đ" }
];

const ADDONS = [
  { name: "Phụ thu suất chiếu Cuối Tuần (Thứ 6 - Chủ Nhật)", price: "+10,000đ / vé" },
  { name: "Phụ thu công nghệ chiếu Đặc Biệt / IMAX 3D", price: "+30,000đ / vé" }
];

export default function CinemaDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  // Resolve old numeric indices to new slugs
  const resolvedSlug = useMemo(() => {
    if (id === '1') return 'lora-nguyen-du';
    if (id === '2') return 'lora-thao-dien';
    if (id === '3') return 'lora-royal-city';
    return id;
  }, [id]);

  const staticDetails = useMemo(() => {
    return CINEMA_STATIC_DETAILS[resolvedSlug] || CINEMA_STATIC_DETAILS['lora-nguyen-du'];
  }, [resolvedSlug]);

  const [cinema, setCinema] = useState(null);
  const [moviesMap, setMoviesMap] = useState({});
  const [showtimes, setShowtimes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [activeSlide, setActiveSlide] = useState(0);
  const [activeDateIndex, setActiveDateIndex] = useState(0);

  // Generate 4 dates starting from today
  const days = useMemo(() => {
    const weekdays = ["Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
    const list = [];
    for (let i = 0; i < 4; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      const dayName = i === 0 ? "Hôm Nay" : weekdays[d.getDay()];
      const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      const queryStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      list.push({
        label: dayName,
        date: dateStr,
        dateQuery: queryStr
      });
    }
    return list;
  }, []);

  const activeDate = days[activeDateIndex];

  // Carousel timer effect
  useEffect(() => {
    if (!staticDetails || !staticDetails.banners) return;
    const timer = setInterval(() => {
      setActiveSlide(prev => (prev + 1) % staticDetails.banners.length);
    }, 4000);
    return () => clearInterval(timer);
  }, [staticDetails]);

  const handlePrevSlide = () => {
    setActiveSlide(prev => (prev - 1 + staticDetails.banners.length) % staticDetails.banners.length);
  };

  const handleNextSlide = () => {
    setActiveSlide(prev => (prev + 1) % staticDetails.banners.length);
  };

  // Fetch all movies metadata mapping
  const fetchMoviesData = useCallback(async () => {
    try {
      const allMovies = await getMovies({ size: 100 });
      const map = {};
      const dataList = allMovies.data || allMovies.content || [];
      dataList.forEach(m => {
        map[m.publicId] = m;
      });
      setMoviesMap(map);
    } catch (e) {
      console.error("Failed to load movies information:", e);
    }
  }, []);

  // Fetch cinema detail & showtimes
  const loadCinemaAndShowtimes = useCallback(async () => {
    if (!resolvedSlug) return;
    setLoading(true);
    setError(null);
    try {
      const details = await getCinemaBySlug(resolvedSlug);
      setCinema(details);
      
      const showtimeData = await getShowtimes({
        cinemaSlug: resolvedSlug,
        date: activeDate.dateQuery
      });
      setShowtimes(showtimeData.data || showtimeData.content || []);
    } catch (err) {
      setError(err.message || "Không thể tải thông tin cụm rạp.");
    } finally {
      setLoading(false);
    }
  }, [resolvedSlug, activeDate]);

  useEffect(() => {
    fetchMoviesData();
  }, [fetchMoviesData]);

  useEffect(() => {
    loadCinemaAndShowtimes();
  }, [loadCinemaAndShowtimes]);

  // Group showtimes by movie, then sort times chronologically
  const showtimesByMovie = useMemo(() => {
    const grouped = {};
    showtimes.forEach(st => {
      if (!st.movie) return;
      const moviePublicId = st.movie.publicId;
      if (!grouped[moviePublicId]) {
        grouped[moviePublicId] = {
          movie: st.movie,
          formats: {}
        };
      }
      
      const formatLabel = st.movieVersion?.versionName || st.movieVersion?.format || '2D Digital';
      if (!grouped[moviePublicId].formats[formatLabel]) {
        grouped[moviePublicId].formats[formatLabel] = [];
      }
      
      grouped[moviePublicId].formats[formatLabel].push(st);
    });

    // Sort times
    Object.keys(grouped).forEach(movieId => {
      Object.keys(grouped[movieId].formats).forEach(format => {
        grouped[movieId].formats[format].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
      });
    });

    return Object.values(grouped);
  }, [showtimes]);

  const formattedStartTime = (timeString) => {
    const d = new Date(timeString);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
  };

  if (loading && !cinema) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-coral border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải thông tin rạp...</p>
        </div>
      </div>
    );
  }

  if (error || !cinema) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-brand-dark px-4 text-center">
        <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 mb-6">
          <AlertTriangle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-zinc-100 mb-2 font-sans">Không tìm thấy cụm rạp</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">
          {error || "Cụm rạp bạn yêu cầu không khả dụng hoặc đã thay đổi tên."}
        </p>
        <button
          onClick={() => navigate('/')}
          className="bg-brand-coral hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Trở về trang chủ
        </button>
      </div>
    );
  }

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen selection:bg-brand-coral selection:text-zinc-950 font-sans font-medium">
      
      {/* ❖ TOP BANNER: Carousel */}
      <div className="w-full h-[400px] md:h-[500px] relative overflow-hidden group">
        
        {/* Images slide */}
        <div 
          className="w-full h-full flex transition-transform duration-700 ease-in-out" 
          style={{ transform: `translateX(-${activeSlide * 100}%)` }}
        >
          {staticDetails.banners.map((url, idx) => (
            <div key={idx} className="w-full h-full shrink-0 relative">
              <img src={url} alt={`Slide ${idx + 1}`} className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/40 to-transparent" />
            </div>
          ))}
        </div>

        {/* Carousel buttons */}
        <button
          onClick={handlePrevSlide}
          className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/60 hover:bg-brand-coral border border-zinc-800 text-white p-3 rounded-full opacity-0 group-hover:opacity-100 transition-all focus:outline-none cursor-pointer"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <button
          onClick={handleNextSlide}
          className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/60 hover:bg-brand-coral border border-zinc-800 text-white p-3 rounded-full opacity-0 group-hover:opacity-100 transition-all focus:outline-none cursor-pointer"
        >
          <ChevronRight className="w-5 h-5" />
        </button>

        {/* Indicators */}
        <div className="absolute bottom-32 left-1/2 -translate-y-1/2 -translate-x-1/2 flex items-center gap-2 z-20">
          {staticDetails.banners.map((_, idx) => (
            <button
              key={idx}
              onClick={() => setActiveSlide(idx)}
              className={`h-1.5 rounded-full transition-all duration-350 ${
                activeSlide === idx ? 'w-6 bg-brand-coral' : 'w-1.5 bg-white/40'
              }`}
            />
          ))}
        </div>

        {/* Info detail banner overlay */}
        <div className="absolute bottom-6 left-0 w-full z-15 px-6 md:px-12 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="space-y-2">
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-wider text-white drop-shadow-lg">
              {cinema.name}
            </h1>
            <div className="flex items-center gap-2 text-zinc-300 text-xs md:text-sm drop-shadow-md">
              <MapPin className="w-4 h-4 text-brand-coral shrink-0" />
              <span>{cinema.address}</span>
            </div>
          </div>
          <div className="flex items-center gap-6 text-xs md:text-sm text-zinc-300 bg-zinc-950/80 backdrop-blur-md px-5 py-3 rounded-2xl border border-zinc-800 self-start md:self-auto">
            <div className="flex items-center gap-2">
              <Phone className="w-4 h-4 text-brand-coral" />
              <span>Hotline: <strong>{staticDetails.hotline}</strong></span>
            </div>
            <div className="h-4 w-[1px] bg-zinc-800" />
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-brand-coral" />
              <span>{staticDetails.hours}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 md:px-12 py-12 space-y-12">
        
        {/* ❖ MIDDLE LAYER: Showtime Selection Scheduler */}
        <div className="space-y-6">
          <div className="border-b border-zinc-900 pb-4">
            <h2 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Lịch Chiếu Phim</h2>
            <p className="text-[10px] text-zinc-550 font-bold uppercase tracking-wider mt-1">
              Chọn ngày chiếu và suất chiếu để tiến hành chọn vị trí ghế trực tuyến
            </p>
          </div>

          {/* 4-Day Horizontal Tabs */}
          <div className="flex items-center gap-3 overflow-x-auto pb-2 scrollbar-none">
            {days.map((item, idx) => {
              const isActive = activeDateIndex === idx;
              return (
                <button
                  key={idx}
                  onClick={() => setActiveDateIndex(idx)}
                  className={`flex flex-col items-center justify-center min-w-[110px] py-3.5 px-4 rounded-xl border transition-all select-none focus:outline-none cursor-pointer ${
                    isActive 
                      ? 'bg-brand-coral border-brand-coral text-white shadow-lg shadow-brand-coral/25 scale-[1.02]' 
                      : 'bg-zinc-900 border-zinc-850 text-zinc-400 hover:text-white hover:bg-zinc-800'
                  }`}
                >
                  <span className="text-[10px] font-black uppercase tracking-wider">{item.label}</span>
                  <span className="text-sm font-black mt-0.5">{item.date}</span>
                </button>
              );
            })}
          </div>

          {/* Showtimes lists or empty state */}
          {showtimesByMovie.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-4">
              {showtimesByMovie.map(({ movie, formats }) => {
                const info = moviesMap[movie.publicId] || {
                  title: movie.title,
                  primaryPoster: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80',
                  ageRating: 'P',
                  genres: ['Phim']
                };

                return (
                  <div key={movie.publicId} className="bg-zinc-900 border border-zinc-850 hover:border-zinc-800 rounded-3xl p-5 shadow-2xl flex gap-5 group">
                    {/* Poster Slot */}
                    <div 
                      onClick={() => navigate(`/movie/${movie.slug}`)}
                      className="w-24 md:w-28 aspect-[2/3] rounded-2xl overflow-hidden shrink-0 border border-zinc-800 cursor-pointer bg-zinc-950"
                    >
                      <img 
                        src={info.primaryPoster} 
                        alt={movie.title} 
                        className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-300"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80";
                        }}
                      />
                    </div>

                    {/* Showtime Details */}
                    <div className="flex-grow space-y-3.5 overflow-hidden">
                      <div className="space-y-1">
                        <h3 
                          onClick={() => navigate(`/movie/${movie.slug}`)}
                          className="text-xs font-black text-white hover:text-brand-coral transition-colors uppercase tracking-wider cursor-pointer line-clamp-1"
                        >
                          {movie.title}
                        </h3>
                        <div className="flex items-center gap-2">
                          <span className="bg-brand-coral/10 border border-brand-coral/30 text-brand-coral text-[8px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded">
                            {info.ageRating || 'P'}
                          </span>
                          <span className="text-[9px] font-semibold text-zinc-500">
                            {info.genres?.join(', ')}
                          </span>
                        </div>
                      </div>

                      {/* Display lists by format */}
                      <div className="space-y-3 max-h-[140px] overflow-y-auto pr-1">
                        {Object.keys(formats).map((formatLabel, fIdx) => (
                          <div key={fIdx} className="space-y-1">
                            <span className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">
                              {formatLabel}
                            </span>
                            <div className="flex flex-wrap gap-2">
                              {formats[formatLabel].map((st) => (
                                <button
                                  key={st.showtimePublicId}
                                  onClick={() => navigate(`/seat-selection?showtimeId=${st.showtimePublicId}`)}
                                  className="bg-zinc-950 border border-zinc-800 hover:bg-brand-coral hover:border-brand-coral hover:text-white transition-all text-[10px] font-black tracking-wider py-1.5 px-2.5 rounded-lg focus:outline-none cursor-pointer"
                                >
                                  {formattedStartTime(st.startTime)}
                                </button>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-900/40 border border-zinc-850 rounded-2xl max-w-xl mx-auto my-6 space-y-3">
              <div className="w-10 h-10 rounded-full bg-zinc-950 flex items-center justify-center text-zinc-500">
                <Film className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-zinc-300">Không có suất chiếu nào vào ngày này.</h3>
              <p className="text-xs text-zinc-550">
                Vui lòng quay lại hoặc chọn ngày tiếp theo để đặt vé.
              </p>
            </div>
          )}
        </div>

        {/* ❖ BOTTOM LAYER: Ledger Price Table & Location Map */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 py-10 border-t border-zinc-900">
          
          {/* Ticket Price info list */}
          <div className="space-y-5">
            <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
              <Film className="w-5 h-5 text-brand-coral" />
              <h2 className="text-base md:text-lg font-black uppercase tracking-wider text-white">Bảng Giá Vé Rạp</h2>
            </div>

            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-xl">
              <table className="w-full border-collapse text-xs font-semibold">
                <thead>
                  <tr className="bg-zinc-950 text-zinc-400 border-b border-zinc-850">
                    <th className="text-left py-3.5 px-4 font-black uppercase tracking-wider">Loại Ghế</th>
                    <th className="text-right py-3.5 px-4 font-black uppercase tracking-wider">Giá Vé</th>
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

            <div className="bg-zinc-900/40 border border-zinc-850 p-4.5 rounded-2xl space-y-2.5">
              <div className="flex items-center gap-1.5 text-zinc-400 text-[10px] font-black uppercase tracking-wider">
                <HelpCircle className="w-3.5 h-3.5 text-brand-coral" />
                <span>Quy định phụ thu & chính sách</span>
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

          {/* Location details & Dynamic map */}
          <div className="space-y-5">
            <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
              <MapPin className="w-5 h-5 text-brand-coral" />
              <h2 className="text-base md:text-lg font-black uppercase tracking-wider text-white">Vị Trí Cụm Rạp</h2>
            </div>

            <div className="space-y-4">
              <div className="text-xs text-zinc-400 space-y-1 bg-zinc-900/30 border border-zinc-850 p-4 rounded-xl">
                <p>Địa chỉ: <strong className="text-zinc-200 font-bold">{cinema.address}</strong></p>
                <p>Hotline hỗ trợ: <strong className="text-zinc-200 font-bold">{staticDetails.hotline}</strong></p>
                <p>Khung giờ hoạt động: <strong className="text-zinc-200 font-bold">{staticDetails.hours}</strong></p>
              </div>

              <div className="w-full h-64 rounded-2xl overflow-hidden shadow-2xl bg-zinc-900 border border-zinc-800">
                <iframe
                  title={`Bản đồ ${cinema.name}`}
                  src={staticDetails.mapUrl}
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
