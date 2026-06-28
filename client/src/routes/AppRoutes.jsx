import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

import Home from "../pages/public/Home";
import Login from "../pages/auth/Login";
import Register from "../pages/auth/Register";
import VerifyOtp from "../pages/auth/VerifyOtp";
import CustomerProfilePage from "../pages/customer/CustomerProfilePage";

// Customer Views
import MovieDiscoveryView from "../pages/customer/MovieDiscoveryPage";
import MovieDetailPage from "../pages/customer/MovieDetailPage";
import CinemaDetailPage from "../pages/customer/CinemaDetailPage";
import MasterBookingFunnelPage from "../pages/customer/MasterBookingFunnelPage";
import SeatSelectionPage from "../pages/customer/SeatSelectionPage";

// Employee Views
import EmployeeDashboardView from "../pages/employee/EmployeeDashboardPage";
import EmployeeCheckInView from "../pages/employee/EmployeeCheckInPage";
import EmployeePOSView from "../pages/employee/EmployeePOSPage";
import EmployeeScheduleView from "../pages/employee/EmployeeSchedulePage";

// Admin Views
import AdminDashboardView from "../pages/admin/AdminDashboardPage";
import AdminMovieView from "../pages/admin/AdminMoviePage";
import AdminCinemaView from "../pages/admin/AdminCinemaPage";
import AdminConcessionInventory from "../pages/admin/AdminConcessionInventoryPage";
import AdminEventView from "../pages/admin/AdminEventPage";
import AdminFinanceView from "../pages/admin/AdminFinancePage";
import AdminMembersView from "../pages/admin/AdminMembersPage";
import AdminSettingsView from "../pages/admin/AdminSettingsPage";
import AdminShowtimeView from "../pages/admin/AdminShowtimePage";
import AdminStaffView from "../pages/admin/AdminStaffPage";

// Simple Loading Spinner for route guards
function PageLoader() {
    return (
        <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
            <div className="flex flex-col items-center gap-4">
                <div className="w-12 h-12 border-4 border-[#ff7a1a] border-t-transparent rounded-full animate-spin"></div>
                <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải...</p>
            </div>
        </div>
    );
}

// Protected Route Guard for authenticated users
function ProtectedRoute({ children }) {
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

// Role-based Route Guard
function RoleRoute({ children, allowedRoles }) {
    const { isAuthenticated, userRole, isInitializing } = useAuth();
    const location = useLocation();

    if (isInitializing) {
        return <PageLoader />;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Normalize comparison by stripping potential "ROLE_" prefixes
    const normalizedRole = userRole ? userRole.replace(/^ROLE_/, "") : "";
    const normalizedAllowedRoles = allowedRoles.map(role => role.replace(/^ROLE_/, ""));

    if (!normalizedAllowedRoles.includes(normalizedRole)) {
        return <Navigate to="/" replace />;
    }

    return children;
}

function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/home" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/verify-otp" element={<VerifyOtp />} />
                
                {/* Protected Customer Profile */}
                <Route path="/profile" element={
                    <ProtectedRoute>
                        <CustomerProfilePage />
                    </ProtectedRoute>
                } />

                {/* Customer Routes */}
                <Route path="/movies" element={<MovieDiscoveryView />} />
                <Route path="/movies/:movieId" element={<MovieDetailPage />} />
                <Route path="/movie/:movieId" element={<MovieDetailPage />} />
                <Route path="/cinema/:id" element={<CinemaDetailPage />} />
                
                {/* Booking & Seats Protected optionally or open */}
                <Route path="/booking" element={<MasterBookingFunnelPage />} />
                <Route path="/seat-selection" element={<SeatSelectionPage />} />

                {/* Employee Routes */}
                <Route path="/employee" element={
                    <RoleRoute allowedRoles={["EMPLOYEE", "STAFF"]}>
                        <EmployeeDashboardView />
                    </RoleRoute>
                } />
                <Route path="/employee/checkin" element={
                    <RoleRoute allowedRoles={["EMPLOYEE", "STAFF"]}>
                        <EmployeeCheckInView />
                    </RoleRoute>
                } />
                <Route path="/employee/pos" element={
                    <RoleRoute allowedRoles={["EMPLOYEE", "STAFF"]}>
                        <EmployeePOSView />
                    </RoleRoute>
                } />
                <Route path="/employee/schedules" element={
                    <RoleRoute allowedRoles={["EMPLOYEE", "STAFF"]}>
                        <EmployeeScheduleView />
                    </RoleRoute>
                } />

                {/* Admin Routes */}
                <Route path="/admin" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminDashboardView />
                    </RoleRoute>
                } />
                <Route path="/admin/movies" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminMovieView />
                    </RoleRoute>
                } />
                <Route path="/admin/cinemas" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminCinemaView />
                    </RoleRoute>
                } />
                <Route path="/admin/concessions" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminConcessionInventory />
                    </RoleRoute>
                } />
                <Route path="/admin/events" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminEventView />
                    </RoleRoute>
                } />
                <Route path="/admin/finance" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminFinanceView />
                    </RoleRoute>
                } />
                <Route path="/admin/members" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminMembersView />
                    </RoleRoute>
                } />
                <Route path="/admin/settings" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminSettingsView />
                    </RoleRoute>
                } />
                <Route path="/admin/showtimes" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminShowtimeView />
                    </RoleRoute>
                } />
                <Route path="/admin/staff" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminStaffView />
                    </RoleRoute>
                } />
            </Routes>
        </BrowserRouter>
    );
}

export default AppRoutes;