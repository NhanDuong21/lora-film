/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';
import { PageLoader, ProtectedRoute } from '@/components/common/RouteGuards';

const Login = lazy(() => import('@/features/auth/pages/Login'));
const Register = lazy(() => import('@/features/auth/pages/Register'));
const VerifyOtp = lazy(() => import('@/features/auth/pages/VerifyOtp'));
const AccountCenterPage = lazy(() => import('@/features/auth/pages/AccountCenterPage'));
const ForgotPassword = lazy(() => import('@/features/auth/pages/ForgotPassword'));
const ResetPassword = lazy(() => import('@/features/auth/pages/ResetPassword'));
const OAuth2RedirectHandler = lazy(() => import('@/features/auth/pages/OAuth2RedirectHandler'));

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
    { path: 'profile', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/tickets', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/offers', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/loyalty', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/notifications', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/profile', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/security', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/security/email', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/security/password', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/security/devices', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'account/help', element: <ProtectedRoute>{lazyPage(<AccountCenterPage />)}</ProtectedRoute> },
    { path: 'change-password', element: <ProtectedRoute><Navigate to="/account/security/password" replace /></ProtectedRoute> },
    { path: 'change-email', element: <ProtectedRoute><Navigate to="/account/security/email" replace /></ProtectedRoute> },
    { path: 'sessions', element: <ProtectedRoute><Navigate to="/account/security/devices" replace /></ProtectedRoute> }
];
