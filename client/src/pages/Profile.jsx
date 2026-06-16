import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
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
        document.title = "Thông Tin Cá Nhân - LoraFilm";

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

        fetchProfile();
    }, [accountId, navigate]);

    return (
        <div className="flex flex-col min-h-screen bg-[#050506] text-white selection:bg-brand-coral selection:text-white">
            <Header />

            <main className="flex-grow pt-28 pb-16 px-6 md:px-12 flex items-center justify-center relative overflow-hidden">
                <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-coral/5 rounded-full filter blur-3xl pointer-events-none"></div>
                <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-brand-yellow/5 rounded-full filter blur-3xl pointer-events-none"></div>

                <article className="bg-zinc-900/80 border border-zinc-800 p-8 rounded-2xl w-full max-w-2xl shadow-2xl shadow-black/50 relative z-10">
                    <header className="border-b border-zinc-800 pb-6 mb-6">
                        <h1 className="text-3xl font-black tracking-tight text-white font-sans uppercase">
                            Thông tin <span className="text-brand-coral">cá nhân</span>
                        </h1>
                        <p className="text-zinc-400 text-sm mt-1">
                            Quản lý thông tin hồ sơ tài khoản LoraFilm của bạn.
                        </p>
                    </header>

                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-12 gap-3">
                            <div className="w-10 h-10 border-4 border-brand-coral border-t-transparent rounded-full animate-spin"></div>
                            <p className="text-zinc-400 text-sm">Đang tải dữ liệu hồ sơ...</p>
                        </div>
                    ) : error ? (
                        <div className="bg-red-950/50 border border-red-800/80 rounded-xl p-4 flex items-start gap-3 text-red-200 text-sm leading-relaxed">
                            <span>{error}</span>
                        </div>
                    ) : profile ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Họ và tên</span>
                                <span className="text-base font-bold text-zinc-100">{profile.fullName || "N/A"}</span>
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Địa chỉ Email</span>
                                <span className="text-base font-bold text-zinc-100">{email || "N/A"}</span>
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Số điện thoại</span>
                                <span className="text-base font-bold text-zinc-100">{profile.phoneNumber || "N/A"}</span>
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Ngày sinh</span>
                                <span className="text-base font-bold text-zinc-100">{profile.birthday || "N/A"}</span>
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Giới tính</span>
                                <span className="text-base font-bold text-zinc-100">
                                    {profile.gender === "MALE" ? "Nam" : profile.gender === "FEMALE" ? "Nữ" : "Khác"}
                                </span>
                            </div>

                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Tỉnh/Thành phố</span>
                                <span className="text-base font-bold text-zinc-100">{profile.provinceName || "N/A"}</span>
                            </div>

                            <div className="flex flex-col gap-1 md:col-span-2 bg-zinc-950/50 border border-zinc-800/80 rounded-xl p-4 mt-2">
                                <span className="text-xs font-black text-zinc-500 uppercase tracking-wider">Số Căn cước công dân (CCCD)</span>
                                <span className="text-lg font-black tracking-widest text-[#ff7a1a] mt-1">
                                    {profile.cccdMasked || "N/A"}
                                </span>
                                <p className="text-[10px] text-zinc-500 mt-1">
                                    Thông tin định danh đã được mã hóa một phần nhằm mục đích bảo vệ dữ liệu cá nhân của bạn.
                                </p>
                            </div>
                        </div>
                    ) : null}
                </article>
            </main>

            <Footer />
        </div>
    );
}
