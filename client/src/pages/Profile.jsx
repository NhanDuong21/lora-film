import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { getUserProfile } from "../services/userService";
import { getUserAccountId, getUserEmail, isAuthenticated } from "../utils/authStorage";
import Header from "../components/layout/Header";
import Footer from "../components/layout/Footer";

export default function Profile() {
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);

    const accountId = getUserAccountId();
    const email = getUserEmail();

    useEffect(() => {
        document.title = "Tài Khoản Thành Viên - LoraFilm";

        if (!isAuthenticated()) {
            navigate("/login");
            return;
        }

        const fetchProfile = async () => {
            try {
                const res = await getUserProfile(accountId);
                if (res.success && res.data) {
                    setProfile(res.data);
                } else {
                    setError("Không thể tải thông tin hồ sơ.");
                }
            } catch (err) {
                setError(err?.message || "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.");
            } finally {
                setLoading(false);
            }
        };

        const timer = setTimeout(() => {
            fetchProfile();
        }, 100);

        return () => clearTimeout(timer);
    }, [accountId, navigate]);

    const formatDate = (dateString) => {
        if (!dateString) return "15/05/1998";
        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) return dateString;
            const day = String(date.getDate()).padStart(2, "0");
            const month = String(date.getMonth() + 1).padStart(2, "0");
            const year = date.getFullYear();
            return `${day}/${month}/${year}`;
        } catch {
            return dateString;
        }
    };

    return (
        <div className="flex flex-col min-h-screen bg-[#050506] text-white selection:bg-[#ff7a1a] selection:text-zinc-950 font-sans">
            <Header />

            <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-6xl mx-auto w-full">
                {/* Header Section & Page Navigation */}
                <div className="flex justify-between items-center mb-6 border-b border-zinc-800 pb-3">
                    <h2 className="text-lg sm:text-2xl font-black text-white uppercase tracking-wider">
                        TÀI KHOẢN THÀNH VIÊN
                    </h2>
                    <Link to="/" className="text-xs sm:text-sm font-bold text-zinc-400 hover:text-[#ff7a1a] transition-colors">
                        Quay lại trang chủ
                    </Link>
                </div>

                {loading ? (
                    <div className="flex flex-col items-center justify-center py-20 gap-3">
                        <div className="w-10 h-10 border-4 border-[#ff7a1a] border-t-transparent rounded-full animate-spin"></div>
                        <p className="text-zinc-400 text-xs sm:text-sm">Đang tải dữ liệu hồ sơ...</p>
                    </div>
                ) : error ? (
                    <div className="bg-red-955/40 border border-red-800/80 rounded-xl p-4 flex items-start gap-3 text-red-200 text-xs sm:text-sm leading-relaxed max-w-xl mx-auto">
                        <span>{error}</span>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 sm:gap-8">
                        
                        {/* Column 1: Left Membership Sidebar Widget */}
                        <div className="flex flex-col gap-5 lg:col-span-1">
                            
                            {/* User Identity Card */}
                            <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-5 flex flex-col items-center text-center shadow-lg">
                                {/* Avatar element container */}
                                <div className="w-20 h-20 rounded-full border-2 border-[#ff7a1a]/40 p-1 mb-3 flex items-center justify-center relative overflow-hidden bg-zinc-950">
                                    <svg className="w-12 h-12 text-zinc-700" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                        <path fillRule="evenodd" d="M18.685 19.097A9.723 9.723 0 0021.75 12c0-5.385-4.365-9.75-9.75-9.75S2.25 6.615 2.25 12a9.723 9.723 0 003.065 7.097A9.716 9.716 0 0012 21.75a9.716 9.716 0 006.685-2.653zm-12.54-1.285A7.486 7.486 0 0112 15a7.486 7.486 0 015.855 2.812A8.224 8.224 0 0112 20.25a8.224 8.224 0 01-6.285-3.078z" clipRule="evenodd" />
                                        <path fillRule="evenodd" d="M12 13.5a3.75 3.75 0 100-7.5 3.75 3.75 0 000 7.5z" clipRule="evenodd" />
                                    </svg>
                                </div>

                                <h3 className="text-base font-black text-white tracking-wide">
                                    {profile?.fullName || "Dương Thiện Nhân"}
                                </h3>

                                <div className="flex items-center gap-2 mt-1.5">
                                    <span className="bg-[#ff7a1a]/20 text-[#ff7a1a] border border-[#ff7a1a]/30 text-[9px] font-black tracking-widest px-2 py-0.5 rounded-full uppercase">
                                        SILVER VIP
                                    </span>
                                    <span className="text-[11px] font-bold text-zinc-400">
                                        250 Điểm
                                    </span>
                                </div>

                                {/* Membership Spending Progress Bar */}
                                <div className="w-full border-t border-zinc-800/80 mt-5 pt-5 text-left">
                                    <div className="flex justify-between items-center text-[10px] font-black tracking-wider text-zinc-400 mb-1.5">
                                        <span>TỔNG CHI TIÊU 2026</span>
                                        <span className="text-white">2.500.000đ</span>
                                    </div>
                                    
                                    <div className="w-full bg-zinc-950 rounded-full h-1.5 overflow-hidden border border-zinc-800/60 relative">
                                        <div className="bg-gradient-to-r from-orange-500 to-[#ff7a1a] h-full rounded-full w-[62.5%]"></div>
                                    </div>
                                    
                                    <div className="flex justify-between text-[8px] sm:text-[9px] text-zinc-500 mt-1.5 font-black uppercase tracking-wider">
                                        <span>0đ Standard</span>
                                        <span>2.000.000đ Bạc</span>
                                        <span>4.000.000đ Vàng</span>
                                    </div>
                                </div>
                            </div>

                            {/* Member Support Navigation Card */}
                            <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-5 shadow-lg">
                                <h4 className="text-[10px] font-black text-zinc-400 tracking-widest uppercase mb-3">
                                    HỖ TRỢ THÀNH VIÊN
                                </h4>
                                <div className="flex flex-col">
                                    <div className="flex items-center justify-between py-2.5 border-b border-zinc-800/80 hover:text-[#ff7a1a] transition-colors cursor-pointer group">
                                        <span className="text-xs text-zinc-300 font-bold group-hover:text-[#ff7a1a]">HOTLINE hỗ trợ (1900 1000)</span>
                                        <svg className="w-3.5 h-3.5 text-zinc-650 group-hover:text-[#ff7a1a] transition-colors" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                            <polyline points="9 18 15 12 9 6" />
                                        </svg>
                                    </div>
                                    <div className="flex items-center justify-between py-2.5 border-b border-zinc-800/80 hover:text-[#ff7a1a] transition-colors cursor-pointer group">
                                        <span className="text-xs text-zinc-300 font-bold group-hover:text-[#ff7a1a]">Email hỗ trợ (support@lorafilm.com)</span>
                                        <svg className="w-3.5 h-3.5 text-zinc-650 group-hover:text-[#ff7a1a] transition-colors" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                            <polyline points="9 18 15 12 9 6" />
                                        </svg>
                                    </div>
                                    <div className="flex items-center justify-between py-2.5 hover:text-[#ff7a1a] transition-colors cursor-pointer group">
                                        <span className="text-xs text-zinc-300 font-bold group-hover:text-[#ff7a1a]">Câu hỏi thường gặp</span>
                                        <svg className="w-3.5 h-3.5 text-zinc-650 group-hover:text-[#ff7a1a] transition-colors" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                            <polyline points="9 18 15 12 9 6" />
                                        </svg>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Column 2: The Right Main Workspace */}
                        <div className="lg:col-span-2 flex flex-col gap-5">
                            
                            {/* Sub-Navigation Tab Bar Menu */}
                            <div className="flex flex-wrap gap-1.5 border-b border-zinc-800/80 pb-2">
                                <button className="bg-[#e28574] text-white text-xs font-black tracking-wider uppercase px-4 py-2 rounded-full select-none shadow-md shadow-orange-950/20">
                                    Thông Tin Cá Nhân
                                </button>
                                <button className="text-zinc-500 hover:text-white text-xs font-black tracking-wider uppercase px-4 py-2 rounded-full transition-colors cursor-not-allowed">
                                    Lịch Sử Giao Dịch
                                </button>
                                <button className="text-zinc-500 hover:text-white text-xs font-black tracking-wider uppercase px-4 py-2 rounded-full transition-colors cursor-not-allowed">
                                    Thông Báo
                                </button>
                                <button className="text-zinc-500 hover:text-white text-xs font-black tracking-wider uppercase px-4 py-2 rounded-full transition-colors cursor-not-allowed">
                                    Quà Tặng
                                </button>
                                <button className="text-zinc-500 hover:text-white text-xs font-black tracking-wider uppercase px-4 py-2 rounded-full transition-colors cursor-not-allowed">
                                    Chính Sách
                                </button>
                            </div>

                            {/* Main Form Profile Details Box */}
                            <article className="bg-white/5 backdrop-blur-md border border-white/10 p-5 sm:p-6 rounded-2xl shadow-2xl relative">
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-5">
                                    
                                    {/* HỌ VÀ TÊN */}
                                    <div className="flex flex-col gap-1.5 w-full relative">
                                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                            HỌ VÀ TÊN
                                        </label>
                                        <div className="relative flex items-center group w-full">
                                            <input
                                                type="text"
                                                readOnly
                                                value={profile?.fullName || "Dương Thiện Nhân"}
                                                className="w-full h-11 bg-zinc-950/60 border border-zinc-800/80 focus:border-brand-coral rounded-xl pl-10 pr-3 text-sm text-zinc-100 outline-none cursor-default font-bold"
                                            />
                                            <div className="absolute left-3.5 text-zinc-500 pointer-events-none flex items-center justify-center">
                                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                                    <circle cx="12" cy="7" r="4" />
                                                </svg>
                                            </div>
                                        </div>
                                    </div>

                                    {/* NGÀY SINH */}
                                    <div className="flex flex-col gap-1.5 w-full relative">
                                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                            NGÀY SINH
                                        </label>
                                        <div className="relative flex items-center w-full">
                                            <input
                                                type="text"
                                                readOnly
                                                value={formatDate(profile?.birthday)}
                                                className="w-full h-11 bg-zinc-950/60 border border-zinc-800/80 rounded-xl px-3 text-sm text-zinc-100 outline-none cursor-default font-bold"
                                            />
                                        </div>
                                    </div>

                                    {/* ĐỊA CHỈ EMAIL */}
                                    <div className="flex flex-col gap-1.5 w-full relative">
                                        <div className="flex justify-between items-center w-full">
                                            <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                                ĐỊA CHỈ EMAIL
                                            </label>
                                            <button type="button" className="text-[11px] font-black text-[#ff7a1a] hover:text-orange-400 focus:outline-none transition-colors">
                                                Thay đổi
                                            </button>
                                        </div>
                                        <div className="relative flex items-center w-full">
                                            <input
                                                type="email"
                                                readOnly
                                                value={profile?.email || email || "member@gmail.com"}
                                                className="w-full h-11 bg-zinc-950/60 border border-zinc-800/80 rounded-xl px-3 text-sm text-zinc-100 outline-none cursor-default font-bold"
                                            />
                                        </div>
                                    </div>

                                    {/* SỐ ĐIỆN THOẠI */}
                                    <div className="flex flex-col gap-1.5 w-full relative">
                                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                            SỐ ĐIỆN THOẠI
                                        </label>
                                        <div className="relative flex items-center w-full">
                                            <input
                                                type="text"
                                                readOnly
                                                value={profile?.phoneNumber || "0987654321"}
                                                className="w-full h-11 bg-zinc-950/60 border border-zinc-800/80 rounded-xl px-3 text-sm text-zinc-100 outline-none cursor-default font-bold"
                                            />
                                        </div>
                                    </div>

                                    {/* GIỚI TÍNH */}
                                    <div className="flex flex-col gap-1.5 w-full relative">
                                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                            GIỚI TÍNH
                                        </label>
                                        <div className="flex gap-3 w-full">
                                            <div className={`flex-1 h-11 flex items-center justify-center text-sm font-bold rounded-xl border transition-all ${(!profile?.gender || profile?.gender === 'MALE') ? 'bg-[#ff7a1a]/10 border-[#ff7a1a] text-[#ff7a1a]' : 'bg-zinc-950/60 border-zinc-800/80 text-zinc-500'}`}>
                                                Nam
                                            </div>
                                            <div className={`flex-1 h-11 flex items-center justify-center text-sm font-bold rounded-xl border transition-all ${(profile?.gender === 'FEMALE') ? 'bg-[#ff7a1a]/10 border-[#ff7a1a] text-[#ff7a1a]' : 'bg-zinc-950/60 border-zinc-800/80 text-zinc-500'}`}>
                                                Nữ
                                            </div>
                                        </div>
                                    </div>

                                    {/* MẬT KHẨU */}
                                    <div className="flex flex-col gap-1.5 w-full relative">
                                        <div className="flex justify-between items-center w-full">
                                            <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                                MẬT KHẨU
                                            </label>
                                            <button type="button" className="text-[11px] font-black text-[#ff7a1a] hover:text-orange-400 focus:outline-none transition-colors">
                                                Thay đổi
                                            </button>
                                        </div>
                                        <div className="relative flex items-center w-full">
                                            <input
                                                type="password"
                                                readOnly
                                                value="............"
                                                className="w-full h-11 bg-zinc-950/60 border border-zinc-800/80 rounded-xl px-3 text-sm text-zinc-100 outline-none cursor-default font-black tracking-widest"
                                            />
                                        </div>
                                    </div>

                                    {/* SỐ CĂN CƯỚC CÔNG DÂN (CCCD) */}
                                    <div className="flex flex-col gap-1.5 w-full relative md:col-span-2">
                                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest">
                                            SỐ CĂN CƯỚC CÔNG DÂN (CCCD)
                                        </label>
                                        <div className="relative flex items-center w-full">
                                            <input
                                                type="text"
                                                readOnly
                                                value={profile?.cccdMasked || "092******749"}
                                                className="w-full h-11 bg-zinc-950/60 border border-zinc-800/80 rounded-xl px-3 text-sm text-[#ff7a1a] outline-none cursor-default font-black tracking-widest"
                                            />
                                        </div>
                                    </div>
                                </div>

                                {/* Submit Control Button */}
                                <div className="flex justify-end mt-6">
                                    <button
                                        type="button"
                                        className="h-11 bg-gradient-to-r from-orange-500 to-[#ff7a1a] hover:from-orange-600 hover:to-orange-500 text-zinc-950 font-black px-8 rounded-xl text-xs sm:text-sm transition-all shadow-lg shadow-amber-500/10 uppercase tracking-widest focus:outline-none cursor-pointer flex items-center justify-center"
                                    >
                                        CẬP NHẬT
                                    </button>
                                </div>
                            </article>
                        </div>
                    </div>
                )}
            </main>

            <Footer />
        </div>
    );
}
