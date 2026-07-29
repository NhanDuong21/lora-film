import { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { login } from "@/features/auth/services/authService";
import { useAuth } from "@/contexts/AuthContext";
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';
import { Mail, Lock, ArrowLeft, Loader2, Eye, EyeOff } from 'lucide-react';
import { getUserPermissions } from '@/utils/authStorage';
import { getAdminLandingPath, hasAdminAreaAccess } from '@/features/internal-staff/admin/permissionAccess';

function Login() {
    const { login: contextLogin } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const [email, setEmail] = useState(() => location.state?.email || "");
    const [password, setPassword] = useState("");
    const [rememberMe, setRememberMe] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [errorMsg, setErrorMsg] = useState(() => location.state?.error || "");
    const [successMessage, setSuccessMessage] = useState(() =>
        location.state?.message || (
            location.state?.verified ? "Xác thực tài khoản thành công! Vui lòng đăng nhập." : ""
        )
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
            const data = await login(email.trim(), password, rememberMe);
            setIsSubmitting(false);

            if (data?.success && data?.data) {
                await contextLogin({ ...data.data, rememberMe });
                setSuccessMessage("Đăng nhập thành công. Đang chuyển hướng...");

                // Navigate based on role-specific paths
                const role = data.data.role;
                setTimeout(() => {
                    if (hasAdminAreaAccess(role, getUserPermissions())) {
                        navigate(getAdminLandingPath(role, getUserPermissions()));
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
        <main className="bg-zinc-950 text-white min-h-screen flex items-center justify-center py-10 sm:py-16 px-4 sm:px-6 relative overflow-hidden select-none">
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

            <article className="bg-zinc-900/80 border border-zinc-800 p-5 sm:p-8 rounded-2xl w-full max-w-md shadow-2xl shadow-black/50 relative z-10 animate-fade-in">
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
                        <div className="flex items-center justify-between">
                            <label htmlFor="password-input" className="text-zinc-400 text-xs font-black uppercase tracking-wider block">
                                Mật khẩu
                            </label>
                            <Link to="/forgot-password" className="text-xs font-bold text-orange-400 hover:underline transition-colors focus:outline-none">
                                Quên mật khẩu?
                            </Link>
                        </div>
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

                    <label className="flex cursor-pointer items-center gap-2 text-xs font-semibold text-zinc-400">
                        <input
                            type="checkbox"
                            checked={rememberMe}
                            onChange={(event) => setRememberMe(event.target.checked)}
                            disabled={isSubmitting}
                            className="h-4 w-4 rounded border-zinc-700 bg-zinc-950 text-orange-500 focus:ring-orange-500"
                        />
                        Duy trì đăng nhập trên thiết bị này
                    </label>

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

                <div className="mt-6">
                    <div className="relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-zinc-800"></div>
                        </div>
                        <div className="relative flex justify-center text-xs">
                            <span className="bg-zinc-900 px-2 text-zinc-500 uppercase tracking-widest font-bold">Hoặc tiếp tục với</span>
                        </div>
                    </div>

                    <a
                        href={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}/oauth2/authorization/google`}
                        className="mt-4 flex w-full items-center justify-center gap-3 rounded-xl bg-white px-4 py-3.5 text-sm font-bold text-zinc-900 transition-all hover:bg-zinc-100 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2 focus:ring-offset-zinc-900"
                    >
                        <svg className="h-5 w-5" viewBox="0 0 24 24">
                            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                        </svg>
                        ĐĂNG NHẬP BẰNG GOOGLE
                    </a>
                </div>

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
