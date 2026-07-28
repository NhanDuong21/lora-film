import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "@/features/auth/services/authService";
import CustomerNoticeModal from "@/components/common/CustomerNoticeModal";
import { getCustomerErrorMessage } from "@/utils/customerErrorMessages";

function Register() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        fullName: "",
        email: "",
        phoneNumber: "",
        cccd: "",
        birthday: "",
        password: "",
        confirmPassword: ""
    });

    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [globalError, setGlobalError] = useState("");
    const [globalSuccess, setGlobalSuccess] = useState("");

    const registrationError = (errorCode, message) => {
        const error = new Error(message);
        error.errorCode = errorCode;
        return error;
    };



    useEffect(() => {
        document.title = "Đăng Ký Tài Khoản - LoraFilm";
    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        if (touched[name]) {
            const fieldError = validateField(name, value);
            setErrors(prev => ({
                ...prev,
                [name]: fieldError
            }));
        }
    };

    const handleBlur = (e) => {
        const { name, value } = e.target;
        setTouched(prev => ({ ...prev, [name]: true }));
        const fieldError = validateField(name, value);
        setErrors(prev => ({
            ...prev,
            [name]: fieldError
        }));
    };

    const validateField = (name, value) => {
        switch (name) {
            case "fullName": {
                if (!value.trim()) return "Họ và tên không được để trống.";
                if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(value)) return "Họ và tên không được chứa số hoặc ký tự đặc biệt.";
                const words = value.trim().split(/\s+/);
                if (words.length < 2) return "Họ và tên phải có ít nhất 2 từ.";
                return "";
            }

            case "email":
                if (!value.trim()) return "Email không được để trống.";
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return "Email không đúng định dạng.";
                return "";

            case "phoneNumber":
                if (!value.trim()) return "Số điện thoại không được để trống.";
                if (!/^0\d{9}$/.test(value)) return "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.";
                return "";

            case "cccd":
                if (!value.trim()) return "Số CCCD không được để trống.";
                if (!/^\d{12}$/.test(value)) return "Số CCCD phải gồm đúng 12 chữ số.";
                return "";

            case "birthday": {
                if (!value.trim()) return "Ngày sinh không được để trống.";
                const birthDate = new Date(value);
                const today = new Date();
                today.setHours(0, 0, 0, 0);
                if (birthDate > today) return "Ngày sinh không được ở tương lai.";
                let age = today.getFullYear() - birthDate.getFullYear();
                const m = today.getMonth() - birthDate.getMonth();
                if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
                    age--;
                }
                if (age < 13) return "Bạn phải từ 13 tuổi trở lên.";
                return "";
            }

            case "password":
                if (!value) return "Mật khẩu không được để trống.";
                if (value.length < 8) return "Mật khẩu phải dài ít nhất 8 ký tự.";
                if (!/[A-Z]/.test(value)) return "Mật khẩu phải chứa ít nhất một chữ hoa.";
                if (!/[a-z]/.test(value)) return "Mật khẩu phải chứa ít nhất một chữ thường.";
                if (!/[0-9]/.test(value)) return "Mật khẩu phải chứa ít nhất một chữ số.";
                if (!/[!@#$%^&*(),.?":{}|<>]/.test(value)) return "Mật khẩu phải chứa ít nhất một ký tự đặc biệt.";
                return "";

            case "confirmPassword":
                if (!value) return "Vui lòng xác nhận mật khẩu.";
                if (value !== formData.password) return "Mật khẩu xác nhận không trùng khớp.";
                return "";

            default:
                return "";
        }
    };

    const validateForm = () => {
        const newErrors = {};
        let isValid = true;

        Object.keys(formData).forEach(key => {
            const error = validateField(key, formData[key]);
            if (error) {
                newErrors[key] = error;
                isValid = false;
            }
        });

        setErrors(newErrors);

        return isValid;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setGlobalError("");
        setGlobalSuccess("");

        const allTouched = {};
        Object.keys(formData).forEach(key => {
            allTouched[key] = true;
        });
        setTouched(allTouched);

        if (validateForm()) {
            setIsSubmitting(true);
            try {
                const payload = {
                    fullName: formData.fullName,
                    email: formData.email,
                    phoneNumber: formData.phoneNumber,
                    cccd: formData.cccd,
                    birthday: formData.birthday,
                    password: formData.password
                };

                const res = await register(payload);

                if (res.success || res.message === "Registration initiated") {
                    const requestId = res.data?.requestId;
                    if (!requestId) {
                        throw registrationError(
                            "REGISTRATION_REQUEST_INVALID",
                            "Registration response did not contain a request ID"
                        );
                    }
                    setGlobalSuccess("Äang kiá»ƒm tra thÃ´ng tin Ä‘Äƒng kÃ½...");
                    await waitUntilOtpIsReady(requestId);
                    // Store email and purpose in sessionStorage for durability
                    sessionStorage.setItem("pending_otp_email", formData.email);
                    sessionStorage.setItem("pending_otp_purpose", "REGISTRATION");

                    setGlobalSuccess("Đăng ký thành công! Đang chuyển hướng sang xác thực OTP...");
                    setTimeout(() => {
                        navigate("/verify-otp", {
                            state: {
                                email: formData.email,
                                purpose: "REGISTRATION"
                            }
                        });
                    }, 1500);
                } else {
                    setGlobalError(getCustomerErrorMessage(
                        res,
                        'Đăng ký không thành công. Vui lòng thử lại.'
                    ));
                }
                setIsSubmitting(false);
            } catch (error) {
                setIsSubmitting(false);
                const errorCode = error?.errorCode || error?.code || error?.error;
                
                // Detailed field mappings
                if (errorCode === "AUTH_EMAIL_ALREADY_EXISTS") {
                    setErrors(prev => ({ ...prev, email: "Email này đã được sử dụng." }));
                    return;
                }
                if (errorCode === "PHONE_NUMBER_ALREADY_EXISTS") {
                    setErrors(prev => ({ ...prev, phoneNumber: "Số điện thoại này đã được sử dụng." }));
                    return;
                }
                if (errorCode === "CCCD_ALREADY_EXISTS") {
                    setErrors(prev => ({ ...prev, cccd: "Số CCCD này đã được sử dụng." }));
                    return;
                }
                if (errorCode === "USER_CCCD_INVALID") {
                    setErrors(prev => ({ ...prev, cccd: "Số CCCD không hợp lệ." }));
                    return;
                }
                if (errorCode === "USER_BIRTHDAY_CCCD_MISMATCH") {
                    setErrors(prev => ({ ...prev, birthday: "Ngày sinh không khớp với thông tin CCCD." }));
                    return;
                }

                if (errorCode === "PHONE_NUMBER_RESERVED") {
                    const retrySecs = error?.data?.retryAfterSeconds || error?.retryAfterSeconds || 60;
                    setGlobalError(`Số điện thoại này thuộc một đăng ký đang chờ xử lý. Vui lòng thử lại sau ${retrySecs} giây.`);
                    return;
                }
                if (errorCode === "CCCD_RESERVED") {
                    const retrySecs = error?.data?.retryAfterSeconds || error?.retryAfterSeconds || 60;
                    setGlobalError(`CCCD này thuộc một đăng ký đang chờ xử lý. Vui lòng thử lại sau ${retrySecs} giây.`);
                    return;
                }

                if (errorCode === "REGISTRATION_ALREADY_PENDING") {
                    setGlobalError("Tài khoản này đang chờ xác thực OTP. Đang chuyển hướng sang trang OTP...");
                    sessionStorage.setItem("pending_otp_email", formData.email);
                    sessionStorage.setItem("pending_otp_purpose", "REGISTRATION");
                    setTimeout(() => {
                        navigate("/verify-otp", {
                            state: {
                                email: formData.email,
                                purpose: "REGISTRATION"
                            }
                        });
                    }, 1500);
                    return;
                }

                if (errorCode === "VALIDATION_ERROR" && error.errors) {
                    const fieldErrors = {};
                    error.errors.forEach(err => {
                        fieldErrors[err.field] = getCustomerErrorMessage(
                            err,
                            'Giá trị này không hợp lệ.'
                        );
                    });
                    setErrors(fieldErrors);
                    setGlobalError("Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra lại các trường.");
                    return;
                }

                const errorMap = {
                    INTERNAL_SERVER_ERROR: "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau."
                };
                const errorMessage = errorMap[errorCode] || getCustomerErrorMessage(
                    error,
                    'Không thể đăng ký tài khoản. Vui lòng thử lại sau.'
                );
                setGlobalError(errorMessage);
            }
        }
    };

    const isSubmitDisabled = isSubmitting;

    return (
        <main className="bg-[#050506] text-white min-h-screen w-full flex items-center justify-center font-sans py-10 px-4 relative overflow-hidden select-none">
            {globalError && (
                <CustomerNoticeModal
                    title="Không thể đăng ký"
                    message={globalError}
                    variant="error"
                    onClose={() => setGlobalError("")}
                />
            )}
            {/* Decorative ambient background lights */}
            <div className="absolute top-[-200px] right-[-200px] w-[600px] h-[600px] bg-brand-orange/10 rounded-full filter blur-[80px] pointer-events-none z-0" />
            <div className="absolute bottom-[-200px] left-[-200px] w-[500px] h-[500px] bg-brand-orange/5 rounded-full filter blur-[80px] pointer-events-none z-0" />

            <article className="bg-[#121218]/85 border border-brand-orange/15 rounded-2xl w-full max-w-[600px] px-5 py-8 sm:px-8 sm:py-10 shadow-[0_15px_35px_rgba(0,0,0,0.6),0_0_25px_rgba(255,122,26,0.05)] backdrop-blur-md relative z-10 hover:border-brand-orange/35 hover:shadow-[0_20px_40px_rgba(0,0,0,0.7),0_0_35px_rgba(255,122,26,0.12)] transition-all duration-300">
                <header className="text-center mb-6 sm:mb-8">
                    <div className="flex items-center justify-center gap-3 mb-2">
                        <div className="flex items-center justify-center text-brand-orange drop-shadow-[0_0_8px_rgba(255,122,26,0.7)]">
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
                                <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
                                <path d="M13 5v2" />
                                <path d="M13 17v2" />
                                <path d="M13 11v2" />
                            </svg>
                        </div>
                        <h2 className="text-2xl sm:text-3xl font-black uppercase tracking-wider text-white">
                            Lora<span className="text-brand-orange">film</span>
                        </h2>
                    </div>
                    <p className="text-zinc-500 text-xs sm:text-sm leading-relaxed mt-1 max-w-sm mx-auto">
                        Đăng ký thành viên để nhận nhiều ưu đãi và đặt vé nhanh hơn
                    </p>
                </header>

                {globalSuccess && (
                    <div className="mb-6 bg-emerald-950/50 border border-emerald-800/80 rounded-xl p-3.5 flex items-center gap-2.5 text-emerald-200 text-xs leading-relaxed">
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
                        <span>{globalSuccess}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-5" noValidate>
                    {/* Họ và tên */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative sm:col-span-2">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="fullName">
                            Họ và tên
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="fullName"
                                name="fullName"
                                type="text"
                                placeholder="Nhập họ và tên của bạn"
                                value={formData.fullName}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.fullName && touched.fullName ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-orange">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>
                            </div>
                        </div>
                        {errors.fullName && touched.fullName && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.fullName}
                            </span>
                        )}
                    </div>

                    {/* Email */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative sm:col-span-2">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="email">
                            Địa chỉ Email
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="email"
                                name="email"
                                type="email"
                                placeholder="Nhập địa chỉ email"
                                value={formData.email}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.email && touched.email ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-orange">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
                            </div>
                        </div>
                        {errors.email && touched.email && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.email}
                            </span>
                        )}
                    </div>

                    {/* Số điện thoại */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="phoneNumber">
                            Số điện thoại
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="phoneNumber"
                                name="phoneNumber"
                                type="text"
                                placeholder="Nhập số điện thoại"
                                value={formData.phoneNumber}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.phoneNumber && touched.phoneNumber ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-orange">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" /></svg>
                            </div>
                        </div>
                        {errors.phoneNumber && touched.phoneNumber && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.phoneNumber}
                            </span>
                        )}
                    </div>

                    {/* Số CCCD */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="cccd">
                            Số CCCD
                        </label>
                        <div className="w-full flex gap-2">
                            <div className="w-full relative flex items-center group">
                                <input
                                    id="cccd"
                                    name="cccd"
                                    type="text"
                                    placeholder="Nhập 12 số CCCD"
                                    value={formData.cccd}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    maxLength={12}
                                    className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-4 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.cccd && touched.cccd ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                    disabled={isSubmitting}
                                />
                                <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-orange">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="12" x="3" y="6" rx="2" /><path d="M3 10h18" /><path d="M7 15h.01" /><path d="M11 15h.01" /></svg>
                                </div>
                            </div>
                        </div>
                        {errors.cccd && touched.cccd && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.cccd}
                            </span>
                        )}
                    </div>

                    {/* Ngày sinh */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative sm:col-span-2">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="birthday">
                            Ngày sinh
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="birthday"
                                name="birthday"
                                type="date"
                                value={formData.birthday}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl px-4 py-3 text-sm text-zinc-100 transition-all outline-none appearance-none ${errors.birthday && touched.birthday ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                disabled={isSubmitting}
                            />
                        </div>
                        {errors.birthday && touched.birthday && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.birthday}
                            </span>
                        )}
                    </div>

                    {/* Mật khẩu */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="password">
                            Mật khẩu
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="password"
                                name="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="Nhập mật khẩu"
                                value={formData.password}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.password && touched.password ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-orange">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
                            </div>
                            <button
                                type="button"
                                className="absolute right-4 text-zinc-500 hover:text-zinc-300 focus:outline-none flex items-center justify-center"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label="Toggle password visibility"
                            >
                                {showPassword ? (
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" /><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" /><path d="M6.61 6.61A13.52 13.52 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" /><line x1="2" x2="22" y1="2" y2="22" /></svg>
                                ) : (
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0z" /><circle cx="12" cy="12" r="3" /></svg>
                                )}
                            </button>
                        </div>
                        {errors.password && touched.password && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.password}
                            </span>
                        )}
                    </div>

                    {/* Xác nhận mật khẩu */}
                    <div className="flex flex-col items-start gap-1.5 w-full relative">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="confirmPassword">
                            Xác nhận mật khẩu
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="confirmPassword"
                                name="confirmPassword"
                                type={showConfirmPassword ? "text" : "password"}
                                placeholder="Nhập lại mật khẩu"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.confirmPassword && touched.confirmPassword ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-orange"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-orange">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
                            </div>
                            <button
                                type="button"
                                className="absolute right-4 text-zinc-500 hover:text-zinc-300 focus:outline-none flex items-center justify-center"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                aria-label="Toggle password visibility"
                            >
                                {showConfirmPassword ? (
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" /><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" /><path d="M6.61 6.61A13.52 13.52 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" /><line x1="2" x2="22" y1="2" y2="22" /></svg>
                                ) : (
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0z" /><circle cx="12" cy="12" r="3" /></svg>
                                )}
                            </button>
                        </div>
                        {errors.confirmPassword && touched.confirmPassword && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.confirmPassword}
                            </span>
                        )}
                    </div>

                    <div className="sm:col-span-2 flex flex-col gap-4 mt-4">
                        <button 
                            type="submit" 
                            className="w-full bg-brand-orange hover:bg-orange-600 disabled:opacity-40 disabled:hover:bg-brand-orange text-zinc-950 font-black py-3.5 rounded-xl text-sm transition-all shadow-lg shadow-amber-500/10 font-sans uppercase tracking-widest flex items-center justify-center gap-2 cursor-pointer" 
                            disabled={isSubmitDisabled}
                        >
                            <span>{isSubmitting ? "Đang đăng ký..." : "Đăng ký tài khoản"}</span>
                            {!isSubmitting && (
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14" /><path d="m12 5 7 7-7 7" /></svg>
                            )}
                        </button>

                        <footer className="text-center text-xs text-zinc-400 mt-2">
                            <span>Đã có tài khoản?</span>
                            <Link to="/login" className="text-orange-400 hover:underline font-semibold ml-1.5">
                                Đăng nhập ngay
                            </Link>
                        </footer>
                    </div>
                </form>
            </article>
        </main>
    );
}

export default Register;
