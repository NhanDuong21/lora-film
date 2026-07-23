import MasterBookingFunnelPage from '@/features/booking/customer/pages/MasterBookingFunnelPage';
import SeatSelectionPage from '@/features/booking/customer/pages/SeatSelectionPage';
import BookingCheckoutPage from '@/features/booking/customer/pages/BookingCheckoutPage';
import BookingSuccessPage from '@/features/booking/customer/pages/BookingSuccessPage';
import BookingHistoryPage from '@/features/booking/customer/pages/BookingHistoryPage';
import BookingDetailPage from '@/features/booking/customer/pages/BookingDetailPage';

export const customerBookingRoutes = [
    { path: '/booking', element: <MasterBookingFunnelPage /> },
    { path: '/seat-selection', element: <SeatSelectionPage /> },
    { path: '/bookings/checkout', element: <BookingCheckoutPage /> },
    { path: '/bookings/success', element: <BookingSuccessPage /> },
    { path: '/bookings', element: <BookingHistoryPage /> },
    { path: '/bookings/history', element: <BookingHistoryPage /> },
    { path: '/bookings/:bookingId', element: <BookingDetailPage /> }
];
