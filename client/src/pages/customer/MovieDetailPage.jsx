import { useState, useMemo } from 'react';
import { Play, Clock, ArrowLeft, Star } from 'lucide-react';
import { useData } from '../../contexts/DataContext';
import TrailerModal from '../../components/common/TrailerModal';

export default function MovieDetailView({ movieId, onSelectShowtime, onBack }) {
  const { movies, showtimes, cinemas } = useData();
  const [selectedDateIndex, setSelectedDateIndex] = useState(0);
  const [isTrailerOpen, setIsTrailerOpen] = useState(false);

  // Find the current movie
  const movie = useMemo(() => {
    const found = movies.find((m) => String(m.id) === String(movieId)) || movies[0];
    if (found) {
      return {
        ...found,
        backdrop_url: found.backdrop_url || found.backdropUrl || found.posterUrl || found.image,
        poster_url: found.poster_url || found.posterUrl || found.image
      };
    }
    return found;
  }, [movies, movieId]);

  // Generate the next 5 dates starting from today
  const dates = useMemo(() => {
    const list = [];
    const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    for (let i = 0; i < 5; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      
      const label = i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : dayNames[d.getDay()];

      const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      list.push({
        label,
        dateStr,
        fullDate: d.toLocaleDateString('vi-VN')
      });
    }
    return list;
  }, []);

  const activeDate = dates[selectedDateIndex];

  // Age rating styling maps
  const ageRatingColor = (rating) => {
    switch (rating) {
      case 'P': return 'bg-emerald-500 text-black';
      case 'T13': return 'bg-blue-500 text-white';
      case 'T16': return 'bg-orange-500 text-white';
      case 'T18': return 'bg-red-600 text-white';
      default: return 'bg-zinc-700 text-white';
    }
  };

  const handleTimeClick = (cinemaName, time, format) => {
    onSelectShowtime({
      movieId: movie.id,
      movieTitle: movie.title,
      date: activeDate.dateStr,
      fullDate: activeDate.fullDate,
      cinema: cinemaName,
      time,
      format
    });
  };

  const cinemaList = useMemo(() => {
    return cinemas.map(c => c.name);
  }, [cinemas]);

  // Dynamic showtimes helper
  const getShowtimesForCinema = (cinemaName, format) => {
    const foundCinema = cinemas.find(c => c.name === cinemaName);
    if (!foundCinema) {
      return format.includes('IMAX') ? ["13:15", "16:45", "19:30"] : ["09:30", "13:15", "16:45", "19:30", "22:15"];
    }
    
    const formatToYYYYMMDD = (str) => {
      if (!str) return '';
      if (str.includes('-')) return str; // already YYYY-MM-DD
      const parts = str.split('/');
      if (parts.length === 3) {
        return `${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}`;
      }
      return str;
    };
    
    const formattedTarget = formatToYYYYMMDD(activeDate.fullDate);
    
    const matching = showtimes.filter(st => {
      const matchMovie = String(st.movieId) === String(movie.id);
      const matchCinema = String(st.cinemaId) === String(foundCinema.id);
      const matchDate = formatToYYYYMMDD(st.date) === formattedTarget;
      
      const hall = foundCinema.halls?.find(h => String(h.id) === String(st.hallId));
      const hallFormat = hall ? hall.format : '2D Digital';
      
      const isImax = format.includes('IMAX');
      const isHallImax = hallFormat.toUpperCase().includes('IMAX');
      const matchFormat = isImax ? isHallImax : !isHallImax;
      
      return matchMovie && matchCinema && matchDate && matchFormat;
    });

    if (matching.length === 0) {
      // Fallback default times so booking is always testable
      return format.includes('IMAX') ? ["13:15", "16:45", "19:30"] : ["09:30", "13:15", "16:45", "19:30", "22:15"];
    }

    return [...new Set(matching.map(st => st.time))].sort();
  };

  return (
    <div className="relative w-full min-h-screen bg-brand-dark text-zinc-100 overflow-x-hidden pb-16">
      {/* 🟢 AMBIENT CINEMATIC BACKDROP LAYER */}
      <div className="absolute top-0 left-0 w-full h-[520px] md:h-[600px] overflow-hidden z-0 pointer-events-none select-none">
        <img 
          src={movie?.backdrop_url || movie?.poster_url} 
          alt="" 
          className="w-full h-full object-cover filter blur-2xl scale-110 opacity-20 transform-gpu will-change-transform"
          loading="eager"
        />
        {/* Infinite Depth Cinema Shroud Overlays */}
        <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/75 to-transparent" />
        <div className="absolute inset-0 bg-brand-dark/50" />
      </div>

      {/* Back Button */}
      <div className="relative z-20 w-full max-w-7xl mx-auto px-4 sm:px-6 pt-6">
        <button
          onClick={onBack}
          className="flex items-center gap-2 bg-black/40 hover:bg-brand-coral/25 text-white border border-white/10 hover:border-brand-coral font-bold px-4 py-2 rounded-full transition-all duration-300 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại</span>
        </button>
      </div>

      {/* Foreground Content Alignment Grid */}
      <div className="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 pt-20 pb-12 flex flex-col md:flex-row gap-8 items-center md:items-end">
        {/* Left Column: Poster */}
        <div className="w-52 md:w-64 aspect-[2/3] rounded-2xl shadow-[0_25px_50px_-12px_rgba(0,0,0,0.8)] border border-zinc-800 shrink-0 transform-gpu relative overflow-hidden bg-zinc-900 group">
          <img
            src={movie.posterUrl || movie.image}
            alt={movie.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            onError={(e) => {
              e.target.onerror = null;
              e.target.src = 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80';
            }}
          />
          <span className={`absolute top-3 left-3 z-10 text-xs font-black px-2.5 py-1 rounded shadow-lg ${ageRatingColor(movie.ageRating)}`}>
            {movie.ageRating}
          </span>
        </div>

        {/* Right Column: Info */}
        <div className="flex-grow flex flex-col justify-end text-center md:text-left mt-4 md:mt-0">
          <div className="flex items-center justify-center md:justify-start gap-2 mb-2">
            <div className="flex items-center gap-1 bg-brand-yellow text-black text-xs font-black px-2 py-0.5 rounded shadow">
              <Star className="w-3 h-3 fill-current" />
              {(movie.rating || 0).toFixed(1)}
            </div>
            <span className="text-zinc-400 text-sm">| {movie.genre || movie.genres?.join(', ')}</span>
          </div>
          
          <h1 className="text-3xl md:text-5xl font-black text-white uppercase tracking-tight mb-4 font-sans drop-shadow-[0_4px_12px_rgba(0,0,0,0.9)]">
            {movie.title}
          </h1>

          <div className="flex items-center justify-center md:justify-start gap-2 text-zinc-400 text-sm mb-6">
            <Clock className="w-4 h-4 text-brand-coral" />
            <span>{movie.duration} phút</span>
          </div>

          <p className="text-zinc-300 text-sm md:text-base leading-relaxed mb-6 max-w-3xl drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
            {movie.synopsis}
          </p>

          <div className="flex justify-center md:justify-start">
            <button
              onClick={() => setIsTrailerOpen(true)}
              className="bg-brand-coral/10 border border-brand-coral hover:bg-brand-coral hover:text-brand-dark text-brand-coral font-black px-6 py-3 rounded-xl text-xs uppercase tracking-widest transition-all duration-300 shadow-lg shadow-brand-coral/5 flex items-center gap-2 mt-6 cursor-pointer"
            >
              <Play className="w-4 h-4 fill-current" />
              Xem Trailer
            </button>
          </div>
        </div>
      </div>

      {/* Main Details Container */}
      <div className="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 mt-8">
        {/* Dynamic Showtime Scheduler Accordion & System */}
        <div className="mt-8 bg-zinc-900/50 border border-zinc-800/80 rounded-3xl p-6 md:p-8 backdrop-blur-sm">
          <h2 className="text-xl md:text-2xl font-black text-white uppercase tracking-wide mb-6">
            Lịch Chiếu & Đặt Vé
          </h2>

          {/* Horizontal Date Selector */}
          <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-zinc-800">
            {dates.map((item, idx) => (
              <button
                key={idx}
                onClick={() => setSelectedDateIndex(idx)}
                className={`flex flex-col items-center justify-center px-5 py-3 rounded-2xl min-w-[90px] border transition-all duration-300 shrink-0 ${
                  selectedDateIndex === idx
                    ? 'bg-brand-coral border-brand-coral text-white font-bold scale-105 shadow-lg shadow-brand-coral/20'
                    : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:border-zinc-700'
                }`}
              >
                <span className="text-[10px] uppercase tracking-wider mb-1">{item.label}</span>
                <span className="text-base font-black">{item.dateStr}</span>
              </button>
            ))}
          </div>

          {/* Cinema Accordion Cluster Rows */}
          <div className="mt-8 space-y-6">
            {cinemaList.map((cinema, cIdx) => {
              const standardTimes = getShowtimesForCinema(cinema, '2D DIGITAL');
              const imaxTimes = getShowtimesForCinema(cinema, 'IMAX 3D');
              
              return (
                <div
                  key={cIdx}
                  className="bg-zinc-950/40 border border-zinc-800/60 rounded-2xl p-5 md:p-6"
                >
                  <h3 className="text-base md:text-lg font-bold text-white mb-4 border-l-2 border-brand-coral pl-3">
                    {cinema}
                  </h3>

                  <div className="space-y-4">
                    {/* 2D Showtimes */}
                    {standardTimes.length > 0 && (
                      <div className="flex flex-col sm:flex-row sm:items-center gap-4 py-2 border-b border-zinc-900">
                        <span className="text-xs font-black text-brand-coral uppercase tracking-widest shrink-0 w-24">
                          2D DIGITAL
                        </span>
                        <div className="flex flex-wrap gap-3">
                          {standardTimes.map((time, tIdx) => (
                            <button
                              key={tIdx}
                              onClick={() => handleTimeClick(cinema, time, '2D DIGITAL')}
                              className="bg-zinc-900 hover:bg-brand-coral text-zinc-300 hover:text-white border border-zinc-800 hover:border-brand-coral text-xs md:text-sm font-semibold py-2 px-4 rounded-xl transition-all duration-300"
                            >
                              {time}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* IMAX 3D Showtimes */}
                    {imaxTimes.length > 0 && (
                      <div className="flex flex-col sm:flex-row sm:items-center gap-4 py-2">
                        <span className="text-xs font-black text-brand-yellow uppercase tracking-widest shrink-0 w-24">
                          IMAX 3D
                        </span>
                        <div className="flex flex-wrap gap-3">
                          {imaxTimes.map((time, tIdx) => (
                            <button
                              key={tIdx}
                              onClick={() => handleTimeClick(cinema, time, 'IMAX 3D')}
                              className="bg-zinc-900 hover:bg-brand-yellow text-zinc-300 hover:text-black border border-zinc-800 hover:border-brand-yellow text-xs md:text-sm font-semibold py-2 px-4 rounded-xl transition-all duration-300"
                            >
                              {time}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Embedded Trailer Modal */}
      <TrailerModal
        isOpen={isTrailerOpen}
        onClose={() => setIsTrailerOpen(false)}
        trailerId={movie.trailerId}
      />
    </div>
  );
}
