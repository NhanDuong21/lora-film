import { BrowserRouter, Routes, Route } from "react-router-dom";
import ScrollToTop from "@/components/common/ScrollToTop";
import { RoleRoute, AdminRedirectGuard } from "@/components/common/RouteGuards";

// Feature Routes
import { authRoutes } from "@/features/auth/routes";
import { customerCatalogRoutes } from "@/features/catalog/customer/routes";
import { adminCatalogRoutes } from "@/features/catalog/admin/routes";
import { customerFacilitiesRoutes } from "@/features/facilities/customer/routes";
import { adminFacilitiesRoutes } from "@/features/facilities/admin/routes";
import { adminSchedulingRoutes } from "@/features/scheduling/admin/routes";
import { customerBookingRoutes } from "@/features/booking/customer/routes";

// Layouts
import MainLayout from "@/components/layout/MainLayout";
import AdminLayout from "@/components/admin/AdminLayout";
import EmployeeLayout from "@/components/employee/EmployeeLayout";

// Employee / Other routes that are not in target architecture but exist in codebase
import EmployeeCheckInView from "@/features/internal-staff/pages/EmployeeCheckInPage";
import EmployeePOSView from "@/features/concessions-sales/pages/EmployeePOSPage";
import EmployeeScheduleView from "@/features/internal-staff/pages/EmployeeSchedulePage";

import AdminDashboardView from "@/features/internal-staff/pages/AdminDashboardPage";
import AdminConcessionInventory from "@/features/concessions-sales/pages/AdminConcessionInventoryPage";
import AdminEventView from "@/features/internal-staff/pages/AdminEventPage";
import AdminFinanceView from "@/features/internal-staff/pages/AdminFinancePage";
import AdminMembersView from "@/features/internal-staff/pages/AdminMembersPage";
import AdminSettingsView from "@/features/internal-staff/pages/AdminSettingsPage";
import AdminStaffView from "@/features/internal-staff/pages/AdminStaffPage";
import AdminConcessionSalesPage from "@/features/concessions-sales/pages/AdminConcessionSalesPage";
import AdminPayrollPage from "@/features/internal-staff/pages/AdminPayrollPage";

function AppRoutes() {
    return (
        <BrowserRouter>
            <ScrollToTop />
            <Routes>
                {/* Public & Customer Routes */}
                <Route element={<AdminRedirectGuard><MainLayout /></AdminRedirectGuard>}>
                    {authRoutes.map((route, index) => (
                        <Route key={`auth-${index}`} path={route.path} element={route.element} />
                    ))}
                    {customerCatalogRoutes.map((route, index) => (
                        <Route key={`cat-cust-${index}`} path={route.path} element={route.element} />
                    ))}
                    {customerFacilitiesRoutes.map((route, index) => (
                        <Route key={`fac-cust-${index}`} path={route.path} element={route.element} />
                    ))}
                    {customerBookingRoutes.map((route, index) => (
                        <Route key={`book-${index}`} path={route.path} element={route.element} />
                    ))}
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
                    {adminCatalogRoutes.map((route, index) => (
                        <Route key={`cat-adm-${index}`} path={route.path} element={route.element} />
                    ))}
                    {adminFacilitiesRoutes.map((route, index) => (
                        <Route key={`fac-adm-${index}`} path={route.path} element={route.element} />
                    ))}
                    {adminSchedulingRoutes.map((route, index) => (
                        <Route key={`sched-${index}`} path={route.path} element={route.element} />
                    ))}
                    
                    {/* Other Admin Routes */}
                    <Route path="concessions" element={<AdminConcessionInventory />} />
                    <Route path="events" element={<AdminEventView />} />
                    <Route path="finance" element={<AdminFinanceView />} />
                    <Route path="members" element={<AdminMembersView />} />
                    <Route path="settings" element={<AdminSettingsView />} />
                    <Route path="staff" element={<AdminStaffView />} />
                    <Route path="concession-sales" element={<AdminConcessionSalesPage />} />
                    <Route path="payroll" element={<AdminPayrollPage />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default AppRoutes;