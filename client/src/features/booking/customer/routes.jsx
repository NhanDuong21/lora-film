import MasterBookingFunnelPage from '@/features/booking/customer/pages/MasterBookingFunnelPage';
import SeatSelectionPage from '@/features/booking/customer/pages/SeatSelectionPage';
import BookingCheckoutPage from '@/features/booking/customer/pages/BookingCheckoutPage';
import BookingSuccessPage from '@/features/booking/customer/pages/BookingSuccessPage';
import BookingHistoryPage from '@/features/booking/customer/pages/BookingHistoryPage';
import BookingDetailPage from '@/features/booking/customer/pages/BookingDetailPage';
import { ProtectedRoute } from '@/components/common/RouteGuards';

export const customerBookingRoutes = [
    { path: '/booking', element: <MasterBookingFunnelPage /> },
    {
        path: '/seat-selection',
        element: (
            <ProtectedRoute>
                <SeatSelectionPage />
            </ProtectedRoute>
        )
    },
    { path: '/bookings/checkout', element: <BookingCheckoutPage /> },
    { path: '/bookings/success', element: <BookingSuccessPage /> },
    { path: '/bookings', element: <BookingHistoryPage /> },
    { path: '/bookings/history', element: <BookingHistoryPage /> },
    { path: '/bookings/:bookingId', element: <BookingDetailPage /> }
];
