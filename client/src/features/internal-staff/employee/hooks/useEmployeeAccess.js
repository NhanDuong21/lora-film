import { useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { hasEmployeeAccess } from '../employeeAccess';

export default function useEmployeeAccess() {
  const { user, userRole } = useAuth();
  const role = userRole || user?.role;
  const permissions = user?.permissions;

  return useCallback(
    (...requiredPermissions) => hasEmployeeAccess(role, permissions || [], requiredPermissions),
    [permissions, role],
  );
}
