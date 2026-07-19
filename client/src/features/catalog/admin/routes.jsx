import AdminMovieView from '@/features/catalog/admin/pages/AdminMoviePage';
import AdminGenrePage from '@/features/catalog/admin/pages/AdminGenrePage';

export const adminCatalogRoutes = [
    { path: 'movies', element: <AdminMovieView /> },
    { path: 'genres', element: <AdminGenrePage triggerToast={(msg) => console.log('Toast:', msg)} /> }
];
