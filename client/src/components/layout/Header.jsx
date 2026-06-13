import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronDown, Menu, X, Search, User, LogOut } from "lucide-react";

export default function Header() {
    const navigate = useNavigate();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [activeDropdown, setActiveDropdown] = useState(null); // 'phim' | 'goc-dien-anh' | 'rap-gia-ve' | null
    const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
    const [isAuthenticated, setIsAuthenticated] = useState(() => !!localStorage.getItem("token"));

    const handleLogout = () => {
        localStorage.removeItem("token");
        setIsAuthenticated(false);
        setProfileDropdownOpen(false);
        navigate("/");
    };

    return (
        <header className="fixed top-0 left-0 w-full z-50 bg-zinc-950/90 backdrop-blur-md px-6 md:px-12 py-2.5 flex justify-between items-center border-b border-zinc-800/80 select-none">
            {/* Logo Section */}
            <div className="flex items-center gap-4 md:gap-6">
                <Link to="/" className="flex items-center gap-2.5 shrink-0 bg-transparent p-0 m-0 border-none shadow-none outline-none group mr-1 md:mr-2 select-none decoration-none transition-transform duration-200 hover:scale-[1.02]">
                    <img 
                        src="https://res.cloudinary.com/dqc4hufot/image/upload/q_auto/f_auto/v1781114108/main-logo_l5is5w.png" 
                        alt="LoraFilm Mascot" 
                        className="h-9 sm:h-10 w-auto object-contain bg-transparent will-change-transform"
                    />
                    <span className="text-xl sm:text-2xl font-black tracking-tight text-white font-sans flex items-center leading-none">
                        Lora
                        <span className="text-brand-coral font-black ml-0.5 group-hover:text-orange-400 transition-colors">
                            Film
                        </span>
                    </span>
                </Link>

                {/* Mua vé button */}
                <button
                    onClick={() => {
                        const element = document.getElementById("movies-section");
                        element?.scrollIntoView({ behavior: "smooth" });
                    }}
                    className="h-9 bg-brand-coral hover:bg-orange-500 text-white text-[11px] font-black rounded-[10px] flex items-center pl-3.5 pr-3 transition-all duration-300 shadow-md shadow-brand-coral/25 uppercase tracking-wider focus:outline-none shrink-0 gap-2 select-none"
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="currentColor"
                        className="w-3.5 h-3.5 text-white"
                    >
                        <path fillRule="evenodd" d="M10.788 3.21c.448-1.077 1.976-1.077 2.424 0l2.082 5.006 5.404.434c1.164.093 1.636 1.545.749 2.305l-4.117 3.527 1.257 5.273c.271 1.136-.964 2.033-1.96 1.425L12 18.354 7.373 21.18c-.996.608-2.231-.29-1.96-1.425l1.257-5.273-4.117-3.527c-.887-.76-.415-2.212.749-2.305l5.404-.434 2.082-5.005Z" clipRule="evenodd" />
                    </svg>
                    <span>MUA VÉ</span>
                    <span className="h-3.5 border-l border-dashed border-white/30"></span>
                    <span className="h-1.5 w-1.5 rounded-full bg-white"></span>
                </button>
            </div>

            {/* Navigation Menus */}
            <nav className="hidden lg:flex items-center gap-6 font-semibold text-xs uppercase tracking-wider">
                {/* Phim Dropdown */}
                <div 
                    className="relative py-2"
                    onMouseEnter={() => setActiveDropdown("phim")}
                    onMouseLeave={() => setActiveDropdown(null)}
                >
                    <button 
                        type="button"
                        className="text-zinc-300 hover:text-brand-coral flex items-center gap-1 transition-colors focus:outline-none"
                    >
                        <span>Phim</span>
                        <ChevronDown className="w-3.5 h-3.5 text-zinc-550" />
                    </button>
                    {activeDropdown === "phim" && (
                        <div className="absolute left-0 mt-2 w-48 bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden shadow-2xl z-50 py-2">
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Phim đang chiếu
                            </Link>
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Phim sắp chiếu
                            </Link>
                        </div>
                    )}
                </div>

                {/* Góc Điện Ảnh Dropdown */}
                <div 
                    className="relative py-2"
                    onMouseEnter={() => setActiveDropdown("goc-dien-anh")}
                    onMouseLeave={() => setActiveDropdown(null)}
                >
                    <button 
                        type="button"
                        className="text-zinc-300 hover:text-brand-coral flex items-center gap-1 transition-colors focus:outline-none"
                    >
                        <span>Góc Điện Ảnh</span>
                        <ChevronDown className="w-3.5 h-3.5 text-zinc-550" />
                    </button>
                    {activeDropdown === "goc-dien-anh" && (
                        <div className="absolute left-0 mt-2 w-48 bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden shadow-2xl z-50 py-2">
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Thể loại phim
                            </Link>
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Diễn viên
                            </Link>
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Đạo diễn
                            </Link>
                        </div>
                    )}
                </div>

                {/* Sự Kiện */}
                <Link
                    to="/"
                    className="text-zinc-300 hover:text-brand-coral py-2 transition-colors focus:outline-none"
                >
                    Sự Kiện
                </Link>

                {/* Rạp/Giá Vé Dropdown */}
                <div 
                    className="relative py-2"
                    onMouseEnter={() => setActiveDropdown("rap-gia-ve")}
                    onMouseLeave={() => setActiveDropdown(null)}
                >
                    <button 
                        type="button"
                        className="text-zinc-300 hover:text-brand-coral flex items-center gap-1 transition-colors focus:outline-none"
                    >
                        <span>Rạp/Giá Vé</span>
                        <ChevronDown className="w-3.5 h-3.5 text-zinc-550" />
                    </button>
                    {activeDropdown === "rap-gia-ve" && (
                        <div className="absolute left-0 mt-2 w-48 bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden shadow-2xl z-50 py-2">
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Lora Nguyễn Du
                            </Link>
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Lora Thảo Điền
                            </Link>
                            <Link
                                to="/"
                                className="block w-full text-left px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                            >
                                Lora Royal City
                            </Link>
                        </div>
                    )}
                </div>

                {/* Rạp Đặc Biệt */}
                <Link
                    to="/"
                    className="text-zinc-300 hover:text-brand-coral py-2 transition-colors focus:outline-none"
                >
                    Rạp Đặc Biệt
                </Link>
            </nav>

            {/* Search and Auth controls */}
            <div className="flex items-center gap-4">
                {/* Search Bar */}
                <div className="relative hidden sm:block">
                    <div className="w-48 md:w-64 h-10 relative flex items-center bg-zinc-900/80 border border-zinc-800 rounded-full pl-4 pr-10 focus-within:border-brand-coral focus-within:w-72 transition-all duration-300">
                        <input
                            type="text"
                            placeholder="Tìm phim, diễn viên..."
                            className="bg-transparent text-white text-xs w-full h-full focus:outline-none placeholder-zinc-500"
                        />
                        <Search className="w-4 h-4 text-zinc-500 absolute right-3 pointer-events-none" />
                    </div>
                </div>

                {/* Auth Check */}
                {isAuthenticated ? (
                    <div className="relative">
                        <button
                            onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                            className="w-9 h-9 rounded-full bg-brand-coral/10 border border-brand-coral/40 flex items-center justify-center text-brand-coral hover:bg-brand-coral/20 transition-all font-black text-sm uppercase focus:outline-none"
                        >
                            <User className="w-4.5 h-4.5 text-brand-coral" />
                        </button>

                        {profileDropdownOpen && (
                            <div className="absolute right-0 mt-3 w-48 bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden shadow-2xl z-50 py-2">
                                <Link
                                    to="/"
                                    onClick={() => setProfileDropdownOpen(false)}
                                    className="block px-4 py-2 text-xs text-zinc-300 hover:bg-zinc-800 hover:text-white transition-colors"
                                >
                                    Thông tin cá nhân
                                </Link>
                                <button
                                    onClick={handleLogout}
                                    className="w-full text-left px-4 py-2 text-xs text-red-400 hover:bg-red-950/20 hover:text-red-300 font-bold border-t border-zinc-800/80 mt-1 flex items-center gap-2"
                                >
                                    <LogOut className="w-3.5 h-3.5" />
                                    <span>Đăng xuất</span>
                                </button>
                            </div>
                        )}
                    </div>
                ) : (
                    <button
                        onClick={() => navigate("/login")}
                        className="bg-brand-coral hover:bg-orange-500 text-white text-xs font-black py-2.5 px-5 rounded-full transition-all duration-300 shadow-lg shadow-brand-coral/15 uppercase tracking-wider focus:outline-none"
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

            {/* Mobile Menu drawer */}
            {mobileMenuOpen && (
                <div className="absolute top-[60px] left-0 w-full bg-zinc-950 border-b border-zinc-800 px-6 py-6 flex flex-col gap-4 lg:hidden z-40">
                    <div className="space-y-1 border-b border-zinc-900 pb-2">
                        <span className="text-[10px] text-zinc-550 font-black tracking-wider uppercase block">Phim</span>
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="block text-zinc-200 hover:text-brand-coral py-1.5 text-xs font-bold uppercase"
                        >
                            Phim đang chiếu
                        </Link>
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="block text-zinc-200 hover:text-brand-coral py-1.5 text-xs font-bold uppercase"
                        >
                            Phim sắp chiếu
                        </Link>
                    </div>

                    <div className="space-y-1 border-b border-zinc-900 pb-2">
                        <span className="text-[10px] text-zinc-550 font-black tracking-wider uppercase block">Góc điện ảnh</span>
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="block text-zinc-200 hover:text-brand-coral py-1.5 text-xs font-bold uppercase"
                        >
                            Thể loại phim
                        </Link>
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="block text-zinc-200 hover:text-brand-coral py-1.5 text-xs font-bold uppercase"
                        >
                            Diễn viên
                        </Link>
                    </div>

                    <div className="flex flex-col gap-2.5">
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="text-zinc-200 hover:text-brand-coral text-xs font-bold uppercase"
                        >
                            Sự Kiện
                        </Link>
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="text-zinc-200 hover:text-brand-coral text-xs font-bold uppercase"
                        >
                            Rạp/Giá Vé
                        </Link>
                        <Link
                            to="/"
                            onClick={() => setMobileMenuOpen(false)}
                            className="text-zinc-200 hover:text-brand-coral text-xs font-bold uppercase"
                        >
                            Rạp Đặc Biệt
                        </Link>
                    </div>
                </div>
            )}
        </header>
    );
}
