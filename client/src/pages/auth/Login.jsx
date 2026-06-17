import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../../services/authService";
import { setPendingAccountId } from "../../utils/authStorage";
import { useAuth } from "../../contexts/AuthContext";

function Login() {
    const { login: contextLogin } = useAuth();
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
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
                contextLogin(data.data);
                setSuccessMessage("Đăng nhập thành công.");
                navigate("/");
            } else {
                setErrorMsg("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.");
            }
        } catch (error) {
            setIsSubmitting(false);

            const errorCode = error?.errorCode || error?.code || error?.error;
            
            if (errorCode === "AUTH_ACCOUNT_NOT_VERIFIED") {
                const accId = error?.data?.accountId || error?.accountId || error?.data?.id;
                if (accId) {
                    setPendingAccountId(accId);
                }
                setErrorMsg("Tài khoản chưa được xác thực. Đang chuyển hướng sang trang xác thực OTP...");
                setTimeout(() => {
                    navigate("/verify-otp");
                }, 1500);
                return;
            }

            const errorMap = {
                AUTH_INVALID_CREDENTIALS: "Email hoặc mật khẩu không chính xác.",
                AUTH_ACCOUNT_INACTIVE: "Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt.",
                VALIDATION_ERROR: "Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra lại.",
                INTERNAL_SERVER_ERROR: "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau."
            };

            const errorMessage = errorMap[errorCode] || error?.message || "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.";
            setErrorMsg(errorMessage);
        }
    };

    return (
        <main className="bg-zinc-950 text-white min-h-screen flex items-center justify-center py-16 px-6 relative overflow-hidden select-none">
            {/* Background ambient decorative shapes */}
            <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-coral/5 rounded-full filter blur-3xl pointer-events-none"></div>
            <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-brand-yellow/5 rounded-full filter blur-3xl pointer-events-none"></div>

            <article className="bg-zinc-900/80 border border-zinc-800 p-8 rounded-2xl w-full max-w-md shadow-2xl shadow-black/50 relative z-10">
                {/* Back to Home Button */}
                <button
                    onClick={() => navigate("/")}
                    className="flex items-center gap-2 text-zinc-400 hover:text-brand-coral transition-colors mb-6 text-sm font-semibold focus:outline-none"
                >
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
                    >
                        <line x1="19" y1="12" x2="5" y2="12" />
                        <polyline points="12 19 5 12 12 5" />
                    </svg>
                    <span>Quay lại trang chủ</span>
                </button>

                <header className="text-center mb-8">
                    <div className="flex justify-center items-center gap-2 mb-2">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="28"
                            height="28"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="#ff7a1a"
                            strokeWidth="2.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
                            <path d="M13 5v2" />
                            <path d="M13 17v2" />
                            <path d="M13 11v2" />
                        </svg>
                        <h2 className="text-2xl font-black tracking-wider uppercase text-white">
                            Lora<span className="text-brand-coral">film</span>
                        </h2>
                    </div>
                    <p className="text-zinc-500 text-xs uppercase tracking-widest mt-1">
                        Truy cập tài khoản LoraFilm
                    </p>

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

                {/* Error Notification Banner */}
                {errorMsg && (
                    <div className="mb-6 bg-red-950/50 border border-red-800/80 rounded-xl p-4 flex items-start gap-3 text-red-200 text-xs leading-relaxed">
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
                            className="w-4 h-4 shrink-0 text-red-500 mt-0.5"
                        >
                            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                            <line x1="12" y1="9" x2="12" y2="13" />
                            <line x1="12" y1="17" x2="12.01" y2="17" />
                        </svg>
                        <span>{errorMsg}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-5" noValidate>
                    <div className="space-y-1">
                        <label htmlFor="email-input" className="text-zinc-400 text-xs font-black uppercase tracking-wider block">
                            Địa chỉ Email
                        </label>
                        <div className="relative">
                            <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-zinc-500 pointer-events-none">
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    width="16"
                                    height="16"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                >
                                    <rect width="20" height="16" x="2" y="4" rx="2" />
                                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                                </svg>
                            </span>
                            <input
                                id="email-input"
                                name="email"
                                type="email"
                                placeholder="example@lorafilm.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-coral rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-colors placeholder:text-zinc-600 outline-none"
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
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    width="16"
                                    height="16"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                >
                                    <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                </svg>
                            </span>
                            <input
                                id="password-input"
                                name="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-coral rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-colors placeholder:text-zinc-600 outline-none"
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
                                {showPassword ? (
                                    <svg
                                        xmlns="http://www.w3.org/2000/svg"
                                        width="16"
                                        height="16"
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                    >
                                        <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
                                        <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
                                        <path d="M6.61 6.61A13.52 13.52 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
                                        <line x1="2" x2="22" y1="2" y2="22" />
                                    </svg>
                                ) : (
                                    <svg
                                        xmlns="http://www.w3.org/2000/svg"
                                        width="16"
                                        height="16"
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                    >
                                        <path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0z" />
                                        <circle cx="12" cy="12" r="3" />
                                    </svg>
                                )}
                            </button>
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full bg-gradient-to-r from-orange-400 to-amber-500 hover:opacity-95 text-zinc-950 font-black py-3.5 rounded-xl text-sm transition-all shadow-lg shadow-amber-500/10 font-sans uppercase tracking-widest mt-6 block text-center cursor-pointer"
                    >
                        {isSubmitting ? (
                            <div className="flex items-center justify-center gap-2">
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
                                    className="animate-spin text-zinc-950"
                                >
                                    <line x1="12" y1="2" x2="12" y2="6" />
                                    <line x1="12" y1="18" x2="12" y2="22" />
                                    <line x1="4.93" y1="4.93" x2="7.76" y2="7.76" />
                                    <line x1="16.24" y1="16.24" x2="19.07" y2="19.07" />
                                    <line x1="2" y1="12" x2="6" y2="12" />
                                    <line x1="18" y1="12" x2="22" y2="12" />
                                    <line x1="4.93" y1="19.07" x2="7.76" y2="16.24" />
                                    <line x1="16.24" y1="7.76" x2="19.07" y2="4.93" />
                                </svg>
                                <span>Đang kiểm tra...</span>
                            </div>
                        ) : (
                            <span>Xác nhận đăng nhập</span>
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
