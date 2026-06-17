import { BrowserRouter, Routes, Route } from "react-router-dom";
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

function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/home" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/verify-otp" element={<VerifyOtp />} />
                <Route path="/profile" element={<CustomerProfilePage />} />

                {/* Customer Routes */}
                <Route path="/movies" element={<MovieDiscoveryView />} />
                <Route path="/movie/:id" element={<MovieDetailPage />} />
                <Route path="/cinema/:id" element={<CinemaDetailPage />} />
                <Route path="/booking" element={<MasterBookingFunnelPage />} />
                <Route path="/seat-selection" element={<SeatSelectionPage />} />

                {/* Employee Routes */}
                <Route path="/employee" element={<EmployeeDashboardView />} />
                <Route path="/employee/checkin" element={<EmployeeCheckInView />} />
                <Route path="/employee/pos" element={<EmployeePOSView />} />
                <Route path="/employee/schedules" element={<EmployeeScheduleView />} />

                {/* Admin Routes */}
                <Route path="/admin" element={<AdminDashboardView />} />
                <Route path="/admin/movies" element={<AdminMovieView />} />
                <Route path="/admin/cinemas" element={<AdminCinemaView />} />
                <Route path="/admin/concessions" element={<AdminConcessionInventory />} />
                <Route path="/admin/events" element={<AdminEventView />} />
                <Route path="/admin/finance" element={<AdminFinanceView />} />
                <Route path="/admin/members" element={<AdminMembersView />} />
                <Route path="/admin/settings" element={<AdminSettingsView />} />
                <Route path="/admin/showtimes" element={<AdminShowtimeView />} />
                <Route path="/admin/staff" element={<AdminStaffView />} />
            </Routes>
        </BrowserRouter>
    );
}

export default AppRoutes;