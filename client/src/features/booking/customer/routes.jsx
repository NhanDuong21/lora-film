import MasterBookingFunnelPage from '@/features/booking/customer/pages/MasterBookingFunnelPage';
import SeatSelectionPage from '@/features/booking/customer/pages/SeatSelectionPage';

export const customerBookingRoutes = [
    { path: '/booking', element: <MasterBookingFunnelPage /> },
    { path: '/seat-selection', element: <SeatSelectionPage /> }
];
