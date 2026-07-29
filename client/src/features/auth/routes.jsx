/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader, ProtectedRoute } from '@/components/common/RouteGuards';

const Login = lazy(() => import('@/features/auth/pages/Login'));
const Register = lazy(() => import('@/features/auth/pages/Register'));
const VerifyOtp = lazy(() => import('@/features/auth/pages/VerifyOtp'));
const CustomerProfilePage = lazy(() => import('@/features/auth/pages/CustomerProfilePage'));
const ForgotPassword = lazy(() => import('@/features/auth/pages/ForgotPassword'));
const ResetPassword = lazy(() => import('@/features/auth/pages/ResetPassword'));
const ChangePassword = lazy(() => import('@/features/auth/pages/ChangePassword'));
const SessionsPage = lazy(() => import('@/features/auth/pages/SessionsPage'));
const OAuth2RedirectHandler = lazy(() => import('@/features/auth/pages/OAuth2RedirectHandler'));
const ChangeEmail = lazy(() => import('@/features/auth/pages/ChangeEmail'));

const lazyPage = (element) => (
    <Suspense fallback={<PageLoader />}>
        {element}
    </Suspense>
);

export const authRoutes = [
    { path: 'login', element: lazyPage(<Login />) },
    { path: 'register', element: lazyPage(<Register />) },
    { path: 'verify-otp', element: lazyPage(<VerifyOtp />) },
    { path: 'forgot-password', element: lazyPage(<ForgotPassword />) },
    { path: 'reset-password', element: lazyPage(<ResetPassword />) },
    { path: 'oauth2/redirect', element: lazyPage(<OAuth2RedirectHandler />) },
    { path: 'profile', element: <ProtectedRoute>{lazyPage(<CustomerProfilePage />)}</ProtectedRoute> },
    { path: 'change-password', element: <ProtectedRoute>{lazyPage(<ChangePassword />)}</ProtectedRoute> },
    { path: 'change-email', element: <ProtectedRoute>{lazyPage(<ChangeEmail />)}</ProtectedRoute> },
    { path: 'sessions', element: <ProtectedRoute>{lazyPage(<SessionsPage />)}</ProtectedRoute> }
];
