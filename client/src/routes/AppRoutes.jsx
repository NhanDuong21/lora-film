import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import ScrollToTop from "../components/common/ScrollToTop";

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
import EmployeeLayout from "../components/employee/EmployeeLayout";
import EmployeeCheckInView from "../pages/employee/EmployeeCheckInPage";
import EmployeePOSView from "../pages/employee/EmployeePOSPage";
import EmployeeScheduleView from "../pages/employee/EmployeeSchedulePage";

// Admin Views
import AdminLayout from "../components/admin/AdminLayout";
import AdminGenrePage from "../pages/admin/AdminGenrePage";
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
import AdminActorPage from "../pages/admin/AdminActorPage";
import AdminConcessionSalesPage from "../pages/admin/AdminConcessionSalesPage";
import AdminPayrollPage from "../pages/admin/AdminPayrollPage";

// Main Layout
import MainLayout from "../components/layout/MainLayout";

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
            <ScrollToTop />
            <Routes>
                {/* Public & Customer Routes wrapped in MainLayout */}
                <Route element={<MainLayout />}>
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
                </Route>

                {/* Employee Routes */}
                <Route path="/employee" element={
                    <RoleRoute allowedRoles={["EMPLOYEE", "STAFF"]}>
                        <EmployeeLayout />
                    </RoleRoute>
                }>
                    <Route index element={<EmployeePOSView />} />
                    <Route path="pos" element={<EmployeePOSView />} />
                    <Route path="checkin" element={<EmployeeCheckInView />} />
                    <Route path="schedules" element={<EmployeeScheduleView />} />
                </Route>

                {/* Admin Routes */}
                <Route path="/admin" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminLayout />
                    </RoleRoute>
                }>
                    <Route index element={<AdminDashboardView />} />
                    <Route path="movies" element={<AdminMovieView />} />
                    <Route path="genres" element={<AdminGenrePage triggerToast={(msg) => console.log('Toast:', msg)} />} />
                    <Route path="cinemas" element={<AdminCinemaView />} />
                    <Route path="concessions" element={<AdminConcessionInventory />} />
                    <Route path="events" element={<AdminEventView />} />
                    <Route path="finance" element={<AdminFinanceView />} />
                    <Route path="members" element={<AdminMembersView />} />
                    <Route path="settings" element={<AdminSettingsView />} />
                    <Route path="showtimes" element={<AdminShowtimeView />} />
                    <Route path="staff" element={<AdminStaffView />} />
                    <Route path="actors" element={<AdminActorPage />} />
                    <Route path="concession-sales" element={<AdminConcessionSalesPage />} />
                    <Route path="payroll" element={<AdminPayrollPage />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default AppRoutes;