import { ProtectedRoute } from '@/components/common/RouteGuards';
import CustomerPromotionCenterPage from './pages/CustomerPromotionCenterPage';

export const customerPromotionRoutes = [
  {
    path: 'promotions',
    element: (
      <ProtectedRoute>
        <CustomerPromotionCenterPage />
      </ProtectedRoute>
    ),
  },
];
