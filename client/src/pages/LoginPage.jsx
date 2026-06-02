import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import "./LoginPage.css";

function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [errors, setErrors] = useState({ email: "", password: "" });
    const [showPassword, setShowPassword] = useState(false);

    // Set page title for SEO/UX best practices
    useEffect(() => {
        document.title = "Sign In - CinePass Ticket Booking";
    }, []);

    const validateForm = () => {
        let valid = true;
        const newErrors = { email: "", password: "" };

        if (!email.trim()) {
            newErrors.email = "Email address is required";
            valid = false;
        }

        if (!password.trim()) {
            newErrors.password = "Password is required";
            valid = false;
        }

        setErrors(newErrors);
        return valid;
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        
        if (validateForm()) {
            // Frontend validation passed. Real auth logic will be wired here later.
            console.log("Form validated successfully:", { email, password });
        }
    };

    return (
        <main className="login-page-container">
            {/* Background glowing gradients for extra premium feeling */}
            <div className="light-leak-top" />
            <div className="light-leak-bottom" />

            <article className="login-card">
                <header className="login-header">
                    <div className="login-brand">
                        <div className="cinema-logo">
                            <svg 
                                xmlns="http://www.w3.org/2000/svg" 
                                width="30" 
                                height="30" 
                                viewBox="0 0 24 24" 
                                fill="none" 
                                stroke="currentColor" 
                                strokeWidth="2.5" 
                                strokeLinecap="round" 
                                strokeLinejoin="round"
                            >
                                <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/>
                                <path d="M13 5v2"/>
                                <path d="M13 17v2"/>
                                <path d="M13 11v2"/>
                            </svg>
                        </div>
                        <h2>Lora<span>film</span></h2>
                    </div>
                    <p className="login-subtitle">Book tickets to your favorite movies in seconds</p>
                </header>

                <form onSubmit={handleSubmit} className="login-form" noValidate>
                    {/* Email Input Group */}
                    <div className="form-group">
                        <label htmlFor="email-input" className="form-label">
                            Email Address
                        </label>
                        <div className="input-wrapper">
                            <input
                                id="email-input"
                                name="email"
                                type="email"
                                placeholder="Enter your email"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value);
                                    if (errors.email) setErrors(prev => ({ ...prev, email: "" }));
                                }}
                                className={`form-input ${errors.email ? "error-border" : ""}`}
                                required
                            />
                            <div className="input-left-icon">
                                <svg 
                                    xmlns="http://www.w3.org/2000/svg" 
                                    width="18" 
                                    height="18" 
                                    viewBox="0 0 24 24" 
                                    fill="none" 
                                    stroke="currentColor" 
                                    strokeWidth="2" 
                                    strokeLinecap="round" 
                                    strokeLinejoin="round"
                                >
                                    <rect width="20" height="16" x="2" y="4" rx="2"/>
                                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
                                </svg>
                            </div>
                        </div>
                        {errors.email && (
                            <span className="error-container" id="email-error">
                                <svg 
                                    xmlns="http://www.w3.org/2000/svg" 
                                    width="14" 
                                    height="14" 
                                    viewBox="0 0 24 24" 
                                    fill="none" 
                                    stroke="currentColor" 
                                    strokeWidth="2.5" 
                                    strokeLinecap="round" 
                                    strokeLinejoin="round"
                                >
                                    <circle cx="12" cy="12" r="10"/>
                                    <line x1="12" x2="12" y1="8" y2="12"/>
                                    <line x1="12" x2="12.01" y1="16" y2="16"/>
                                </svg>
                                {errors.email}
                            </span>
                        )}
                    </div>

                    {/* Password Input Group */}
                    <div className="form-group">
                        <label htmlFor="password-input" className="form-label">
                            Password
                        </label>
                        <div className="input-wrapper">
                            <input
                                id="password-input"
                                name="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="Enter your password"
                                value={password}
                                onChange={(e) => {
                                    setPassword(e.target.value);
                                    if (errors.password) setErrors(prev => ({ ...prev, password: "" }));
                                }}
                                className={`form-input ${errors.password ? "error-border" : ""}`}
                                required
                            />
                            <div className="input-left-icon">
                                <svg 
                                    xmlns="http://www.w3.org/2000/svg" 
                                    width="18" 
                                    height="18" 
                                    viewBox="0 0 24 24" 
                                    fill="none" 
                                    stroke="currentColor" 
                                    strokeWidth="2" 
                                    strokeLinecap="round" 
                                    strokeLinejoin="round"
                                >
                                    <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/>
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                </svg>
                            </div>
                            <button
                                type="button"
                                className="password-toggle-btn"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label={showPassword ? "Hide password" : "Show password"}
                            >
                                {showPassword ? (
                                    <svg 
                                        xmlns="http://www.w3.org/2000/svg" 
                                        width="18" 
                                        height="18" 
                                        viewBox="0 0 24 24" 
                                        fill="none" 
                                        stroke="currentColor" 
                                        strokeWidth="2" 
                                        strokeLinecap="round" 
                                        strokeLinejoin="round"
                                    >
                                        <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/>
                                        <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/>
                                        <path d="M6.61 6.61A13.52 13.52 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/>
                                        <line x1="2" x2="22" y1="2" y2="22"/>
                                    </svg>
                                ) : (
                                    <svg 
                                        xmlns="http://www.w3.org/2000/svg" 
                                        width="18" 
                                        height="18" 
                                        viewBox="0 0 24 24" 
                                        fill="none" 
                                        stroke="currentColor" 
                                        strokeWidth="2" 
                                        strokeLinecap="round" 
                                        strokeLinejoin="round"
                                    >
                                        <path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0z"/>
                                        <circle cx="12" cy="12" r="3"/>
                                    </svg>
                                )}
                            </button>
                        </div>
                        {errors.password && (
                            <span className="error-container" id="password-error">
                                <svg 
                                    xmlns="http://www.w3.org/2000/svg" 
                                    width="14" 
                                    height="14" 
                                    viewBox="0 0 24 24" 
                                    fill="none" 
                                    stroke="currentColor" 
                                    strokeWidth="2.5" 
                                    strokeLinecap="round" 
                                    strokeLinejoin="round"
                                >
                                    <circle cx="12" cy="12" r="10"/>
                                    <line x1="12" x2="12" y1="8" y2="12"/>
                                    <line x1="12" x2="12.01" y1="16" y2="16"/>
                                </svg>
                                {errors.password}
                            </span>
                        )}
                    </div>

                    {/* Submit Button */}
                    <button type="submit" className="login-btn">
                        <span>Sign In</span>
                        <svg 
                            xmlns="http://www.w3.org/2000/svg" 
                            width="18" 
                            height="18" 
                            viewBox="0 0 24 24" 
                            fill="none" 
                            stroke="currentColor" 
                            strokeWidth="2" 
                            strokeLinecap="round" 
                            strokeLinejoin="round"
                        >
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                            <polyline points="10 17 15 12 10 7"/>
                            <line x1="15" x2="3" y1="12" y2="12"/>
                        </svg>
                    </button>
                </form>

                {/* Footer Navigation */}
                <footer className="login-footer">
                    <span>Don't have an account?</span>
                    <Link to="/register" className="register-link">
                        Sign Up
                    </Link>
                </footer>
            </article>
        </main>
    );
}

export default LoginPage;
