import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Film, ChevronDown, Check, MapPin, AlertCircle } from 'lucide-react';
import { getMovies, getCinemas, getShowtimes } from '@/features/catalog/customer/services/movieService';
import { seatSelectionPath } from '@/features/catalog/customer/utils/customerMovieFlow';
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import BookingStepper from '../components/BookingStepper';

const getItems = payload => payload?.data || payload?.content || payload || [];
const getMovieKey = movie => movie?.publicId || movie?.id || movie?.slug;
const getMoviePoster = movie => movie?.posterUrl || movie?.primaryPoster || movie?.image || '';
const getGenreNames = movie => (movie?.genres || [])
  .map(genre => typeof genre === 'string' ? genre : genre?.name)
  .filter(Boolean)
  .join(', ');

const createDateOptions = movie => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const releaseDate = movie?.releaseDate ? new Date(`${movie.releaseDate}T00:00:00`) : null;
  const startDate = releaseDate && !Number.isNaN(releaseDate.getTime()) && releaseDate > today
    ? releaseDate
    : today;
  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(startDate);
    date.setDate(startDate.getDate() + index);
    const isToday = date.getTime() === today.getTime();
    const isTomorrow = date.getTime() === today.getTime() + 86_400_000;

    return {
      dateStr: date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      dateQuery: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`,
      label: isToday ? 'Hôm nay' : isTomorrow ? 'Ngày mai' : weekdays[date.getDay()]
    };
  });
};

function MovieChoiceGroup({ title, movies, status, selectedMovie, onSelect }) {
  const isUpcoming = status === 'UPCOMING';

  return (
    <section className="space-y-3" aria-label={title}>
      <div className="flex items-center justify-between gap-3">
        <h4 className={`text-[10px] font-black uppercase tracking-[0.18em] ${
          isUpcoming ? 'text-amber-400' : 'text-brand-orange'
        }`}>
          {title}
        </h4>
        <span className="rounded-full border border-zinc-800 bg-zinc-950 px-2.5 py-1 text-[9px] font-black text-zinc-500">
          {movies.length} phim
        </span>
      </div>

      {movies.length === 0 ? (
        <p className="rounded-xl border border-dashed border-zinc-800 bg-zinc-950/60 px-4 py-5 text-center text-xs text-zinc-500">
          Chưa có phim trong nhóm này.
        </p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {movies.map(movie => {
            const movieKey = getMovieKey(movie);
            const isSelected = getMovieKey(selectedMovie) === movieKey;
            const poster = getMoviePoster(movie);
            return (
              <button
                key={movieKey}
                type="button"
                onClick={() => onSelect(movie)}
                aria-pressed={isSelected}
                className={`flex items-center gap-4 rounded-xl border p-3 text-left transition-all ${
                  isSelected
                    ? 'border-brand-orange bg-brand-orange/10 shadow-md'
                    : 'border-zinc-800 bg-zinc-950 hover:border-zinc-700'
                }`}
              >
                <div className="flex h-[72px] w-12 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-zinc-800 bg-zinc-900">
                  {poster ? (
                    <img src={poster} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <Film className="h-5 w-5 text-zinc-600" aria-hidden="true" />
                  )}
                </div>
                <div className="min-w-0 flex-1 space-y-1">
                  <span className={`inline-flex rounded-full px-2 py-0.5 text-[8px] font-black uppercase tracking-wider ${
                    isUpcoming
                      ? 'bg-amber-500/10 text-amber-400'
                      : 'bg-brand-orange/10 text-brand-orange'
                  }`}>
                    {isUpcoming ? 'Sắp chiếu' : 'Đang chiếu'}
                  </span>
                  <h5 className="truncate text-xs font-black leading-tight text-white">{movie.title}</h5>
                  <p className="truncate text-[9px] font-bold uppercase tracking-wider text-zinc-500">
                    {getGenreNames(movie) || 'Đang cập nhật thể loại'}
                  </p>
                  <div className="flex items-center gap-2">
                    {movie.ageRating && (
                      <span className="rounded border border-zinc-800 bg-zinc-900 px-1 text-[8px] font-black uppercase text-brand-yellow">
                        {movie.ageRating}
                      </span>
                    )}
                    {movie.durationMinutes && (
                      <span className="text-[9px] font-semibold text-zinc-400">{movie.durationMinutes} phút</span>
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default function MasterBookingFunnelPage() {
  const navigate = useNavigate();
  
  const [nowShowingMovies, setNowShowingMovies] = useState([]);
  const [upcomingMovies, setUpcomingMovies] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [showtimes, setShowtimes] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [showtimeLoading, setShowtimeLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  const [activeSection, setActiveSection] = useState('location'); // 'location' | 'movie' | 'showtime'
  
  // Selections
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [selectedCinema, setSelectedCinema] = useState(null);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedShowtime, setSelectedShowtime] = useState(null);

  // Fetch all active cinemas plus both customer-facing movie groups.
  const fetchInitialData = useCallback(async () => {
    setLoading(true);
    setNotice(null);
    try {
      const [cinemasData, nowShowingData, upcomingData] = await Promise.all([
        getCinemas({ page: 0, size: 100 }),
        getMovies({ page: 0, size: 100, status: 'NOW_SHOWING', sort: 'releaseDate,asc' }),
        getMovies({ page: 0, size: 100, status: 'UPCOMING', sort: 'releaseDate,asc' })
      ]);
      const currentMovies = getItems(nowShowingData);
      const currentMovieIds = new Set(currentMovies.map(getMovieKey));
      
      setCinemas(getItems(cinemasData));
      setNowShowingMovies(currentMovies);
      setUpcomingMovies(getItems(upcomingData).filter(movie => !currentMovieIds.has(getMovieKey(movie))));
    } catch {
      setNotice({
        title: 'Không thể tải phòng vé',
        message: 'Không thể tải danh sách phim và rạp lúc này. Vui lòng thử lại.',
        variant: 'error'
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchInitialData();
  }, [fetchInitialData]);

  // Group cinemas by city/region dynamically
  const regionsList = useMemo(() => {
    const regions = new Map();
    cinemas.forEach(cinema => {
      const city = cinema.city?.trim() || 'Khu vực khác';
      const id = city
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '') || 'other';
      if (!regions.has(id)) {
        regions.set(id, { id, name: city, theaters: [] });
      }
      regions.get(id).theaters.push(cinema);
    });

    return [...regions.values()]
      .map(region => ({
        ...region,
        theaters: region.theaters.sort((left, right) => left.name.localeCompare(right.name, 'vi'))
      }))
      .sort((left, right) => left.name.localeCompare(right.name, 'vi'));
  }, [cinemas]);

  // Upcoming movies begin at their release date; current movies begin today.
  const dates = useMemo(() => createDateOptions(selectedMovie), [selectedMovie]);

  // Fetch showtimes once movie, cinema, and date are selected
  const fetchShowtimesData = useCallback(async signal => {
    if (!selectedMovie || !selectedCinema || !selectedDate) return;
    await Promise.resolve();
    if (signal?.aborted) return;
    setShowtimeLoading(true);
    setShowtimes([]);
    try {
      const showtimeData = await getShowtimes({
        movieSlug: selectedMovie.slug,
        cinemaSlug: selectedCinema.slug,
        date: selectedDate.dateQuery,
        signal
      });
      setShowtimes(getItems(showtimeData));
    } catch (e) {
      if (e?.name === 'CanceledError') return;
      console.error(e);
      setShowtimes([]);
      setNotice({
        title: 'Không thể tải lịch chiếu',
        message: 'Lịch chiếu hiện chưa tải được. Vui lòng chọn lại hoặc thử lại sau.',
        variant: 'error'
      });
    } finally {
      if (!signal?.aborted) setShowtimeLoading(false);
    }
  }, [selectedMovie, selectedCinema, selectedDate]);

  useEffect(() => {
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchShowtimesData(controller.signal);
    return () => controller.abort();
  }, [fetchShowtimesData]);

  // Group showtimes by movie version format
  const showtimesByFormat = useMemo(() => {
    const grouped = {};
    showtimes.forEach(st => {
      const formatLabel = st.movieVersion?.versionName || st.movieVersion?.format || '2D Digital';
      if (!grouped[formatLabel]) {
        grouped[formatLabel] = [];
      }
      grouped[formatLabel].push(st);
    });

    // Sort times chronologically
    Object.keys(grouped).forEach(format => {
      grouped[format].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
    });

    return grouped;
  }, [showtimes]);

  const handleSelectRegion = (region) => {
    setSelectedRegion(region);
    setSelectedCinema(null);
    setSelectedMovie(null);
    setSelectedDate(null);
    setSelectedShowtime(null);
    setShowtimes([]);
    setActiveSection('location'); // Stay on location to choose cinema!
  };

  const handleSelectCinema = (cinema) => {
    setSelectedCinema(cinema);
    setSelectedMovie(null);
    setSelectedDate(null);
    setSelectedShowtime(null);
    setShowtimes([]);
    setActiveSection('movie');
  };

  const handleSelectMovie = (movie) => {
    setSelectedMovie(movie);
    setSelectedDate(createDateOptions(movie)[0]);
    setSelectedShowtime(null);
    setShowtimes([]);
    setActiveSection('showtime');
  };

  const handleSelectShowtime = (st) => {
    setSelectedShowtime(st);
  };

  const handleContinue = () => {
    if (!selectedShowtime) return;
    navigate(seatSelectionPath(selectedShowtime.showtimePublicId));
  };

  const formattedStartTime = (timeString) => {
    const d = new Date(timeString);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải lịch rạp chiếu...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-950 px-4 pb-16 pt-6 font-sans font-medium text-zinc-100 selection:bg-brand-orange selection:text-zinc-950 md:px-8">
      {notice && (
        <CustomerNoticeModal
          title={notice.title}
          message={notice.message}
          variant={notice.variant}
          onClose={() => setNotice(null)}
        />
      )}
      <div className="max-w-7xl mx-auto">
        <BookingStepper currentStep={1} />
        
        {/* Header Title */}
        <div className="mb-8">
          <h1 className="text-2xl md:text-3xl font-black uppercase tracking-wider text-white">Mua Vé Trực Tuyến</h1>
          <p className="text-xs text-zinc-500 font-bold uppercase tracking-wider mt-2">
            Trải nghiệm điện ảnh đỉnh cao, đặt vé dễ dàng qua các bước đơn giản
          </p>
        </div>

        {/* Accordions and sticky sidebar layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT ACCORDIONS */}
          <div className="lg:col-span-2 space-y-4">
            
            {/* Accordion 1: Chọn vị trí */}
            <div className="border border-zinc-800/80 rounded-2xl overflow-hidden bg-zinc-900 shadow-xl">
              <button
                onClick={() => setActiveSection('location')}
                className="w-full text-left px-6 py-4 flex items-center justify-between hover:bg-zinc-850/50 transition-colors focus:outline-none cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                    selectedCinema ? 'bg-emerald-500 text-black' : 'bg-brand-orange text-white'
                  }`}>
                    {selectedCinema ? <Check className="w-3 h-3 stroke-[3]" /> : '1'}
                  </div>
                  <div>
                    <h3 className="text-xs font-black text-white uppercase tracking-wider">Chọn Rạp Chiếu</h3>
                    {selectedCinema && (
                      <p className="text-[9px] text-emerald-400 font-bold uppercase mt-0.5">{selectedCinema.name}</p>
                    )}
                  </div>
                </div>
                <ChevronDown className={`w-4 h-4 text-zinc-500 transition-transform ${activeSection === 'location' ? 'rotate-180' : ''}`} />
              </button>

              {activeSection === 'location' && (
                <div className="px-6 pb-6 pt-2 border-t border-zinc-800/50 space-y-6 animate-in fade-in duration-200">
                  {/* Select Region */}
                  <div className="space-y-2">
                    <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Khu vực chiếu phim</label>
                    <div className="flex flex-wrap gap-3">
                      {regionsList.map((r) => (
                        <button
                          key={r.id}
                          onClick={() => handleSelectRegion(r)}
                          className={`px-5 py-2.5 rounded-full text-xs font-bold transition-all border cursor-pointer ${
                            selectedRegion?.id === r.id
                              ? 'bg-brand-orange border-brand-orange text-white shadow-lg shadow-brand-orange/15'
                              : 'bg-zinc-950 border-zinc-850 text-zinc-400 hover:text-white hover:bg-zinc-800'
                          }`}
                        >
                          {r.name}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Select Cinema within selected region */}
                  {selectedRegion && (
                    <div className="space-y-3">
                      <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Chọn rạp cụ thể</label>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {selectedRegion.theaters.map((cinemaObj) => (
                          <div
                            key={cinemaObj.slug}
                            onClick={() => handleSelectCinema(cinemaObj)}
                            className={`p-4 rounded-xl border flex flex-col justify-between cursor-pointer transition-all ${
                              selectedCinema?.slug === cinemaObj.slug
                                ? 'bg-brand-orange/10 border-brand-orange shadow-md shadow-brand-orange/5 text-white'
                                : 'bg-zinc-950 border-zinc-850 hover:border-zinc-700 text-zinc-300'
                            }`}
                          >
                            <span className="text-xs font-black uppercase leading-tight">{cinemaObj.name}</span>
                            <span className="text-[9px] text-zinc-500 font-semibold mt-1 flex items-center gap-1">
                              <MapPin className="w-3 h-3 text-brand-orange shrink-0" />
                              <span className="truncate">{cinemaObj.address}</span>
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Accordion 2: Chọn phim */}
            <div className={`border rounded-2xl overflow-hidden bg-zinc-900 shadow-xl transition-all ${
              selectedCinema ? 'border-zinc-800/80' : 'border-zinc-900 opacity-50 pointer-events-none'
            }`}>
              <button
                disabled={!selectedCinema}
                onClick={() => setActiveSection('movie')}
                className="w-full text-left px-6 py-4 flex items-center justify-between hover:bg-zinc-850/50 transition-colors focus:outline-none cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                    selectedMovie ? 'bg-emerald-500 text-black' : 'bg-brand-orange text-white'
                  }`}>
                    {selectedMovie ? <Check className="w-3 h-3 stroke-[3]" /> : '2'}
                  </div>
                  <div>
                    <h3 className="text-xs font-black text-white uppercase tracking-wider">Chọn phim</h3>
                    {selectedMovie && (
                      <p className="text-[9px] text-emerald-400 font-bold uppercase mt-0.5">{selectedMovie.title}</p>
                    )}
                  </div>
                </div>
                <ChevronDown className={`w-4 h-4 text-zinc-500 transition-transform ${activeSection === 'movie' ? 'rotate-180' : ''}`} />
              </button>

              {activeSection === 'movie' && selectedCinema && (
                <div className="space-y-7 border-t border-zinc-800/50 px-6 pb-6 pt-5 animate-in fade-in duration-200">
                  <MovieChoiceGroup
                    title="Phim đang chiếu"
                    movies={nowShowingMovies}
                    status="NOW_SHOWING"
                    selectedMovie={selectedMovie}
                    onSelect={handleSelectMovie}
                  />
                  <div className="border-t border-zinc-800/70" />
                  <MovieChoiceGroup
                    title="Phim sắp chiếu"
                    movies={upcomingMovies}
                    status="UPCOMING"
                    selectedMovie={selectedMovie}
                    onSelect={handleSelectMovie}
                  />
                </div>
              )}
            </div>

            {/* Accordion 3: Chọn suất */}
            <div className={`border rounded-2xl overflow-hidden bg-zinc-900 shadow-xl transition-all ${
              selectedMovie ? 'border-zinc-800/80' : 'border-zinc-900 opacity-50 pointer-events-none'
            }`}>
              <button
                disabled={!selectedMovie}
                onClick={() => setActiveSection('showtime')}
                className="w-full text-left px-6 py-4 flex items-center justify-between hover:bg-zinc-850/50 transition-colors focus:outline-none cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                    selectedShowtime ? 'bg-emerald-500 text-black' : 'bg-brand-orange text-white'
                  }`}>
                    {selectedShowtime ? <Check className="w-3 h-3 stroke-[3]" /> : '3'}
                  </div>
                  <div>
                    <h3 className="text-xs font-black text-white uppercase tracking-wider">Chọn suất chiếu</h3>
                    {selectedShowtime && (
                      <p className="text-[9px] text-emerald-400 font-bold uppercase mt-0.5">
                        {formattedStartTime(selectedShowtime.startTime)} ({selectedShowtime.movieVersion?.versionName || selectedShowtime.movieVersion?.format})
                      </p>
                    )}
                  </div>
                </div>
                <ChevronDown className={`w-4 h-4 text-zinc-500 transition-transform ${activeSection === 'showtime' ? 'rotate-180' : ''}`} />
              </button>

              {activeSection === 'showtime' && selectedMovie && (
                <div className="px-6 pb-6 pt-2 border-t border-zinc-800/50 space-y-6 animate-in fade-in duration-200">
                  
                  {/* Select Date */}
                  <div className="space-y-2">
                    <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Chọn ngày xem</label>
                    <div className="flex flex-wrap gap-2">
                      {dates.map((dateObj) => (
                        <button
                          key={dateObj.dateStr}
                          type="button"
                          onClick={() => {
                            setSelectedDate(dateObj);
                            setSelectedShowtime(null);
                            setShowtimes([]);
                          }}
                          className={`px-4 py-2 rounded-xl border text-center transition-all cursor-pointer ${
                            selectedDate?.dateStr === dateObj.dateStr
                              ? 'bg-brand-orange border-brand-orange text-white shadow-lg shadow-brand-orange/10 font-bold'
                              : 'bg-zinc-950 border-zinc-850 text-zinc-400 hover:text-white'
                          }`}
                        >
                          <p className="text-[8px] font-black uppercase tracking-widest leading-none mb-0.5">{dateObj.label}</p>
                          <p className="text-xs font-extrabold">{dateObj.dateStr}</p>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Slot selection */}
                  <div className="space-y-4 pt-2">
                    <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Suất chiếu khả dụng</label>
                    
                    {showtimeLoading ? (
                      <div className="flex items-center justify-center gap-3 rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-xs font-semibold text-zinc-400">
                        <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-700 border-t-brand-orange" />
                        Đang tải suất chiếu…
                      </div>
                    ) : Object.keys(showtimesByFormat).length === 0 ? (
                      <div className="p-4 bg-zinc-950 border border-zinc-850 rounded-xl text-center text-xs text-zinc-500 font-semibold">
                        {selectedMovie.status === 'UPCOMING'
                          ? 'Phim sắp chiếu chưa có suất mở bán vào ngày này.'
                          : 'Không tìm thấy suất chiếu nào vào ngày đã chọn.'}
                      </div>
                    ) : (
                      <div className="p-4 bg-zinc-950 border border-zinc-850 rounded-xl space-y-4">
                        {Object.keys(showtimesByFormat).map((formatLabel) => (
                          <div key={formatLabel} className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4 py-1.5">
                            <span className="text-[9px] font-black uppercase tracking-wider text-brand-orange min-w-[90px]">{formatLabel}</span>
                            <div className="flex flex-wrap gap-2">
                              {showtimesByFormat[formatLabel].map((st) => {
                                const isSelected = selectedShowtime?.showtimePublicId === st.showtimePublicId;
                                return (
                                  <button
                                    key={st.showtimePublicId}
                                    type="button"
                                    onClick={() => handleSelectShowtime(st)}
                                    className={`px-3 py-1.5 rounded-lg text-[10px] font-black tracking-wider transition-all cursor-pointer ${
                                      isSelected
                                        ? 'bg-brand-orange text-white shadow-md shadow-brand-orange/10'
                                        : 'bg-zinc-900 hover:bg-zinc-800 text-zinc-300'
                                    }`}
                                  >
                                    {formattedStartTime(st.startTime)}
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

          </div>

          {/* RIGHT STICKY LEDGER */}
          <div className="space-y-6 lg:sticky lg:top-24 h-fit">
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl flex flex-col">
              
              {/* Poster header */}
              <div className="aspect-[16/10] bg-zinc-950 relative overflow-hidden border-b border-zinc-800 flex items-center justify-center">
                {selectedMovie ? (
                  <>
                    {getMoviePoster(selectedMovie) ? (
                      <img src={getMoviePoster(selectedMovie)} alt={selectedMovie.title} className="w-full h-full object-cover" />
                    ) : (
                      <Film className="h-10 w-10 text-zinc-700" aria-hidden="true" />
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/20 to-transparent" />
                  </>
                ) : (
                  <div className="flex flex-col items-center gap-2 text-zinc-650 py-10">
                    <Film className="w-10 h-10 animate-pulse" />
                    <span className="text-[10px] font-black uppercase tracking-wider">Chưa chọn phim</span>
                  </div>
                )}

                {selectedMovie && (
                  <div className="absolute bottom-3 left-4 right-4">
                    <h3 className="text-sm font-black text-white leading-tight line-clamp-1">{selectedMovie.title}</h3>
                    <p className="text-[9px] text-zinc-400 font-bold mt-1 uppercase tracking-wider truncate">
                      {selectedMovie.durationMinutes} phút • {getGenreNames(selectedMovie)}
                    </p>
                  </div>
                )}
              </div>

              {/* Selection details list */}
              <div className="p-5 space-y-4 flex-grow text-xs font-semibold text-zinc-350">
                
                <div className="space-y-2.5">
                  <div className="flex justify-between items-center text-[10px] font-black uppercase tracking-wider text-zinc-500 pb-1 border-b border-zinc-850">
                    <span>Chi tiết đặt lịch</span>
                    <span>Tóm tắt</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Rạp chiếu:</span>
                    <span className="text-right text-zinc-300 font-bold">{selectedCinema ? selectedCinema.name : '--'}</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Ngày chiếu:</span>
                    <span className="text-right text-zinc-300 font-bold">{selectedDate ? `${selectedDate.label} (${selectedDate.dateStr})` : '--'}</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Suất chiếu:</span>
                    <span className="text-right text-zinc-300 font-bold">
                      {selectedShowtime ? `${formattedStartTime(selectedShowtime.startTime)} (${selectedShowtime.movieVersion?.versionName || selectedShowtime.movieVersion?.format})` : '--'}
                    </span>
                  </div>
                </div>

                <div className="border-t border-dashed border-zinc-800 my-4" />

                <div className="flex justify-between items-center py-1">
                  <span className="text-zinc-500 font-bold">Tổng cộng:</span>
                  <span className="text-sm font-black text-brand-orange uppercase">0 đ</span>
                </div>

                {selectedShowtime && (
                  <div className="flex gap-2 p-2.5 bg-zinc-950 border border-zinc-850 rounded-lg text-[9px] font-bold text-zinc-550 leading-normal">
                    <AlertCircle className="w-3.5 h-3.5 text-zinc-600 shrink-0" />
                    <span>Giá vé chính thức và dịch vụ đi kèm sẽ được tính cụ thể tại bước chọn ghế ngồi tiếp theo.</span>
                  </div>
                )}

                {/* Submit buttons */}
                <div className="space-y-2 pt-4">
                  <button
                    onClick={handleContinue}
                    disabled={!selectedShowtime}
                    className={`w-full font-black py-3 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 ${
                      selectedShowtime
                        ? 'bg-brand-orange hover:bg-opacity-95 text-white cursor-pointer shadow-lg shadow-brand-orange/20 active:scale-[0.98]'
                        : 'bg-zinc-800 text-zinc-500 cursor-not-allowed select-none'
                    }`}
                  >
                    Tiếp tục
                  </button>

                  <button
                    onClick={() => navigate('/')}
                    className="w-full text-center text-[10px] font-black uppercase tracking-wider text-zinc-500 hover:text-white py-2.5 transition-colors focus:outline-none cursor-pointer"
                  >
                    Hủy đặt vé
                  </button>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
