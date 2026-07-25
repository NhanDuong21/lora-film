<<<<<<< HEAD
/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader, ProtectedRoute } from '@/components/common/RouteGuards';

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
=======
import MasterBookingFunnelPage from '@/features/booking/customer/pages/MasterBookingFunnelPage';
import SeatSelectionPage from '@/features/booking/customer/pages/SeatSelectionPage';
import BookingCheckoutPage from '@/features/booking/customer/pages/BookingCheckoutPage';
import BookingSuccessPage from '@/features/booking/customer/pages/BookingSuccessPage';
import BookingHistoryPage from '@/features/booking/customer/pages/BookingHistoryPage';
import BookingDetailPage from '@/features/booking/customer/pages/BookingDetailPage';
import { ProtectedRoute } from '@/components/common/RouteGuards';

export const customerBookingRoutes = [
    { path: '/booking', element: <MasterBookingFunnelPage /> },
>>>>>>> d0d21e568889d2198123b2a6aed226977d8da0bc
    {
        path: '/seat-selection',
        element: (
            <ProtectedRoute>
<<<<<<< HEAD
                {lazyPage(SeatSelectionPage)}
            </ProtectedRoute>
        )
    },
    { path: '/bookings/checkout', element: lazyPage(BookingCheckoutPage) },
    { path: '/bookings/success', element: lazyPage(BookingSuccessPage) },
    { path: '/bookings', element: lazyPage(BookingHistoryPage) },
    { path: '/bookings/history', element: lazyPage(BookingHistoryPage) },
    { path: '/bookings/:bookingId', element: lazyPage(BookingDetailPage) }
=======
                <SeatSelectionPage />
            </ProtectedRoute>
        )
    },
    { path: '/bookings/checkout', element: <BookingCheckoutPage /> },
    { path: '/bookings/success', element: <BookingSuccessPage /> },
    { path: '/bookings', element: <BookingHistoryPage /> },
    { path: '/bookings/history', element: <BookingHistoryPage /> },
    { path: '/bookings/:bookingId', element: <BookingDetailPage /> }
>>>>>>> d0d21e568889d2198123b2a6aed226977d8da0bc
];
