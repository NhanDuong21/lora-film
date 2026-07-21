import AdminMovieView from '@/features/catalog/admin/pages/AdminMoviePage';
import AdminGenrePage from '@/features/catalog/admin/pages/AdminGenrePage';
import AdminMovieDetailPage from '@/features/catalog/admin/pages/AdminMovieDetailPage';

export const adminCatalogRoutes = [
    { path: 'movies', element: <AdminMovieView /> },
    { path: 'movies/:moviePublicId', element: <AdminMovieDetailPage /> },
    { path: 'genres', element: <AdminGenrePage triggerToast={(msg) => console.log('Toast:', msg)} /> }
];
