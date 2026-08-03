import {
  getAdminLandingPath,
  hasAdminAreaAccess,
} from '@/features/internal-staff/admin/permissionAccess';

const BLOCKED_CUSTOMER_DESTINATIONS = new Set([
  '/login',
  '/401',
  '/403',
  '/500',
]);

const isCustomerSafeDestination = pathname => Boolean(pathname)
  && !BLOCKED_CUSTOMER_DESTINATIONS.has(pathname)
  && !pathname.startsWith('/admin')
  && !pathname.startsWith('/employee');

export const resolvePostLoginPath = ({ role, permissions = [], from }) => {
  if (hasAdminAreaAccess(role, permissions)) {
    return getAdminLandingPath(role, permissions);
  }

  const normalizedRole = String(role || '').replace(/^ROLE_/, '');
  if (normalizedRole === 'STAFF' || normalizedRole === 'EMPLOYEE') {
    return '/employee';
  }

  if (!isCustomerSafeDestination(from?.pathname)) {
    return '/';
  }

  return `${from.pathname}${from.search || ''}${from.hash || ''}`;
};
