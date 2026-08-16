import Home from '@/features/catalog/customer/pages/Home';
import MovieDiscoveryView from '@/features/catalog/customer/pages/MovieDiscoveryPage';
import MovieDetailRoute from '@/features/catalog/customer/pages/MovieDetailRoute';
import CustomerInformationPage from '@/features/catalog/customer/pages/CustomerInformationPage';

export const customerCatalogRoutes = [
    { path: '/', element: <Home /> },
    { path: '/home', element: <Home /> },
    { path: '/movies', element: <MovieDiscoveryView /> },
    { path: '/movies/:movieId', element: <MovieDetailRoute /> },
    { path: '/movie/:movieId', element: <MovieDetailRoute /> },
    { path: '/support/:topic', element: <CustomerInformationPage /> }
];
