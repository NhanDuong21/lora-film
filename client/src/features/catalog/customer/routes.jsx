import Home from '@/features/catalog/customer/pages/Home';
import MovieDiscoveryView from '@/features/catalog/customer/pages/MovieDiscoveryPage';
import MovieDetailRoute from '@/features/catalog/customer/pages/MovieDetailRoute';
import CustomerInformationPage from '@/features/catalog/customer/pages/CustomerInformationPage';
import PeopleDirectoryPage from '@/features/catalog/customer/pages/PeopleDirectoryPage';
import PersonDetailPage from '@/features/catalog/customer/pages/PersonDetailPage';

export const customerCatalogRoutes = [
    { path: '/', element: <Home /> },
    { path: '/home', element: <Home /> },
    { path: '/movies', element: <MovieDiscoveryView /> },
    { path: '/movies/:movieId', element: <MovieDetailRoute /> },
    { path: '/movie/:movieId', element: <MovieDetailRoute /> },
    { path: '/dien-vien', element: <PeopleDirectoryPage role="ACTOR" /> },
    { path: '/dao-dien', element: <PeopleDirectoryPage role="DIRECTOR" /> },
    { path: '/nghe-si/:personSlug', element: <PersonDetailPage /> },
    { path: '/support/:topic', element: <CustomerInformationPage /> }
];
