/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

const EmployeeCheckInView = lazy(() => import('./pages/EmployeeCheckInPage'));
const EmployeeScheduleView = lazy(() => import('./pages/EmployeeSchedulePage'));
const EmployeePayrollPage = lazy(() => import('./pages/EmployeePayrollPage'));
const EmployeeDashboardPage = lazy(() => import('./pages/EmployeeDashboardPage'));

const lazyPage = (element) => (
    <Suspense fallback={<PageLoader />}>
        {element}
    </Suspense>
);

export const employeeStaffRoutes = [
    { path: 'dashboard', element: lazyPage(<EmployeeDashboardPage />) },
    { path: 'checkin', element: lazyPage(<EmployeeCheckInView />) },
    { path: 'schedules', element: lazyPage(<EmployeeScheduleView />) },
    { path: 'payroll', element: lazyPage(<EmployeePayrollPage />) }
];
