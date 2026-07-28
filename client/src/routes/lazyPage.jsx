import { Suspense } from 'react';
import { PageLoader } from '@/components/common/RouteGuards';

export const lazyPage = (Page, props = {}) => (
  <Suspense fallback={<PageLoader />}>
    <Page {...props} />
  </Suspense>
);
