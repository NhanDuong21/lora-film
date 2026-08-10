import AdminMovieView from '@/features/catalog/admin/pages/AdminMoviePage';
import AdminGenrePage from '@/features/catalog/admin/pages/AdminGenrePage';
import AdminMovieDetailPage from '@/features/catalog/admin/pages/AdminMovieDetailPage';
import AdminMovieOperationsPage from '@/features/movie-operations/admin/pages/AdminMovieOperationsPage';

export const adminCatalogRoutes = [
    { path: 'movie-operations', element: <AdminMovieOperationsPage /> },
    { path: 'movies', element: <AdminMovieView /> },
    { path: 'movies/:moviePublicId', element: <AdminMovieDetailPage /> },
    { path: 'genres', element: <AdminGenrePage /> }
];
