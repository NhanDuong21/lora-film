import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";

export function PageLoader() {
    return (
        <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
            <div className="flex flex-col items-center gap-4">
                <div className="w-12 h-12 border-4 border-[#ff7a1a] border-t-transparent rounded-full animate-spin"></div>
                <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải...</p>
            </div>
        </div>
    );
}

export function ProtectedRoute({ children }) {
    const { isAuthenticated, isInitializing } = useAuth();
    const location = useLocation();

    if (isInitializing) {
        return <PageLoader />;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    return children;
}

export function RoleRoute({ children, allowedRoles }) {
    const { isAuthenticated, userRole, isInitializing } = useAuth();
    const location = useLocation();

    if (isInitializing) {
        return <PageLoader />;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    const normalizedRole = userRole ? userRole.replace(/^ROLE_/, "") : "";
    const normalizedAllowedRoles = allowedRoles.map(role => role.replace(/^ROLE_/, ""));

    if (!normalizedAllowedRoles.includes(normalizedRole)) {
        return <Navigate to="/" replace />;
    }

    return children;
}

export function AdminRedirectGuard({ children }) {
    const { isAuthenticated, userRole, isInitializing } = useAuth();

    if (isInitializing) {
        return <PageLoader />;
    }

    const normalizedRole = userRole ? userRole.replace(/^ROLE_/, "") : "";
    if (isAuthenticated && normalizedRole === "ADMIN") {
        return <Navigate to="/admin" replace />;
    }

    return children;
}
