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
  && !pathname.startsWith('/employee')
  && !pathname.startsWith('/manager');

export const resolvePostLoginPath = ({ role, permissions = [], from }) => {
  const normalizedRole = String(role || '').replace(/^ROLE_/, '');

  // Managers own a separate, cinema-scoped workspace. Some manager permissions
  // overlap with finance/admin permissions, so role routing must win here.
  if (normalizedRole === 'MANAGER') {
    return '/manager';
  }

  if (hasAdminAreaAccess(role, permissions)) {
    return getAdminLandingPath(role, permissions);
  }

  if (normalizedRole === 'EMPLOYEE') {
    return '/employee';
  }

  if (!isCustomerSafeDestination(from?.pathname)) {
    return '/';
  }

  return `${from.pathname}${from.search || ''}${from.hash || ''}`;
};
