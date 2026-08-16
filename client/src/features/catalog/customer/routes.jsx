import Home from '@/features/catalog/customer/pages/Home';
import MovieDiscoveryView from '@/features/catalog/customer/pages/MovieDiscoveryPage';
import MovieDetailPage from '@/features/catalog/customer/pages/MovieDetailPage';
import CustomerInformationPage from '@/features/catalog/customer/pages/CustomerInformationPage';

export const customerCatalogRoutes = [
    { path: '/', element: <Home /> },
    { path: '/home', element: <Home /> },
    { path: '/movies', element: <MovieDiscoveryView /> },
    { path: '/movies/:movieId', element: <MovieDetailPage /> },
    { path: '/movie/:movieId', element: <MovieDetailPage /> },
    { path: '/support/:topic', element: <CustomerInformationPage /> }
];
