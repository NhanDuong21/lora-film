import { useState, useEffect, useMemo } from 'react';
import { Trash2, Calendar, Clock, Film, Filter, ChevronRight, RefreshCw } from 'lucide-react';

export default function AdminShowtimeView({ 
  showtimes, 
  movies, 
  theaters, 
  updateShowtimesState, 
  triggerToast 
}) {
  // Unified Scheduler Configuration States
  const [scheduleCycle, setScheduleCycle] = useState({ from: '2026-05-28', to: '2026-05-31' }); // 4-day operational cycle range
  const [focusedDate, setFocusedDate] = useState('2026-05-28'); // Active focused date pointer

  const [operatingHours, setOperatingHours] = useState({ start: '08:00', end: '22:00' });
  const [goldenHour, setGoldenHour] = useState('19:00');
  const [selectedMovies, setSelectedMovies] = useState([]); // Array of checked movie IDs

  // Target Cinema complex selection
  const [selectedTheaterId, setSelectedTheaterId] = useState(theaters[0]?.id || '');

  // Helper: Convert "HH:MM" to minutes from midnight
  const timeToMinutes = (timeStr) => {
    if (!timeStr) return 0;
    const [h, m] = timeStr.split(':').map(Number);
    return (h || 0) * 60 + (m || 0);
  };

  // Helper: Convert minutes from midnight to "HH:MM"
  const minutesToTime = (minutes) => {
    const h = Math.floor(minutes / 60) % 24;
    const m = minutes % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  };

  // Helper: Generate all date strings in the range [from, to] inclusive
  const getDatesInRange = (from, to) => {
    if (!from || !to) return [];
    const list = [];
    const start = new Date(from);
    const end = new Date(to);
    const current = new Date(start);
    let limit = 0;
    while (current <= end && limit < 30) { // Safety ceiling to prevent infinite loops
      list.push(current.toISOString().split('T')[0]);
      current.setDate(current.getDate() + 1);
      limit++;
    }
    return list;
  };

  const dateTokens = useMemo(() => {
    return getDatesInRange(scheduleCycle.from, scheduleCycle.to);
  }, [scheduleCycle.from, scheduleCycle.to]);

  // Self-healing: if focusedDate is no longer in range, point to first index
  useEffect(() => {
    if (dateTokens.length > 0 && !dateTokens.includes(focusedDate)) {
      setFocusedDate(dateTokens[0]);
    }
  }, [dateTokens, focusedDate]);

  // Helper: Format YYYY-MM-DD to DD/MM/YYYY
  const formatDateVN = (dateStr) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
  };

  // Helper: Format YYYY-MM-DD to DD/MM
  const formatDateVNShort = (dateStr) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}`;
    }
    return dateStr;
  };

  // Helper: Get day name in Vietnamese
  const getVNWeekday = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const days = ['Chủ Nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
    return days[d.getDay()];
  };

  // Helper: Generate unique movie visual profile classes
  const getMovieColorClasses = (index) => {
    const colors = [
      'border-l-purple-500 text-purple-400',
      'border-l-emerald-500 text-emerald-400',
      'border-l-amber-500 text-amber-400',
      'border-l-blue-500 text-blue-400',
      'border-l-rose-500 text-rose-400',
      'border-l-cyan-500 text-cyan-400'
    ];
    return colors[index % colors.length];
  };

  // Select theater halls
  const activeTheater = useMemo(() => {
    return theaters.find(t => String(t.id) === String(selectedTheaterId)) || theaters[0];
  }, [theaters, selectedTheaterId]);

  const activeHalls = useMemo(() => {
    return activeTheater?.halls || [];
  }, [activeTheater]);

  // Dynamic horizontal track scale positioning math
  const startHourNum = parseInt(operatingHours.start.split(':')[0]) || 8;
  const endHourNum = parseInt(operatingHours.end.split(':')[0]) || 22;
  const startMin = startHourNum * 60;
  const endMin = endHourNum * 60;
  const totalMin = endMin - startMin;

  const calculateLeftOffset = (timeStr) => {
    const mins = timeToMinutes(timeStr);
    const offset = mins - startMin;
    const percent = (offset / totalMin) * 100;
    return Math.max(0, Math.min(100, percent));
  };

  const calculateWidthScale = (durationVal) => {
    const dur = parseInt(durationVal) || 120;
    const percent = (dur / totalMin) * 100;
    return Math.max(1, Math.min(100, percent));
  };

  // Filter showtimes of active cinema on active focusedDate
  const dayShowtimes = useMemo(() => {
    return showtimes.filter(st => {
      const isSameTheater = String(st.cinemaId) === String(selectedTheaterId);
      const isSameDate = String(st.date) === String(focusedDate);
      return isSameTheater && isSameDate;
    });
  }, [showtimes, selectedTheaterId, focusedDate]);

  // Sequential timescale milestones for horizontal header
  const milestones = useMemo(() => {
    const list = [];
    const step = 2; // plot every 2 hours
    for (let h = startHourNum; h <= endHourNum; h += step) {
      list.push(`${String(h).padStart(2, '0')}:00`);
    }
    const lastStr = `${String(endHourNum).padStart(2, '0')}:00`;
    if (!list.includes(lastStr)) {
      list.push(lastStr);
    }
    return list;
  }, [startHourNum, endHourNum]);

  // Execute client-side Mock Showtime Scheduler Algorithm for active schedule range
  const handleAutoGenerate = () => {
    if (selectedMovies.length === 0) {
      triggerToast('Vui lòng chọn ít nhất một bộ phim cần xếp lịch!', 'error');
      return;
    }

    if (dateTokens.length === 0) {
      triggerToast('Vui lòng cấu hình khoảng ngày hợp lệ!', 'error');
      return;
    }

    if (!selectedTheaterId || !activeTheater) {
      triggerToast('Vui lòng lựa chọn chi nhánh rạp hợp lệ!', 'error');
      return;
    }

    if (activeHalls.length === 0) {
      triggerToast('Cụm rạp này chưa có phòng chiếu!', 'error');
      return;
    }

    const baseStartMin = timeToMinutes(operatingHours.start || '08:00');
    const baseEndMin = timeToMinutes(operatingHours.end || '22:00');
    if (baseStartMin >= baseEndMin) {
      triggerToast('Giờ đóng cửa phải sau giờ mở cửa!', 'error');
      return;
    }

    // Phase 1: Dynamic Movie Priority Weights
    const getMovieWeight = (movie) => {
      const rating = parseFloat(movie.rating) || 0;
      const status = movie.status || '';
      const genresList = Array.isArray(movie.genres)
        ? movie.genres.map(g => g.toLowerCase())
        : String(movie.genres || movie.genre || '').toLowerCase().split(',').map(g => g.trim());
      const isAction = genresList.some(g => g === 'hành động');
      const genreScore = isAction ? 3 : 1;
      const statusScore = status === 'DANG_CHIEU' ? 5 : 2;
      return (rating * 2) + statusScore + genreScore;
    };

    const sortedMovies = [...selectedMovies]
      .map(id => movies.find(m => String(m.id) === String(id)))
      .filter(Boolean)
      .sort((a, b) => getMovieWeight(b) - getMovieWeight(a));

    const newGenerated = [];

    // Phase 2 & 3: Round-Robin Hall Balancing & Multi-Day Variance
    dateTokens.forEach((dateStr, dayIdx) => {
      // Check if the date falls on Friday, Saturday, or Sunday (Cuối tuần)
      const parts = dateStr.split('-').map(Number);
      const dateObj = new Date(parts[0], parts[1] - 1, parts[2]);
      const dayOfWeek = dateObj.getDay();
      const isWeekend = dayOfWeek === 5 || dayOfWeek === 6 || dayOfWeek === 0;

      const cleanUpBuffer = isWeekend ? 15 : 20;
      const closingTime = isWeekend ? '23:30' : (operatingHours.end || '22:00');
      const currentEndLimit = timeToMinutes(closingTime);
      const startMin = timeToMinutes(operatingHours.start || '08:00');

      // Offset starting movie index by day factor to create variation across dates
      const shiftedQueue = sortedMovies.map((_, idx) => 
        sortedMovies[(idx + dayIdx) % sortedMovies.length]
      );

      // Track cursor times individually per hall
      const hallCursors = {};
      activeHalls.forEach(hall => {
        hallCursors[hall.id] = startMin;
      });

      let activeScheduling = true;
      let loopGuard = 0;
      let dayQueue = [...shiftedQueue];

      while (activeScheduling && loopGuard < 500) {
        loopGuard++;
        let scheduledAny = false;

        // Distribute movies round-robin across available halls
        for (let hIdx = 0; hIdx < activeHalls.length; hIdx++) {
          const hall = activeHalls[hIdx];

          if (dayQueue.length === 0) {
            dayQueue = [...shiftedQueue];
          }

          if (dayQueue.length === 0) {
            activeScheduling = false;
            break;
          }

          const movie = dayQueue.shift();
          const duration = parseInt(movie.duration) || 120;
          let currentMinutes = hallCursors[hall.id];

          let foundSlot = false;
          let tempMinutes = currentMinutes;

          while (tempMinutes + duration <= currentEndLimit) {
            // Intercept and prevent showtime collisions in the active hall
            const hasCollision = newGenerated.some(st => 
              st.date === dateStr && 
              String(st.hallId) === String(hall.id) &&
              (() => {
                const stStart = timeToMinutes(st.time);
                const stMovie = movies.find(m => String(m.id) === String(st.movieId));
                const stDur = parseInt(stMovie?.duration) || 120;
                const stEnd = stStart + stDur + cleanUpBuffer;
                return (tempMinutes < stEnd && (tempMinutes + duration + cleanUpBuffer) > stStart);
              })()
            );

            if (!hasCollision) {
              foundSlot = true;
              break;
            }
            tempMinutes += 5; // Advance to resolve collision
          }

          if (foundSlot) {
            const newShowtimeItem = {
              id: `st_auto_${Math.random().toString(36).substr(2, 9)}`,
              movieId: movie.id,
              cinemaId: selectedTheaterId,
              hallId: hall.id,
              date: dateStr,
              time: minutesToTime(tempMinutes),
              price: hall.format.toUpperCase().includes('IMAX') ? 140000 : 90000
            };
            newGenerated.push(newShowtimeItem);
            
            // Advance the cursor past the scheduled screening and buffer
            hallCursors[hall.id] = tempMinutes + duration + cleanUpBuffer;
            scheduledAny = true;
          }
        }

        if (!scheduledAny) {
          activeScheduling = false; // Stop scheduling once halls are completely full
        }
      }
    });

    // Merge generated showtimes: purge target dates & theater records & write new
    const preservedShowtimes = showtimes.filter(st => {
      const isSameTheater = String(st.cinemaId) === String(selectedTheaterId);
      const isInTargetRange = dateTokens.includes(st.date);
      return !(isSameTheater && isInTargetRange);
    });

    const finalState = [...preservedShowtimes, ...newGenerated];
    updateShowtimesState(finalState);
    triggerToast(`Đã tự động lập lịch ${newGenerated.length} suất chiếu thành công cho chu kỳ ${formatDateVN(scheduleCycle.from)} - ${formatDateVN(scheduleCycle.to)}!`);
  };

  // Quick deletion trigger
  const handleClearSchedules = () => {
    if (confirm(`Bạn có chắc muốn xóa toàn bộ lịch chiếu của rạp này trong khoảng ngày từ ${formatDateVN(scheduleCycle.from)} đến ${formatDateVN(scheduleCycle.to)}?`)) {
      const preserved = showtimes.filter(st => {
        const isSameTheater = String(st.cinemaId) === String(selectedTheaterId);
        const isInTargetRange = dateTokens.includes(st.date);
        return !(isSameTheater && isInTargetRange);
      });
      updateShowtimesState(preserved);
      triggerToast('Đã dọn dẹp các suất chiếu trong khung lịch.');
    }
  };

  // Singular Schedule Pruner
  const handleDeleteShowtime = (id) => {
    const updated = showtimes.filter(st => String(st.id) !== String(id));
    updateShowtimesState(updated);
    triggerToast('Đã xóa suất chiếu thành công!');
  };

  return (
    <div className="space-y-6">
      {/* View Title */}
      <div className="flex justify-between items-center pb-2 border-b border-zinc-800">
        <div>
          <h3 className="text-base font-bold text-zinc-50 uppercase tracking-wide">TỰ ĐỘNG LẬP LỊCH CHIẾU</h3>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wide">Bản tiến độ tuyến tính theo phòng chiếu &amp; tối ưu hóa thời gian thực</p>
        </div>
      </div>

      {/* ➊ Top Control Filter Ribbon & Cycle Selectors */}
      <div className="w-full bg-zinc-900 border border-zinc-800 rounded-2xl p-5 mb-4 flex flex-wrap items-center justify-between text-xs text-zinc-300">
        <div className="flex flex-wrap items-center gap-5">
          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase">Chi Nhánh Rạp</label>
            <select 
              value={selectedTheaterId} 
              onChange={(e) => setSelectedTheaterId(e.target.value)}
              className="bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2.5 text-xs text-zinc-150 focus:outline-none focus:border-brand-coral"
            >
              {theaters.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase">Từ Ngày</label>
            <input 
              type="date" 
              value={scheduleCycle.from} 
              onChange={(e) => setScheduleCycle({ ...scheduleCycle, from: e.target.value })}
              className="bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase">Đến Ngày</label>
            <input 
              type="date" 
              value={scheduleCycle.to} 
              onChange={(e) => setScheduleCycle({ ...scheduleCycle, to: e.target.value })}
              className="bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase">Giờ Mở Cửa</label>
            <input 
              type="time" 
              value={operatingHours.start} 
              onChange={(e) => setOperatingHours({ ...operatingHours, start: e.target.value })}
              className="bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase">Giờ Đóng Cửa</label>
            <input 
              type="time" 
              value={operatingHours.end} 
              onChange={(e) => setOperatingHours({ ...operatingHours, end: e.target.value })}
              className="bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase text-amber-500">Giờ Vàng (Golden Hour)</label>
            <input 
              type="time" 
              value={goldenHour} 
              onChange={(e) => setGoldenHour(e.target.value)}
              className="bg-zinc-950 border border-amber-500/40 rounded-xl px-3 py-2 text-xs text-amber-400 focus:outline-none focus:border-amber-500"
            />
          </div>
        </div>

        <div className="flex items-center gap-3 mt-4 flex-grow justify-end lg:mt-0 lg:flex-grow-0">
          <button 
            type="button"
            onClick={handleClearSchedules}
            className="border border-red-900/40 text-red-400 hover:bg-red-950/20 px-4 py-2.5 rounded-xl text-xs font-semibold transition-all"
          >
            Dọn Lịch Chiếu
          </button>
          <button 
            type="button"
            onClick={handleAutoGenerate}
            className="bg-amber-500 hover:bg-amber-600 text-black font-bold px-5 py-2.5 rounded-xl transition-all shadow-lg shadow-amber-500/10 flex items-center gap-2"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>TỰ ĐỘNG TẠO SUẤT CHIẾU</span>
          </button>
        </div>
      </div>

      {/* ➋ Reactive 4-Day Navigation Tab Strip (Thanh chuyển đổi ngày hiển thị) */}
      <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-4 mb-4 mt-2 animate-fade-in">
        {dateTokens.map(dateStr => {
          const isActive = dateStr === focusedDate;
          const weekday = getVNWeekday(dateStr);
          const formatted = formatDateVNShort(dateStr);
          return (
            <button
              key={dateStr}
              type="button"
              onClick={() => setFocusedDate(dateStr)}
              className={isActive 
                ? "bg-amber-500 text-black font-bold shadow-lg rounded-xl px-4 py-2 text-xs transition-all"
                : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/40 px-4 py-2 text-xs rounded-xl transition-all"
              }
            >
              {weekday} - {formatted}
            </button>
          );
        })}
        {dateTokens.length === 0 && (
          <span className="text-zinc-600 text-xs italic">Cấu hình ngày hợp lệ để hiển thị các tab</span>
        )}
      </div>

      {/* LAYER 2: 100% Full-Width Linear Timetable Canvas (Tầng bảng tiến độ giải phóng bề ngang) */}
      <div className="w-full max-w-full bg-zinc-900/30 border border-zinc-800 rounded-2xl p-6 mb-6 flex flex-col overflow-x-hidden animate-fade-in">
        <div className="w-full flex flex-col">
          
          {/* ➊ X-Axis Time Header Track (Dải giờ trên cùng) */}
          <div className="flex border-b border-zinc-800 pb-3 mb-2 sticky top-0 bg-zinc-950/80 z-20 items-center">
            <div className="w-[160px] shrink-0 text-left text-xs font-bold text-zinc-500 uppercase tracking-wider">
              Phòng Chiếu
            </div>
            <div className="flex-grow relative h-6">
              {milestones.map((timeStr) => {
                const leftPercent = calculateLeftOffset(timeStr);
                return (
                  <div 
                    key={timeStr} 
                    style={{ left: `${leftPercent}%` }} 
                    className="absolute -translate-x-1/2 text-[10px] text-zinc-400 font-mono font-bold"
                  >
                    {timeStr}
                  </div>
                );
              })}
            </div>
          </div>

          {/* ➋ Y-Axis Row Allocation Grid (Hàng ngang Phòng Chiếu) */}
          <div className="flex flex-col">
            {activeHalls.map((hall) => {
              const hallShowtimes = dayShowtimes.filter(st => String(st.hallId) === String(hall.id));
              return (
                <div 
                  key={hall.id}
                  className="w-full flex items-center border-b border-zinc-800/60 min-h-[110px] relative py-4 group/row"
                >
                  
                  {/* Left Room Tag (Width fixed to 160px) */}
                  <div className="w-[160px] shrink-0 pr-4 flex flex-col justify-center select-none">
                    <span className="font-bold text-xs text-zinc-100 tracking-wide leading-snug">{hall.name}</span>
                    <span className="text-xs text-amber-500 font-mono font-bold bg-amber-500/5 px-2 py-1 rounded border border-amber-500/20 w-fit mt-1.5 uppercase">
                      {hall.format}
                    </span>
                    <span className="text-[9px] text-zinc-500 font-semibold mt-1">Sức chứa: {hall.capacity} ghế</span>
                  </div>

                  {/* Right Column Slot Area (Fluid Timeline Canvas) */}
                  <div className="flex-grow relative h-20 bg-zinc-950/20 rounded-xl border border-zinc-800/30 overflow-hidden">
                    {/* Hour milestone background grid overlays */}
                    {milestones.map((timeStr) => {
                      const leftPercent = calculateLeftOffset(timeStr);
                      return (
                        <div 
                          key={`line-${timeStr}`} 
                          style={{ left: `${leftPercent}%` }} 
                          className="absolute top-0 bottom-0 border-l border-zinc-800/20 w-[1px] pointer-events-none"
                        />
                      );
                    })}

                    {/* ➌ Visual Duration Block Capsules */}
                    {hallShowtimes.map((st) => {
                      const movie = movies.find(m => String(m.id) === String(st.movieId));
                      const movieIndex = movies.findIndex(m => String(m.id) === String(st.movieId));
                      const duration = parseInt(movie?.duration) || 120;
                      
                      const leftPercent = calculateLeftOffset(st.time);
                      const widthPercent = calculateWidthScale(duration);
                      const borderClass = movieIndex >= 0 ? getMovieColorClasses(movieIndex).split(' ')[0] : 'border-l-amber-500';
                      const endTimeStr = minutesToTime(timeToMinutes(st.time) + duration);

                      return (
                        <div
                          key={st.id}
                          style={{
                            left: `${leftPercent}%`,
                            width: `${widthPercent}%`
                          }}
                          className={`absolute border border-zinc-800/80 bg-zinc-900/95 flex flex-col justify-between items-start py-2.5 px-3 rounded-xl border-l-4 ${borderClass} shadow-md group overflow-visible h-fit min-h-[90px] transition-all duration-200 hover:scale-[1.02] hover:z-20`}
                        >
                          <div className="flex-1 w-full overflow-hidden mb-1">
                            <h4 className="text-[11px] md:text-xs font-bold text-zinc-100 whitespace-normal break-words line-clamp-2 leading-tight block w-full mb-1" title={movie?.title}>
                              {movie?.title || 'Phim Chưa Xác Định'}
                            </h4>
                          </div>

                          <div className="w-full mt-auto pt-1 border-t border-zinc-800/40 select-none flex items-center justify-between gap-1">
                            <span className="text-[9px] md:text-[10px] font-mono font-bold text-amber-400 bg-amber-500/10 border border-amber-500/20 px-1.5 py-0.5 rounded block w-fit whitespace-nowrap">
                              {st.time} - {endTimeStr}
                            </span>
                            <span className="text-[8px] md:text-[9px] font-mono font-bold text-zinc-500 shrink-0">
                              {st.price.toLocaleString('vi-VN')} đ
                            </span>
                          </div>

                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteShowtime(st.id);
                            }}
                            className="absolute top-1 right-1 p-1 rounded-md bg-black/60 border border-zinc-800 hover:bg-red-950 hover:border-red-500 hover:text-white text-zinc-400 opacity-0 group-hover:opacity-100 transition-all duration-150"
                            title="Xóa suất chiếu"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        </div>
                      );
                    })}

                    {hallShowtimes.length === 0 && (
                      <div className="absolute inset-0 flex items-center justify-center text-zinc-650 text-[10px] font-black uppercase tracking-wider select-none pointer-events-none">
                        Chưa lập lịch chiếu trong ngày
                      </div>
                    )}

                  </div>
                </div>
              );
            })}
          </div>

        </div>
      </div>

      {/* LAYER 3: Baseline Horizontal Movie Selector Grid (Tầng chọn phim chuyển xuống đáy) */}
      <div className="w-full bg-zinc-900/50 border border-zinc-800 rounded-2xl p-5 mt-4">
        <div className="flex items-center justify-between pb-3 border-b border-zinc-800">
          <div className="flex items-center gap-3">
            <span className="text-xs font-black uppercase text-zinc-300 tracking-wider">DANH SÁCH PHIM CHỈ ĐỊNH XẾP LỊCH</span>
            <span className="text-[10px] font-bold text-amber-500 px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20">
              {selectedMovies.length}/{movies.length}
            </span>
          </div>
          <div className="flex items-center gap-3 text-[10px] text-zinc-500 font-bold">
            <button 
              type="button"
              onClick={() => setSelectedMovies(movies.map(m => String(m.id)))}
              className="hover:text-amber-500 transition-colors uppercase"
            >
              Chọn tất cả
            </button>
            <span className="text-zinc-700">|</span>
            <button 
              type="button"
              onClick={() => setSelectedMovies([])}
              className="hover:text-red-400 transition-colors uppercase"
            >
              Bỏ chọn
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mt-3">
          {movies.map((movie) => {
            const isChecked = selectedMovies.includes(String(movie.id));
            return (
              <div 
                key={movie.id} 
                onClick={() => {
                  const idStr = String(movie.id);
                  if (isChecked) {
                    setSelectedMovies(selectedMovies.filter(id => id !== idStr));
                  } else {
                    setSelectedMovies([...selectedMovies, idStr]);
                  }
                }}
                className={`bg-zinc-950 border p-3 rounded-xl flex items-center gap-3 hover:border-zinc-700 transition-colors cursor-pointer select-none ${
                  isChecked 
                    ? 'border-amber-500/50 bg-amber-500/5 text-white' 
                    : 'border-zinc-800/80 text-zinc-400'
                }`}
              >
                <input 
                  type="checkbox"
                  checked={isChecked}
                  onChange={() => {}} // Synced on parent div onClick
                  className="rounded border-zinc-800 bg-zinc-950 text-amber-500 focus:ring-amber-500/20 w-3.5 h-3.5 shrink-0 pointer-events-none"
                />
                <img 
                  src={movie.imageUrl || movie.posterUrl} 
                  alt={movie.title}
                  className="w-8 h-12 object-cover rounded shrink-0"
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200&auto=format&fit=crop&q=60';
                  }}
                />
                <div className="flex-1 min-w-0">
                  <h4 className="text-[11px] font-medium text-zinc-200 truncate leading-snug" title={movie.title}>{movie.title}</h4>
                  <p className="text-[9px] text-zinc-500 mt-0.5">{movie.duration} phút</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
