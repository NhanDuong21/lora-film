/* eslint-disable react-refresh/only-export-components */
import { lazy, Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

const Dashboard = lazy(() => import('./pages/NotificationDashboardPage'));
const Templates = lazy(() => import('./pages/NotificationTemplateListPage'));
const Editor = lazy(() => import('./pages/NotificationTemplateEditorPage'));
const Operations = lazy(() => import('./pages/NotificationOperationsPage'));

const page = element => <Suspense fallback={<PageLoader />}>{element}</Suspense>;

export const adminNotificationRoutes = [
    { path: 'notifications', element: page(<Dashboard />) },
    { path: 'notification-templates', element: page(<Templates />) },
    { path: 'notification-templates/:templateKey', element: page(<Editor />) },
    { path: 'notification-operations', element: page(<Operations />) },
];
