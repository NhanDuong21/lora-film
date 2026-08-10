import AdminCinemaView from '@/features/facilities/admin/pages/AdminCinemaPage';
import AdminCinemaDetailPage from '@/features/facilities/admin/pages/AdminCinemaDetailPage';
import AdminRoomPage from '@/features/facilities/admin/pages/AdminRoomPage';
import AdminRoomCreatePage from '@/features/facilities/admin/pages/AdminRoomCreatePage';
import AdminAuditoriumDetailPage from '@/features/facilities/admin/pages/AdminAuditoriumDetailPage';

import AdminSeatTypePage from '@/features/facilities/admin/pages/AdminSeatTypePage';

export const adminFacilitiesRoutes = [
    { path: 'cinemas', element: <AdminCinemaView /> },
    { path: 'cinemas/:cinemaPublicId', element: <AdminCinemaDetailPage /> },
    { path: 'seat-types', element: <AdminSeatTypePage /> },
    { path: 'rooms', element: <AdminRoomPage /> },
    { path: 'rooms/create', element: <AdminRoomCreatePage /> },
    { path: 'rooms/edit/:roomId', element: <AdminAuditoriumDetailPage /> }
];
