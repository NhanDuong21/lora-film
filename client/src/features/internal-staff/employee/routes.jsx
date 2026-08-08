/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader, PermissionRoute } from '@/components/common/RouteGuards';
import EmployeeIndexRedirect from './components/EmployeeIndexRedirect';
import { EMPLOYEE_PERMISSIONS } from './employeeAccess';

const EmployeeCheckInView = lazy(() => import('./pages/EmployeeCheckInPage'));
const EmployeeScheduleView = lazy(() => import('./pages/EmployeeSchedulePage'));
const EmployeePayrollPage = lazy(() => import('./pages/EmployeePayrollPage'));
const EmployeeDashboardPage = lazy(() => import('./pages/EmployeeDashboardPage'));

const lazyPage = (element) => (
    <Suspense fallback={<PageLoader />}>
        {element}
    </Suspense>
);

const requirePermission = (element, requiredPermissions, requireAll = false) => (
    <PermissionRoute requiredPermissions={requiredPermissions} requireAll={requireAll}>
        {element}
    </PermissionRoute>
);

export const employeeStaffRoutes = [
    { index: true, element: <EmployeeIndexRedirect /> },
    {
        path: 'dashboard',
        element: requirePermission(
            lazyPage(<EmployeeDashboardPage />),
            [EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW]
        )
    },
    {
        path: 'checkin',
        element: requirePermission(
            lazyPage(<EmployeeCheckInView />),
            [EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW, EMPLOYEE_PERMISSIONS.ATTENDANCE_UPDATE],
            true
        )
    },
    {
        path: 'schedules',
        element: requirePermission(
            lazyPage(<EmployeeScheduleView />),
            [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW]
        )
    },
    {
        path: 'payroll',
        element: requirePermission(
            lazyPage(<EmployeePayrollPage />),
            [EMPLOYEE_PERMISSIONS.PAYROLL_VIEW]
        )
    }
];
