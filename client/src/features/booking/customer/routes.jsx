/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader, ProtectedRoute } from '@/components/common/RouteGuards';

const MasterBookingFunnelPage = lazy(() => import('@/features/booking/customer/pages/MasterBookingFunnelPage'));
const SeatSelectionPage = lazy(() => import('@/features/booking/customer/pages/SeatSelectionPage'));
const BookingCheckoutPage = lazy(() => import('@/features/booking/customer/pages/BookingCheckoutPage'));
const BookingSuccessPage = lazy(() => import('@/features/booking/customer/pages/BookingSuccessPage'));
const BookingFailedPage = lazy(() => import('@/features/booking/customer/pages/BookingFailedPage'));
const BookingHistoryPage = lazy(() => import('@/features/booking/customer/pages/BookingHistoryPage'));
const BookingDetailPage = lazy(() => import('@/features/booking/customer/pages/BookingDetailPage'));

const lazyPage = (Page) => (
    <Suspense fallback={<PageLoader />}>
        <Page />
    </Suspense>
);

const protectedPage = Page => (
    <ProtectedRoute>
        {lazyPage(Page)}
    </ProtectedRoute>
);

export const customerBookingRoutes = [
    { path: '/booking', element: lazyPage(MasterBookingFunnelPage) },
    {
        path: '/seat-selection',
        element: (
            <ProtectedRoute>
                {lazyPage(SeatSelectionPage)}
            </ProtectedRoute>
        )
    },
    { path: '/bookings/checkout', element: protectedPage(BookingCheckoutPage) },
    { path: '/bookings/success', element: protectedPage(BookingSuccessPage) },
    { path: '/bookings/failed', element: protectedPage(BookingFailedPage) },
    { path: '/bookings', element: protectedPage(BookingHistoryPage) },
    { path: '/bookings/history', element: protectedPage(BookingHistoryPage) },
    { path: '/bookings/:bookingId', element: protectedPage(BookingDetailPage) }
];
