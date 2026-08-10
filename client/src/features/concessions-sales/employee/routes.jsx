import { Navigate } from 'react-router-dom';

export const employeeConcessionRoutes = [
    { path: 'pos', element: <Navigate to="/employee/payments/cash" replace /> }
];
