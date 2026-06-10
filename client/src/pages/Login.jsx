import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../services/authService";
import "./Login.css";

function Login() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        document.title = "Sign In - LoraFilm Ticket Booking";
    }, []);

    const handleSubmit = async (e, customEmail, customPassword) => {
        if (e) {
            e.preventDefault();
        }
        setErrorMsg("");
        setSuccessMessage("");

        const loginEmail = customEmail !== undefined ? customEmail : email;
        const loginPassword = customPassword !== undefined ? customPassword : password;

        // Form Validation
        if (!loginEmail.trim() || !loginPassword.trim()) {
            setErrorMsg("Please enter both email and password.");
            return;
        }

        if (loginPassword.length < 6) {
            setErrorMsg("Password must be at least 6 characters long.");
            return;
        }

        setIsSubmitting(true);
        try {
            const data = await login(loginEmail, loginPassword);
            setIsSubmitting(false);

            // Extract token from standard API response structure
            const token = data?.data?.token || data?.token;

            if (token) {
                // Store the JWT token securely in localStorage
                localStorage.setItem("token", token);
                setSuccessMessage("Login successfully! Redirecting to home dashboard...");

                // Short delay to allow user to see the success state transition
                setTimeout(() => {
                    navigate("/");
                }, 1500);
            } else {
                setErrorMsg("Authentication succeeded but no token was returned by the server.");
            }
        } catch (error) {
            setIsSubmitting(false);

            // Extract the message payload from server error or default fallback
            const errorMessage = error?.message || error?.error || "Invalid email or password. Please check your credentials.";
            setErrorMsg(errorMessage);
        }
    };

    const handleFastFill = (roleType) => {
        let fillEmail = "";
        const fillPass = "123456";

        if (roleType === "ADMIN") {
            fillEmail = "admin@lorafilm.com";
        } else if (roleType === "EMPLOYEE") {
            fillEmail = "staff@lorafilm.com";
        } else {
            fillEmail = "member@gmail.com";
        }

        setEmail(fillEmail);
        setPassword(fillPass);

        // Instantly invoke the submit procedure
        handleSubmit(null, fillEmail, fillPass);
    };

    return (
        <main className="login-page-container">
            <div className="light-leak-top" />
            <div className="light-leak-bottom" />

            <article className="login-card">
                {/* Back to Home Button */}
                <button
                    onClick={() => navigate("/")}
                    className="back-link-btn"
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
                    <span>Back to home</span>
                </button>

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
                                <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
                                <path d="M13 5v2" />
                                <path d="M13 17v2" />
                                <path d="M13 11v2" />
                            </svg>
                        </div>
                        <h2>Lora<span>film</span></h2>
                    </div>
                    <p className="login-subtitle">Book tickets to your favorite movies in seconds</p>
                    
                    {/* Success Message Banner */}
                    {successMessage && (
                        <div className="success-banner">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                width="20"
                                height="20"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <polyline points="20 6 9 17 4 12" />
                            </svg>
                            <span>{successMessage}</span>
                        </div>
                    )}
                </header>

                {/* Error Notification Banner */}
                {errorMsg && (
                    <div className="error-alert-banner">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="18"
                            height="18"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            style={{ flexShrink: 0, marginTop: "2px" }}
                        >
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                            <line x1="12" y1="8" x2="12" y2="12" />
                            <line x1="12" y1="16" x2="12.01" y2="16" />
                        </svg>
                        <span>{errorMsg}</span>
                    </div>
                )}

                <form onSubmit={(e) => handleSubmit(e)} className="login-form" noValidate>
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
                                onChange={(e) => setEmail(e.target.value)}
                                className="form-input"
                                required
                                disabled={isSubmitting}
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
                                    <rect width="20" height="16" x="2" y="4" rx="2" />
                                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                                </svg>
                            </div>
                        </div>
                    </div>

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
                                onChange={(e) => setPassword(e.target.value)}
                                className="form-input"
                                required
                                disabled={isSubmitting}
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
                                    <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                </svg>
                            </div>
                            <button
                                type="button"
                                className="password-toggle-btn"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label={showPassword ? "Hide password" : "Show password"}
                                disabled={isSubmitting}
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
                                        <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
                                        <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
                                        <path d="M6.61 6.61A13.52 13.52 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
                                        <line x1="2" x2="22" y1="2" y2="22" />
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
                                        <path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0z" />
                                        <circle cx="12" cy="12" r="3" />
                                    </svg>
                                )}
                            </button>
                        </div>
                    </div>

                    <button type="submit" className="login-btn" disabled={isSubmitting}>
                        {isSubmitting ? (
                            <>
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    width="18"
                                    height="18"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2.5"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    className="spinner"
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
                                <span>Verifying...</span>
                            </>
                        ) : (
                            <>
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
                                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                                    <polyline points="10 17 15 12 10 7" />
                                    <line x1="15" x2="3" y1="12" y2="12" />
                                </svg>
                            </>
                        )}
                    </button>
                </form>

                {/* Demo Fast-Fill Section */}
                <div className="demo-divider">Demo Fast-Fill Accounts</div>
                <div className="demo-pills-container">
                    <button
                        onClick={() => handleFastFill("ADMIN")}
                        className="demo-pill admin"
                        disabled={isSubmitting}
                    >
                        <div className="pill-left-group">
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
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                            </svg>
                            <span>Quick Admin login</span>
                        </div>
                        <span className="pill-badge admin">Admin</span>
                    </button>

                    <button
                        onClick={() => handleFastFill("EMPLOYEE")}
                        className="demo-pill staff"
                        disabled={isSubmitting}
                    >
                        <div className="pill-left-group">
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
                                <path d="M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                <rect width="20" height="14" x="2" y="6" rx="2" />
                            </svg>
                            <span>Quick Staff login</span>
                        </div>
                        <span className="pill-badge staff">Staff</span>
                    </button>

                    <button
                        onClick={() => handleFastFill("CUSTOMER")}
                        className="demo-pill member"
                        disabled={isSubmitting}
                    >
                        <div className="pill-left-group">
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
                                <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
                                <circle cx="12" cy="7" r="4" />
                            </svg>
                            <span>Quick Customer login</span>
                        </div>
                        <span className="pill-badge member">Member</span>
                    </button>
                </div>

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

export default Login;
