import AdminDashboardView from './pages/AdminDashboardPage';
import AdminEventView from './pages/AdminEventPage';
import AdminFinanceView from './pages/AdminFinancePage';
import AdminMembersView from './pages/AdminMembersPage';
import AdminSettingsView from './pages/AdminSettingsPage';
import AdminStaffView from './pages/AdminStaffPage';
import AdminPayrollPage from './pages/AdminPayrollPage';
import AdminDepartmentPage from './pages/AdminDepartmentPage';
import AdminPositionPage from './pages/AdminPositionPage';
import AdminRolePage from './pages/AdminRolePage';
import AdminPermissionPage from './pages/AdminPermissionPage';
import AdminAuthAuditPage from './pages/AdminAuthAuditPage';
import AdminEmployeeDocumentPage from './pages/AdminEmployeeDocumentPage';

export const adminStaffRoutes = [
    { index: true, element: <AdminDashboardView /> },
    { path: 'events', element: <AdminEventView /> },
    { path: 'finance', element: <AdminFinanceView /> },
    { path: 'members', element: <AdminMembersView /> },
    { path: 'settings', element: <AdminSettingsView /> },
    { path: 'staff', element: <AdminStaffView /> },
    { path: 'departments', element: <AdminDepartmentPage /> },
    { path: 'positions', element: <AdminPositionPage /> },
    { path: 'payroll', element: <AdminPayrollPage /> },
    { path: 'roles', element: <AdminRolePage /> },
    { path: 'permissions', element: <AdminPermissionPage /> },
    { path: 'audits', element: <AdminAuthAuditPage /> },
    { path: 'staff/:accountId/documents', element: <AdminEmployeeDocumentPage /> }
];
