/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader, PermissionRoute } from '@/components/common/RouteGuards';
import EmployeeIndexRedirect from './components/EmployeeIndexRedirect';
import { EMPLOYEE_PERMISSIONS } from './employeeAccess';

const EmployeeCheckInView = lazy(() => import('./pages/EmployeeCheckInPage'));
const EmployeeScheduleView = lazy(() => import('./pages/EmployeeSchedulePage'));
const EmployeePayrollPage = lazy(() => import('./pages/EmployeePayrollPage'));
const EmployeeDashboardPage = lazy(() => import('./pages/EmployeeDashboardPage'));
const EmployeeBoxOfficePage = lazy(() => import('./pages/EmployeeBoxOfficePage'));
const EmployeeOrdersPage = lazy(() => import('./pages/EmployeeOrdersPage'));
const EmployeeOrderDetailPage = lazy(() => import('./pages/EmployeeOrderDetailPage'));
const EmployeeCashSessionPage = lazy(() => import('./pages/EmployeeCashSessionPage'));
const EmployeeTicketScanPage = lazy(() => import('./pages/EmployeeTicketScanPage'));
const EmployeeTicketShowtimesPage = lazy(() => import('./pages/EmployeeTicketShowtimesPage'));
const EmployeeTicketHistoryPage = lazy(() => import('./pages/EmployeeTicketHistoryPage'));
const EmployeeTicketHandoffPage = lazy(() => import('./pages/EmployeeTicketHandoffPage'));

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
        path: 'box-office',
        element: requirePermission(
            lazyPage(<EmployeeBoxOfficePage />),
            [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE, EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
            true
        )
    },
    {
        path: 'orders',
        element: requirePermission(
            lazyPage(<EmployeeOrdersPage />),
            [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE]
        )
    },
    {
        path: 'orders/:bookingPublicId',
        element: requirePermission(
            lazyPage(<EmployeeOrderDetailPage />),
            [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE]
        )
    },
    {
        path: 'cash-session',
        element: requirePermission(
            lazyPage(<EmployeeCashSessionPage />),
            [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE, EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
            true
        )
    },
    {
        path: 'dashboard',
        element: requirePermission(
            lazyPage(<EmployeeDashboardPage />),
            [EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW]
        )
    },
    {
        path: 'ticket-scan',
        element: requirePermission(
            lazyPage(<EmployeeTicketScanPage />),
            [EMPLOYEE_PERMISSIONS.TICKET_SCAN]
        )
    },
    {
        path: 'ticket-showtimes',
        element: requirePermission(
            lazyPage(<EmployeeTicketShowtimesPage />),
            [EMPLOYEE_PERMISSIONS.TICKET_SCAN]
        )
    },
    {
        path: 'ticket-history',
        element: requirePermission(
            lazyPage(<EmployeeTicketHistoryPage />),
            [EMPLOYEE_PERMISSIONS.TICKET_SCAN]
        )
    },
    {
        path: 'ticket-handoff',
        element: requirePermission(
            lazyPage(<EmployeeTicketHandoffPage />),
            [EMPLOYEE_PERMISSIONS.TICKET_SCAN]
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
