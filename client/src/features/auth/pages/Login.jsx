import { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { login } from "@/features/auth/services/authService";
import { useAuth } from "@/contexts/AuthContext";
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';
import { Mail, Lock, ArrowLeft, Loader2, Eye, EyeOff } from 'lucide-react';

function Login() {
    const { login: contextLogin } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const [email, setEmail] = useState(() => location.state?.email || "");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [successMessage, setSuccessMessage] = useState(() => 
        location.state?.verified ? "Xác thực tài khoản thành công! Vui lòng đăng nhập." : ""
    );
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        document.title = "Đăng Nhập - LoraFilm Ticket Booking";
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMsg("");
        setSuccessMessage("");

        if (!email.trim() || !password.trim()) {
            setErrorMsg("Vui lòng nhập đầy đủ email và mật khẩu.");
            return;
        }

        setIsSubmitting(true);
        try {
            const data = await login(email, password);
            setIsSubmitting(false);

            if (data?.success && data?.data) {
                await contextLogin(data.data);
                setSuccessMessage("Đăng nhập thành công. Đang chuyển hướng...");

                // Navigate based on role-specific paths
                const role = data.data.role;
                setTimeout(() => {
                    if (role === "ADMIN" || role === "ROLE_ADMIN" || role === "ROLE_ACCOUNTANT") {
                        navigate("/admin");
                    } else if (role === "EMPLOYEE" || role === "STAFF" || role === "ROLE_STAFF") {
                        navigate("/employee");
                    } else {
                        const from = location.state?.from;
                        const redirectTo = from?.pathname
                            ? `${from.pathname}${from.search || ""}${from.hash || ""}`
                            : "/";
                        navigate(redirectTo, { replace: true });
                    }
                }, 400);
            } else {
                setErrorMsg("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.");
            }
        } catch (error) {
            setIsSubmitting(false);

            const errorCode = error?.errorCode || error?.code || error?.error;
            
            if (errorCode === "AUTH_ACCOUNT_NOT_VERIFIED") {
                // Clear any stored/temporary auth state
                sessionStorage.setItem("pending_otp_email", email);
                sessionStorage.setItem("pending_otp_purpose", "REGISTRATION");

                setErrorMsg("Tài khoản chưa được xác thực. Đang chuyển hướng sang trang xác thực OTP...");
                setTimeout(() => {
                    navigate("/verify-otp", {
                        state: {
                            email: email,
                            purpose: "REGISTRATION"
                        }
                    });
                }, 1500);
                return;
            }

            const errorMap = {
                AUTH_INVALID_CREDENTIALS: "Email hoặc mật khẩu không chính xác.",
                AUTH_ACCOUNT_INACTIVE: "Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt.",
                VALIDATION_ERROR: "Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra lại.",
                INTERNAL_SERVER_ERROR: "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau."
            };

            const errorMessage = errorMap[errorCode] || getCustomerErrorMessage(
                error,
                'Không thể đăng nhập. Vui lòng thử lại sau.'
            );
            setErrorMsg(errorMessage);
        }
    };

    return (
        <main className="bg-zinc-950 text-white min-h-screen flex items-center justify-center py-16 px-6 relative overflow-hidden select-none">
            {errorMsg && (
                <CustomerNoticeModal
                    title="Không thể đăng nhập"
                    message={errorMsg}
                    variant="error"
                    onClose={() => setErrorMsg("")}
                />
            )}
            {/* Background ambient decorative shapes */}
            <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-orange/5 rounded-full filter blur-3xl pointer-events-none"></div>
            <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-brand-yellow/5 rounded-full filter blur-3xl pointer-events-none"></div>

            <article className="bg-zinc-900/80 border border-zinc-800 p-8 rounded-2xl w-full max-w-md shadow-2xl shadow-black/50 relative z-10 animate-fade-in">
                {/* Back to Home Button */}
                <button
                    onClick={() => navigate("/")}
                    className="flex items-center gap-2 text-zinc-400 hover:text-brand-orange transition-colors mb-6 text-sm font-semibold focus:outline-none"
                >
                    <ArrowLeft className="w-4 h-4" />
                    <span>Quay lại trang chủ</span>
                </button>

                <header className="text-center mb-8">
                    <h2 className="text-2xl md:text-3xl font-black tracking-wider uppercase text-white">ĐĂNG NHẬP</h2>
                    <p className="text-zinc-500 text-xs uppercase tracking-widest mt-1">Truy cập tài khoản LoraFilm</p>

                    {/* Success Message Banner */}
                    {successMessage && (
                        <div className="mt-4 bg-emerald-950/50 border border-emerald-800/80 rounded-xl p-3 flex items-center justify-center gap-2 text-emerald-200 text-xs leading-relaxed">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                width="16"
                                height="16"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                className="shrink-0 text-emerald-500"
                            >
                                <polyline points="20 6 9 17 4 12" />
                            </svg>
                            <span>{successMessage}</span>
                        </div>
                    )}
                </header>

                <form onSubmit={handleSubmit} className="space-y-5" noValidate>
                    <div className="space-y-1">
                        <label htmlFor="email-input" className="text-zinc-400 text-xs font-black uppercase tracking-wider block">
                            Địa chỉ Email
                        </label>
                        <div className="relative">
                            <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-zinc-500 pointer-events-none">
                                <Mail className="w-4 h-4" />
                            </span>
                            <input
                                id="email-input"
                                name="email"
                                type="email"
                                placeholder="example@lorafilm.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-colors placeholder:text-zinc-600 outline-none"
                                required
                                disabled={isSubmitting}
                            />
                        </div>
                    </div>

                    <div className="space-y-1">
                        <label htmlFor="password-input" className="text-zinc-400 text-xs font-black uppercase tracking-wider block">
                            Mật khẩu
                        </label>
                        <div className="relative">
                            <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-zinc-500 pointer-events-none">
                                <Lock className="w-4 h-4" />
                            </span>
                            <input
                                id="password-input"
                                name="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-colors placeholder:text-zinc-650 outline-none"
                                required
                                disabled={isSubmitting}
                            />
                            <button
                                type="button"
                                className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-zinc-500 hover:text-zinc-300 focus:outline-none"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label={showPassword ? "Hide password" : "Show password"}
                                disabled={isSubmitting}
                            >
                                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full bg-brand-orange hover:opacity-95 text-zinc-950 font-black py-3.5 rounded-xl text-sm transition-all shadow-lg shadow-amber-500/10 font-sans uppercase tracking-widest mt-6 block text-center cursor-pointer"
                    >
                        {isSubmitting ? (
                            <div className="flex items-center justify-center gap-2">
                                <Loader2 className="w-4 h-4 animate-spin text-zinc-950" />
                                <span>ĐANG KIỂM TRA...</span>
                            </div>
                        ) : (
                            <span>XÁC NHẬN ĐĂNG NHẬP</span>
                        )}
                    </button>
                </form>

                <footer className="text-center mt-6 text-xs text-zinc-400">
                    <span>Chưa có tài khoản LoraFilm? </span>
                    <Link to="/register" className="text-orange-400 hover:underline font-medium">
                        Đăng ký thành viên ngay
                    </Link>
                </footer>
            </article>
        </main>
    );
}

export default Login;
