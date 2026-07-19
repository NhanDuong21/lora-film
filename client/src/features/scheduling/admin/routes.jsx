import AdminShowtimeView from '@/features/scheduling/admin/pages/AdminShowtimePage';
import AdminShowtimeCreatePage from '@/features/scheduling/admin/pages/AdminShowtimeCreatePage';

export const adminSchedulingRoutes = [
    { path: 'showtimes', element: <AdminShowtimeView /> },
    { path: 'showtimes/create', element: <AdminShowtimeCreatePage /> }
];
