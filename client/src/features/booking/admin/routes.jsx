import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

const AdminBookingDashboardPage = lazy(() => import('@/features/booking/admin/pages/AdminBookingDashboardPage'));
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
