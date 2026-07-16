import EmployeeCheckInView from './pages/EmployeeCheckInPage';
import EmployeeScheduleView from './pages/EmployeeSchedulePage';

export const employeeStaffRoutes = [
    { path: 'checkin', element: <EmployeeCheckInView /> },
    { path: 'schedules', element: <EmployeeScheduleView /> }
];
