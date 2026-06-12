import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./RegisterPage.css";

function RegisterPage() {
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
    const dateInputRef = useRef(null);
    useEffect(() => {
        document.title = "Create Account - CinePass Ticket Booking";
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
                if (typeof dateInputRef.current.showPicker === 'function') {
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
                if (!value.trim()) return "Full name is required";
                if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(value)) return "Full name should not contain numbers or special characters";
                const words = value.trim().split(/\s+/);
                if (words.length < 2) return "Full name should contain at least 2 words";
                return "";

            case "email":
                if (!value.trim()) return "Email address is required";
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return "Please enter a valid email format (e.g., user@example.com)";
                return "";

            case "citizenId":
                if (!value.trim()) return "Citizen ID is required";
                if (!/^\d+$/.test(value)) return "Citizen ID must contain only numbers";
                if (value.length !== 12) return "Citizen ID must be exactly 12 digits";
                return "";

            case "gender":
                if (!value) return "Gender is required";
                return "";

            case "dob":
                if (!value.trim()) return "Date of birth is required";
                if (value.length < 10) return "Please enter complete date (DD/MM/YYYY)";

                const dobRegex = /^(\d{2})\/(\d{2})\/(\d{4})$/;
                if (!dobRegex.test(value)) return "Invalid date format";

                const [dayStr, monthStr, yearStr] = value.split("/");
                const day = parseInt(dayStr, 10);
                const month = parseInt(monthStr, 10);
                const year = parseInt(yearStr, 10);

                if (month < 1 || month > 12) return "Month must be between 01 and 12";

                const birthDate = new Date(year, month - 1, day);

                if (birthDate.getFullYear() !== year || birthDate.getMonth() !== month - 1 || birthDate.getDate() !== day) {
                    return "Please enter a valid calendar date";
                }

                const today = new Date();
                today.setHours(0, 0, 0, 0);

                if (birthDate > today) return "Date of birth cannot be in the future";

                let age = today.getFullYear() - birthDate.getFullYear();
                const m = today.getMonth() - birthDate.getMonth();
                if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
                    age--;
                }

                if (age < 13) return "You must be at least 13 years old";
                return "";

            case "password":
                if (!value) return "Password is required";
                if (value.length < 8) return "Password must be at least 8 characters long";
                if (!/[A-Z]/.test(value)) return "Must contain at least one uppercase letter";
                if (!/[a-z]/.test(value)) return "Must contain at least one lowercase letter";
                if (!/[0-9]/.test(value)) return "Must contain at least one number";
                if (!/[!@#$%^&*(),.?":{}|<>]/.test(value)) return "Must contain at least one special character";
                return "";

            case "confirmPassword":
                if (!value) return "Confirm password is required";
                if (value !== formData.password) return "Passwords do not match";
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

    const handleSubmit = (e) => {
        e.preventDefault();

        const allTouched = {};
        Object.keys(formData).forEach(key => {
            allTouched[key] = true;
        });
        setTouched(allTouched);

        if (validateForm()) {
            setIsSubmitting(true);

            setTimeout(() => {
                alert("Register successfully!");
                setIsSubmitting(false);
                navigate("/login");
            }, 500);
        }
    };

    return (
        <main className="register-page-container">
            <div className="light-leak-top" />
            <div className="light-leak-bottom" />

            <article className="register-card">
                <header className="register-header">
                    <div className="register-brand">
                        <div className="cinema-logo">
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
                        <h2>Lora<span>film</span></h2>
                    </div>
                    <p className="register-subtitle">Join us to get exclusive movie deals and faster booking</p>
                </header>

                <form onSubmit={handleSubmit} className="register-form" noValidate>
                    <div className="form-group full-width">
                        <label className="form-label" htmlFor="fullName">Full Name</label>
                        <div className="input-wrapper">
                            <input
                                id="fullName"
                                name="fullName"
                                type="text"
                                placeholder="Enter your full name"
                                value={formData.fullName}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`form-input ${errors.fullName && touched.fullName ? "error-border" : ""}`}
                            />
                            <div className="input-left-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>
                            </div>
                        </div>
                        {errors.fullName && touched.fullName && <span className="error-container">{errors.fullName}</span>}
                    </div>
                    <div className="form-group full-width">
                        <label className="form-label" htmlFor="email">Email Address</label>
                        <div className="input-wrapper">
                            <input
                                id="email"
                                name="email"
                                type="email"
                                placeholder="Enter your email"
                                value={formData.email}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`form-input ${errors.email && touched.email ? "error-border" : ""}`}
                            />
                            <div className="input-left-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
                            </div>
                        </div>
                        {errors.email && touched.email && <span className="error-container">{errors.email}</span>}
                    </div>
                    <div className="form-group full-width">
                        <label className="form-label" htmlFor="citizenId">Citizen ID Number</label>
                        <div className="input-wrapper">
                            <input
                                id="citizenId"
                                name="citizenId"
                                type="text"
                                placeholder="Enter your 12-digit citizen ID"
                                value={formData.citizenId}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                maxLength={12}
                                className={`form-input ${errors.citizenId && touched.citizenId ? "error-border" : ""}`}
                            />
                            <div className="input-left-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="12" x="3" y="6" rx="2" /><path d="M3 10h18" /><path d="M7 15h.01" /><path d="M11 15h.01" /></svg>
                            </div>
                        </div>
                        {errors.citizenId && touched.citizenId && <span className="error-container">{errors.citizenId}</span>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="gender">Gender</label>
                        <div className="input-wrapper">
                            <select
                                id="gender"
                                name="gender"
                                value={formData.gender}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`form-input ${errors.gender && touched.gender ? "error-border" : ""}`}
                            >
                                <option value="" disabled>Select Gender</option>
                                <option value="male">Male</option>
                                <option value="female">Female</option>
                                <option value="other">Other</option>
                            </select>
                            <div className="input-left-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" /><path d="M12 15l0 5" /><path d="M10 18l4 0" /><path d="M19 5l-4 4" /><path d="M19 9l-4 -4" /><path d="M16.5 7.5l3.5 -3.5" /></svg>
                            </div>
                            <div className="select-arrow">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6" /></svg>
                            </div>
                        </div>
                        {errors.gender && touched.gender && <span className="error-container">{errors.gender}</span>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="dob">Date of Birth</label>
                        <div className="input-wrapper">
                            <input
                                id="dob"
                                name="dob"
                                type="text"
                                placeholder="DD/MM/YYYY"
                                value={formData.dob}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                maxLength={10}
                                className={`form-input ${errors.dob && touched.dob ? "error-border" : ""}`}
                            />
                            <div
                                className="input-left-icon clickable-icon"
                                onClick={handleCalendarClick}
                                title="Open calendar"
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
                        {errors.dob && touched.dob && <span className="error-container">{errors.dob}</span>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password">Password</label>
                        <div className="input-wrapper">
                            <input
                                id="password"
                                name="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="Enter your password"
                                value={formData.password}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`form-input ${errors.password && touched.password ? "error-border" : ""}`}
                            />
                            <div className="input-left-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
                            </div>
                            <button
                                type="button"
                                className="password-toggle-btn"
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
                        {errors.password && touched.password && <span className="error-container">{errors.password}</span>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="confirmPassword">Confirm Password</label>
                        <div className="input-wrapper">
                            <input
                                id="confirmPassword"
                                name="confirmPassword"
                                type={showConfirmPassword ? "text" : "password"}
                                placeholder="Confirm your password"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                className={`form-input ${errors.confirmPassword && touched.confirmPassword ? "error-border" : ""}`}
                            />
                            <div className="input-left-icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
                            </div>
                            <button
                                type="button"
                                className="password-toggle-btn"
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
                        {errors.confirmPassword && touched.confirmPassword && <span className="error-container">{errors.confirmPassword}</span>}
                    </div>

                    <div className="submit-container">
                        <button type="submit" className="register-btn" disabled={isSubmitting}>
                            <span>{isSubmitting ? "Registering..." : "Register Account"}</span>
                            {!isSubmitting && (
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14" /><path d="m12 5 7 7-7 7" /></svg>
                            )}
                        </button>

                        <footer className="register-footer">
                            <span>Already have an account?</span>
                            <Link to="/login" className="login-link">
                                Sign In
                            </Link>
                        </footer>
                    </div>
                </form>
            </article>
        </main>
    );
}

export default RegisterPage;
