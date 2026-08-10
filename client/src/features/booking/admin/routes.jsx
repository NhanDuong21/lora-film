import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

// Route modules intentionally export route data alongside lazy components.
// eslint-disable-next-line react-refresh/only-export-components
const AdminBookingDashboardPage = lazy(() => import('@/features/booking/admin/pages/AdminBookingDashboardPage'));
// eslint-disable-next-line react-refresh/only-export-components
const AdminBookingDetailPage = lazy(() => import('@/features/booking/admin/pages/AdminBookingDetailPage'));

const lazyPage = (Page) => (
    <Suspense fallback={<PageLoader />}>
        <Page />
    </Suspense>
);

export const adminBookingRoutes = [
    { path: 'bookings', element: lazyPage(AdminBookingDashboardPage) },
    { path: 'bookings/:bookingId', element: lazyPage(AdminBookingDetailPage) }
];
