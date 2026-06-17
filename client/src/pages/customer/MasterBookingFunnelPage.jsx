import { useState, useMemo } from 'react';
import { Film, Star, ChevronDown, Check, MapPin, AlertCircle } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function MasterBookingFunnel({ onBackHome, onBookTicket }) {
  const { movies, cinemas, showtimes } = useData();
  const [activeSection, setActiveSection] = useState('location'); // 'location' | 'movie' | 'showtime'
  
  // Selection states
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null); // { dateStr, fullDateStr }
  const [selectedShowtime, setSelectedShowtime] = useState(null); // { cinema, time, format }

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
      if (addr.includes('hồ chí minh') || addr.includes('hcm') || addr.includes('thủ đức')) {
        list[0].theaters.push(c.name);
      } else if (addr.includes('hà nội') || addr.includes('hn')) {
        list[1].theaters.push(c.name);
      } else if (addr.includes('đà nẵng') || addr.includes('dn')) {
        list[2].theaters.push(c.name);
      } else if (addr.includes('nha trang') || addr.includes('khánh hòa')) {
        list[3].theaters.push(c.name);
      } else {
        list[0].theaters.push(c.name);
      }
    });

    return list.filter(r => r.theaters.length > 0);
  }, [cinemas]);

  // Generate next 4 dates starting from today
  const dates = useMemo(() => {
    const list = [];
    for (let i = 0; i < 4; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      const fullDateStr = d.toLocaleDateString('vi-VN');
      const label = i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : d.toLocaleDateString('vi-VN', { weekday: 'long' });
      list.push({ dateStr, fullDateStr, label });
    }
    return list;
  }, []);

  // Filter only now showing movies for booking funnel
  const availableMovies = useMemo(() => {
    return movies.filter(m => m.status === 'NOW_SHOWING' || m.status === 'DANG_CHIEU');
  }, [movies]);

  // Filter theaters based on selected region
  const availableTheaters = useMemo(() => {
    if (!selectedRegion) return [];
    return selectedRegion.theaters;
  }, [selectedRegion]);

  // Retrieve dynamic showtimes filtered by movie, cinema, and date
  const getShowtimesForTheater = (theaterName) => {
    const foundCinema = cinemas.find(c => c.name === theaterName);
    if (!foundCinema || !selectedMovie || !selectedDate) {
      return [
        { format: '2D DIGITAL', times: ['09:30', '13:15', '16:45'] },
        { format: '3D DIGITAL', times: ['19:30'] },
        { format: 'IMAX 3D', times: ['22:15'] }
      ];
    }

    const formatToYYYYMMDD = (str) => {
      if (!str) return '';
      if (str.includes('-')) return str;
      const parts = str.split('/');
      if (parts.length === 3) {
        return `${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}`;
      }
      return str;
    };
    
    const formattedTarget = formatToYYYYMMDD(selectedDate.fullDateStr);

    const matching = showtimes.filter(st => 
      String(st.movieId) === String(selectedMovie.id) &&
      String(st.cinemaId) === String(foundCinema.id) &&
      formatToYYYYMMDD(st.date) === formattedTarget
    );

    if (matching.length === 0) {
      return [
        { format: '2D DIGITAL', times: ['09:30', '13:15', '16:45'] },
        { format: '3D DIGITAL', times: ['19:30'] },
        { format: 'IMAX 3D', times: ['22:15'] }
      ];
    }

    const formatsMap = {};
    matching.forEach(st => {
      const hall = foundCinema.halls?.find(h => String(h.id) === String(st.hallId));
      let format = '2D DIGITAL';
      if (hall) {
        if (hall.format.toUpperCase().includes('IMAX')) format = 'IMAX 3D';
        else if (hall.format.toUpperCase().includes('3D')) format = '3D DIGITAL';
      }
      if (!formatsMap[format]) {
        formatsMap[format] = new Set();
      }
      formatsMap[format].add(st.time);
    });

    return Object.entries(formatsMap).map(([format, timesSet]) => ({
      format,
      times: [...timesSet].sort()
    }));
  };

  const handleSelectRegion = (region) => {
    setSelectedRegion(region);
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

  const handleSelectShowtime = (cinema, time, format) => {
    setSelectedShowtime({ cinema, time, format });
  };

  const handleContinue = () => {
    if (!selectedMovie || !selectedShowtime || !selectedDate) return;

    const bookingPayload = {
      movieId: selectedMovie.id,
      movieTitle: selectedMovie.title,
      cinema: selectedShowtime.cinema,
      time: selectedShowtime.time,
      format: selectedShowtime.format,
      date: selectedDate.dateStr,
      fullDate: selectedDate.fullDateStr,
      selectedSeats: []
    };

    onBookTicket(bookingPayload);
  };

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen py-8 px-4 md:px-8">
      <div className="max-w-7xl mx-auto space-y-8">
        
        {/* Funnel Title */}
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Lập Lịch Vé Xem Phim</h1>
          <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
            Đặt lịch xem phim nhanh chóng trong 3 bước accordion tiện lợi
          </p>
        </div>

        {/* Asymmetric Split Layout Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT PANEL: Accordions (2/3 Width) */}
          <div className="lg:col-span-2 space-y-4">
            
            {/* Accordion 1: Chọn vị trí */}
            <div className="border border-zinc-800/80 rounded-2xl overflow-hidden bg-zinc-900 shadow-xl">
              <button
                onClick={() => setActiveSection('location')}
                className="w-full text-left px-6 py-4 flex items-center justify-between hover:bg-zinc-850/50 transition-colors focus:outline-none"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                    selectedRegion ? 'bg-emerald-500 text-black' : 'bg-blue-600 text-white'
                  }`}>
                    {selectedRegion ? <Check className="w-3 h-3 stroke-[3]" /> : '1'}
                  </div>
                  <div>
                    <h3 className="text-xs font-black text-white uppercase tracking-wider">Chọn vị trí</h3>
                    {selectedRegion && (
                      <p className="text-[9px] text-emerald-400 font-bold uppercase mt-0.5">{selectedRegion.name}</p>
                    )}
                  </div>
                </div>
                <ChevronDown className={`w-4 h-4 text-zinc-500 transition-transform ${activeSection === 'location' ? 'rotate-180' : ''}`} />
              </button>

              {activeSection === 'location' && (
                <div className="px-6 pb-6 pt-2 border-t border-zinc-800/50 space-y-4 animate-in fade-in duration-200">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Khu vực chiếu phim</label>
                  <div className="flex flex-wrap gap-3">
                    {regionsList.map((r) => (
                      <button
                        key={r.id}
                        onClick={() => handleSelectRegion(r)}
                        className={`px-6 py-3 rounded-full text-xs font-bold transition-all border ${
                          selectedRegion?.id === r.id
                            ? 'bg-blue-600 border-blue-500 text-white shadow-lg shadow-blue-600/15'
                            : 'bg-zinc-950 border-zinc-850 text-zinc-400 hover:text-white hover:bg-zinc-800'
                        }`}
                      >
                        {r.name}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Accordion 2: Chọn phim */}
            <div className={`border rounded-2xl overflow-hidden bg-zinc-900 shadow-xl transition-all ${
              selectedRegion ? 'border-zinc-800/80' : 'border-zinc-900 opacity-50 pointer-events-none'
            }`}>
              <button
                disabled={!selectedRegion}
                onClick={() => setActiveSection('movie')}
                className="w-full text-left px-6 py-4 flex items-center justify-between hover:bg-zinc-850/50 transition-colors focus:outline-none"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                    selectedMovie ? 'bg-emerald-500 text-black' : 'bg-blue-600 text-white'
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

              {activeSection === 'movie' && selectedRegion && (
                <div className="px-6 pb-6 pt-2 border-t border-zinc-800/50 space-y-4 animate-in fade-in duration-200">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Phim đang chiếu tại {selectedRegion.name}</label>
                  
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {availableMovies.map((movie) => (
                      <div
                        key={movie.id}
                        onClick={() => handleSelectMovie(movie)}
                        className={`p-3 rounded-xl border flex items-center gap-4 cursor-pointer transition-all ${
                          selectedMovie?.id === movie.id
                            ? 'bg-zinc-855/70 border-amber-500 shadow-md shadow-amber-500/5'
                            : 'bg-zinc-950 border-zinc-850 hover:border-zinc-700'
                        }`}
                      >
                        <div className="w-12 h-18 rounded-lg overflow-hidden shrink-0 border border-zinc-800 animate-fade-in bg-zinc-900">
                          <img src={movie.posterUrl || movie.image} alt={movie.title} className="w-full h-full object-cover" />
                        </div>
                        <div className="space-y-1.5 min-w-0">
                          <h4 className="text-xs font-black text-white leading-tight truncate">{movie.title}</h4>
                          <p className="text-[9px] text-zinc-500 font-bold uppercase tracking-wider truncate">{movie.genre || movie.genres?.join(', ')}</p>
                          <div className="flex items-center gap-2">
                            <span className="text-[8px] font-black uppercase bg-zinc-900 border border-zinc-800 text-brand-yellow px-1 rounded">
                              {movie.ageRating || 'T16'}
                            </span>
                            <div className="flex items-center gap-0.5 text-brand-yellow">
                              <Star className="w-3 h-3 fill-brand-yellow text-brand-yellow" />
                              <span className="text-[9px] font-bold text-zinc-400">{(movie.rating || 0).toFixed(1)}</span>
                            </div>
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
                className="w-full text-left px-6 py-4 flex items-center justify-between hover:bg-zinc-850/50 transition-colors focus:outline-none"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                    selectedShowtime ? 'bg-emerald-500 text-black' : 'bg-blue-600 text-white'
                  }`}>
                    {selectedShowtime ? <Check className="w-3 h-3 stroke-[3]" /> : '3'}
                  </div>
                  <div>
                    <h3 className="text-xs font-black text-white uppercase tracking-wider">Chọn suất chiếu</h3>
                    {selectedShowtime && (
                      <p className="text-[9px] text-emerald-400 font-bold uppercase mt-0.5">
                        {selectedShowtime.cinema} - {selectedShowtime.time} ({selectedShowtime.format})
                      </p>
                    )}
                  </div>
                </div>
                <ChevronDown className={`w-4 h-4 text-zinc-500 transition-transform ${activeSection === 'showtime' ? 'rotate-180' : ''}`} />
              </button>

              {activeSection === 'showtime' && selectedMovie && (
                <div className="px-6 pb-6 pt-2 border-t border-zinc-800/50 space-y-6 animate-in fade-in duration-200">
                  
                  {/* Date Selector Inside Accordion */}
                  <div className="space-y-2">
                    <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Chọn ngày xem</label>
                    <div className="flex flex-wrap gap-2">
                      {dates.map((dateObj) => (
                        <button
                          key={dateObj.dateStr}
                          type="button"
                          onClick={() => {
                            setSelectedDate(dateObj);
                            setSelectedShowtime(null); // Clear selected slot on date change
                          }}
                          className={`px-4 py-2 rounded-xl border text-center transition-all ${
                            selectedDate?.dateStr === dateObj.dateStr
                              ? 'bg-amber-500 border-amber-500 text-black shadow-lg shadow-amber-500/10 font-bold'
                              : 'bg-zinc-950 border-zinc-850 text-zinc-400 hover:text-white'
                          }`}
                        >
                          <p className="text-[8px] font-black uppercase tracking-widest leading-none mb-0.5">{dateObj.label}</p>
                          <p className="text-xs font-extrabold">{dateObj.dateStr}</p>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Theater and Slot List */}
                  <div className="space-y-4 pt-2">
                    <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">Danh sách rạp và suất chiếu</label>
                    
                    {availableTheaters.length === 0 ? (
                      <div className="p-4 bg-zinc-950 border border-zinc-850 rounded-xl text-center text-xs text-zinc-500 font-semibold">
                        Không tìm thấy rạp chiếu khả dụng ở khu vực đã chọn.
                      </div>
                    ) : (
                      availableTheaters.map((theater) => {
                        const formats = getShowtimesForTheater(theater);
                        return (
                          <div key={theater} className="p-4 bg-zinc-950 border border-zinc-850 rounded-xl space-y-3">
                            
                            <div className="flex items-center gap-2">
                              <MapPin className="w-4 h-4 text-amber-500 shrink-0" />
                              <h4 className="text-xs font-black text-white uppercase">{theater}</h4>
                            </div>

                            <div className="space-y-3">
                              {formats.map((tf) => (
                                <div key={tf.format} className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4 py-1.5 border-t border-zinc-900/80 last:border-b-0">
                                  <span className="text-[9px] font-black uppercase tracking-wider text-zinc-500 min-w-[90px]">{tf.format}</span>
                                  <div className="flex flex-wrap gap-2">
                                    {tf.times.map((time) => {
                                      const isSelected = selectedShowtime?.cinema === theater && selectedShowtime?.time === time && selectedShowtime?.format === tf.format;
                                      return (
                                        <button
                                          key={time}
                                          type="button"
                                          onClick={() => handleSelectShowtime(theater, time, tf.format)}
                                          className={`px-3 py-1.5 rounded-lg text-[10px] font-black tracking-wider transition-all ${
                                            isSelected
                                              ? 'bg-blue-600 text-white shadow-md shadow-blue-600/10'
                                              : 'bg-zinc-900 hover:bg-zinc-800 text-zinc-300'
                                          }`}
                                        >
                                          {time}
                                        </button>
                                      );
                                    })}
                                  </div>
                                </div>
                              ))}
                            </div>

                          </div>
                        );
                      })
                    )}
                  </div>

                </div>
              )}
            </div>

          </div>

          {/* RIGHT PANEL: Sticky Ledger Card (1/3 Width) */}
          <div className="space-y-6 lg:sticky lg:top-24 h-fit">
            
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl flex flex-col">
              
              {/* Image poster section */}
              <div className="aspect-[16/10] bg-zinc-950 relative overflow-hidden border-b border-zinc-800 flex items-center justify-center">
                {selectedMovie ? (
                  <>
                    <img src={selectedMovie.posterUrl || selectedMovie.image} alt={selectedMovie.title} className="w-full h-full object-cover" />
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
                      {selectedMovie.duration} phút • {selectedMovie.genre || selectedMovie.genres?.join(', ')}
                    </p>
                  </div>
                )}
              </div>

              {/* Selection details */}
              <div className="p-5 space-y-4 flex-grow text-xs font-semibold text-zinc-350">
                
                <div className="space-y-2.5">
                  <div className="flex justify-between items-center text-[10px] font-black uppercase tracking-wider text-zinc-500 pb-1 border-b border-zinc-850">
                    <span>Chi tiết đặt lịch</span>
                    <span>Tóm tắt</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Khu vực:</span>
                    <span className="text-right text-zinc-300 font-bold">{selectedRegion ? selectedRegion.name : '--'}</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Rạp:</span>
                    <span className="text-right text-zinc-300 font-bold">{selectedShowtime ? selectedShowtime.cinema : '--'}</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Ngày chiếu:</span>
                    <span className="text-right text-zinc-300 font-bold">{selectedDate ? `${selectedDate.label} (${selectedDate.dateStr})` : '--'}</span>
                  </div>

                  <div className="flex items-start gap-2 justify-between">
                    <span className="text-zinc-500">Suất chiếu:</span>
                    <span className="text-right text-zinc-300 font-bold">
                      {selectedShowtime ? `${selectedShowtime.time} (${selectedShowtime.format})` : '--'}
                    </span>
                  </div>
                </div>

                {/* Dotted separation line */}
                <div className="border-t border-dashed border-zinc-800 my-4" />

                {/* Pricing summary widget */}
                <div className="flex justify-between items-center py-1">
                  <span className="text-zinc-500 font-bold">Tổng cộng:</span>
                  <span className="text-sm font-black text-amber-500 uppercase">0 đ</span>
                </div>

                {selectedShowtime && (
                  <div className="flex gap-2 p-2.5 bg-zinc-950 border border-zinc-850 rounded-lg text-[9px] font-bold text-zinc-550 leading-normal">
                    <AlertCircle className="w-3.5 h-3.5 text-zinc-600 shrink-0" />
                    <span>Giá vé chính thức và dịch vụ đi kèm sẽ được tính cụ thể tại bước chọn ghế & bắp nước tiếp theo.</span>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="space-y-2 pt-4">
                  <button
                    onClick={handleContinue}
                    disabled={!selectedShowtime}
                    className={`w-full font-black py-3 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 ${
                      selectedShowtime
                        ? 'bg-amber-500 hover:bg-amber-600 text-black cursor-pointer shadow-lg shadow-amber-500/20 active:scale-[0.98]'
                        : 'bg-zinc-800 text-zinc-500 cursor-not-allowed select-none'
                    }`}
                  >
                    Tiếp tục
                  </button>

                  <button
                    onClick={onBackHome}
                    className="w-full text-center text-[10px] font-black uppercase tracking-wider text-zinc-500 hover:text-white py-2.5 transition-colors focus:outline-none"
                  >
                    Quay lại
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
