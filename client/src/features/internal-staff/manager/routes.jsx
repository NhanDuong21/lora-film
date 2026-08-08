/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

const ManagerDashboardPage = lazy(() => import('./pages/ManagerDashboardPage'));
const ManagerShowtimesPage = lazy(() => import('./pages/ManagerShowtimesPage'));
const ManagerCinemaPage = lazy(() => import('./pages/ManagerCinemaPage'));

const lazyPage = element => <Suspense fallback={<PageLoader />}>{element}</Suspense>;

export const managerRoutes = [
  { index: true, element: lazyPage(<ManagerDashboardPage />) },
  { path: 'showtimes', element: lazyPage(<ManagerShowtimesPage />) },
  { path: 'cinema', element: lazyPage(<ManagerCinemaPage />) },
];
