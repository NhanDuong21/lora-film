import AdminConcessionInventory from './pages/AdminConcessionInventoryPage';
import AdminConcessionSalesPage from './pages/AdminConcessionSalesPage';

export const adminConcessionRoutes = [
    { path: 'concessions', element: <AdminConcessionInventory /> },
    { path: 'concession-sales', element: <AdminConcessionSalesPage /> }
];
