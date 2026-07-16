import AdminDashboardView from './pages/AdminDashboardPage';
import AdminEventView from './pages/AdminEventPage';
import AdminFinanceView from './pages/AdminFinancePage';
import AdminMembersView from './pages/AdminMembersPage';
import AdminSettingsView from './pages/AdminSettingsPage';
import AdminStaffView from './pages/AdminStaffPage';
import AdminPayrollPage from './pages/AdminPayrollPage';

export const adminStaffRoutes = [
    { index: true, element: <AdminDashboardView /> },
    { path: 'events', element: <AdminEventView /> },
    { path: 'finance', element: <AdminFinanceView /> },
    { path: 'members', element: <AdminMembersView /> },
    { path: 'settings', element: <AdminSettingsView /> },
    { path: 'staff', element: <AdminStaffView /> },
    { path: 'payroll', element: <AdminPayrollPage /> }
];
