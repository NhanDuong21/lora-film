import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapPin, Phone, Clock, Film, ChevronLeft, ChevronRight, HelpCircle, AlertTriangle, ExternalLink } from 'lucide-react';
import {
  getCinemaBySlug,
  getMovies,
  getSeatLayout,
  getShowtimes
} from '@/features/catalog/customer/services/movieService';
import { seatSelectionPath } from '@/features/catalog/customer/utils/customerMovieFlow';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import {
  buildCinemaMap,
  DAY_LABELS,
  formatOperatingHour,
  formatTicketPrice,
  getCinemaImages,
  getCurrentOperatingHour,
  summarizeTicketPrices
} from '@/features/facilities/customer/utils/cinemaPresentation';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='600' height='900'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop stop-color='%2327272a'/><stop offset='1' stop-color='%2309090b'/></linearGradient></defs><rect width='100%25' height='100%25' fill='url(%23g)'/><text x='50%25' y='48%25' text-anchor='middle' fill='%23ff7a00' font-family='sans-serif' font-size='52' font-weight='700'>LoraFilm</text><text x='50%25' y='54%25' text-anchor='middle' fill='%23a1a1aa' font-family='sans-serif' font-size='24'>Đang cập nhật poster</text></svg>";

export default function CinemaDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [cinema, setCinema] = useState(null);
  const [moviesMap, setMoviesMap] = useState({});
  const [showtimes, setShowtimes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showtimesLoading, setShowtimesLoading] = useState(false);
  const [priceLoading, setPriceLoading] = useState(false);
  const [ticketPrices, setTicketPrices] = useState([]);
  const [priceError, setPriceError] = useState('');
  const [pricedShowtimeCount, setPricedShowtimeCount] = useState(0);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  
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
  const cinemaImages = useMemo(() => getCinemaImages(cinema), [cinema]);
  const currentOperatingHour = useMemo(() => getCurrentOperatingHour(cinema), [cinema]);
  const cinemaMap = useMemo(() => buildCinemaMap(cinema), [cinema]);
  const operatingHours = useMemo(
    () => [...(cinema?.operatingHours || [])].sort(
      (left, right) => Number(left.dayOfWeek) - Number(right.dayOfWeek)
    ),
    [cinema]
  );

  // Carousel timer effect
  useEffect(() => {
    if (cinemaImages.length <= 1) return undefined;
    const timer = setInterval(() => {
      setActiveSlide(prev => (prev + 1) % cinemaImages.length);
    }, 4000);
    return () => clearInterval(timer);
  }, [cinemaImages]);

  const handlePrevSlide = () => {
    setActiveSlide(prev => (prev - 1 + cinemaImages.length) % cinemaImages.length);
  };

  const handleNextSlide = () => {
    setActiveSlide(prev => (prev + 1) % cinemaImages.length);
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
      setNotice({
        title: 'Thiếu một phần thông tin phim',
        message: 'Một số tên hoặc hình ảnh phim có thể chưa hiển thị. Vui lòng thử lại sau.',
        variant: 'warning'
      });
    }
  }, []);

  const loadCinema = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const details = await getCinemaBySlug(id);
      setCinema(details);
      setActiveSlide(0);
    } catch (err) {
      setCinema(null);
      setError(getCustomerErrorMessage(
        err,
        'Không thể tải thông tin cụm rạp. Vui lòng thử lại.'
      ));
    } finally {
      setLoading(false);
    }
  }, [id]);

  const loadShowtimesAndPrices = useCallback(async () => {
    if (!cinema?.slug || !activeDate?.dateQuery) return;

    setShowtimesLoading(true);
    setPriceLoading(true);
    setPriceError('');
    setTicketPrices([]);
    setPricedShowtimeCount(0);

    try {
      const showtimeData = await getShowtimes({
        cinemaSlug: cinema.slug,
        date: activeDate.dateQuery,
        page: 0,
        size: 100
      });
      const showtimeList = showtimeData?.data || showtimeData?.content || [];
      setShowtimes(showtimeList);
      setShowtimesLoading(false);

      if (showtimeList.length === 0) {
        setPriceError('Chưa có suất chiếu mở bán để hiển thị giá cho ngày này.');
        return;
      }

      const pricedShowtimes = showtimeList
        .filter(showtime => showtime?.showtimePublicId)
        .slice(0, 20);
      const layoutResults = await Promise.allSettled(
        pricedShowtimes.map(showtime => getSeatLayout(showtime.showtimePublicId))
      );
      const layouts = layoutResults
        .filter(result => result.status === 'fulfilled' && result.value)
        .map(result => result.value);
      const priceSummary = summarizeTicketPrices(layouts);

      setTicketPrices(priceSummary);
      setPricedShowtimeCount(layouts.length);
      if (priceSummary.length === 0) {
        setPriceError('Các suất chiếu ngày này chưa có dữ liệu giá vé khả dụng.');
      }
    } catch (err) {
      setShowtimes([]);
      setPriceError('Không thể tải giá vé ở thời điểm hiện tại.');
      setNotice({
        title: 'Không thể tải lịch chiếu',
        message: getCustomerErrorMessage(err, 'Lịch chiếu hiện chưa khả dụng. Vui lòng thử lại sau.'),
        variant: 'warning'
      });
    } finally {
      setShowtimesLoading(false);
      setPriceLoading(false);
    }
  }, [activeDate, cinema]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMoviesData();
  }, [fetchMoviesData]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadCinema();
  }, [loadCinema]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadShowtimesAndPrices();
  }, [loadShowtimesAndPrices]);

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
          <div className="w-12 h-12 border-4 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
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
          className="bg-brand-orange hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Trở về trang chủ
        </button>
      </div>
    );
  }

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      {notice && (
        <CustomerNoticeModal
          title={notice.title}
          message={notice.message}
          variant={notice.variant}
          onClose={() => setNotice(null)}
        />
      )}
      
      {/* ❖ TOP BANNER: Carousel */}
      <div className="w-full h-[400px] md:h-[500px] relative overflow-hidden group">
        
        {cinemaImages.length > 0 ? (
          <div
            className="flex h-full w-full transition-transform duration-700 ease-in-out"
            style={{ transform: `translateX(-${activeSlide * 100}%)` }}
          >
            {cinemaImages.map((media, idx) => (
              <div key={media.publicId || media.url} className="relative h-full w-full shrink-0">
                <img
                  src={media.url}
                  alt={media.title || `${cinema.name} - hình ${idx + 1}`}
                  className="h-full w-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/40 to-transparent" />
              </div>
            ))}
          </div>
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-zinc-900 via-zinc-950 to-black">
            <div className="flex flex-col items-center gap-3 text-zinc-600">
              <Film className="h-14 w-14" />
              <span className="text-sm font-bold">Rạp chưa cập nhật hình ảnh</span>
            </div>
            <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-transparent to-transparent" />
          </div>
        )}

        {cinemaImages.length > 1 && (
          <>
            <button
              type="button"
              aria-label="Ảnh rạp trước"
              onClick={handlePrevSlide}
              className="absolute left-4 top-1/2 -translate-y-1/2 rounded-full border border-zinc-800 bg-black/60 p-3 text-white opacity-0 transition-all hover:bg-brand-orange focus:outline-none group-hover:opacity-100"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <button
              type="button"
              aria-label="Ảnh rạp tiếp theo"
              onClick={handleNextSlide}
              className="absolute right-4 top-1/2 -translate-y-1/2 rounded-full border border-zinc-800 bg-black/60 p-3 text-white opacity-0 transition-all hover:bg-brand-orange focus:outline-none group-hover:opacity-100"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
            <div className="absolute bottom-32 left-1/2 z-20 flex -translate-x-1/2 -translate-y-1/2 items-center gap-2">
              {cinemaImages.map((media, idx) => (
                <button
                  key={media.publicId || media.url}
                  type="button"
                  aria-label={`Xem hình rạp ${idx + 1}`}
                  onClick={() => setActiveSlide(idx)}
                  className={`h-1.5 rounded-full transition-all ${
                    activeSlide === idx ? 'w-6 bg-brand-orange' : 'w-1.5 bg-white/40'
                  }`}
                />
              ))}
            </div>
          </>
        )}

        {/* Info detail banner overlay */}
        <div className="absolute bottom-6 left-0 w-full z-15 px-6 md:px-12 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-3xl md:text-5xl font-black uppercase tracking-wider text-white drop-shadow-lg">
                {cinema.name}
              </h1>
              {cinema.status === 'TEMPORARILY_CLOSED' && (
                <span className="rounded-full border border-amber-400/30 bg-amber-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-amber-300">
                  Tạm ngưng hoạt động
                </span>
              )}
            </div>
            <div className="flex items-center gap-2 text-zinc-300 text-xs md:text-sm drop-shadow-md">
              <MapPin className="w-4 h-4 text-brand-orange shrink-0" />
              <span>{cinema.address}</span>
            </div>
          </div>
          <div className="flex items-center gap-6 text-xs md:text-sm text-zinc-300 bg-zinc-950/80 backdrop-blur-md px-5 py-3 rounded-2xl border border-zinc-800 self-start md:self-auto">
            <div className="flex items-center gap-2">
              <Phone className="w-4 h-4 text-brand-orange" />
              <span>Hotline: <strong>{cinema.hotline || 'Chưa cập nhật'}</strong></span>
            </div>
            <div className="h-4 w-[1px] bg-zinc-800" />
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-brand-orange" />
              <span>{formatOperatingHour(currentOperatingHour)}</span>
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
                      ? 'bg-brand-orange border-brand-orange text-white shadow-lg shadow-brand-orange/25 scale-[1.02]' 
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
          {showtimesLoading ? (
            <div className="flex items-center justify-center gap-3 py-16 text-sm font-semibold text-zinc-500">
              <span className="h-6 w-6 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
              Đang tải lịch chiếu...
            </div>
          ) : showtimesByMovie.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-4">
              {showtimesByMovie.map(({ movie, formats }) => {
                const info = moviesMap[movie.publicId] || {
                  title: movie.title,
                  ageRating: 'P',
                  genres: ['Phim']
                };
                const posterUrl = info.posterUrl || info.primaryPoster || movie.posterUrl;
                const moviePath = `/movies/${movie.slug || movie.publicId}`;
                const moviePreview = {
                  ...info,
                  ...movie,
                  primaryPoster: posterUrl,
                  posterUrl
                };
                const openMovieDetail = () => navigate(moviePath, {
                  state: { moviePreview }
                });
                const genreText = (info.genres || [])
                  .map(genre => typeof genre === 'string' ? genre : genre?.name)
                  .filter(Boolean)
                  .join(', ');

                return (
                  <div key={movie.publicId} className="bg-zinc-900 border border-zinc-850 hover:border-zinc-800 rounded-3xl p-5 shadow-2xl flex gap-5 group">
                    {/* Poster Slot */}
                    <div 
                      onClick={openMovieDetail}
                      className="w-24 md:w-28 aspect-[2/3] rounded-2xl overflow-hidden shrink-0 border border-zinc-800 cursor-pointer bg-zinc-950"
                    >
                      <img
                        src={posterUrl || FALLBACK_POSTER}
                        alt={movie.title}
                        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
                        onError={event => {
                          event.currentTarget.onerror = null;
                          event.currentTarget.src = FALLBACK_POSTER;
                        }}
                      />
                    </div>

                    {/* Showtime Details */}
                    <div className="flex-grow space-y-3.5 overflow-hidden">
                      <div className="space-y-1">
                        <h3 
                          onClick={openMovieDetail}
                          className="text-xs font-black text-white hover:text-brand-orange transition-colors uppercase tracking-wider cursor-pointer line-clamp-1"
                        >
                          {movie.title}
                        </h3>
                        <div className="flex items-center gap-2">
                          <span className="bg-brand-orange/10 border border-brand-orange/30 text-brand-orange text-[8px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded">
                            {info.ageRating || 'P'}
                          </span>
                          <span className="text-[9px] font-semibold text-zinc-500">
                            {genreText || 'Đang cập nhật thể loại'}
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
                                  onClick={() => navigate(seatSelectionPath(st.showtimePublicId))}
                                  className="bg-zinc-950 border border-zinc-800 hover:bg-brand-orange hover:border-brand-orange hover:text-white transition-all text-[10px] font-black tracking-wider py-1.5 px-2.5 rounded-lg focus:outline-none cursor-pointer"
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
              <Film className="w-5 h-5 text-brand-orange" />
              <div>
                <h2 className="text-base md:text-lg font-black uppercase tracking-wider text-white">Bảng Giá Vé Rạp</h2>
                <p className="mt-0.5 text-[10px] font-bold uppercase tracking-wider text-zinc-500">
                  Giá thực tế của các suất chiếu ngày {activeDate.date}
                </p>
              </div>
            </div>

            {priceLoading ? (
              <div className="flex min-h-44 items-center justify-center gap-3 rounded-2xl border border-zinc-850 bg-zinc-900 text-sm font-semibold text-zinc-500">
                <span className="h-5 w-5 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
                Đang tổng hợp giá vé...
              </div>
            ) : ticketPrices.length > 0 ? (
              <div className="overflow-hidden rounded-2xl border border-zinc-850 bg-zinc-900 shadow-xl">
                <table className="w-full border-collapse text-xs font-semibold">
                  <thead>
                    <tr className="border-b border-zinc-850 bg-zinc-950 text-zinc-400">
                      <th className="px-4 py-3.5 text-left font-black uppercase tracking-wider">Loại Ghế</th>
                      <th className="px-4 py-3.5 text-right font-black uppercase tracking-wider">Giá Vé</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-850 text-zinc-200">
                    {ticketPrices.map(item => (
                      <tr key={`${item.code}-${item.currency}`} className="transition-colors hover:bg-zinc-950/20">
                        <td className="px-4 py-3.5 font-bold text-zinc-300">
                          {item.name}
                          <span className="ml-2 text-[9px] font-bold uppercase text-zinc-600">{item.code}</span>
                        </td>
                        <td className="px-4 py-3.5 text-right font-black text-amber-500">
                          {formatTicketPrice(item)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="flex min-h-44 flex-col items-center justify-center gap-2 rounded-2xl border border-zinc-850 bg-zinc-900/60 px-6 text-center">
                <Film className="h-7 w-7 text-zinc-700" />
                <p className="text-sm font-bold text-zinc-400">{priceError}</p>
              </div>
            )}

            <div className="space-y-2.5 rounded-2xl border border-zinc-850 bg-zinc-900/40 p-4">
              <div className="flex items-center gap-1.5 text-zinc-400 text-[10px] font-black uppercase tracking-wider">
                <HelpCircle className="w-3.5 h-3.5 text-brand-orange" />
                <span>Nguồn dữ liệu giá vé</span>
              </div>
              <p className="text-xs leading-relaxed text-zinc-400">
                Giá được lấy trực tiếp từ {pricedShowtimeCount} suất chiếu đang mở bán của rạp.
                Nếu một loại ghế có nhiều mức giá, hệ thống hiển thị khoảng thấp nhất – cao nhất.
                Giá cuối cùng được xác nhận khi bạn chọn suất chiếu và ghế.
              </p>
            </div>
          </div>

          {/* Location details & Dynamic map */}
          <div className="space-y-5">
            <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
              <MapPin className="w-5 h-5 text-brand-orange" />
              <h2 className="text-base md:text-lg font-black uppercase tracking-wider text-white">Vị Trí Cụm Rạp</h2>
            </div>

            <div className="space-y-4">
              <div className="space-y-3 rounded-xl border border-zinc-850 bg-zinc-900/30 p-4 text-xs text-zinc-400">
                {cinema.description && (
                  <p className="border-b border-zinc-800 pb-3 leading-relaxed text-zinc-300">
                    {cinema.description}
                  </p>
                )}
                <p>Địa chỉ: <strong className="font-bold text-zinc-200">{cinema.address}</strong></p>
                <p>Khu vực: <strong className="font-bold text-zinc-200">{[cinema.district, cinema.city].filter(Boolean).join(', ')}</strong></p>
                <p>Hotline hỗ trợ: <strong className="font-bold text-zinc-200">{cinema.hotline || 'Chưa cập nhật'}</strong></p>
                <p>Hôm nay: <strong className="font-bold text-zinc-200">{formatOperatingHour(currentOperatingHour)}</strong></p>
              </div>

              {operatingHours.length > 0 && (
                <div className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-xl border border-zinc-850 bg-zinc-900/30 p-4 text-xs">
                  {operatingHours.map(hour => (
                    <div key={hour.dayOfWeek} className="flex items-center justify-between gap-3 border-b border-zinc-800/60 py-1.5">
                      <span className="text-zinc-500">{DAY_LABELS[hour.dayOfWeek] || `Ngày ${hour.dayOfWeek}`}</span>
                      <strong className={hour.isClosed ? 'text-red-400' : 'text-zinc-200'}>
                        {formatOperatingHour(hour)}
                      </strong>
                    </div>
                  ))}
                </div>
              )}

              {Array.isArray(cinema.activeAuditoriums) && cinema.activeAuditoriums.length > 0 && (
                <div className="rounded-xl border border-zinc-850 bg-zinc-900/30 p-4">
                  <p className="mb-3 text-[10px] font-black uppercase tracking-wider text-zinc-500">
                    Phòng chiếu đang hoạt động
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {cinema.activeAuditoriums.map(auditorium => (
                      <span key={auditorium.publicId} className="rounded-lg border border-zinc-800 bg-zinc-950 px-2.5 py-1.5 text-[10px] font-bold text-zinc-300">
                        {auditorium.name} · {auditorium.screenType}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {cinemaMap ? (
                <>
                  <div className="h-64 w-full overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-2xl">
                    <iframe
                      title={`Bản đồ ${cinema.name}`}
                      src={cinemaMap.embedUrl}
                      className="h-full w-full border-0"
                      loading="lazy"
                      referrerPolicy="no-referrer-when-downgrade"
                    />
                  </div>
                  <a
                    href={cinemaMap.externalUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-brand-orange hover:text-orange-400"
                  >
                    Mở bản đồ lớn
                    <ExternalLink className="h-3.5 w-3.5" />
                  </a>
                </>
              ) : (
                <div className="flex h-40 items-center justify-center rounded-2xl border border-zinc-850 bg-zinc-900/50 text-sm font-semibold text-zinc-600">
                  Rạp chưa cập nhật tọa độ bản đồ
                </div>
              )}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
