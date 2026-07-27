import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { Film, Star, ChevronDown, Check, MapPin, AlertCircle } from 'lucide-react';
import { getMovies, getCinemas, getShowtimes } from '@/features/catalog/customer/services/movieService';
import { seatSelectionPath } from '@/features/catalog/customer/utils/customerMovieFlow';
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import BookingStepper from '../components/BookingStepper';

export default function MasterBookingFunnelPage() {
  const navigate = useNavigate();
  
  const [movies, setMovies] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [showtimes, setShowtimes] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const [activeSection, setActiveSection] = useState('location'); // 'location' | 'movie' | 'showtime'
  
  // Selections
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [selectedCinema, setSelectedCinema] = useState(null);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedShowtime, setSelectedShowtime] = useState(null);

  // Fetch all initial cinemas and movies
  const fetchInitialData = useCallback(async () => {
    setLoading(true);
    setNotice(null);
    try {
      const cinemasData = await getCinemas();
      const moviesData = await getMovies({ size: 100, status: 'NOW_SHOWING' });
      
      setCinemas(cinemasData.data || cinemasData.content || cinemasData || []);
      setMovies(moviesData.data || moviesData.content || moviesData || []);
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
    const list = [
      { id: 'hcm', name: 'TP Hồ Chí Minh', theaters: [] },
      { id: 'hn', name: 'Hà Nội', theaters: [] },
      { id: 'dn', name: 'Đà Nẵng', theaters: [] },
      { id: 'kh', name: 'Khánh Hòa', theaters: [] }
    ];
    
    cinemas.forEach(c => {
      const addr = (c.address || '').toLowerCase();
      if (addr.includes('hồ chí minh') || addr.includes('hcm') || addr.includes('quận 1') || addr.includes('quận 2')) {
        list[0].theaters.push(c);
      } else if (addr.includes('hà nội') || addr.includes('hn') || addr.includes('thanh xuân')) {
        list[1].theaters.push(c);
      } else if (addr.includes('đà nẵng') || addr.includes('dn')) {
        list[2].theaters.push(c);
      } else if (addr.includes('nha trang') || addr.includes('khánh hòa')) {
        list[3].theaters.push(c);
      } else {
        list[0].theaters.push(c);
      }
    });

    return list.filter(r => r.theaters.length > 0);
  }, [cinemas]);

  // Generate next 4 dates starting from today
  const dates = useMemo(() => {
    const list = [];
    const weekdays = ["Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
    for (let i = 0; i < 4; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      const queryStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      const label = i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : weekdays[d.getDay()];
      list.push({ dateStr, dateQuery: queryStr, label });
    }
    return list;
  }, []);

  // Fetch showtimes once movie, cinema, and date are selected
  const fetchShowtimesData = useCallback(async () => {
    if (!selectedMovie || !selectedCinema || !selectedDate) return;
    try {
      const showtimeData = await getShowtimes({
        movieSlug: selectedMovie.slug,
        cinemaSlug: selectedCinema.slug,
        date: selectedDate.dateQuery
      });
      setShowtimes(showtimeData.data || showtimeData.content || []);
    } catch (e) {
      console.error(e);
      setShowtimes([]);
      setNotice({
        title: 'Không thể tải lịch chiếu',
        message: 'Lịch chiếu hiện chưa tải được. Vui lòng chọn lại hoặc thử lại sau.',
        variant: 'error'
      });
    }
  }, [selectedMovie, selectedCinema, selectedDate]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchShowtimesData();
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
    setActiveSection('location'); // Stay on location to choose cinema!
  };

  const handleSelectCinema = (cinema) => {
    setSelectedCinema(cinema);
    setSelectedMovie(null);
    setSelectedDate(null);
    setSelectedShowtime(null);
    setActiveSection('movie');
  };

  const handleSelectMovie = (movie) => {
    setSelectedMovie(movie);
    setSelectedDate(dates[0]); // Default to today
    setSelectedShowtime(null);
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
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-28 pb-16 px-4 md:px-8 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
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
                <div className="px-6 pb-6 pt-2 border-t border-zinc-800/50 space-y-4 animate-in fade-in duration-200">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Phim đang chiếu</label>
                  
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {movies.map((movie) => (
                      <div
                        key={movie.publicId}
                        onClick={() => handleSelectMovie(movie)}
                        className={`p-3 rounded-xl border flex items-center gap-4 cursor-pointer transition-all ${
                          selectedMovie?.publicId === movie.publicId
                            ? 'bg-brand-orange/10 border-brand-orange shadow-md'
                            : 'bg-zinc-950 border-zinc-850 hover:border-zinc-700'
                        }`}
                      >
                        <div className="w-12 h-18 rounded-lg overflow-hidden shrink-0 border border-zinc-800 bg-zinc-900">
                          <img src={movie.primaryPoster} alt={movie.title} className="w-full h-full object-cover" />
                        </div>
                        <div className="space-y-1 min-w-0">
                          <h4 className="text-xs font-black text-white leading-tight truncate">{movie.title}</h4>
                          <p className="text-[9px] text-zinc-550 font-bold uppercase tracking-wider truncate">{movie.genres?.join(', ')}</p>
                          <div className="flex items-center gap-2">
                            <span className="text-[8px] font-black uppercase bg-zinc-900 border border-zinc-800 text-brand-yellow px-1 rounded">
                              {movie.ageRating}
                            </span>
                            <span className="text-[9px] font-semibold text-zinc-400">{movie.durationMinutes} phút</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
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
                    
                    {Object.keys(showtimesByFormat).length === 0 ? (
                      <div className="p-4 bg-zinc-950 border border-zinc-850 rounded-xl text-center text-xs text-zinc-500 font-semibold">
                        Không tìm thấy suất chiếu nào vào ngày đã chọn.
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
                    <img src={selectedMovie.primaryPoster} alt={selectedMovie.title} className="w-full h-full object-cover" />
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
                      {selectedMovie.durationMinutes} phút • {selectedMovie.genres?.join(', ')}
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
