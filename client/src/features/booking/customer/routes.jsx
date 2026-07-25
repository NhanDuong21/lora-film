/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

const MasterBookingFunnelPage = lazy(() => import('@/features/booking/customer/pages/MasterBookingFunnelPage'));
const SeatSelectionPage = lazy(() => import('@/features/booking/customer/pages/SeatSelectionPage'));
const BookingCheckoutPage = lazy(() => import('@/features/booking/customer/pages/BookingCheckoutPage'));
const BookingSuccessPage = lazy(() => import('@/features/booking/customer/pages/BookingSuccessPage'));
const BookingHistoryPage = lazy(() => import('@/features/booking/customer/pages/BookingHistoryPage'));
const BookingDetailPage = lazy(() => import('@/features/booking/customer/pages/BookingDetailPage'));

const lazyPage = (Page) => (
    <Suspense fallback={<PageLoader />}>
        <Page />
    </Suspense>
);

export const customerBookingRoutes = [
    { path: '/booking', element: lazyPage(MasterBookingFunnelPage) },
    { path: '/seat-selection', element: lazyPage(SeatSelectionPage) },
    { path: '/bookings/checkout', element: lazyPage(BookingCheckoutPage) },
    { path: '/bookings/success', element: lazyPage(BookingSuccessPage) },
    { path: '/bookings', element: lazyPage(BookingHistoryPage) },
    { path: '/bookings/history', element: lazyPage(BookingHistoryPage) },
    { path: '/bookings/:bookingId', element: lazyPage(BookingDetailPage) }
];
