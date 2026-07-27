import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Loader2 } from 'lucide-react';
import { jwtDecode } from "jwt-decode";

function OAuth2RedirectHandler() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        const handleOAuth2Callback = async () => {
            const searchParams = new URLSearchParams(
                location.hash?.startsWith('#') ? location.hash.substring(1) : location.search
            );
            const accessToken = searchParams.get("accessToken");
            const refreshToken = searchParams.get("refreshToken");
            const expiresInStr = searchParams.get("expiresIn");
            const error = searchParams.get("error");

            if (error) {
                navigate("/login", { state: { error: "Xác thực OAuth2 thất bại." }, replace: true });
                return;
            }

            if (accessToken) {
                try {
                    let decodedToken = null;
                    let role = "";
                    let email = "";
                    try {
                        decodedToken = jwtDecode(accessToken);
                        role = decodedToken.role || "";
                        email = decodedToken.sub || "";
                    } catch (e) {
                        console.error("Failed to decode token", e);
                    }

                    const expiresIn = expiresInStr ? parseInt(expiresInStr, 10) : 3600000;
                    
                    const sessionData = {
                        accessToken,
                        refreshToken,
                        tokenType: "Bearer",
                        expiresIn,
                        role,
                        email,
                        accountId: decodedToken?.userId
                    };

                    await login(sessionData);

                    setTimeout(() => {
                        if (role === "ADMIN" || role === "ROLE_ADMIN" || role === "ROLE_ACCOUNTANT") {
                            navigate("/admin", { replace: true });
                        } else if (role === "EMPLOYEE" || role === "STAFF" || role === "ROLE_STAFF") {
                            navigate("/employee", { replace: true });
                        } else {
                            navigate("/", { replace: true });
                        }
                    }, 400);

                } catch (e) {
                    console.error("Error processing OAuth2 login", e);
                    navigate("/login", { state: { error: "Lỗi xử lý đăng nhập." }, replace: true });
                }
            } else {
                navigate("/login", { replace: true });
            }
        };

        handleOAuth2Callback();
    }, [location, login, navigate]);

    return (
        <main className="bg-zinc-950 text-white min-h-screen flex items-center justify-center">
            <div className="flex flex-col items-center gap-4">
                <Loader2 className="w-10 h-10 animate-spin text-brand-orange" />
                <p className="text-zinc-400 font-medium tracking-widest text-sm uppercase">Đang xử lý đăng nhập...</p>
            </div>
        </main>
    );
}

export default OAuth2RedirectHandler;
