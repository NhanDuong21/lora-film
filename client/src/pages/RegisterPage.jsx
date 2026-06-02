import { Link } from "react-router-dom";

function RegisterPage() {
    return (
        <div style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            height: "100vh",
            backgroundColor: "#08080c",
            color: "#ffffff",
            fontFamily: "system-ui, -apple-system, sans-serif"
        }}>
            <h1 style={{ color: "#E50914", fontSize: "2.5rem", marginBottom: "1rem" }}>Register Page</h1>
            <p style={{ color: "#8c8c8c", marginBottom: "2rem" }}>Registration is under construction.</p>
            <Link to="/login" style={{
                color: "#ffffff",
                textDecoration: "none",
                padding: "10px 20px",
                border: "1px solid #E50914",
                borderRadius: "4px",
                backgroundColor: "transparent",
                transition: "all 0.3s ease",
                fontWeight: "500"
            }}>
                Back to Login
            </Link>
        </div>
    );
}

export default RegisterPage;
