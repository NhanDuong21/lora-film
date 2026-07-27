import AdminScoreViewerPage from '@/features/score/admin/pages/AdminScoreViewerPage';
import AdminMembershipTiersPage from '@/features/score/admin/pages/AdminMembershipTiersPage';

export const adminScoreRoutes = [
  {
    path: 'scores/viewer',
    element: <AdminScoreViewerPage />
  },
  {
    path: 'scores/tiers',
    element: <AdminMembershipTiersPage />
  }
];
