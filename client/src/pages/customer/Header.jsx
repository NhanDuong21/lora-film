import { useState, useMemo } from 'react';
import { 
  Film, ChevronDown, Menu, X, Bell, Star, Search, User, History, LogOut 
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useData } from '../../contexts/DataContext';

export default function Header({ onNavigate }) {
  const { user, userRole, isAuthenticated, logout } = useAuth();
  const { movies } = useData();
  
  // Mobile drawer state
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  
  // Navigation dropdown visibility states
  const [activeDropdown, setActiveDropdown] = useState(null); // 'phim' | 'hoc-dien-anh' | null
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  
  // Cinema info modal state
  const [infoModalContent, setInfoModalContent] = useState(null);
 
  // Centralized search query state
  const [searchQuery, setSearchQuery] = useState('');
 
  // Multi-Criteria Search Matching Logic (Client-Side)
  const searchResults = useMemo(() => {
    if (!searchQuery.trim()) return { movies: [], stars: [] };
    const query = searchQuery.toLowerCase().trim();
 
    // 1. Phim (Movies Category Match)
    const matchedMovies = movies.filter(movie => 
      movie.title.toLowerCase().includes(query) ||
      (movie.genres && movie.genres.some(g => g.toLowerCase().includes(query)))
    );
 
    // 2. Diễn viên & Đạo diễn matching (Stars/Directors)
    const matchedStarsMap = new Map();
 
    movies.forEach(movie => {
      // Check cast array actors
      if (movie.cast && Array.isArray(movie.cast)) {
        movie.cast.forEach(actor => {
          if (actor.name.toLowerCase().includes(query)) {
            if (!matchedStarsMap.has(actor.name)) {
              matchedStarsMap.set(actor.name, {
                name: actor.name,
                type: 'actor',
                avatarUrl: actor.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80'
              });
            }
          }
        });
      }

      // Check director object
      if (movie.director && movie.director.name) {
        if (movie.director.name.toLowerCase().includes(query)) {
          if (!matchedStarsMap.has(movie.director.name)) {
            matchedStarsMap.set(movie.director.name, {
              name: movie.director.name,
              type: 'director',
              avatarUrl: movie.director.avatarUrl || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop&q=80'
            });
          }
        }
      }
    });

    return {
      movies: matchedMovies,
      stars: Array.from(matchedStarsMap.values())
    };
  }, [searchQuery]);

  const handleLogoClick = (e) => {
    e.preventDefault();
    onNavigate('home', null);
  };

  const handleLogoutClick = () => {
    logout();
    setProfileDropdownOpen(false);
    onNavigate('home', null);
  };

  // Mua Ve ticket button click action (routes to booking funnel)
  const handleQuickTicketClick = () => {
    onNavigate('booking-funnel', null);
  };

  // Dropdown option handlers
  const handlePhimOptionClick = (tab) => {
    setActiveDropdown(null);
    setMobileMenuOpen(false);
    onNavigate('home', { activeTab: tab });
  };

  const handleInfoOptionClick = (optionName) => {
    setActiveDropdown(null);
    setMobileMenuOpen(false);
    if (optionName === 'Thể loại phim') {
      onNavigate('discovery', null);
    } else if (optionName === 'Diễn viên') {
      onNavigate('actors', null);
    } else if (optionName === 'Đạo diễn') {
      onNavigate('directors', null);
    } else if (optionName === 'Khuyến mãi và Sự kiện') {
      onNavigate('events', null);
    } else {
      setInfoModalContent(optionName);
    }
  };

  return (
    <header className="fixed top-0 left-0 w-full z-50 bg-zinc-950/95 backdrop-blur-md px-6 md:px-12 py-3.5 flex justify-between items-center border-b border-zinc-800/80 transition-all duration-300">
      
      {/* LEFT SECTION: Brand Logo & Mua Ve Coupon Stub */}
      <div className="flex items-center gap-6">
        {/* Brand Logo */}
        <a href="#/" onClick={handleLogoClick} className="flex items-center gap-2.5 shrink-0 bg-transparent p-0 m-0 border-none shadow-none outline-none group mr-4 md:mr-6 select-none decoration-none transition-transform duration-200 hover:scale-[1.02]">
          {/* Left: Upscaled Mascot Asset Icon Container */}
          <img 
            src="/images/main-logo.png" 
            alt="LoraFilm Mascot" 
            className="h-9 sm:h-10 md:h-11 w-auto object-contain bg-transparent will-change-transform"
          />
          
          {/* Right: Bold, Large High-Contrast Branding Typography */}
          <span className="text-xl sm:text-2xl md:text-3xl font-black tracking-tight text-white font-sans flex items-center leading-none">
            Lora
            <span className="text-amber-500 font-black ml-0.5 group-hover:text-amber-400 transition-colors">
              Film
            </span>
          </span>
        </a>

        {/* Orange Ticket Button ("Mua Vé" with custom coupon stub notch styling) */}
        <div className="hidden sm:flex items-center shrink-0">
          {/* Left coupon body */}
          <button
            onClick={handleQuickTicketClick}
            className="bg-orange-500 hover:bg-orange-600 transition-colors text-white text-[11px] font-black uppercase tracking-wider pl-4 pr-3 py-2 rounded-l-lg flex items-center gap-1.5 shadow-lg shadow-orange-500/20 h-9"
          >
            <Star className="w-3.5 h-3.5 fill-white text-white" />
            <span>Mua Vé</span>
          </button>
          
          {/* Dashed Tear-off Line */}
          <div className="h-9 w-[1px] border-r border-dashed border-white/40 bg-orange-500"></div>
          
          {/* Right notched coupon stub */}
          <button
            onClick={handleQuickTicketClick}
            className="bg-orange-500 hover:bg-orange-600 transition-colors text-white w-7 h-9 rounded-r-lg relative flex items-center justify-center shadow-lg shadow-orange-500/20 shrink-0"
          >
            {/* Notch Cutouts */}
            <div className="absolute -top-1.5 -left-1.5 w-3 h-3 bg-zinc-950 rounded-full border border-zinc-800/80"></div>
            <div className="absolute -bottom-1.5 -left-1.5 w-3 h-3 bg-zinc-950 rounded-full border border-zinc-800/80"></div>
            <div className="w-1 h-1 bg-white/70 rounded-full"></div>
          </button>
        </div>
      </div>

      {/* CENTER SECTION: Structured Navigation Dropdown Menus */}
      <nav className="hidden lg:flex items-center gap-6 font-semibold text-xs uppercase tracking-wider">
        
        {/* Phim Dropdown Menu */}
        <div 
          className="relative py-2"
          onMouseEnter={() => setActiveDropdown('phim')}
          onMouseLeave={() => setActiveDropdown(null)}
        >
          <button 
            type="button"
            className="text-zinc-300 hover:text-orange-500 flex items-center gap-1 transition-colors duration-250 focus:outline-none"
          >
            <span>Phim</span>
            <ChevronDown className="w-3 h-3 shrink-0 text-zinc-500 group-hover:text-orange-500" />
          </button>
          {activeDropdown === 'phim' && (
            <div className="absolute left-0 mt-2 w-48 bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden shadow-2xl z-50 py-2">
              <button
                onClick={() => handlePhimOptionClick('NOW_SHOWING')}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Phim đang chiếu
              </button>
              <button
                onClick={() => handlePhimOptionClick('COMING_SOON')}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Phim sắp chiếu
              </button>
            </div>
          )}
        </div>

        {/* Góc Điện Ảnh Dropdown Menu */}
        <div 
          className="relative py-2"
          onMouseEnter={() => setActiveDropdown('goc-dien-anh')}
          onMouseLeave={() => setActiveDropdown(null)}
        >
          <button 
            type="button"
            className="text-zinc-300 hover:text-orange-500 flex items-center gap-1 transition-colors duration-250 focus:outline-none"
          >
            <span>Góc Điện Ảnh</span>
            <ChevronDown className="w-3 h-3 shrink-0 text-zinc-500" />
          </button>
          {activeDropdown === 'goc-dien-anh' && (
            <div className="absolute left-0 mt-2 w-48 bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden shadow-2xl z-50 py-2">
              <button
                onClick={() => handleInfoOptionClick('Thể loại phim')}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Thể loại phim
              </button>
              <button
                onClick={() => handleInfoOptionClick('Diễn viên')}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Diễn viên
              </button>
              <button
                onClick={() => handleInfoOptionClick('Đạo diễn')}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Đạo diễn
              </button>
            </div>
          )}
        </div>

        {/* Sự Kiện Link */}
        <button
          onClick={() => handleInfoOptionClick('Khuyến mãi và Sự kiện')}
          className="text-zinc-300 hover:text-orange-500 py-2 transition-colors duration-250 focus:outline-none"
        >
          Sự Kiện
        </button>

        {/* Rạp/Giá Vé Dropdown Menu */}
        <div 
          className="relative py-2"
          onMouseEnter={() => setActiveDropdown('rap-gia-ve')}
          onMouseLeave={() => setActiveDropdown(null)}
        >
          <button 
            type="button"
            className="text-zinc-300 hover:text-orange-500 flex items-center gap-1 transition-colors duration-250 focus:outline-none"
          >
            <span>Rạp/Giá Vé</span>
            <ChevronDown className="w-3 h-3 shrink-0 text-zinc-500" />
          </button>
          {activeDropdown === 'rap-gia-ve' && (
            <div className="absolute left-0 mt-2 w-56 bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden shadow-2xl z-50 py-2">
              <button
                onClick={() => {
                  setActiveDropdown(null);
                  onNavigate('cinema-detail', { cinemaId: 1 });
                }}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Lora Nguyễn Du
              </button>
              <button
                onClick={() => {
                  setActiveDropdown(null);
                  onNavigate('cinema-detail', { cinemaId: 2 });
                }}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Lora Thảo Điền
              </button>
              <button
                onClick={() => {
                  setActiveDropdown(null);
                  onNavigate('cinema-detail', { cinemaId: 3 });
                }}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Lora Royal City
              </button>
            </div>
          )}
        </div>

        {/* Rạp Đặc Biệt Link */}
        <button
          onClick={() => handleInfoOptionClick('Trải nghiệm Rạp Đặc Biệt')}
          className="text-zinc-300 hover:text-orange-500 py-2 transition-colors duration-250 focus:outline-none"
        >
          Rạp Đặc Biệt
        </button>

      </nav>

      {/* RIGHT SECTION: Live Auth Session Status Dropdown */}
      <div className="flex items-center gap-4">
        {/* Expanded Omni-Search Input Field UI */}
        <div className="relative">
          <div className="relative w-64 md:w-72 bg-zinc-900/90 border border-zinc-800 focus-within:border-brand-coral/60 rounded-full px-4 h-10 flex items-center text-xs text-zinc-100 transition-colors duration-200 outline-none">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm phim, diễn viên, đạo diễn..."
              className="bg-transparent text-white text-xs w-full h-full focus:outline-none placeholder-zinc-600"
            />
            <Search className="w-4 h-4 text-zinc-500 absolute right-3 pointer-events-none" />
          </div>

          {/* Quick-Result Floating Dropdown Portal */}
          {searchQuery.length > 0 && (
            <div className="absolute top-12 right-0 w-80 max-h-96 overflow-y-auto bg-zinc-900 border border-zinc-800 rounded-xl shadow-2xl p-3 z-50 divide-y divide-zinc-800/50">
              
              {/* Movies Matches Section */}
              {searchResults.movies.length > 0 && (
                <div className="py-2 first:pt-0">
                  <div className="text-[10px] font-black tracking-wider text-zinc-500 uppercase mb-2 px-1">
                    PHIM
                  </div>
                  <div className="space-y-2">
                    {searchResults.movies.map((phim) => (
                      <div
                        key={phim.id}
                        onClick={() => {
                          setSearchQuery('');
                          onNavigate('detail', { movieId: phim.id });
                        }}
                        className="flex items-center gap-3 p-1.5 hover:bg-zinc-800/60 rounded-lg cursor-pointer transition-colors"
                      >
                        <img
                          src={phim.posterUrl || phim.image}
                          alt={phim.title}
                          className="w-8 h-12 rounded object-cover border border-zinc-800"
                        />
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-black text-white truncate">{phim.title}</p>
                          <span className="inline-block mt-1 text-[8px] font-black uppercase bg-zinc-950 border border-zinc-850 text-orange-500 px-1 py-0.5 rounded">
                            {phim.ageRating || 'T16'}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Stars & Directors Matches Section */}
              {searchResults.stars.length > 0 && (
                <div className="py-2">
                  <div className="text-[10px] font-black tracking-wider text-zinc-550 uppercase mb-2 px-1">
                    DIỄN VIÊN & ĐẠO DIỄN
                  </div>
                  <div className="space-y-2">
                    {searchResults.stars.map((star, sIdx) => (
                      <div
                        key={sIdx}
                        onClick={() => {
                          setSearchQuery('');
                          if (star.type === 'actor') {
                            onNavigate('actor-detail', { actorName: star.name });
                          } else {
                            onNavigate('director-detail', { directorName: star.name });
                          }
                        }}
                        className="flex items-center gap-3 p-1.5 hover:bg-zinc-800/60 rounded-lg cursor-pointer transition-colors"
                      >
                        <img
                          src={star.avatarUrl}
                          alt={star.name}
                          className="w-8 h-8 rounded-full object-cover border border-zinc-800"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80';
                          }}
                        />
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-black text-white truncate">{star.name}</p>
                          <p className="text-[8px] font-black text-zinc-500 uppercase tracking-widest mt-0.5">
                            {star.type === 'actor' ? 'Diễn viên' : 'Đạo diễn'}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Fallback Empty row */}
              {searchResults.movies.length === 0 && searchResults.stars.length === 0 && (
                <div className="py-6 text-center">
                  <p className="text-xs font-bold text-zinc-500">
                    Không tìm thấy kết quả phù hợp
                  </p>
                </div>
              )}

            </div>
          )}
        </div>

        {/* Auth adaptive controls */}
        {isAuthenticated ? (
          <div className="flex items-center gap-3 relative">
            
            {/* Notification Bell (Only for CUSTOMER role) */}
            {userRole === 'CUSTOMER' && (
              <button 
                onClick={() => handleInfoOptionClick('Thông báo thành viên')}
                className="relative p-2 rounded-xl bg-zinc-900 border border-zinc-800/80 hover:bg-zinc-800 text-zinc-450 hover:text-white transition-all focus:outline-none"
              >
                <Bell className="w-4 h-4" />
                <span className="absolute top-1 right-1 w-2 h-2 bg-orange-500 rounded-full"></span>
              </button>
            )}

            {/* Profile Dropdown avatar */}
            <div className="relative">
              <button
                onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                className="w-9 h-9 rounded-full bg-orange-500/10 border border-orange-500/40 flex items-center justify-center text-orange-500 hover:bg-orange-500/20 transition-all font-black text-sm uppercase focus:outline-none"
              >
                {user?.fullName ? user.fullName.charAt(0) : 'U'}
              </button>

              {profileDropdownOpen && (
                <div className="absolute right-0 mt-3 w-56 bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl z-50 py-2">
                  <div className="px-4 py-2 border-b border-zinc-800 mb-1">
                    <p className="text-zinc-500 text-[10px] uppercase font-bold tracking-wider">Tài khoản</p>
                    <p className="text-sm font-bold text-white truncate">{user?.fullName}</p>
                    <p className="text-[10px] text-orange-500 font-semibold uppercase">{userRole}</p>
                  </div>

                  {userRole === 'CUSTOMER' ? (
                    <>
                      <button
                        onClick={() => {
                          setProfileDropdownOpen(false);
                          onNavigate('profile', { initialTab: 'info' });
                        }}
                        className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white flex items-center gap-2"
                      >
                        <User className="w-3.5 h-3.5 text-zinc-500" />
                        <span>Hồ sơ cá nhân</span>
                      </button>
                      <button
                        onClick={() => {
                          setProfileDropdownOpen(false);
                          onNavigate('profile', { initialTab: 'history' });
                        }}
                        className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white flex items-center gap-2"
                      >
                        <History className="w-3.5 h-3.5 text-zinc-500" />
                        <span>Lịch sử đặt vé</span>
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => {
                        setProfileDropdownOpen(false);
                        if (userRole === 'ADMIN' || userRole === 'ROLE_ADMIN' || userRole === 'ROLE_ACCOUNTANT') onNavigate('admin', null);
                        if (userRole === 'EMPLOYEE' || userRole === 'ROLE_STAFF') onNavigate('employee', null);
                      }}
                      className="w-full text-left px-4 py-2.5 text-xs text-orange-500 hover:bg-zinc-800 font-bold flex items-center gap-2"
                    >
                      <span>Vào trang quản lý</span>
                    </button>
                  )}

                  <button
                    onClick={handleLogoutClick}
                    className="w-full text-left px-4 py-2.5 text-xs text-red-400 hover:bg-red-950/20 hover:text-red-300 font-bold border-t border-zinc-800 mt-1 flex items-center gap-2"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Đăng xuất</span>
                  </button>
                </div>
              )}

            </div>

          </div>
        ) : (
          <button
            onClick={() => onNavigate('login', { voluntary: true })}
            className="bg-orange-500 hover:bg-orange-600 text-white text-xs font-black py-2.5 px-5 rounded-full transition-all duration-300 shadow-lg shadow-orange-500/10 uppercase tracking-wider focus:outline-none"
          >
            Đăng Nhập
          </button>
        )}

        {/* Mobile Menu trigger */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="lg:hidden flex items-center justify-center p-2 text-zinc-400 hover:text-white focus:outline-none"
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>

      </div>

      {/* MOBILE DRAWERS */}
      {mobileMenuOpen && (
        <div className="absolute top-[65px] left-0 w-full bg-zinc-950 border-b border-zinc-800 px-6 py-6 flex flex-col gap-4 lg:hidden z-40 animate-in slide-in-from-top duration-300">
          
          {/* Mua Ve Link for Mobile */}
          <button
            onClick={() => {
              setMobileMenuOpen(false);
              handleQuickTicketClick();
            }}
            className="w-full bg-orange-500 text-white py-3 rounded-xl text-xs font-black uppercase tracking-wider flex items-center justify-center gap-2"
          >
            <Star className="w-4 h-4 fill-white text-white" />
            <span>Mua Vé Nhanh</span>
          </button>

          {/* Phim Section */}
          <div className="space-y-1 border-b border-zinc-900 pb-2">
            <span className="text-[10px] text-zinc-500 font-black tracking-wider uppercase block">Phim</span>
            <button
              onClick={() => handlePhimOptionClick('NOW_SHOWING')}
              className="w-full text-left text-zinc-200 hover:text-orange-500 py-1.5 text-xs font-bold uppercase"
            >
              Phim đang chiếu
            </button>
            <button
              onClick={() => handlePhimOptionClick('COMING_SOON')}
              className="w-full text-left text-zinc-200 hover:text-orange-500 py-1.5 text-xs font-bold uppercase"
            >
              Phim sắp chiếu
            </button>
          </div>

          {/* Góc Điện Ảnh Section */}
          <div className="space-y-1 border-b border-zinc-900 pb-2">
            <span className="text-[10px] text-zinc-500 font-black tracking-wider uppercase block">Góc điện ảnh</span>
            <button
              onClick={() => handleInfoOptionClick('Thể loại phim')}
              className="w-full text-left text-zinc-200 hover:text-orange-500 py-1.5 text-xs font-bold uppercase"
            >
              Thể loại phim
            </button>
            <button
              onClick={() => handleInfoOptionClick('Diễn viên')}
              className="w-full text-left text-zinc-200 hover:text-orange-500 py-1.5 text-xs font-bold uppercase"
            >
              Diễn viên
            </button>
            <button
              onClick={() => handleInfoOptionClick('Đạo diễn')}
              className="w-full text-left text-zinc-200 hover:text-orange-500 py-1.5 text-xs font-bold uppercase"
            >
              Đạo diễn
            </button>
          </div>

          {/* Other links */}
          <div className="flex flex-col gap-2.5">
            <button
              onClick={() => { setMobileMenuOpen(false); handleInfoOptionClick('Khuyến mãi và Sự kiện'); }}
              className="w-full text-left text-zinc-200 hover:text-orange-500 text-xs font-bold uppercase"
            >
              Sự Kiện
            </button>
            <button
              onClick={() => { setMobileMenuOpen(false); handleInfoOptionClick('Rạp và Giá vé'); }}
              className="w-full text-left text-zinc-200 hover:text-orange-500 text-xs font-bold uppercase"
            >
              Rạp/Giá Vé
            </button>
            <button
              onClick={() => { setMobileMenuOpen(false); handleInfoOptionClick('Trải nghiệm Rạp Đặc Biệt'); }}
              className="w-full text-left text-zinc-200 hover:text-orange-500 text-xs font-bold uppercase"
            >
              Rạp Đặc Biệt
            </button>
          </div>

        </div>
      )}

      {/* GALAXY INFO CONTENT DISPLAY OVERLAY MODAL */}
      {infoModalContent && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[100] p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-6">
            <div>
              <h3 className="text-base font-black text-white uppercase tracking-wider">{infoModalContent}</h3>
              <p className="text-zinc-500 text-[10px] mt-0.5">Hệ thống thông tin giải trí LoraFilm</p>
            </div>
            
            <div className="text-xs text-zinc-300 leading-relaxed py-4 border-y border-zinc-800">
              {infoModalContent === 'Tìm kiếm phim' ? (
                <div className="space-y-4">
                  <p>Nhập tên phim bạn muốn tìm kiếm:</p>
                  <input 
                    type="text" 
                    placeholder="Tìm tên phim, diễn viên..." 
                    className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-white focus:border-orange-500 focus:outline-none"
                  />
                </div>
              ) : (
                <p>
                  Thông tin mục **{infoModalContent}** đang được đồng bộ và cập nhật tự động từ ban quản lý rạp. Vui lòng quay lại sau!
                </p>
              )}
            </div>

            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setInfoModalContent(null)}
                className="bg-orange-500 hover:bg-orange-600 text-white font-black py-2.5 px-6 rounded-xl text-xs uppercase tracking-wider transition-colors"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

    </header>
  );
}
