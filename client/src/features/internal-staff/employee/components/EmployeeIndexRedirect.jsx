import { Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { getEmployeeLandingPath } from '../employeeAccess';

export default function EmployeeIndexRedirect() {
  const { user, userRole } = useAuth();
  const destination = getEmployeeLandingPath(
    userRole || user?.role,
    user?.permissions || [],
  );

  return <Navigate to={destination} replace />;
}
