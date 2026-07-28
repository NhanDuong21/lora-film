import { useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { hasPermissionAccess } from '../permissionAccess';

export default function useAdminAccess() {
  const { user, userRole } = useAuth();
  const role = userRole || user?.role;
  const permissions = user?.permissions;

  return useCallback(
    (...requiredPermissions) =>
      hasPermissionAccess(role, permissions || [], ...requiredPermissions),
    [role, permissions]
  );
}
