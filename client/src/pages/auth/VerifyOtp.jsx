import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { verifyOtp, resendOtp } from "../../services/authService";

function VerifyOtp() {
    const navigate = useNavigate();
    const location = useLocation();
    
    const email = location.state?.email || sessionStorage.getItem("pending_otp_email") || "";
    const purpose = location.state?.purpose || sessionStorage.getItem("pending_otp_purpose") || "REGISTRATION";
    
    const [inputEmail, setInputEmail] = useState("");
    const [otpCode, setOtpCode] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [countdown, setCountdown] = useState(60);
    const [isResending, setIsResending] = useState(false);

    const activeEmail = email || inputEmail;

    useEffect(() => {
        document.title = "Xác Thực OTP - LoraFilm";
    }, []);

    useEffect(() => {
        if (countdown <= 0) return;
        const timer = setInterval(() => {
            setCountdown(prev => prev - 1);
        }, 1000);
        return () => clearInterval(timer);
    }, [countdown]);

    const handleResend = async () => {
        setError("");
        setSuccess("");

        if (!activeEmail.trim()) {
            setError("Vui lòng nhập địa chỉ email để nhận OTP.");
            return;
        }

        setIsResending(true);
        try {
            const res = await resendOtp(activeEmail, purpose);
            setIsResending(false);
            if (res.success) {
                setSuccess("Mã OTP mới đã được gửi thành công. Vui lòng kiểm tra hộp thư.");
                setOtpCode(""); // Reset OTP input on successful resend
                
                // Set cooldown from backend response if available, default to 60s
                const resendCooldown = res.data?.resendAvailableIn || 60;
                setCountdown(resendCooldown);
            } else {
                setError(res.message || "Gửi lại mã thất bại. Vui lòng thử lại.");
            }
        } catch (err) {
            setIsResending(false);
            const errorCode = err?.errorCode || err?.code || err?.error;
            
            if (errorCode === "AUTH_ACCOUNT_ALREADY_VERIFIED") {
                setSuccess("Tài khoản này đã được xác thực trước đó. Đang chuyển hướng sang Đăng nhập...");
                setTimeout(() => {
                    navigate("/login", { state: { email: activeEmail } });
                }, 2000);
                return;
            }

            if (errorCode === "OTP_RATE_LIMIT") {
                const retryAfter = err?.data?.retryAfter || err?.retryAfter || 60;
                setError(`Gửi OTP quá nhanh. Vui lòng thử lại sau ${retryAfter} giây.`);
                setCountdown(retryAfter);
                return;
            }

            setError(err?.message || "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.");
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        if (!activeEmail.trim()) {
            setError("Vui lòng cung cấp email xác thực.");
            return;
        }

        if (!otpCode.trim()) {
            setError("Vui lòng nhập mã xác thực OTP.");
            return;
        }

        if (!/^\d{6}$/.test(otpCode)) {
            setError("Mã OTP phải gồm 6 chữ số.");
            return;
        }

        setIsSubmitting(true);
        try {
            const res = await verifyOtp(activeEmail, otpCode, purpose);
            setIsSubmitting(false);
            if (res.success) {
                // Clear pending session storage keys
                sessionStorage.removeItem("pending_otp_email");
                sessionStorage.removeItem("pending_otp_purpose");

                setSuccess("Xác thực tài khoản thành công! Đang chuyển hướng sang Đăng nhập...");
                setTimeout(() => {
                    navigate("/login", {
                        replace: true,
                        state: {
                            email: activeEmail,
                            verified: true
                        }
                    });
                }, 1500);
            } else {
                setError(res.message || "Xác thực thất bại. Vui lòng thử lại.");
            }
        } catch (err) {
            setIsSubmitting(false);
            const errorCode = err?.errorCode || err?.code || err?.error;
            const errorMap = {
                AUTH_INVALID_OTP: "Mã OTP không chính xác. Vui lòng kiểm tra lại.",
                AUTH_VERIFICATION_EXPIRED: "Mã OTP đã hết hạn. Vui lòng bấm gửi lại mã.",
                AUTH_ACCOUNT_NOT_FOUND: "Không tìm thấy tài khoản tương ứng.",
                INTERNAL_SERVER_ERROR: "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau."
            };
            setError(errorMap[errorCode] || err?.message || "Lỗi xác thực. Vui lòng thử lại.");
        }
    };

    return (
        <main className="bg-[#050506] text-white min-h-screen w-full flex items-center justify-center font-sans py-10 px-4 relative overflow-hidden select-none">
            <div className="absolute top-[-200px] right-[-200px] w-[600px] h-[600px] bg-brand-coral/10 rounded-full filter blur-[80px] pointer-events-none z-0" />
            <div className="absolute bottom-[-200px] left-[-200px] w-[500px] h-[500px] bg-brand-coral/5 rounded-full filter blur-[80px] pointer-events-none z-0" />

            <article className="bg-[#121218]/85 border border-brand-coral/15 rounded-2xl w-full max-w-[450px] px-5 py-8 sm:px-8 sm:py-10 shadow-[0_15px_35px_rgba(0,0,0,0.6),0_0_25px_rgba(255,122,26,0.05)] backdrop-blur-md relative z-10 hover:border-brand-coral/35 hover:shadow-[0_20px_40px_rgba(0,0,0,0.7),0_0_35px_rgba(255,122,26,0.12)] transition-all duration-300">
                <header className="text-center mb-6 sm:mb-8">
                    <div className="flex items-center justify-center gap-3 mb-2">
                        <div className="flex items-center justify-center text-[#ff7a1a] drop-shadow-[0_0_8px_rgba(255,122,26,0.7)]">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                width="32"
                                height="32"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                            </svg>
                        </div>
                        <h2 className="text-2xl font-black uppercase tracking-wider text-white">
                            Xác thực <span className="text-[#ff7a1a]">OTP</span>
                        </h2>
                    </div>
                    <p className="text-zinc-500 text-xs sm:text-sm leading-relaxed mt-1 max-w-sm mx-auto">
                        Vui lòng nhập mã xác thực OTP 6 chữ số để kích hoạt tài khoản của bạn.
                    </p>
                </header>

                {success && (
                    <div className="mb-6 bg-emerald-950/50 border border-emerald-800/80 rounded-xl p-3.5 flex items-center gap-2.5 text-emerald-200 text-xs leading-relaxed animate-fadeIn">
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
                        <span>{success}</span>
                    </div>
                )}

                {error && (
                    <div className="mb-6 bg-red-950/50 border border-red-800/80 rounded-xl p-4 flex items-start gap-3 text-red-200 text-xs leading-relaxed animate-fadeIn">
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
                        <span>{error}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
                    {email ? (
                        <div className="flex flex-col items-start gap-1 w-full text-xs">
                            <span className="text-zinc-500 font-bold uppercase tracking-wider">Email xác thực:</span>
                            <span className="text-zinc-300 font-black tracking-wide bg-zinc-950/60 border border-zinc-900 px-4 py-3 rounded-xl w-full select-all">{email}</span>
                        </div>
                    ) : (
                        <div className="flex flex-col items-start gap-1.5 w-full relative">
                            <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="inputEmail">
                                Địa chỉ Email
                            </label>
                            <input
                                id="inputEmail"
                                name="inputEmail"
                                type="email"
                                placeholder="example@gmail.com"
                                value={inputEmail}
                                onChange={(e) => setInputEmail(e.target.value)}
                                className="w-full bg-zinc-950 border border-zinc-800 focus:bg-zinc-900/40 rounded-xl px-4 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none focus:border-[#ff7a1a]"
                                disabled={isSubmitting}
                            />
                        </div>
                    )}

                    <div className="flex flex-col items-start gap-1.5 w-full relative mt-2">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="otp">
                            Mã xác thực
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="otp"
                                name="otp"
                                type="text"
                                placeholder="Nhập 6 số OTP"
                                value={otpCode}
                                onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                className="w-full bg-zinc-950 border border-zinc-800 focus:bg-zinc-900/40 rounded-xl px-4 py-3 text-center text-lg tracking-[0.5em] font-black text-zinc-100 transition-all placeholder:text-zinc-600 outline-none focus:border-[#ff7a1a]"
                                disabled={isSubmitting}
                                maxLength={6}
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="w-full bg-[#ff7a1a] hover:bg-orange-500 disabled:opacity-40 disabled:hover:bg-[#ff7a1a] text-zinc-950 font-black py-3.5 rounded-xl text-sm transition-all shadow-lg shadow-amber-500/10 font-sans uppercase tracking-widest flex items-center justify-center gap-2 cursor-pointer mt-4"
                        disabled={isSubmitting}
                    >
                        <span>{isSubmitting ? "Đang xác thực..." : "Xác nhận mã"}</span>
                    </button>

                    <div className="text-center mt-4">
                        {countdown > 0 ? (
                            <button
                                type="button"
                                disabled
                                className="text-zinc-500 font-bold text-xs uppercase tracking-widest bg-transparent border-none cursor-not-allowed select-none animate-pulse"
                            >
                                Gửi lại mã sau ({countdown}s)
                            </button>
                        ) : (
                            <button
                                type="button"
                                onClick={handleResend}
                                disabled={isResending}
                                className="text-[#ff7a1a] hover:text-orange-400 font-black text-xs uppercase tracking-widest bg-transparent border-none cursor-pointer transition-colors focus:outline-none"
                            >
                                {isResending ? "Đang gửi..." : "Gửi lại mã"}
                            </button>
                        )}
                    </div>
                </form>
            </article>
        </main>
    );
}

export default VerifyOtp;
