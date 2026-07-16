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

import { adminConcessionRoutes } from "@/features/concessions-sales/admin/routes";
import { employeeConcessionRoutes } from "@/features/concessions-sales/employee/routes";
import { adminStaffRoutes } from "@/features/internal-staff/admin/routes";
import { employeeStaffRoutes } from "@/features/internal-staff/employee/routes";

// Layouts
import MainLayout from "@/components/layout/MainLayout";
import AdminLayout from "@/components/admin/AdminLayout";
import EmployeeLayout from "@/components/employee/EmployeeLayout";

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
                    {employeeConcessionRoutes.map((route, index) => (
                        route.index ? 
                            <Route key={`conc-emp-${index}`} index element={route.element} /> : 
                            <Route key={`conc-emp-${index}`} path={route.path} element={route.element} />
                    ))}
                    {employeeStaffRoutes.map((route, index) => (
                        <Route key={`staff-emp-${index}`} path={route.path} element={route.element} />
                    ))}
                </Route>

                {/* Admin Routes */}
                <Route path="/admin" element={
                    <RoleRoute allowedRoles={["ADMIN"]}>
                        <AdminLayout />
                    </RoleRoute>
                }>
                    {adminStaffRoutes.map((route, index) => (
                        route.index ? 
                            <Route key={`staff-adm-${index}`} index element={route.element} /> : 
                            <Route key={`staff-adm-${index}`} path={route.path} element={route.element} />
                    ))}
                    {adminCatalogRoutes.map((route, index) => (
                        <Route key={`cat-adm-${index}`} path={route.path} element={route.element} />
                    ))}
                    {adminFacilitiesRoutes.map((route, index) => (
                        <Route key={`fac-adm-${index}`} path={route.path} element={route.element} />
                    ))}
                    {adminSchedulingRoutes.map((route, index) => (
                        <Route key={`sched-${index}`} path={route.path} element={route.element} />
                    ))}
                    {adminConcessionRoutes.map((route, index) => (
                        <Route key={`conc-adm-${index}`} path={route.path} element={route.element} />
                    ))}
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default AppRoutes;