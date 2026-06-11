import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../services/authService";

function Register() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        fullName: "",
        email: "",
        citizenId: "",
        gender: "",
        dob: "",
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
    const dateInputRef = useRef(null);

    useEffect(() => {
        document.title = "Đăng Ký Tài Khoản - LoraFilm";
    }, []);

    const handleChange = (e) => {
        let { name, value } = e.target;

        if (name === "dob") {
            const digits = value.replace(/\D/g, "");
            const limitedDigits = digits.substring(0, 8);

            let formattedValue = "";
            if (limitedDigits.length > 0) {
                formattedValue += limitedDigits.substring(0, 2);
                if (limitedDigits.length > 2) {
                    formattedValue += "/" + limitedDigits.substring(2, 4);
                    if (limitedDigits.length > 4) {
                        formattedValue += "/" + limitedDigits.substring(4, 8);
                    }
                }
            }
            value = formattedValue;
        }

        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        if (touched[name] || name === "dob") {
            const fieldError = validateField(name, value);
            setErrors(prev => ({
                ...prev,
                [name]: fieldError
            }));
        }
    };

    const handleCalendarClick = () => {
        if (dateInputRef.current) {
            try {
                if (typeof dateInputRef.current.showPicker === "function") {
                    dateInputRef.current.showPicker();
                } else {
                    dateInputRef.current.click();
                }
            } catch (err) {
                dateInputRef.current.click();
            }
        }
    };

    const handleDateChange = (e) => {
        const dateValue = e.target.value; 
        if (!dateValue) return;

        const [y, m, d] = dateValue.split("-");
        const formattedDate = `${d}/${m}/${y}`;

        setFormData(prev => ({
            ...prev,
            dob: formattedDate
        }));

        setTouched(prev => ({ ...prev, dob: true }));
        const fieldError = validateField("dob", formattedDate);
        setErrors(prev => ({
            ...prev,
            dob: fieldError
        }));
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
            case "fullName":
                if (!value.trim()) return "Họ và tên không được để trống";
                if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(value)) return "Họ và tên không được chứa số hoặc ký tự đặc biệt";
                const words = value.trim().split(/\s+/);
                if (words.length < 2) return "Họ và tên phải có ít nhất 2 từ";
                return "";

            case "email":
                if (!value.trim()) return "Email không được để trống";
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return "Email không đúng định dạng (ví dụ: user@example.com)";
                return "";

            case "citizenId":
                if (!value.trim()) return "Số CCCD không được để trống";
                if (!/^\d+$/.test(value)) return "Số CCCD chỉ được phép chứa số";
                if (value.length !== 12) return "Số CCCD phải có đúng 12 chữ số";
                return "";

            case "gender":
                if (!value) return "Vui lòng chọn giới tính";
                return "";

            case "dob":
                if (!value.trim()) return "Ngày sinh không được để trống";
                if (value.length < 10) return "Vui lòng nhập đầy đủ ngày sinh (DD/MM/YYYY)";

                const dobRegex = /^(\d{2})\/(\d{2})\/(\d{4})$/;
                if (!dobRegex.test(value)) return "Định dạng ngày sinh không hợp lệ";

                const [dayStr, monthStr, yearStr] = value.split("/");
                const day = parseInt(dayStr, 10);
                const month = parseInt(monthStr, 10);
                const year = parseInt(yearStr, 10);

                if (month < 1 || month > 12) return "Tháng sinh phải từ 01 đến 12";

                const birthDate = new Date(year, month - 1, day);

                if (birthDate.getFullYear() !== year || birthDate.getMonth() !== month - 1 || birthDate.getDate() !== day) {
                    return "Vui lòng nhập một ngày hợp lệ trên lịch";
                }

                const today = new Date();
                today.setHours(0, 0, 0, 0);

                if (birthDate > today) return "Ngày sinh không được ở tương lai";

                let age = today.getFullYear() - birthDate.getFullYear();
                const m = today.getMonth() - birthDate.getMonth();
                if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
                    age--;
                }

                if (age < 13) return "Bạn phải từ 13 tuổi trở lên";
                return "";

            case "password":
                if (!value) return "Mật khẩu không được để trống";
                if (value.length < 8) return "Mật khẩu phải dài ít nhất 8 ký tự";
                if (!/[A-Z]/.test(value)) return "Mật khẩu phải chứa ít nhất một chữ hoa";
                if (!/[a-z]/.test(value)) return "Mật khẩu phải chứa ít nhất một chữ thường";
                if (!/[0-9]/.test(value)) return "Mật khẩu phải chứa ít nhất một chữ số";
                if (!/[!@#$%^&*(),.?":{}|<>]/.test(value)) return "Mật khẩu phải chứa ít nhất một ký tự đặc biệt";
                return "";

            case "confirmPassword":
                if (!value) return "Vui lòng nhập lại mật khẩu để xác nhận";
                if (value !== formData.password) return "Mật khẩu xác nhận không trùng khớp";
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
                // Convert dob from DD/MM/YYYY to yyyy-MM-dd
                const [dayStr, monthStr, yearStr] = formData.dob.split("/");
                const dobIso = `${yearStr}-${monthStr}-${dayStr}`;

                const payload = {
                    email: formData.email,
                    password: formData.password,
                    confirmPassword: formData.confirmPassword,
                    fullName: formData.fullName,
                    citizenId: formData.citizenId,
                    gender: formData.gender,
                    dob: dobIso
                };

                await register(payload);
                setIsSubmitting(false);

                setGlobalSuccess("Tài khoản đã được tạo thành công! Đang chuyển hướng sang Đăng nhập...");
                
                // Clear state variables
                setFormData({
                    fullName: "",
                    email: "",
                    citizenId: "",
                    gender: "",
                    dob: "",
                    password: "",
                    confirmPassword: ""
                });
                setTouched({});
                setErrors({});

                setTimeout(() => {
                    navigate("/login");
                }, 2000);

            } catch (error) {
                setIsSubmitting(false);
                const errorMessage = error?.message || error?.error || "Đăng ký không thành công. Vui lòng kiểm tra lại thông tin.";
                setGlobalError(errorMessage);
            }
        }
    };

    return (
        <main className="bg-[#050506] text-white min-h-screen w-full flex items-center justify-center font-sans py-10 px-4 relative overflow-hidden select-none">
            {/* Decorative ambient background lights */}
            <div className="absolute top-[-200px] right-[-200px] w-[600px] h-[600px] bg-brand-coral/10 rounded-full filter blur-[80px] pointer-events-none z-0" />
            <div className="absolute bottom-[-200px] left-[-200px] w-[500px] h-[500px] bg-brand-coral/5 rounded-full filter blur-[80px] pointer-events-none z-0" />

            <article className="bg-[#121218]/85 border border-brand-coral/15 rounded-2xl w-full max-w-[600px] px-5 py-8 sm:px-8 sm:py-10 shadow-[0_15px_35px_rgba(0,0,0,0.6),0_0_25px_rgba(255,122,26,0.05)] backdrop-blur-md relative z-10 hover:border-brand-coral/35 hover:shadow-[0_20px_40px_rgba(0,0,0,0.7),0_0_35px_rgba(255,122,26,0.12)] transition-all duration-300">
                <header className="text-center mb-6 sm:mb-8">
                    <div className="flex items-center justify-center gap-3 mb-2">
                        <div className="flex items-center justify-center text-brand-coral drop-shadow-[0_0_8px_rgba(255,122,26,0.7)]">
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
                            Lora<span className="text-brand-coral">film</span>
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

                {globalError && (
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
                        <span>{globalError}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-5" noValidate>
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
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.fullName && touched.fullName ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral">
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
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.email && touched.email ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral">
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

                    <div className="flex flex-col items-start gap-1.5 w-full relative sm:col-span-2">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="citizenId">
                            Số CCCD
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="citizenId"
                                name="citizenId"
                                type="text"
                                placeholder="Nhập 12 số căn cước công dân"
                                value={formData.citizenId}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                maxLength={12}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.citizenId && touched.citizenId ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="12" x="3" y="6" rx="2" /><path d="M3 10h18" /><path d="M7 15h.01" /><path d="M11 15h.01" /></svg>
                            </div>
                        </div>
                        {errors.citizenId && touched.citizenId && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.citizenId}
                            </span>
                        )}
                    </div>

                    <div className="flex flex-col items-start gap-1.5 w-full relative">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="gender">
                            Giới tính
                        </label>
                        <div className="w-full relative flex items-center group">
                            <select
                                id="gender"
                                name="gender"
                                value={formData.gender}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all outline-none appearance-none cursor-pointer ${errors.gender && touched.gender ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            >
                                <option value="" disabled>Chọn giới tính</option>
                                <option value="male">Nam</option>
                                <option value="female">Nữ</option>
                                <option value="other">Khác</option>
                            </select>
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" /><path d="M12 15l0 5" /><path d="M10 18l4 0" /><path d="M19 5l-4 4" /><path d="M19 9l-4 -4" /><path d="M16.5 7.5l3.5 -3.5" /></svg>
                            </div>
                            <div className="absolute right-4 pointer-events-none text-zinc-500">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6" /></svg>
                            </div>
                        </div>
                        {errors.gender && touched.gender && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.gender}
                            </span>
                        )}
                    </div>

                    <div className="flex flex-col items-start gap-1.5 w-full relative">
                        <label className="text-[10px] sm:text-xs font-black text-zinc-400 uppercase tracking-widest" htmlFor="dob">
                            Ngày sinh
                        </label>
                        <div className="w-full relative flex items-center group">
                            <input
                                id="dob"
                                name="dob"
                                type="text"
                                placeholder="DD/MM/YYYY"
                                value={formData.dob}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                maxLength={10}
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.dob && touched.dob ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            />
                            <div 
                                className="absolute left-4 text-zinc-500 hover:text-brand-coral cursor-pointer flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral" 
                                onClick={handleCalendarClick}
                                title="Mở lịch chọn"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2" /><line x1="16" x2="16" y1="2" y2="6" /><line x1="8" x2="8" y1="2" y2="6" /><line x1="3" x2="21" y1="10" y2="10" /></svg>
                            </div>
                            <input
                                type="date"
                                ref={dateInputRef}
                                onChange={handleDateChange}
                                style={{ position: "absolute", opacity: 0, width: 0, height: 0, pointerEvents: "none" }}
                                max={new Date().toISOString().split("T")[0]}
                            />
                        </div>
                        {errors.dob && touched.dob && (
                            <span className="text-red-500 text-[11px] font-medium mt-1 flex items-center gap-1.5">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><circle cx="12" cy="12" r="10" /><line x1="12" x2="12" y1="8" y2="12" /><line x1="12" x2="12.01" y1="16" y2="16" /></svg>
                                {errors.dob}
                            </span>
                        )}
                    </div>

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
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.password && touched.password ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral">
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
                                className={`w-full bg-zinc-950 border focus:bg-zinc-900/40 rounded-xl pl-11 pr-10 py-3 text-sm text-zinc-100 transition-all placeholder:text-zinc-600 outline-none ${errors.confirmPassword && touched.confirmPassword ? "border-red-500/80 focus:border-red-500/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" : "border-zinc-800 focus:border-brand-coral"}`}
                                disabled={isSubmitting}
                            />
                            <div className="absolute left-4 text-zinc-500 pointer-events-none flex items-center justify-center transition-colors duration-300 group-focus-within:text-brand-coral">
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
                            className="w-full bg-gradient-to-r from-orange-400 to-amber-500 hover:opacity-95 disabled:opacity-50 text-zinc-950 font-black py-3.5 rounded-xl text-sm transition-all shadow-lg shadow-amber-500/10 font-sans uppercase tracking-widest flex items-center justify-center gap-2 cursor-pointer" 
                            disabled={isSubmitting}
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
