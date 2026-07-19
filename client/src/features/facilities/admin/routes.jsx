import AdminCinemaView from '@/features/facilities/admin/pages/AdminCinemaPage';
import AdminRoomPage from '@/features/facilities/admin/pages/AdminRoomPage';
import AdminRoomCreatePage from '@/features/facilities/admin/pages/AdminRoomCreatePage';
import AdminRoomEditPage from '@/features/facilities/admin/pages/AdminRoomEditPage';

export const adminFacilitiesRoutes = [
    { path: 'cinemas', element: <AdminCinemaView /> },
    { path: 'rooms', element: <AdminRoomPage /> },
    { path: 'rooms/create', element: <AdminRoomCreatePage /> },
    { path: 'rooms/edit/:roomId', element: <AdminRoomEditPage /> }
];
