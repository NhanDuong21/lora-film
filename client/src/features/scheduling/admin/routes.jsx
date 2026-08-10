import AdminShowtimeView from '@/features/scheduling/admin/pages/AdminShowtimePage';
import AdminShowtimeCreatePage from '@/features/scheduling/admin/pages/AdminShowtimeCreatePage';
import AdminShowtimeDetailPage from '@/features/scheduling/admin/pages/AdminShowtimeDetailPage';
import AdminAutoScheduleCreatePage from '@/features/scheduling/admin/pages/AdminAutoScheduleCreatePage';
import AdminAutoSchedulePreviewPage from '@/features/scheduling/admin/pages/AdminAutoSchedulePreviewPage';
import AdminAutoScheduleHistoryPage from '@/features/scheduling/admin/pages/AdminAutoScheduleHistoryPage';

export const adminSchedulingRoutes = [
    { path: 'showtimes', element: <AdminShowtimeView /> },
    { path: 'showtimes/create', element: <AdminShowtimeCreatePage /> },
    { path: 'showtimes/:id', element: <AdminShowtimeDetailPage /> },
    { path: 'showtime-schedules', element: <AdminAutoScheduleHistoryPage /> },
    { path: 'showtime-schedules/create', element: <AdminAutoScheduleCreatePage /> },
    { path: 'showtime-schedules/:id', element: <AdminAutoSchedulePreviewPage /> }
];
