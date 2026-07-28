/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader, ProtectedRoute } from '@/components/common/RouteGuards';

const PaymentReturnPage = lazy(() => import('./customer/pages/PaymentReturnPage'));
const EmployeeCashPaymentPage = lazy(() => import('./employee/pages/EmployeeCashPaymentPage'));
const AdminPaymentsPage = lazy(() => import('./admin/pages/AdminPaymentsPage'));
const AdminPaymentDetailPage = lazy(() => import('./admin/pages/AdminPaymentDetailPage'));

const lazyPage = Page => <Suspense fallback={<PageLoader />}><Page /></Suspense>;
const mockPaymentEnabled =
  import.meta.env.DEV && import.meta.env.VITE_PAYMENT_MOCK_ENABLED === 'true';
const MockPaymentPage = mockPaymentEnabled
  ? lazy(() => import('./customer/pages/MockPaymentPage'))
  : null;

export const customerPaymentRoutes = [
  {
    path: '/payments/return',
    element: <ProtectedRoute>{lazyPage(PaymentReturnPage)}</ProtectedRoute>,
  },
  ...(mockPaymentEnabled
    ? [{
        path: '/payments/mock/:paymentPublicId',
        element: <ProtectedRoute>{lazyPage(MockPaymentPage)}</ProtectedRoute>,
      }]
    : []),
];

export const employeePaymentRoutes = [
  { path: 'payments/cash', element: lazyPage(EmployeeCashPaymentPage) },
];

export const adminPaymentRoutes = [
  { path: 'payments', element: lazyPage(AdminPaymentsPage) },
  { path: 'payments/:paymentPublicId', element: lazyPage(AdminPaymentDetailPage) },
];
