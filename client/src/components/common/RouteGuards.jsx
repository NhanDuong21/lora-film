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
        return <Navigate to="/403" replace />;
    }

    return children;
}

export function PermissionRoute({ children, requiredPermissions = [], requireAll = false }) {
    const { isAuthenticated, user, isInitializing } = useAuth();
    const location = useLocation();

    if (isInitializing) return <PageLoader />;
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    const permissions = user?.permissions || [];
    const normalizedRole = String(user?.role || "").replace(/^ROLE_/, "");
    const hasSuperAdminAccess = normalizedRole === "ADMIN"
        || permissions.includes("PERM_ROOT_ACCESS");
    const allowed = hasSuperAdminAccess || requiredPermissions.length === 0 || (
        requireAll
            ? requiredPermissions.every((permission) => permissions.includes(permission))
            : requiredPermissions.some((permission) => permissions.includes(permission))
    );

    return allowed ? children : <Navigate to="/403" replace />;
}

export function AdminRedirectGuard({ children }) {
    const { isAuthenticated, userRole, isInitializing } = useAuth();
    const location = useLocation();

    if (isInitializing) {
        return <PageLoader />;
    }

    const normalizedRole = userRole ? userRole.replace(/^ROLE_/, "") : "";
    if (isAuthenticated && location.pathname === "/") {
        if (normalizedRole === "ADMIN") return <Navigate to="/admin" replace />;
        if (normalizedRole === "MANAGER") return <Navigate to="/manager" replace />;
    }

    return children;
}
