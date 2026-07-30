import AdminScoreViewerPage from '@/features/score/admin/pages/AdminScoreViewerPage';
import AdminMembershipTiersPage from '@/features/score/admin/pages/AdminMembershipTiersPage';
import AdminScoreDashboardPage from '@/features/score/admin/pages/AdminScoreDashboardPage';
import AdminScoreAdjustmentsPage from '@/features/score/admin/pages/AdminScoreAdjustmentsPage';
import AdminScoreReconciliationPage from '@/features/score/admin/pages/AdminScoreReconciliationPage';
import AdminScoreAuditLogsPage from '@/features/score/admin/pages/AdminScoreAuditLogsPage';

export const adminScoreRoutes = [
  {
    path: 'scores/dashboard',
    element: <AdminScoreDashboardPage />
  },
  {
    path: 'scores/adjustments',
    element: <AdminScoreAdjustmentsPage />
  },
  {
    path: 'scores/reconciliation',
    element: <AdminScoreReconciliationPage />
  },
  {
    path: 'scores/audit-logs',
    element: <AdminScoreAuditLogsPage />
  },
  {
    path: 'scores/viewer',
    element: <AdminScoreViewerPage />
  },
  {
    path: 'scores/tiers',
    element: <AdminMembershipTiersPage />
  }
];
