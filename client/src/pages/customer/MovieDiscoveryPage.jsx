import { useState, useMemo } from 'react';
import { 
  Star, Eye, ThumbsUp, Play, RefreshCw, AlertCircle, Film 
} from 'lucide-react';
import { useData } from '../../contexts/DataContext';
import TrailerModal from '../../components/common/TrailerModal';

// Map country dynamically based on movie title/ID
const getMovieCountry = (movie) => {
  const vnTitles = [
    'Lat Mat 7: Mot Dieu Uoc', 
    'Nam Muoi', 
    'De Men Cuoc Phieu Luu Toi Xu', 
    'Buon Than Ban Thanh', 
    'Tham Tu Kien: Ky An Buong Bau', 
    'Chien Binh Bao Tap'
  ];
  const usTitles = [
    'Tu Vu Tru John Wick: Ballerina', 
    'Biet Doi Sam Set', 
    'Captain America: The Gioi Moi', 
    'Minecraft: Phim Dien Anh', 
    'Superman: Kien Thiet', 
    'Biet Doi Bat Hao'
  ];
  const jpTitles = ['Conan: Ky An Tram Tau'];
  const krTitles = ['Dinh Thinh La Yeu'];

  if (vnTitles.some(t => movie.title.toLowerCase().includes(t.toLowerCase()))) return 'Vietnam';
  if (usTitles.some(t => movie.title.toLowerCase().includes(t.toLowerCase()))) return 'USA';
  if (jpTitles.some(t => movie.title.toLowerCase().includes(t.toLowerCase()))) return 'Japan';
  if (krTitles.some(t => movie.title.toLowerCase().includes(t.toLowerCase()))) return 'Korea';
  return 'USA'; // fallback default
};

// Map release year dynamically based on status/ID
const getMovieYear = (movie) => {
  if (movie.status === 'COMING_SOON' || movie.status === 'SAP_CHIEU') return '2026';
  if (String(movie.id).replace('m', '') % 2 === 0) return '2025';
  return '2024';
};

export default function MovieDiscoveryView({ onBackHome, onBuyTicket, initialTab = 'ALL' }) {
  const { movies, cinemas } = useData();
  const cinemaClusters = useMemo(() => cinemas.map(c => c.name), [cinemas]);
  // Upper horizontal bar filter states
  const [selectedGenre, setSelectedGenre] = useState('ALL');
  const [selectedCountry, setSelectedCountry] = useState('ALL');
  const [selectedYear, setSelectedYear] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState(initialTab);
  const [selectedSort, setSelectedSort] = useState('VIEWS'); // 'VIEWS' | 'RATING'

  // Like toggles state
  const [likedMovies, setLikedMovies] = useState({});

  // Active trailer modal
  const [activeTrailerId, setActiveTrailerId] = useState(null);

  // Quick booking sidebar form states
  const [quickMovieId, setQuickMovieId] = useState('');
  const [quickCinema, setQuickCinema] = useState('');
  const [quickDate, setQuickDate] = useState('');

  // Static options datasets
  const genresList = [
    { label: 'Tất cả thể loại', value: 'ALL' },
    { label: 'Hành Động', value: 'Hanh Dong' },
    { label: 'Lãng Mạn', value: 'Lang Man' },
    { label: 'Hài Hước', value: 'Hai Huoc' },
    { label: 'Kịch Tính', value: 'Kich Tinh' },
    { label: 'Viễn Tưởng', value: 'Vien Tuong' },
    { label: 'Hoạt Hình', value: 'Hoat Hinh' },
    { label: 'Kinh Dị', value: 'Kinh Di' },
    { label: 'Trinh Thám', value: 'Trinh Tham' },
    { label: 'Gia Đình', value: 'Gia Dinh' }
  ];

  const countriesList = [
    { label: 'Tất cả quốc gia', value: 'ALL' },
    { label: 'Việt Nam', value: 'Vietnam' },
    { label: 'Mỹ', value: 'USA' },
    { label: 'Hàn Quốc', value: 'Korea' },
    { label: 'Nhật Bản', value: 'Japan' }
  ];

  const yearsList = [
    { label: 'Tất cả năm', value: 'ALL' },
    { label: '2024', value: '2024' },
    { label: '2025', value: '2025' },
    { label: '2026', value: '2026' }
  ];

  const statusList = [
    { label: 'Tất cả trạng thái', value: 'ALL' },
    { label: 'Phim Đang Chiếu', value: 'NOW_SHOWING' },
    { label: 'Phim Sắp Chiếu', value: 'COMING_SOON' }
  ];

  const sortList = [
    { label: 'Xem Nhiều Nhất', value: 'VIEWS' },
    { label: 'Đánh Giá Cao Nhất', value: 'RATING' }
  ];

  // Compute views dynamically
  const getMovieViews = (movie) => {
    const baseViews = movie.id * 1530 + 420;
    return baseViews + (likedMovies[movie.id] ? 1 : 0);
  };

  // 1. Reactive Filtration Engine via useMemo
  const filteredAndSortedMovies = useMemo(() => {
    const getViews = (m) => {
      const baseViews = m.id * 1530 + 420;
      return baseViews + (likedMovies[m.id] ? 1 : 0);
    };

    let result = [...movies];

    // Filter by Genre
    if (selectedGenre !== 'ALL') {
      result = result.filter(m => 
        m.genres && m.genres.some(g => g.toLowerCase().includes(selectedGenre.toLowerCase()))
      );
    }

    // Filter by Country
    if (selectedCountry !== 'ALL') {
      result = result.filter(m => getMovieCountry(m) === selectedCountry);
    }

    // Filter by Year
    if (selectedYear !== 'ALL') {
      result = result.filter(m => getMovieYear(m) === selectedYear);
    }

    if (selectedStatus !== 'ALL') {
      result = result.filter(m => {
        if (selectedStatus === 'NOW_SHOWING') {
          return m.status === 'NOW_SHOWING' || m.status === 'DANG_CHIEU' || m.status === 'SHOWING';
        }
        if (selectedStatus === 'COMING_SOON') {
          return m.status === 'COMING_SOON' || m.status === 'SAP_CHIEU' || m.status === 'UPCOMING';
        }
        return m.status === selectedStatus;
      });
    }

    // Sort
    if (selectedSort === 'VIEWS') {
      result.sort((a, b) => getViews(b) - getViews(a));
    } else if (selectedSort === 'RATING') {
      result.sort((a, b) => b.rating - a.rating);
    }

    return result;
  }, [selectedGenre, selectedCountry, selectedYear, selectedStatus, selectedSort, likedMovies]);

  // Reset all sub-filters
  const handleResetFilters = () => {
    setSelectedGenre('ALL');
    setSelectedCountry('ALL');
    setSelectedYear('ALL');
    setSelectedStatus('ALL');
    setSelectedSort('VIEWS');
  };

  // Like toggler
  const toggleLike = (e, movieId) => {
    e.stopPropagation();
    setLikedMovies(prev => ({
      ...prev,
      [movieId]: !prev[movieId]
    }));
  };

  // Launch trailer handler
  const handleTrailerOpen = (e, trailerId) => {
    e.stopPropagation();
    setActiveTrailerId(trailerId);
  };

  // Launch ticket booking from left panel card
  const handleBuyTicketClick = (e, movie) => {
    e.stopPropagation();
    const defaultBooking = {
      movieId: movie.id,
      movieTitle: movie.title,
      cinema: 'Lora Nguyen Du',
      time: '19:30',
      format: '2D DIGITAL',
      date: new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      fullDate: new Date().toLocaleDateString('vi-VN'),
      selectedSeats: []
    };
    onBuyTicket(defaultBooking);
  };

  // Submit quick booking sidebar widget
  const handleQuickBookingSubmit = (e) => {
    e.preventDefault();
    if (!quickMovieId || !quickCinema || !quickDate) return;

    const matchedMovie = movies.find(m => String(m.id) === String(quickMovieId));
    if (!matchedMovie) return;

    const bookingPayload = {
      movieId: matchedMovie.id,
      movieTitle: matchedMovie.title,
      cinema: quickCinema,
      time: '19:30', // default time slot
      format: '2D DIGITAL',
      date: quickDate === 'Hôm nay' 
        ? new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
        : new Date(Date.now() + 86400000).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      fullDate: quickDate === 'Hôm nay' 
        ? new Date().toLocaleDateString('vi-VN')
        : new Date(Date.now() + 86400000).toLocaleDateString('vi-VN'),
      selectedSeats: []
    };
    onBuyTicket(bookingPayload);
  };


  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen py-8 px-4 md:px-8">
      <div className="max-w-7xl mx-auto space-y-8">
        
        {/* Header Breadcrumbs block */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-zinc-900">
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Khám Phá Điện Ảnh</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">Lọc danh sách phim đang chiếu & sắp chiếu theo nhu cầu</p>
          </div>
          <button
            onClick={onBackHome}
            className="text-xs font-bold text-zinc-500 hover:text-brand-coral transition-colors self-start sm:self-center"
          >
            Quay lại trang chủ
          </button>
        </div>

        {/* 1. UPPER HORIZONTAL BAR: Multi-Criteria Filter Widget */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-5 flex flex-wrap gap-4 items-center justify-between shadow-2xl">
          
          <div className="flex flex-wrap gap-3 flex-grow lg:flex-nowrap">
            {/* Thể loại Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Thể Loại</label>
              <select
                value={selectedGenre}
                onChange={(e) => setSelectedGenre(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-coral focus:outline-none transition-colors"
              >
                {genresList.map(g => (
                  <option key={g.value} value={g.value}>{g.label}</option>
                ))}
              </select>
            </div>

            {/* Quốc gia Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Quốc Gia</label>
              <select
                value={selectedCountry}
                onChange={(e) => setSelectedCountry(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-coral focus:outline-none transition-colors"
              >
                {countriesList.map(c => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>

            {/* Năm Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Năm Phát Hành</label>
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-coral focus:outline-none transition-colors"
              >
                {yearsList.map(y => (
                  <option key={y.value} value={y.value}>{y.label}</option>
                ))}
              </select>
            </div>

            {/* Đang chiếu / Sắp chiếu Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Trạng Thái</label>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-coral focus:outline-none transition-colors"
              >
                {statusList.map(s => (
                  <option key={s.value} value={s.value}>{s.label}</option>
                ))}
              </select>
            </div>

            {/* Sắp xếp Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Sắp Xếp Theo</label>
              <select
                value={selectedSort}
                onChange={(e) => setSelectedSort(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-coral focus:outline-none transition-colors"
              >
                {sortList.map(s => (
                  <option key={s.value} value={s.value}>{s.label}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Reset Filters action */}
          <button
            onClick={handleResetFilters}
            className="flex items-center gap-1.5 bg-zinc-950 hover:bg-zinc-800 text-zinc-400 hover:text-white border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-bold transition-all focus:outline-none shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Đặt lại bộ lọc</span>
          </button>

        </div>

        {/* ASYMMETRIC SPLIT SCREEN VIEW GRID */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT PANEL: Filtered Movie Row List (2/3 Width) */}
          <div className="lg:col-span-2 space-y-6">
            
            {filteredAndSortedMovies.length === 0 ? (
              <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-12 text-center space-y-3">
                <AlertCircle className="w-12 h-12 text-zinc-650 mx-auto" />
                <h3 className="text-sm font-black uppercase tracking-wider text-white">Không Tìm Thấy Phim Phù Hợp</h3>
                <p className="text-zinc-500 text-xs max-w-sm mx-auto">
                  Không có tác phẩm nào khớp với tiêu chuẩn tìm kiếm của bạn. Hãy thử thay đổi bộ lọc hoặc đặt lại bộ lọc.
                </p>
                <button
                  onClick={handleResetFilters}
                  className="bg-brand-coral hover:bg-opacity-95 text-white font-bold py-2.5 px-6 rounded-xl text-xs uppercase tracking-wider transition-colors inline-block mt-2 focus:outline-none"
                >
                  Đặt lại bộ lọc
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {filteredAndSortedMovies.map((movie) => {
                  const views = getMovieViews(movie);
                  const isLiked = likedMovies[movie.id];
                  
                  return (
                    <div 
                      key={movie.id}
                      className="bg-zinc-900 border border-zinc-800 hover:border-zinc-700/80 rounded-3xl p-4 flex gap-4 md:gap-6 shadow-xl transition-all duration-300 relative group overflow-hidden"
                    >
                      {/* Image Block (Left Aspect Ratio container) */}
                      <div className="w-28 sm:w-36 shrink-0 aspect-[2/3] rounded-2xl overflow-hidden bg-zinc-950 border border-zinc-850 relative">
                        <img 
                          src={movie.posterUrl || movie.image} 
                          alt={movie.title}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        
                        {/* Hover dual-button overlay on desktop */}
                        <div className="absolute inset-0 bg-black/75 opacity-0 group-hover:opacity-100 flex flex-col items-center justify-center gap-3 transition-opacity duration-300 hidden md:flex p-2 z-10">
                          <button
                            onClick={(e) => handleBuyTicketClick(e, movie)}
                            className="w-full bg-brand-coral hover:bg-opacity-90 text-white font-bold py-2 rounded-xl text-[10px] uppercase tracking-wider transition-colors"
                          >
                            Mua Vé
                          </button>
                          <button
                            onClick={(e) => handleTrailerOpen(e, movie.trailerId)}
                            className="w-full bg-zinc-900 border border-zinc-700 hover:bg-zinc-800 text-white font-bold py-2 rounded-xl text-[10px] uppercase tracking-wider transition-colors flex items-center justify-center gap-1"
                          >
                            <Play className="w-3 h-3 fill-white" />
                            <span>Trailer</span>
                          </button>
                        </div>
                      </div>

                      {/* Right Details Block */}
                      <div className="flex-grow flex flex-col justify-between py-1">
                        <div className="space-y-2">
                          
                          {/* Title + Like Pill Header */}
                          <div className="flex items-start justify-between gap-4">
                            <h3 className="font-black text-sm md:text-base text-white leading-snug group-hover:text-brand-coral transition-colors">
                              {movie.title}
                            </h3>
                            
                            {/* Blue like action toggle pill */}
                            <button
                              type="button"
                              onClick={(e) => toggleLike(e, movie.id)}
                              className={`shrink-0 flex items-center gap-1.5 px-3 py-1 rounded-full border text-[9px] font-black uppercase transition-all duration-300 ${
                                isLiked 
                                  ? 'bg-blue-600 border-blue-600 text-white shadow-lg shadow-blue-600/20' 
                                  : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:text-white hover:border-zinc-700'
                              }`}
                            >
                              <ThumbsUp className={`w-3 h-3 ${isLiked ? 'fill-white' : ''}`} />
                              <span>{isLiked ? 'Đã Thích' : 'Thích'}</span>
                            </button>
                          </div>

                          {/* Meta Tags */}
                          <div className="flex flex-wrap gap-2 items-center">
                            <span className="text-[9px] font-black uppercase tracking-wider bg-brand-coral/10 text-brand-coral border border-brand-coral/20 px-2 py-0.5 rounded">
                              {movie.ageRating || 'T13'}
                            </span>
                            <span className="text-[10px] text-zinc-400 font-bold">
                              {movie.duration || '112 phút'}
                            </span>
                            <span className="text-[10px] text-zinc-500 font-semibold">
                              {getMovieCountry(movie)}
                            </span>
                            <span className="text-[10px] text-zinc-500 font-semibold">
                              {getMovieYear(movie)}
                            </span>
                          </div>

                          {/* Views + rating badges */}
                          <div className="flex gap-2">
                            <span className="text-[9px] font-bold text-zinc-400 bg-zinc-950 border border-zinc-850 px-2 py-0.5 rounded-full flex items-center gap-1 select-none">
                              <Eye className="w-3 h-3 text-zinc-500" />
                              {views.toLocaleString('vi-VN')} lượt xem
                            </span>
                            <span className="text-[9px] font-bold text-brand-yellow bg-zinc-950 border border-zinc-850 px-2 py-0.5 rounded-full flex items-center gap-1 select-none">
                              <Star className="w-3 h-3 fill-brand-yellow text-brand-yellow" />
                              {movie.rating}
                            </span>
                          </div>

                          {/* Short Description */}
                          <p className="text-xs text-zinc-400 leading-relaxed max-w-xl line-clamp-2 md:line-clamp-3">
                            {movie.synopsis}
                          </p>

                        </div>

                        {/* Interactive actions for mobile/tablet */}
                        <div className="flex gap-2 mt-4 md:hidden">
                          <button
                            onClick={(e) => handleBuyTicketClick(e, movie)}
                            className="flex-grow bg-brand-coral hover:bg-opacity-95 text-white font-bold py-2.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
                          >
                            Mua Vé
                          </button>
                          <button
                            onClick={(e) => handleTrailerOpen(e, movie.trailerId)}
                            className="bg-zinc-950 border border-zinc-800 hover:bg-zinc-800 text-white font-bold px-4 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-colors flex items-center gap-1"
                          >
                            <Play className="w-3.5 h-3.5 fill-white" />
                            <span>Trailer</span>
                          </button>
                        </div>

                      </div>
                    </div>
                  );
                })}
              </div>
            )}

          </div>

          {/* RIGHT PANEL: Sticky Booking & Promotion Sidebar (1/3 Width) */}
          <div className="space-y-6 lg:sticky lg:top-24 h-fit">
            
            {/* Widget A: "Mua Vé Nhanh" (Quick Booking Form Card) */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl">
              
              {/* Blue header card style matching reference image */}
              <div className="bg-gradient-to-r from-blue-700 to-indigo-900 text-white font-black text-xs uppercase tracking-wider py-4 px-5 shadow-inner flex items-center justify-between">
                <span>Mua Vé Nhanh</span>
                <Film className="w-4 h-4 text-white/50" />
              </div>

              {/* Form Body */}
              <form onSubmit={handleQuickBookingSubmit} className="p-5 space-y-4">
                
                {/* 1. Chọn Phim Dropdown */}
                <div className="space-y-1.5">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">1. Chọn Phim</label>
                  <select
                    value={quickMovieId}
                    onChange={(e) => {
                      setQuickMovieId(e.target.value);
                      setQuickCinema('');
                      setQuickDate('');
                    }}
                    className="w-full bg-zinc-950 border border-zinc-800 text-zinc-200 text-xs font-semibold rounded-xl py-3 px-3.5 focus:border-blue-600 focus:outline-none transition-colors"
                  >
                    <option value="">-- Chọn Phim --</option>
                    {movies.filter(m => m.status === 'NOW_SHOWING' || m.status === 'DANG_CHIEU' || m.status === 'SHOWING').map(m => (
                      <option key={m.id} value={m.id}>{m.title}</option>
                    ))}
                  </select>
                </div>

                {/* 2. Chọn Rạp Dropdown (Disabled until movie is selected) */}
                <div className="space-y-1.5">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">2. Chọn Rạp</label>
                  <select
                    disabled={!quickMovieId}
                    value={quickCinema}
                    onChange={(e) => {
                      setQuickCinema(e.target.value);
                      setQuickDate('');
                    }}
                    className={`w-full bg-zinc-950 border text-xs font-semibold rounded-xl py-3 px-3.5 focus:outline-none transition-colors ${
                      quickMovieId 
                        ? 'border-zinc-800 text-zinc-200 focus:border-blue-600 cursor-default' 
                        : 'border-zinc-900 text-zinc-650 cursor-not-allowed select-none'
                    }`}
                  >
                    <option value="">-- Chọn Rạp --</option>
                    {cinemaClusters.map(c => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>

                {/* 3. Chọn Ngày Dropdown (Disabled until cinema is selected) */}
                <div className="space-y-1.5">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">3. Chọn Ngày Chiếu</label>
                  <select
                    disabled={!quickCinema}
                    value={quickDate}
                    onChange={(e) => setQuickDate(e.target.value)}
                    className={`w-full bg-zinc-950 border text-xs font-semibold rounded-xl py-3 px-3.5 focus:outline-none transition-colors ${
                      quickCinema 
                        ? 'border-zinc-800 text-zinc-200 focus:border-blue-600 cursor-default' 
                        : 'border-zinc-900 text-zinc-650 cursor-not-allowed select-none'
                    }`}
                  >
                    <option value="">-- Chọn Ngày --</option>
                    <option value="Hôm nay">Hôm nay</option>
                    <option value="Ngày mai">Ngày mai</option>
                  </select>
                </div>

                {/* Submit Action Button */}
                <button
                  type="submit"
                  disabled={!quickMovieId || !quickCinema || !quickDate}
                  className={`w-full font-black py-3.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 ${
                    quickMovieId && quickCinema && quickDate
                      ? 'bg-blue-600 hover:bg-blue-700 text-white cursor-pointer shadow-lg shadow-blue-600/20 active:scale-[0.98]'
                      : 'bg-zinc-800 text-zinc-500 cursor-not-allowed select-none'
                  }`}
                >
                  Mua Vé Nhanh
                </button>

              </form>

            </div>

            {/* Widget B: "PHIM ĐANG CHIẾU" (Mini Promotion Grid Banner) */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 space-y-4 shadow-2xl">
              
              <div className="border-b border-zinc-800 pb-2 flex justify-between items-center">
                <span className="text-white text-[10px] font-black uppercase tracking-wider">Phim Đang Chiếu</span>
                <span className="text-[8px] font-black uppercase tracking-widest text-brand-yellow animate-pulse">Hot Now</span>
              </div>

              {/* Stack list */}
              <div className="space-y-4">
                {movies.slice(0, 3).map((movie) => (
                  <div 
                    key={movie.id}
                    onClick={(e) => handleBuyTicketClick(e, movie)}
                    className="flex gap-3 hover:bg-white/5 p-1.5 rounded-xl transition-colors cursor-pointer group"
                  >
                    {/* Poster */}
                    <div className="w-12 h-18 rounded-lg overflow-hidden shrink-0 bg-zinc-950 border border-zinc-800">
                      <img src={movie.posterUrl || movie.image} alt={movie.title} className="w-full h-full object-cover" />
                    </div>

                    {/* Details */}
                    <div className="space-y-1.5 flex flex-col justify-center">
                      <h4 className="text-xs font-extrabold text-zinc-200 group-hover:text-brand-coral transition-colors line-clamp-1">
                        {movie.title}
                      </h4>
                      <div className="flex items-center gap-2">
                        <span className="text-[8px] font-black uppercase bg-zinc-950 border border-zinc-800 text-brand-yellow px-1.5 py-0.5 rounded">
                          {movie.ageRating || 'T16'}
                        </span>
                        <div className="flex items-center gap-0.5">
                          <Star className="w-3 h-3 fill-brand-yellow text-brand-yellow" />
                          <span className="text-[9px] font-bold text-zinc-400">{movie.rating}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

            </div>

          </div>

        </div>

      </div>

      {/* Dynamic Embedded Trailer Modal */}
      {activeTrailerId && (
        <TrailerModal
          isOpen={!!activeTrailerId}
          onClose={() => setActiveTrailerId(null)}
          trailerId={activeTrailerId}
        />
      )}

    </div>
  );
}
