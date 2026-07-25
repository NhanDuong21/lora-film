import AdminBookingDashboardPage from '@/features/booking/admin/pages/AdminBookingDashboardPage';
import AdminBookingDetailPage from '@/features/booking/admin/pages/AdminBookingDetailPage';

export const adminBookingRoutes = [
    { path: 'bookings', element: <AdminBookingDashboardPage /> },
    { path: 'bookings/:bookingId', element: <AdminBookingDetailPage /> }
];
