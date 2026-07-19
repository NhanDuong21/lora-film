import AdminShowtimeView from '@/features/scheduling/admin/pages/AdminShowtimePage';
import AdminShowtimeCreatePage from '@/features/scheduling/admin/pages/AdminShowtimeCreatePage';
import AdminAutoScheduleCreatePage from '@/features/scheduling/admin/pages/AdminAutoScheduleCreatePage';

export const adminSchedulingRoutes = [
    { path: 'showtimes', element: <AdminShowtimeView /> },
    { path: 'showtimes/create', element: <AdminShowtimeCreatePage /> },
    { path: 'showtime-schedules/create', element: <AdminAutoScheduleCreatePage /> }
];
