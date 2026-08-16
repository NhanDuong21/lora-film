import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ChevronDown,
  Award,
  Gift,
  LayoutDashboard,
  LogOut,
  Menu,
  Search,
  ShieldCheck,
  Star,
  Ticket,
  X
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { resolvePostLoginPath } from '@/features/auth/utils/loginRedirect';
import { getCinemas } from '@/features/catalog/customer/services/movieService';
import CustomerNotificationBell from '@/features/notifications/customer/components/CustomerNotificationBell';
import { getOptimizedImageUrl } from '@/utils/imageOptimization';
import scoreCustomerService from '@/features/score/customer/services/scoreCustomerService';
import queryCache from '@/utils/queryCache';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
const resolveMediaUrl = value => value?.startsWith('/') ? `${apiBaseUrl}${value}` : value;

const dropdownPanelClass =
  'overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/95 p-2 shadow-[0_20px_55px_-18px_rgba(0,0,0,0.95)] backdrop-blur-xl';

const dropdownItemClass =
  'flex min-h-11 w-full items-center rounded-xl px-4 text-left text-sm font-bold text-zinc-300 transition-colors hover:bg-zinc-800 hover:text-white focus:outline-none focus-visible:bg-zinc-800 focus-visible:text-white';

const focusRingClass =
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange/70 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950';

function NavDropdown({
  id,
  label,
  isOpen,
  isActive,
  onOpen,
  onClose,
  onToggle,
  children
}) {
  const menuId = `header-menu-${id}`;

  return (
    <div
      className="relative flex h-20 items-center"
      onMouseEnter={() => onOpen(id)}
      onMouseLeave={onClose}
    >
      <button
        type="button"
        aria-expanded={isOpen}
        aria-controls={menuId}
        aria-haspopup="menu"
        onClick={() => onToggle(id)}
        className={`group inline-flex min-h-11 items-center gap-1.5 rounded-lg px-1 text-[13px] font-bold whitespace-nowrap transition-colors ${focusRingClass} ${
          isOpen || isActive
            ? 'text-brand-orange'
            : 'text-zinc-300 hover:text-brand-orange'
        }`}
      >
        <span>{label}</span>
        <ChevronDown
          aria-hidden="true"
          className={`h-3.5 w-3.5 transition duration-200 ${
            isOpen ? 'rotate-180 text-brand-orange' : 'text-zinc-500 group-hover:text-brand-orange'
          }`}
        />
      </button>

      {isOpen && (
        <div className="absolute left-1/2 top-full z-50 w-60 -translate-x-1/2 pt-2">
          <div id={menuId} role="menu" aria-label={label} className={dropdownPanelClass}>
            {children}
          </div>
        </div>
      )}
    </div>
  );
}

export default function Header() {
  const { user, userRole, isAuthenticated, logout } = useAuth();
  const [failedAvatarUrl, setFailedAvatarUrl] = useState(null);
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const headerRef = useRef(null);
  const profileMenuRef = useRef(null);

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [activeDropdown, setActiveDropdown] = useState(null);
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const [infoModalContent, setInfoModalContent] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [cinemas, setCinemas] = useState([]);
  const [cinemaMenuLoading, setCinemaMenuLoading] = useState(true);
  const [cinemaMenuError, setCinemaMenuError] = useState('');
  const [headerScore, setHeaderScore] = useState(null);

  const normalizedRole = (userRole || '').replace(/^ROLE_/, '');
  const isCustomer = normalizedRole === 'CUSTOMER';
  const brandPath = isAuthenticated && normalizedRole === 'ADMIN' ? '/admin' : '/';
  const managementPath = resolvePostLoginPath({
    role: userRole,
    permissions: user?.permissions || [],
  });

  const avatarLoadFailed = Boolean(user?.avatarUrl && failedAvatarUrl === user.avatarUrl);

  const loadCinemaMenu = useCallback(async () => {
    setCinemaMenuLoading(true);
    setCinemaMenuError('');
    try {
      const cinemaPage = await getCinemas({ page: 0, size: 100 });
      setCinemas(Array.isArray(cinemaPage?.data) ? cinemaPage.data : []);
    } catch {
      setCinemas([]);
      setCinemaMenuError('Không thể tải danh sách rạp');
    } finally {
      setCinemaMenuLoading(false);
    }
  }, []);

  useEffect(() => {
    const closeOpenMenus = event => {
      if (!headerRef.current?.contains(event.target)) {
        setActiveDropdown(null);
        setProfileDropdownOpen(false);
        setMobileMenuOpen(false);
      }
    };

    const closeOnEscape = event => {
      if (event.key === 'Escape') {
        setActiveDropdown(null);
        setProfileDropdownOpen(false);
        setMobileMenuOpen(false);
        setInfoModalContent(null);
      }
    };

    document.addEventListener('pointerdown', closeOpenMenus);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOpenMenus);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadCinemaMenu();
  }, [loadCinemaMenu]);

  useEffect(() => {
    if (!isAuthenticated || !isCustomer) {
      return undefined;
    }
    let active = true;
    queryCache.fetchQuery(
      'customer-score-balance',
      () => scoreCustomerService.getScoreBalance(),
      { staleTime: 30000 }
    )
      .then(response => {
        if (active) setHeaderScore(response?.data || null);
      })
      .catch(() => {
        if (active) setHeaderScore(null);
      });
    return () => {
      active = false;
    };
  }, [isAuthenticated, isCustomer, user?.id]);

  useEffect(() => {
    if (!profileDropdownOpen) return;
    window.requestAnimationFrame(() => {
      profileMenuRef.current?.querySelector('[role="menuitem"]')?.focus();
    });
  }, [profileDropdownOpen]);

  const closeNavigation = () => {
    setActiveDropdown(null);
    setMobileMenuOpen(false);
  };

  const navigateFromHeader = path => {
    closeNavigation();
    navigate(path);
  };

  const handleLogoutClick = async () => {
    await logout();
    setProfileDropdownOpen(false);
    navigate('/');
  };

  const handleInfoOptionClick = optionName => {
    closeNavigation();
    if (optionName === 'Thể loại phim') {
      navigate('/movies');
      return;
    }
    if (optionName === 'Khuyến mãi và Sự kiện') {
      navigate('/promotions');
      return;
    }
    setInfoModalContent(optionName);
  };

  const handleSearchSubmit = event => {
    event.preventDefault();
    const normalizedQuery = searchQuery.trim();
    if (!normalizedQuery) return;
    closeNavigation();
    navigate(`/movies?search=${encodeURIComponent(normalizedQuery)}`);
  };

  const toggleDropdown = id => {
    setProfileDropdownOpen(false);
    setActiveDropdown(current => (current === id ? null : id));
  };

  const openDropdown = id => {
    setProfileDropdownOpen(false);
    setActiveDropdown(id);
  };

  const headerTier = String(headerScore?.currentTier?.tierName || 'Thành viên')
    .replace(/\b(vip|member|membership)\b/gi, '')
    .replace(/\s+/g, ' ')
    .trim() || 'Thành viên';
  const headerPoints = Number(headerScore?.currentPoints || 0);

  return (
    <>
      <header
        ref={headerRef}
        className="fixed inset-x-0 top-0 z-50 h-20 border-b border-zinc-800/80 bg-zinc-950/95 backdrop-blur-xl"
      >
        <div className="mx-auto flex h-full w-full max-w-7xl items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
          <div className="flex min-w-0 shrink-0 items-center gap-5 xl:gap-7">
            <Link
              to={brandPath}
              aria-label="Về trang chủ LoraFilm"
              className={`group flex shrink-0 items-center gap-2.5 rounded-xl transition-transform hover:scale-[1.02] ${focusRingClass}`}
            >
              <img
                src="/images/main-logo.png"
                alt=""
                className="h-10 w-auto object-contain sm:h-11"
              />
              <span className="flex items-center text-2xl font-black leading-none tracking-tight text-white sm:text-[28px]">
                Lora
                <span className="ml-0.5 text-amber-500 transition-colors group-hover:text-amber-400">
                  Film
                </span>
              </span>
            </Link>

            <button
              type="button"
              onClick={() => navigateFromHeader('/booking')}
              aria-label="Mua vé nhanh"
              className={`group relative hidden h-11 w-[154px] shrink-0 overflow-hidden rounded-xl bg-brand-orange text-white shadow-[0_10px_28px_-10px_rgba(255,122,0,0.85)] transition hover:-translate-y-0.5 hover:bg-orange-600 xl:flex ${focusRingClass}`}
            >
              <span className="flex flex-1 items-center justify-center gap-2 pl-4 pr-3 text-xs font-black uppercase tracking-wide">
                <Star aria-hidden="true" className="h-4 w-4 fill-current" />
                Mua vé
              </span>
              <span
                aria-hidden="true"
                className="flex w-10 items-center justify-center border-l border-dashed border-white/55"
              >
                <span className="h-1.5 w-1.5 rounded-full bg-white/90 transition-transform group-hover:scale-125" />
              </span>
              <span
                aria-hidden="true"
                className="absolute -top-1.5 right-10 h-3 w-3 translate-x-1/2 rounded-full bg-zinc-950"
              />
              <span
                aria-hidden="true"
                className="absolute -bottom-1.5 right-10 h-3 w-3 translate-x-1/2 rounded-full bg-zinc-950"
              />
            </button>
          </div>

          <nav
            aria-label="Điều hướng chính"
            className="hidden min-w-0 items-center justify-center gap-4 lg:flex xl:gap-5"
          >
            <NavDropdown
              id="movies"
              label="Phim"
              isOpen={activeDropdown === 'movies'}
              isActive={pathname.startsWith('/movies')}
              onOpen={openDropdown}
              onClose={() => setActiveDropdown(null)}
              onToggle={toggleDropdown}
            >
              <button
                type="button"
                role="menuitem"
                onClick={() => navigateFromHeader('/movies?status=NOW_SHOWING')}
                className={dropdownItemClass}
              >
                Phim đang chiếu
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={() => navigateFromHeader('/movies?status=UPCOMING')}
                className={dropdownItemClass}
              >
                Phim sắp chiếu
              </button>
            </NavDropdown>

            <NavDropdown
              id="cinema-corner"
              label="Góc Điện Ảnh"
              isOpen={activeDropdown === 'cinema-corner'}
              isActive={false}
              onOpen={openDropdown}
              onClose={() => setActiveDropdown(null)}
              onToggle={toggleDropdown}
            >
              {['Thể loại phim', 'Diễn viên', 'Đạo diễn'].map(item => (
                <button
                  key={item}
                  type="button"
                  role="menuitem"
                  onClick={() => handleInfoOptionClick(item)}
                  className={dropdownItemClass}
                >
                  {item}
                </button>
              ))}
            </NavDropdown>

            <button
              type="button"
              onClick={() => handleInfoOptionClick('Khuyến mãi và Sự kiện')}
              className={`min-h-11 rounded-lg px-1 text-[13px] font-bold whitespace-nowrap text-zinc-300 transition-colors hover:text-brand-orange ${focusRingClass}`}
            >
              Khuyến Mãi
            </button>

            <NavDropdown
              id="cinemas"
              label="Rạp/Giá Vé"
              isOpen={activeDropdown === 'cinemas'}
              isActive={pathname.startsWith('/cinema/')}
              onOpen={openDropdown}
              onClose={() => setActiveDropdown(null)}
              onToggle={toggleDropdown}
            >
              {cinemaMenuLoading ? (
                <div role="status" className="px-4 py-3 text-sm font-semibold text-zinc-500">
                  Đang tải danh sách rạp...
                </div>
              ) : cinemaMenuError ? (
                <button
                  type="button"
                  role="menuitem"
                  onClick={loadCinemaMenu}
                  className={`${dropdownItemClass} text-amber-400`}
                >
                  Tải lại danh sách rạp
                </button>
              ) : cinemas.length > 0 ? (
                cinemas.map(cinema => (
                  <button
                    key={cinema.publicId}
                    type="button"
                    role="menuitem"
                    onClick={() => navigateFromHeader(`/cinema/${cinema.slug || cinema.publicId}`)}
                    className={dropdownItemClass}
                  >
                    {cinema.name}
                  </button>
                ))
              ) : (
                <div className="px-4 py-3 text-sm font-semibold text-zinc-500">
                  Chưa có rạp đang hoạt động
                </div>
              )}
            </NavDropdown>

            <button
              type="button"
              onClick={() => handleInfoOptionClick('Trải nghiệm Rạp Đặc Biệt')}
              className={`min-h-11 rounded-lg px-1 text-[13px] font-bold whitespace-nowrap text-zinc-300 transition-colors hover:text-brand-orange ${focusRingClass}`}
            >
              Rạp Đặc Biệt
            </button>
          </nav>

          <div className="flex shrink-0 items-center gap-2 sm:gap-3">
            <form
              role="search"
              onSubmit={handleSearchSubmit}
              className="relative hidden 2xl:block"
            >
              <label htmlFor="header-search" className="sr-only">
                Tìm kiếm phim
              </label>
              <input
                id="header-search"
                type="search"
                value={searchQuery}
                onChange={event => setSearchQuery(event.target.value)}
                placeholder="Tìm phim..."
                className="h-11 w-56 rounded-full border border-zinc-800 bg-zinc-900/90 py-2 pl-5 pr-12 text-sm text-white outline-none transition placeholder:text-zinc-500 hover:border-zinc-700 focus:border-brand-orange/70 focus:ring-2 focus:ring-brand-orange/15"
              />
              <button
                type="submit"
                aria-label="Tìm kiếm"
                className="absolute right-1.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-zinc-500 transition hover:bg-zinc-800 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange/70"
              >
                <Search aria-hidden="true" className="h-4 w-4" />
              </button>
            </form>

            {isAuthenticated ? (
              <div className="flex items-center gap-2.5">
                {isCustomer && <CustomerNotificationBell />}

                <div className="relative">
                  <button
                    type="button"
                    aria-label="Mở menu tài khoản"
                    aria-expanded={profileDropdownOpen}
                    aria-controls="profile-menu"
                    onClick={() => {
                      setActiveDropdown(null);
                      setProfileDropdownOpen(current => !current);
                    }}
                    className={`flex h-11 w-11 items-center justify-center overflow-hidden rounded-full border border-brand-orange/50 bg-brand-orange/10 text-sm font-black uppercase text-brand-orange transition hover:bg-brand-orange/20 ${focusRingClass}`}
                  >
                    {user?.avatarUrl && !avatarLoadFailed ? (
                      <img
                        src={getOptimizedImageUrl(resolveMediaUrl(user.avatarUrl), {
                          width: 192,
                          height: 192,
                          quality: 90,
                          gravity: 'face',
                        })}
                        alt={user?.fullName || 'Ảnh đại diện'}
                        className="h-full w-full object-cover"
                        referrerPolicy="no-referrer"
                        onError={() => setFailedAvatarUrl(user.avatarUrl)}
                      />
                    ) : (
                      user?.fullName?.charAt(0) || 'U'
                    )}
                  </button>

                  {profileDropdownOpen && (
                    <div className="absolute right-0 top-full w-64 pt-3">
                      <div id="profile-menu" ref={profileMenuRef} role="menu" aria-label="Tài khoản của tôi" className={dropdownPanelClass}>
                        <div className="mb-1 border-b border-zinc-800 px-3 pb-3 pt-1">
                          <p className="text-[10px] font-bold uppercase tracking-widest text-zinc-500">
                            Tài khoản của tôi
                          </p>
                          <p className="truncate text-sm font-bold text-white">{user?.fullName}</p>
                          {isCustomer ? (
                            <p className="mt-1 text-[11px] font-semibold text-zinc-400">
                              <span className="text-brand-orange">{headerTier}</span> · {headerScore ? `${headerPoints.toLocaleString('vi-VN')} điểm` : 'Đang tải điểm'}
                            </p>
                          ) : (
                            <p className="mt-1 text-[11px] font-semibold text-brand-orange">Tài khoản quản lý</p>
                          )}
                        </div>

                        {isCustomer ? (
                          <>
                            <button type="button" role="menuitem" onClick={() => { setProfileDropdownOpen(false); navigate('/account'); }} className={dropdownItemClass}>
                              <LayoutDashboard aria-hidden="true" className="mr-2 h-4 w-4 text-zinc-500" /> Tài khoản của tôi
                            </button>
                            <button type="button" role="menuitem" onClick={() => { setProfileDropdownOpen(false); navigate('/account/tickets'); }} className={dropdownItemClass}>
                              <Ticket aria-hidden="true" className="mr-2 h-4 w-4 text-zinc-500" /> Vé của tôi
                            </button>
                            <button type="button" role="menuitem" onClick={() => { setProfileDropdownOpen(false); navigate('/account/offers'); }} className={dropdownItemClass}>
                              <Gift aria-hidden="true" className="mr-2 h-4 w-4 text-zinc-500" /> Ưu đãi của tôi
                            </button>
                            <button type="button" role="menuitem" onClick={() => { setProfileDropdownOpen(false); navigate('/account/loyalty'); }} className={dropdownItemClass}>
                              <Award aria-hidden="true" className="mr-2 h-4 w-4 text-zinc-500" /> Điểm & hạng thành viên
                            </button>
                            <button type="button" role="menuitem" onClick={() => { setProfileDropdownOpen(false); navigate('/account/security'); }} className={dropdownItemClass}>
                              <ShieldCheck aria-hidden="true" className="mr-2 h-4 w-4 text-zinc-500" /> Bảo mật tài khoản
                            </button>
                          </>
                        ) : (
                          <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setProfileDropdownOpen(false);
                              navigate(managementPath);
                            }}
                            className={`${dropdownItemClass} text-brand-orange`}
                          >
                            Vào trang quản lý
                          </button>
                        )}

                        <button
                          type="button"
                          role="menuitem"
                          onClick={handleLogoutClick}
                          className={`${dropdownItemClass} mt-1 border-t border-zinc-800 text-red-400 hover:bg-red-950/30 hover:text-red-300`}
                        >
                          <LogOut aria-hidden="true" className="mr-2 h-4 w-4" />
                          Đăng xuất
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => navigate('/login')}
                className={`hidden min-h-11 rounded-full bg-brand-orange px-5 text-xs font-black uppercase tracking-wide text-white shadow-lg shadow-brand-orange/10 transition hover:bg-orange-600 sm:block ${focusRingClass}`}
              >
                Đăng nhập
              </button>
            )}

            <button
              type="button"
              onClick={() => {
                setActiveDropdown(null);
                setProfileDropdownOpen(false);
                setMobileMenuOpen(current => !current);
              }}
              aria-label={mobileMenuOpen ? 'Đóng menu điều hướng' : 'Mở menu điều hướng'}
              aria-expanded={mobileMenuOpen}
              aria-controls="mobile-navigation"
              className={`flex h-11 w-11 items-center justify-center rounded-xl border border-zinc-800 text-zinc-300 transition hover:bg-zinc-900 hover:text-white lg:hidden ${focusRingClass}`}
            >
              {mobileMenuOpen ? (
                <X aria-hidden="true" className="h-5 w-5" />
              ) : (
                <Menu aria-hidden="true" className="h-5 w-5" />
              )}
            </button>
          </div>
        </div>

        {mobileMenuOpen && (
          <div
            id="mobile-navigation"
            className="absolute inset-x-0 top-full max-h-[calc(100vh-5rem)] overflow-y-auto border-b border-zinc-800 bg-zinc-950 px-4 py-5 shadow-2xl lg:hidden"
          >
            <div className="mx-auto flex max-w-7xl flex-col gap-5 sm:px-2">
              <form role="search" onSubmit={handleSearchSubmit} className="relative">
                <label htmlFor="mobile-header-search" className="sr-only">
                  Tìm kiếm phim
                </label>
                <input
                  id="mobile-header-search"
                  type="search"
                  value={searchQuery}
                  onChange={event => setSearchQuery(event.target.value)}
                  placeholder="Tìm phim..."
                  className="h-12 w-full rounded-xl border border-zinc-800 bg-zinc-900 pl-4 pr-12 text-sm text-white outline-none placeholder:text-zinc-500 focus:border-brand-orange"
                />
                <button
                  type="submit"
                  aria-label="Tìm kiếm"
                  className="absolute right-1 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-lg text-zinc-400"
                >
                  <Search aria-hidden="true" className="h-5 w-5" />
                </button>
              </form>

              <button
                type="button"
                onClick={() => navigateFromHeader('/booking')}
                className={`flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange text-sm font-black uppercase tracking-wide text-white shadow-lg shadow-brand-orange/20 ${focusRingClass}`}
              >
                <Star aria-hidden="true" className="h-4 w-4 fill-current" />
                Mua vé nhanh
              </button>

              {!isAuthenticated && (
                <button
                  type="button"
                  onClick={() => navigateFromHeader('/login')}
                  className={`min-h-12 w-full rounded-xl border border-brand-orange/60 text-sm font-black uppercase tracking-wide text-brand-orange sm:hidden ${focusRingClass}`}
                >
                  Đăng nhập
                </button>
              )}

              <div className="grid gap-5 sm:grid-cols-2">
                <section aria-labelledby="mobile-movies-heading">
                  <h2
                    id="mobile-movies-heading"
                    className="mb-2 text-[11px] font-black uppercase tracking-[0.18em] text-zinc-500"
                  >
                    Phim
                  </h2>
                  <button
                    type="button"
                    onClick={() => navigateFromHeader('/movies?status=NOW_SHOWING')}
                    className={dropdownItemClass}
                  >
                    Phim đang chiếu
                  </button>
                  <button
                    type="button"
                    onClick={() => navigateFromHeader('/movies?status=UPCOMING')}
                    className={dropdownItemClass}
                  >
                    Phim sắp chiếu
                  </button>
                </section>

                <section aria-labelledby="mobile-cinema-corner-heading">
                  <h2
                    id="mobile-cinema-corner-heading"
                    className="mb-2 text-[11px] font-black uppercase tracking-[0.18em] text-zinc-500"
                  >
                    Góc Điện Ảnh
                  </h2>
                  {['Thể loại phim', 'Diễn viên', 'Đạo diễn'].map(item => (
                    <button
                      key={item}
                      type="button"
                      onClick={() => handleInfoOptionClick(item)}
                      className={dropdownItemClass}
                    >
                      {item}
                    </button>
                  ))}
                </section>

                <section aria-labelledby="mobile-cinemas-heading">
                  <h2
                    id="mobile-cinemas-heading"
                    className="mb-2 text-[11px] font-black uppercase tracking-[0.18em] text-zinc-500"
                  >
                    Rạp/Giá Vé
                  </h2>
                  {cinemaMenuLoading ? (
                    <p className="px-4 py-3 text-sm font-semibold text-zinc-500">
                      Đang tải danh sách rạp...
                    </p>
                  ) : cinemaMenuError ? (
                    <button
                      type="button"
                      onClick={loadCinemaMenu}
                      className={`${dropdownItemClass} text-amber-400`}
                    >
                      Tải lại danh sách rạp
                    </button>
                  ) : cinemas.length > 0 ? (
                    cinemas.map(cinema => (
                      <button
                        key={cinema.publicId}
                        type="button"
                        onClick={() => navigateFromHeader(`/cinema/${cinema.slug || cinema.publicId}`)}
                        className={dropdownItemClass}
                      >
                        {cinema.name}
                      </button>
                    ))
                  ) : (
                    <p className="px-4 py-3 text-sm font-semibold text-zinc-500">
                      Chưa có rạp đang hoạt động
                    </p>
                  )}
                </section>

                <section aria-labelledby="mobile-more-heading">
                  <h2
                    id="mobile-more-heading"
                    className="mb-2 text-[11px] font-black uppercase tracking-[0.18em] text-zinc-500"
                  >
                    Khám phá thêm
                  </h2>
                  <button
                    type="button"
                    onClick={() => handleInfoOptionClick('Khuyến mãi và Sự kiện')}
                    className={dropdownItemClass}
                  >
                    Khuyến mãi
                  </button>
                  <button
                    type="button"
                    onClick={() => handleInfoOptionClick('Trải nghiệm Rạp Đặc Biệt')}
                    className={dropdownItemClass}
                  >
                    Rạp Đặc Biệt
                  </button>
                </section>
              </div>
            </div>
          </div>
        )}
      </header>

      {infoModalContent && (
        <div
          role="presentation"
          onMouseDown={event => {
            if (event.target === event.currentTarget) setInfoModalContent(null);
          }}
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="header-info-title"
            className="w-full max-w-md space-y-6 rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-2xl md:p-8"
          >
            <div>
              <h2
                id="header-info-title"
                className="text-base font-black uppercase tracking-wider text-white"
              >
                {infoModalContent}
              </h2>
              <p className="mt-1 text-xs text-zinc-500">Hệ thống thông tin giải trí LoraFilm</p>
            </div>
            <p className="border-y border-zinc-800 py-5 text-sm leading-relaxed text-zinc-300">
              Thông tin mục <strong className="text-white">{infoModalContent}</strong> đang được
              đồng bộ và cập nhật từ ban quản lý rạp. Vui lòng quay lại sau.
            </p>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setInfoModalContent(null)}
                className={`rounded-xl bg-brand-orange px-6 py-2.5 text-xs font-black uppercase tracking-wider text-white transition hover:bg-orange-600 ${focusRingClass}`}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
