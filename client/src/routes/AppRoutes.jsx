import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import ScrollToTop from "@/components/common/ScrollToTop";

// Centralized route paths
import { routePaths } from "@/routes/routePaths";

// Feature-based Pages
import Home from "@/features/movies-genres/pages/Home";
import Login from "@/features/auth/pages/Login";
import Register from "@/features/auth/pages/Register";
import VerifyOtp from "@/features/auth/pages/VerifyOtp";
import CustomerProfilePage from "@/features/auth/pages/CustomerProfilePage";

// Customer Views
import MovieDiscoveryView from "@/features/movies-genres/pages/MovieDiscoveryPage";
import MovieDetailPage from "@/features/movies-genres/pages/MovieDetailPage";
import CinemaDetailPage from "@/features/cinemas-rooms/pages/CinemaDetailPage";
import MasterBookingFunnelPage from "@/features/showtimes-pricing/pages/MasterBookingFunnelPage";
import SeatSelectionPage from "@/features/showtimes-pricing/pages/SeatSelectionPage";

// Employee Views
import EmployeeLayout from "@/components/employee/EmployeeLayout";
import EmployeeCheckInView from "@/features/internal-staff/pages/EmployeeCheckInPage";
import EmployeePOSView from "@/features/concessions-sales/pages/EmployeePOSPage";
import EmployeeScheduleView from "@/features/internal-staff/pages/EmployeeSchedulePage";

// Admin Views
import AdminLayout from "@/components/admin/AdminLayout";
import AdminGenrePage from "@/features/movies-genres/pages/AdminGenrePage";
import AdminDashboardView from "@/features/internal-staff/pages/AdminDashboardPage";
import AdminMovieView from "@/features/movies-genres/pages/AdminMoviePage";
import AdminCinemaView from "@/features/cinemas-rooms/pages/AdminCinemaPage";
import AdminConcessionInventory from "@/features/concessions-sales/pages/AdminConcessionInventoryPage";
import AdminEventView from "@/features/internal-staff/pages/AdminEventPage";
import AdminFinanceView from "@/features/internal-staff/pages/AdminFinancePage";
import AdminMembersView from "@/features/internal-staff/pages/AdminMembersPage";
import AdminSettingsView from "@/features/internal-staff/pages/AdminSettingsPage";
import AdminShowtimeView from "@/features/showtimes-pricing/pages/AdminShowtimePage";
import AdminStaffView from "@/features/internal-staff/pages/AdminStaffPage";
import AdminConcessionSalesPage from "@/features/concessions-sales/pages/AdminConcessionSalesPage";
import AdminPayrollPage from "@/features/internal-staff/pages/AdminPayrollPage";
import AdminRoomPage from "@/features/cinemas-rooms/pages/AdminRoomPage";
import AdminRoomCreatePage from "@/features/cinemas-rooms/pages/AdminRoomCreatePage";
import AdminRoomEditPage from "@/features/cinemas-rooms/pages/AdminRoomEditPage";

// Main Layout
import MainLayout from "@/components/layout/MainLayout";

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

// Redirect Admin users away from public/customer routes
function AdminRedirectGuard({ children }) {
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

function AppRoutes() {
    return (
        <BrowserRouter>
            <ScrollToTop />
            <Routes>
                {/* Public & Customer Routes wrapped in MainLayout */}
                <Route element={<AdminRedirectGuard><MainLayout /></AdminRedirectGuard>}>
                    <Route path={routePaths.home} element={<Home />} />
                    <Route path="/home" element={<Home />} />
                    <Route path={routePaths.login} element={<Login />} />
                    <Route path={routePaths.register} element={<Register />} />
                    <Route path={routePaths.verifyOtp} element={<VerifyOtp />} />
                    
                    {/* Protected Customer Profile */}
                    <Route path={routePaths.profile} element={
                        <ProtectedRoute>
                            <CustomerProfilePage />
                        </ProtectedRoute>
                    } />

                    {/* Customer Routes */}
                    <Route path={routePaths.movies} element={<MovieDiscoveryView />} />
                    <Route path={routePaths.movieDetail} element={<MovieDetailPage />} />
                    <Route path="/movie/:movieId" element={<MovieDetailPage />} />
                    <Route path={routePaths.cinemaDetail} element={<CinemaDetailPage />} />
                    
                    {/* Booking & Seats Protected optionally or open */}
                    <Route path={routePaths.booking} element={<MasterBookingFunnelPage />} />
                    <Route path={routePaths.seatSelection} element={<SeatSelectionPage />} />
                </Route>

                {/* Employee Routes */}
                <Route path={routePaths.employee.root} element={
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
                <Route path={routePaths.admin.root} element={
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
                    <Route path="concession-sales" element={<AdminConcessionSalesPage />} />
                    <Route path="payroll" element={<AdminPayrollPage />} />
                    <Route path="rooms" element={<AdminRoomPage />} />
                    <Route path="rooms/create" element={<AdminRoomCreatePage />} />
                    <Route path="rooms/edit/:roomId" element={<AdminRoomEditPage />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default AppRoutes;