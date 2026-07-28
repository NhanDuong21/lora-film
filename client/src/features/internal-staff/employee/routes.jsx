import EmployeeCheckInView from './pages/EmployeeCheckInPage';
import EmployeeScheduleView from './pages/EmployeeSchedulePage';
import EmployeePayrollPage from './pages/EmployeePayrollPage';

export const employeeStaffRoutes = [
    { path: 'checkin', element: <EmployeeCheckInView /> },
    { path: 'schedules', element: <EmployeeScheduleView /> },
    { path: 'payroll', element: <EmployeePayrollPage /> }
];
