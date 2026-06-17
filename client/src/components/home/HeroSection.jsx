import { useState, useMemo } from 'react';
import { Play, ChevronDown } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useData } from '../../contexts/DataContext';

const SHOWTIMES = ["09:30", "13:15", "16:45", "19:30", "22:15"];

export default function Hero({ onBookTicket, onNavigate }) {
  const { isAuthenticated } = useAuth();
  const { movies, cinemas } = useData();
  const videoSource = 'https://res.cloudinary.com/dqc4hufot/video/upload/0603_ugvo4m.mp4';

  // Selection states
  const [selectedMovieId, setSelectedMovieId] = useState('');
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [selectedDateIndex, setSelectedDateIndex] = useState('');
  const [selectedTime, setSelectedTime] = useState('');

  // 4 consecutive dates starting from today
  const dates = useMemo(() => {
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

  const selectedDate = selectedDateIndex !== '' ? dates[selectedDateIndex] : null;
  const isBookingFormValid = selectedMovieId && selectedCinemaId && selectedDateIndex !== '' && selectedTime;

  const handleQuickBooking = () => {
    if (!isBookingFormValid) return;

    const movie = movies.find(m => String(m.id) === String(selectedMovieId));
    const cinema = cinemas.find(t => String(t.id) === String(selectedCinemaId));

    const bookingPayload = {
      movieId: movie.id,
      movieTitle: movie.title,
      cinema: cinema.name,
      time: selectedTime,
      format: '2D DIGITAL',
      date: selectedDate.date,
      fullDate: selectedDate.fullDate,
      selectedSeats: []
    };

    if (!isAuthenticated) {
      sessionStorage.setItem('lora_pending_booking', JSON.stringify(bookingPayload));
      localStorage.setItem('lora_pending_booking', JSON.stringify(bookingPayload));
      sessionStorage.setItem('lora_booking_interrupted', 'true');
      onNavigate('login', null);
    } else {
      onBookTicket(bookingPayload);
    }
  };

  return (
    <>
      <section className="relative min-h-[85vh] md:min-h-[90vh] flex flex-col justify-between pt-24 pb-16 px-6 md:px-12 overflow-hidden bg-brand-dark">
        
        {/* Background Video Implementation */}
        <div className="absolute inset-0 w-full h-full z-0 overflow-hidden">
          <video
            autoPlay
            loop
            muted
            playsInline
            className="w-full h-full object-cover scale-105 opacity-75 transition-all duration-700" 
            src={videoSource}
          />
        </div>

        {/* Backdrop overlay gradient */}
        <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/20 to-brand-dark/40 z-10 backdrop-blur-[1px]"></div>

        {/* Main Left Content Area */}
        <div className="relative z-20 max-w-4xl mt-auto mb-4 animate-in fade-in slide-in-from-bottom duration-1000">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-red-950/40 backdrop-blur-md border border-red-500/30 rounded-full shadow-[0_0_15px_rgba(239,68,68,0.15)] mb-4 animate-fade-in">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
            </span>
            <span className="text-red-400 text-xs font-bold uppercase tracking-widest">
              TOP PICK | LỰA CHỌN CỦA BẠN HÔM NAY
            </span>
          </div>

          <h1 className="text-4xl md:text-7xl font-black text-white tracking-tight uppercase mb-2 leading-none">
            KHÁM PHÁ THẾ GIỚI <br />
            <span className="text-gradient">PHIM CỦA BẠN</span>
          </h1>

          <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4 mb-6">
            <span className="text-2xl md:text-3xl font-black text-brand-coral tracking-widest uppercase">
              LORA FILM
            </span>
            <span className="hidden sm:inline text-gray-500">|</span>
            <span className="text-sm md:text-base text-gray-400 font-medium italic tracking-wider">
              "MOVIE TICKETS, YOUR WAY"
            </span>
          </div>

          <div className="flex flex-wrap gap-4">
            <button 
              onClick={() => {
                const element = document.getElementById('phim');
                element?.scrollIntoView({ behavior: 'smooth' });
              }}
              className="group flex items-center gap-2 bg-brand-coral text-white font-bold px-8 py-4 rounded-full hover:bg-opacity-95 hover:shadow-brand-coral/25 shadow-lg transition-all transform hover:scale-105 duration-300"
            >
              <Play className="w-5 h-5 fill-current text-white" />
              ĐẶT VÉ NGAY
            </button>
            
            <button 
              onClick={() => {
                const element = document.getElementById('steps');
                element?.scrollIntoView({ behavior: 'smooth' });
              }}
              className="flex items-center gap-2 bg-white/5 hover:bg-white/10 text-white border border-white/10 font-bold px-6 py-4 rounded-full transition-all duration-300"
            >
              Tìm hiểu thêm
            </button>
          </div>
        </div>
      </section>

      {/* Premium Cinematic Dark Capsule Booking Widget */}
      <div className="relative z-30 w-full max-w-5xl mx-auto -mt-14 md:-mt-16 px-4">
        <div className="bg-zinc-950/65 backdrop-blur-md border border-zinc-800 rounded-full flex items-center justify-between p-2 h-14 relative">
          
          <div className="flex-1 grid grid-cols-4 h-full items-center">
            
            {/* Step 1: Phim */}
            <div className="flex items-center gap-2 px-4 border-r border-zinc-800/60 h-8">
              <div className="flex-1 min-w-0">
                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Phim</label>
                <select 
                  value={selectedMovieId} 
                  onChange={(e) => {
                    setSelectedMovieId(e.target.value);
                    setSelectedCinemaId('');
                    setSelectedDateIndex('');
                    setSelectedTime('');
                  }}
                  className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate"
                >
                  <option value="" className="bg-zinc-950 text-zinc-550">Chọn Phim...</option>
                  {movies.map(movie => (
                    <option key={movie.id} value={movie.id} className="bg-zinc-950 text-white">{movie.title}</option>
                  ))}
                </select>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
            </div>

            {/* Step 2: Rạp */}
            <div className={`flex items-center gap-2 px-4 border-r border-zinc-800/60 h-8 transition-opacity ${!selectedMovieId ? 'opacity-40' : ''}`}>
              <div className="flex-1 min-w-0">
                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Rạp</label>
                <select 
                  value={selectedCinemaId} 
                  onChange={(e) => {
                    setSelectedCinemaId(e.target.value);
                    setSelectedDateIndex('');
                    setSelectedTime('');
                  }}
                  disabled={!selectedMovieId}
                  className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate disabled:cursor-not-allowed"
                >
                  <option value="" className="bg-zinc-950 text-zinc-550">Chọn Rạp...</option>
                  {cinemas.map(t => (
                    <option key={t.id} value={t.id} className="bg-zinc-950 text-white">{t.name}</option>
                  ))}
                </select>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
            </div>

            {/* Step 3: Ngày */}
            <div className={`flex items-center gap-2 px-4 border-r border-zinc-800/60 h-8 transition-opacity ${!selectedCinemaId ? 'opacity-40' : ''}`}>
              <div className="flex-1 min-w-0">
                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Ngày</label>
                <select 
                  value={selectedDateIndex} 
                  onChange={(e) => {
                    setSelectedDateIndex(e.target.value);
                    setSelectedTime('');
                  }}
                  disabled={!selectedCinemaId}
                  className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate disabled:cursor-not-allowed"
                >
                  <option value="" className="bg-zinc-950 text-zinc-550">Chọn Ngày...</option>
                  {dates.map((d, idx) => (
                    <option key={idx} value={idx} className="bg-zinc-950 text-white">{d.label} ({d.date})</option>
                  ))}
                </select>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
            </div>

            {/* Step 4: Suất */}
            <div className={`flex items-center gap-2 px-4 h-8 transition-opacity ${selectedDateIndex === '' ? 'opacity-40' : ''}`}>
              <div className="flex-1 min-w-0">
                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Suất</label>
                <select 
                  value={selectedTime} 
                  onChange={(e) => setSelectedTime(e.target.value)}
                  disabled={selectedDateIndex === ''}
                  className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate disabled:cursor-not-allowed"
                >
                  <option value="" className="bg-zinc-950 text-zinc-550">Chọn Suất...</option>
                  {SHOWTIMES.map((time, idx) => (
                    <option key={idx} value={time} className="bg-zinc-950 text-white">{time}</option>
                  ))}
                </select>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
            </div>

          </div>

          {/* Primary Action Rounded Pill Button */}
          <button
            onClick={handleQuickBooking}
            disabled={!isBookingFormValid}
            className={`bg-orange-500 hover:bg-orange-600 text-white font-medium px-6 h-10 rounded-full transition-all duration-200 shadow-md shadow-orange-500/10 cursor-pointer text-sm uppercase tracking-wider select-none shrink-0 ${
              !isBookingFormValid ? 'opacity-40 pointer-events-none' : ''
            }`}
          >
            Mua vé nhanh
          </button>

        </div>
      </div>
    </>
  );
}