import EmployeePOSView from './pages/EmployeePOSPage';

export const employeeConcessionRoutes = [
    { index: true, element: <EmployeePOSView /> },
    { path: 'pos', element: <EmployeePOSView /> }
];
