/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';
import { PageLoader, PermissionRoute } from '@/components/common/RouteGuards';
import { useAuth } from '@/contexts/AuthContext';
import { getAdminLandingPath, hasPermissionAccess } from './permissionAccess';

const AdminDashboardView = lazy(() => import('./pages/AdminDashboardPage'));
const AdminMembersView = lazy(() => import('./pages/AdminMembersPage'));
const AdminStaffView = lazy(() => import('./pages/AdminStaffPage'));
const AdminPayrollPage = lazy(() => import('./pages/AdminPayrollPage'));
const AdminDepartmentPage = lazy(() => import('./pages/AdminDepartmentPage'));
const AdminPositionPage = lazy(() => import('./pages/AdminPositionPage'));
const AdminRolePage = lazy(() => import('./pages/AdminRolePage'));
const AdminPermissionPage = lazy(() => import('./pages/AdminPermissionPage'));
const AdminAuthAuditPage = lazy(() => import('./pages/AdminAuthAuditPage'));
const AdminEmployeeDocumentPage = lazy(() => import('./pages/AdminEmployeeDocumentPage'));
const AdminAccountPage = lazy(() => import('./pages/AdminAccountPage'));
const AdminUserAuditPage = lazy(() => import('./pages/AdminUserAuditPage'));
const AdminAnalyticsPage = lazy(() => import('@/features/analytics/admin/pages/AdminAnalyticsPage'));
const AdminMyAccountPage = lazy(() => import('./pages/AdminMyAccountPage'));

const lazyPage = (element) => (
    <Suspense fallback={<PageLoader />}>
        {element}
    </Suspense>
);

const requirePermission = (element, ...requiredPermissions) => (
    <PermissionRoute requiredPermissions={requiredPermissions}>
        {lazyPage(element)}
    </PermissionRoute>
);

function AdminLanding() {
    const { user, userRole } = useAuth();
    const role = userRole || user?.role;
    const permissions = user?.permissions || [];

    if (hasPermissionAccess(role, permissions, 'DASHBOARD_VIEW')) {
        return lazyPage(<AdminDashboardView />);
    }

    return <Navigate to={getAdminLandingPath(role, permissions)} replace />;
}

export const adminStaffRoutes = [
    { index: true, element: <AdminLanding /> },
    { path: 'me', element: lazyPage(<AdminMyAccountPage />) },
    { path: 'finance', element: <Navigate to="/admin/analytics" replace /> },
    { path: 'analytics', element: requirePermission(<AdminAnalyticsPage />, 'PERM_VIEW_FINANCE', 'DASHBOARD_VIEW') },
    { path: 'members', element: requirePermission(<AdminMembersView />, 'CUSTOMER_VIEW') },
    { path: 'staff', element: requirePermission(<AdminStaffView />, 'EMPLOYEE_VIEW') },
    { path: 'departments', element: requirePermission(<AdminDepartmentPage />, 'DEPARTMENT_VIEW') },
    { path: 'positions', element: requirePermission(<AdminPositionPage />, 'POSITION_VIEW') },
    { path: 'payroll', element: requirePermission(<AdminPayrollPage />, 'PAYROLL_VIEW') },
    { path: 'roles', element: requirePermission(<AdminRolePage />, 'ROLE_VIEW') },
    { path: 'accounts', element: requirePermission(<AdminAccountPage />, 'SYSTEM_CONFIGURATION') },
    { path: 'permissions', element: requirePermission(<AdminPermissionPage />, 'PERMISSION_VIEW') },
    { path: 'audits', element: requirePermission(<AdminAuthAuditPage />, 'SYSTEM_CONFIGURATION') },
    { path: 'user-audits', element: requirePermission(<AdminUserAuditPage />, 'USER_AUDIT_VIEW', 'SYSTEM_CONFIGURATION') },
    {
        path: 'staff/:accountId/documents',
        element: requirePermission(<AdminEmployeeDocumentPage />, 'EMPLOYEE_VIEW')
    }
];
