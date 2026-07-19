import Home from '@/features/catalog/customer/pages/Home';
import MovieDiscoveryView from '@/features/catalog/customer/pages/MovieDiscoveryPage';
import MovieDetailPage from '@/features/catalog/customer/pages/MovieDetailPage';

export const customerCatalogRoutes = [
    { path: '/', element: <Home /> },
    { path: '/home', element: <Home /> },
    { path: '/movies', element: <MovieDiscoveryView /> },
    { path: '/movies/:movieId', element: <MovieDetailPage /> },
    { path: '/movie/:movieId', element: <MovieDetailPage /> }
];
