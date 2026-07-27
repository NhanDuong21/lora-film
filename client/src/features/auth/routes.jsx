// eslint-disable-next-line no-unused-vars
import { Navigate } from 'react-router-dom';
import Login from '@/features/auth/pages/Login';
import Register from '@/features/auth/pages/Register';
import VerifyOtp from '@/features/auth/pages/VerifyOtp';
import CustomerProfilePage from '@/features/auth/pages/CustomerProfilePage';
import { ProtectedRoute } from '@/components/common/RouteGuards';
import ForgotPassword from '@/features/auth/pages/ForgotPassword';
import ResetPassword from '@/features/auth/pages/ResetPassword';
import ChangePassword from '@/features/auth/pages/ChangePassword';
import SessionsPage from '@/features/auth/pages/SessionsPage';
import OAuth2RedirectHandler from '@/features/auth/pages/OAuth2RedirectHandler';

export const authRoutes = [
    { path: 'login', element: <Login /> },
    { path: 'register', element: <Register /> },
    { path: 'verify-otp', element: <VerifyOtp /> },
    { path: 'forgot-password', element: <ForgotPassword /> },
    { path: 'reset-password', element: <ResetPassword /> },
    { path: 'oauth2/redirect', element: <OAuth2RedirectHandler /> },
    { path: 'profile', element: <ProtectedRoute><CustomerProfilePage /></ProtectedRoute> },
    { path: 'change-password', element: <ProtectedRoute><ChangePassword /></ProtectedRoute> },
    { path: 'sessions', element: <ProtectedRoute><SessionsPage /></ProtectedRoute> }
];
