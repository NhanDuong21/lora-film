/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

const ManagerDashboardPage = lazy(() => import('./pages/ManagerDashboardPage'));
const ManagerShowtimesPage = lazy(() => import('./pages/ManagerShowtimesPage'));
const ManagerCinemaPage = lazy(() => import('./pages/ManagerCinemaPage'));
const ManagerRoomsPage = lazy(() => import('./pages/ManagerRoomsPage'));
const ManagerStaffPage = lazy(() => import('./pages/ManagerStaffPage'));
const ManagerReportsPage = lazy(() => import('./pages/ManagerReportsPage'));
const ManagerBookingsPage = lazy(() => import('./pages/ManagerBookingsPage'));
const ManagerPaymentsPage = lazy(() => import('./pages/ManagerPaymentsPage'));
const ManagerBookingDetailPage = lazy(() => import('./pages/ManagerBookingDetailPage'));
const ManagerPaymentDetailPage = lazy(() => import('./pages/ManagerPaymentDetailPage'));

const lazyPage = element => <Suspense fallback={<PageLoader />}>{element}</Suspense>;

export const managerRoutes = [
  { index: true, element: lazyPage(<ManagerDashboardPage />) },
  { path: 'showtimes', element: lazyPage(<ManagerShowtimesPage />) },
  { path: 'rooms', element: lazyPage(<ManagerRoomsPage />) },
  { path: 'bookings', element: lazyPage(<ManagerBookingsPage />) },
  { path: 'bookings/:bookingPublicId', element: lazyPage(<ManagerBookingDetailPage />) },
  { path: 'payments', element: lazyPage(<ManagerPaymentsPage />) },
  { path: 'payments/:paymentPublicId', element: lazyPage(<ManagerPaymentDetailPage />) },
  { path: 'staff', element: lazyPage(<ManagerStaffPage />) },
  { path: 'reports', element: lazyPage(<ManagerReportsPage />) },
  { path: 'cinema', element: lazyPage(<ManagerCinemaPage />) },
];
