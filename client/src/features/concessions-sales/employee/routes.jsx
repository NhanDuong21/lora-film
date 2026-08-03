import { Navigate } from 'react-router-dom';

export const employeeConcessionRoutes = [
    { index: true, element: <Navigate to="payments/cash" replace /> },
    { path: 'pos', element: <Navigate to="/employee/payments/cash" replace /> }
];
