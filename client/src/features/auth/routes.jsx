// eslint-disable-next-line no-unused-vars
import { Navigate } from 'react-router-dom';
import Login from '@/features/auth/pages/Login';
import Register from '@/features/auth/pages/Register';
import VerifyOtp from '@/features/auth/pages/VerifyOtp';
import CustomerProfilePage from '@/features/auth/pages/CustomerProfilePage';
import { ProtectedRoute } from '@/components/common/RouteGuards';

export const authRoutes = [
    { path: 'login', element: <Login /> },
    { path: 'register', element: <Register /> },
    { path: 'verify-otp', element: <VerifyOtp /> },
    { path: 'profile', element: <ProtectedRoute><CustomerProfilePage /></ProtectedRoute> }
];
