import AdminPricingPolicyDetailPage from './pages/AdminPricingPolicyDetailPage';
import AdminPricingPolicyFormPage from './pages/AdminPricingPolicyFormPage';
import AdminPricingPolicyListPage from './pages/AdminPricingPolicyListPage';
import AdminShowtimePricingPage from './pages/AdminShowtimePricingPage';

export const adminPricingRoutes = [
  { path: 'pricing', element: <AdminPricingPolicyListPage /> },
  { path: 'pricing/create', element: <AdminPricingPolicyFormPage /> },
  { path: 'pricing/:id/edit', element: <AdminPricingPolicyFormPage /> },
  { path: 'pricing/:id', element: <AdminPricingPolicyDetailPage /> },
  { path: 'showtimes/:id/pricing', element: <AdminShowtimePricingPage /> },
];
