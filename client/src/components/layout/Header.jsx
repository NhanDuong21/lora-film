import { useState } from 'react';
import { 
  ChevronDown, Menu, X, Star, Search, User, LogOut, KeyRound, Mail
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import CustomerNotificationBell from '@/features/notifications/customer/components/CustomerNotificationBell';

export default function Header() {
  const { user, userRole, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const isCustomer = (userRole || '').replace(/^ROLE_/, '') === 'CUSTOMER';
  
  // Mobile drawer state
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  
  // Navigation dropdown visibility states
  const [activeDropdown, setActiveDropdown] = useState(null); // 'phim' | 'hoc-dien-anh' | null
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  
  // Cinema info modal state
  const [infoModalContent, setInfoModalContent] = useState(null);
 
  // Centralized search query state
  const [searchQuery, setSearchQuery] = useState('');
 

  const handleLogoutClick = async () => {
    await logout();
    setProfileDropdownOpen(false);
    navigate('/');
  };

  const handleQuickTicketClick = () => {
    navigate('/booking');
  };

  const handlePhimOptionClick = () => {
    setActiveDropdown(null);
    setMobileMenuOpen(false);
    navigate('/movies');
  };

  const handleInfoOptionClick = (optionName) => {
    setActiveDropdown(null);
    setMobileMenuOpen(false);
    if (optionName === 'Thể loại phim') {
      navigate('/movies');
    } else if (optionName === 'Diễn viên') {
      setInfoModalContent('Diễn viên');
    } else if (optionName === 'Đạo diễn') {
      setInfoModalContent('Đạo diễn');
    } else if (optionName === 'Khuyến mãi và Sự kiện') {
      setInfoModalContent('Khuyến mãi và Sự kiện');
    } else {
      setInfoModalContent(optionName);
    }
  };

  return (
    <header className="fixed top-0 left-0 w-full z-50 bg-zinc-950/95 backdrop-blur-md px-3 sm:px-6 md:px-12 py-3.5 flex justify-between items-center border-b border-zinc-800/80 transition-all duration-300">
      
      {/* LEFT SECTION: Brand Logo & Mua Ve Coupon Stub */}
      <div className="flex items-center gap-6">
        <Link to={isAuthenticated && (userRole?.replace(/^ROLE_/, '') === 'ADMIN') ? '/admin' : '/'} className="flex items-center gap-2.5 shrink-0 bg-transparent p-0 m-0 border-none shadow-none outline-none group mr-4 md:mr-6 select-none decoration-none transition-transform duration-200 hover:scale-[1.02]">
          <img 
            src="/images/main-logo.png" 
            alt="LoraFilm Mascot" 
            className="h-9 sm:h-10 md:h-11 w-auto object-contain bg-transparent will-change-transform"
          />
          <span className="text-xl sm:text-2xl md:text-3xl font-black tracking-tight text-white font-sans flex items-center leading-none">
            Lora
            <span className="text-amber-500 font-black ml-0.5 group-hover:text-amber-400 transition-colors">
              Film
            </span>
          </span>
        </Link>

        {/* Orange Ticket Button */}
        <div className="hidden sm:flex items-center shrink-0">
          <button
            onClick={handleQuickTicketClick}
            className="bg-brand-orange hover:bg-orange-600 transition-colors text-white text-[11px] font-black uppercase tracking-wider pl-4 pr-3 py-2 rounded-l-lg flex items-center gap-1.5 shadow-lg shadow-brand-orange/20 h-9"
          >
            <Star className="w-3.5 h-3.5 fill-white text-white" />
            <span>Mua Vé</span>
          </button>
          <div className="h-9 w-[1px] border-r border-dashed border-white/40 bg-brand-orange"></div>
          <button
            onClick={handleQuickTicketClick}
            className="bg-brand-orange hover:bg-orange-600 transition-colors text-white w-7 h-9 rounded-r-lg relative flex items-center justify-center shadow-lg shadow-brand-orange/20 shrink-0"
          >
            <div className="absolute -top-1.5 -left-1.5 w-3 h-3 bg-zinc-950 rounded-full border border-zinc-800/80"></div>
            <div className="absolute -bottom-1.5 -left-1.5 w-3 h-3 bg-zinc-950 rounded-full border border-zinc-800/80"></div>
            <div className="w-1 h-1 bg-white/70 rounded-full"></div>
          </button>
        </div>
      </div>

      {/* CENTER SECTION: Structured Navigation Dropdown Menus */}
      <nav className="hidden lg:flex items-center gap-6 font-semibold text-xs uppercase tracking-wider">
        <div 
          className="relative py-2"
          onMouseEnter={() => setActiveDropdown('phim')}
          onMouseLeave={() => setActiveDropdown(null)}
        >
          <button 
            type="button"
            className="text-zinc-300 hover:text-brand-orange flex items-center gap-1 transition-colors duration-250 focus:outline-none"
          >
            <span>Phim</span>
            <ChevronDown className="w-3 h-3 shrink-0 text-zinc-500 group-hover:text-brand-orange" />
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

        <div 
          className="relative py-2"
          onMouseEnter={() => setActiveDropdown('goc-dien-anh')}
          onMouseLeave={() => setActiveDropdown(null)}
        >
          <button 
            type="button"
            className="text-zinc-300 hover:text-brand-orange flex items-center gap-1 transition-colors duration-250 focus:outline-none"
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

        <button
          onClick={() => handleInfoOptionClick('Khuyến mãi và Sự kiện')}
          className="text-zinc-300 hover:text-brand-orange py-2 transition-colors duration-250 focus:outline-none"
        >
          Sự Kiện
        </button>

        <div 
          className="relative py-2"
          onMouseEnter={() => setActiveDropdown('rap-gia-ve')}
          onMouseLeave={() => setActiveDropdown(null)}
        >
          <button 
            type="button"
            className="text-zinc-300 hover:text-brand-orange flex items-center gap-1 transition-colors duration-250 focus:outline-none"
          >
            <span>Rạp/Giá Vé</span>
            <ChevronDown className="w-3 h-3 shrink-0 text-zinc-500" />
          </button>
          {activeDropdown === 'rap-gia-ve' && (
            <div className="absolute left-0 mt-2 w-56 bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden shadow-2xl z-50 py-2">
              <button
                onClick={() => {
                  setActiveDropdown(null);
                  navigate('/cinema/1');
                }}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Lora Nguyễn Du
              </button>
              <button
                onClick={() => {
                  setActiveDropdown(null);
                  navigate('/cinema/2');
                }}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Lora Thảo Điền
              </button>
              <button
                onClick={() => {
                  setActiveDropdown(null);
                  navigate('/cinema/3');
                }}
                className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white font-bold transition-colors"
              >
                Lora Royal City
              </button>
            </div>
          )}
        </div>

        <button
          onClick={() => handleInfoOptionClick('Trải nghiệm Rạp Đặc Biệt')}
          className="text-zinc-300 hover:text-brand-orange py-2 transition-colors duration-250 focus:outline-none"
        >
          Rạp Đặc Biệt
        </button>

      </nav>

      {/* RIGHT SECTION: Live Auth Session Status Dropdown */}
      <div className="flex items-center gap-1 sm:gap-4">
        <div className="relative hidden xl:block">
          <div className="relative w-64 md:w-72 bg-zinc-900/90 border border-zinc-800 focus-within:border-brand-orange/60 rounded-full px-4 h-10 flex items-center text-xs text-zinc-100 transition-colors duration-200 outline-none">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm phim, diễn viên, đạo diễn..."
              className="bg-transparent text-white text-xs w-full h-full focus:outline-none placeholder-zinc-600"
            />
            <Search className="w-4 h-4 text-zinc-500 absolute right-3 pointer-events-none" />
          </div>
        </div>

        {isAuthenticated ? (
          <div className="flex items-center gap-3 relative">
            
            {isCustomer && <CustomerNotificationBell />}

            <div className="relative">
              <button
                onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                className="w-9 h-9 rounded-full bg-brand-orange/10 border border-brand-orange/40 flex items-center justify-center text-brand-orange hover:bg-brand-orange/20 transition-all font-black text-sm uppercase focus:outline-none overflow-hidden"
              >
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt={user?.fullName} className="w-full h-full object-cover" />
                ) : (
                  user?.fullName ? user.fullName.charAt(0) : 'U'
                )}
              </button>

              {profileDropdownOpen && (
                <div className="absolute right-0 mt-3 w-56 bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl z-50 py-2">
                  <div className="px-4 py-2 border-b border-zinc-800 mb-1">
                    <p className="text-zinc-500 text-[10px] uppercase font-bold tracking-wider">Tài khoản</p>
                    <p className="text-sm font-bold text-white truncate">{user?.fullName}</p>
                    <p className="text-[10px] text-brand-orange font-semibold uppercase">{userRole}</p>
                  </div>

                  {userRole === 'CUSTOMER' ? (
                    <>
                      <button
                        onClick={() => {
                          setProfileDropdownOpen(false);
                          navigate('/profile');
                        }}
                        className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white flex items-center gap-2"
                      >
                        <User className="w-3.5 h-3.5 text-zinc-500" />
                        <span>Hồ sơ cá nhân</span>
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => {
                        setProfileDropdownOpen(false);
                        if (userRole === 'ADMIN' || userRole === 'ROLE_ADMIN' || userRole === 'ROLE_ACCOUNTANT') navigate('/admin');
                        if (userRole === 'EMPLOYEE' || userRole === 'ROLE_STAFF') navigate('/employee');
                      }}
                      className="w-full text-left px-4 py-2.5 text-xs text-brand-orange hover:bg-zinc-800 font-bold flex items-center gap-2"
                    >
                      <span>Vào trang quản lý</span>
                    </button>
                  )}

                  <button
                    onClick={() => {
                      setProfileDropdownOpen(false);
                      navigate('/change-email');
                    }}
                    className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white flex items-center gap-2"
                  >
                    <Mail className="w-3.5 h-3.5 text-zinc-500" />
                    <span>Thay đổi email</span>
                  </button>
                  <button
                    onClick={() => {
                      setProfileDropdownOpen(false);
                      navigate('/change-password');
                    }}
                    className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white flex items-center gap-2"
                  >
                    <KeyRound className="w-3.5 h-3.5 text-zinc-500" />
                    <span>Đổi mật khẩu</span>
                  </button>
                  <button
                    onClick={() => {
                      setProfileDropdownOpen(false);
                      navigate('/sessions');
                    }}
                    className="w-full text-left px-4 py-2.5 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white flex items-center gap-2"
                  >
                    <KeyRound className="w-3.5 h-3.5 text-zinc-500" />
                    <span>Phiên đăng nhập</span>
                  </button>

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
            onClick={() => navigate('/login')}
            className="hidden sm:block bg-brand-orange hover:bg-orange-600 text-white text-xs font-black py-2.5 px-5 rounded-full transition-all duration-300 shadow-lg shadow-brand-orange/10 uppercase tracking-wider focus:outline-none"
          >
            Đăng Nhập
          </button>
        )}

        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label={mobileMenuOpen ? 'Đóng menu điều hướng' : 'Mở menu điều hướng'}
          aria-expanded={mobileMenuOpen}
          aria-controls="mobile-navigation"
          className="lg:hidden flex items-center justify-center p-2 text-zinc-400 hover:text-white focus:outline-none"
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>

      </div>

      {/* MOBILE DRAWERS */}
      {mobileMenuOpen && (
        <div id="mobile-navigation" className="absolute top-[65px] left-0 w-full bg-zinc-950 border-b border-zinc-800 px-6 py-6 flex flex-col gap-4 lg:hidden z-40 animate-in slide-in-from-top duration-300">
          {!isAuthenticated && (
            <button
              onClick={() => {
                setMobileMenuOpen(false);
                navigate('/login');
              }}
              className="sm:hidden w-full border border-brand-orange text-brand-orange py-3 rounded-xl text-xs font-black uppercase tracking-wider"
            >
              Đăng Nhập
            </button>
          )}
          <button
            onClick={() => {
              setMobileMenuOpen(false);
              handleQuickTicketClick();
            }}
            className="w-full bg-brand-orange text-white py-3 rounded-xl text-xs font-black uppercase tracking-wider flex items-center justify-center gap-2"
          >
            <Star className="w-4 h-4 fill-white text-white" />
            <span>Mua Vé Nhanh</span>
          </button>

          <div className="space-y-1 border-b border-zinc-900 pb-2">
            <span className="text-[10px] text-zinc-500 font-black tracking-wider uppercase block">Phim</span>
            <button
              onClick={() => handlePhimOptionClick('NOW_SHOWING')}
              className="w-full text-left text-zinc-200 hover:text-brand-orange py-1.5 text-xs font-bold uppercase"
            >
              Phim đang chiếu
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
              <p>
                Thông tin mục **{infoModalContent}** đang được đồng bộ và cập nhật tự động từ ban quản lý rạp. Vui lòng quay lại sau!
              </p>
            </div>

            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setInfoModalContent(null)}
                className="bg-brand-orange hover:bg-orange-600 text-white font-black py-2.5 px-6 rounded-xl text-xs uppercase tracking-wider transition-colors"
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
