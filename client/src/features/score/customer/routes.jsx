import React from 'react';
import LoyaltyCenterPage from '@/features/score/customer/pages/LoyaltyCenterPage';
import { ProtectedRoute } from '@/components/common/RouteGuards';

export const customerScoreRoutes = [
  {
    path: 'loyalty',
    element: (
      <ProtectedRoute>
        <LoyaltyCenterPage />
      </ProtectedRoute>
    )
  }
];
