/* eslint-disable react-refresh/only-export-components */
/* eslint-disable react-hooks/set-state-in-effect */
import { createContext, useContext, useState, useEffect } from 'react';
import { 
  isAuthenticated as checkAuthenticated,
  getUserEmail,
  getUserRole,
  getUserAccountId,
  clearAuthData,
  setAuthData as saveAuthData
} from '../utils/authStorage';
import { getUserProfile } from '../services/userService';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => checkAuthenticated());
  const [userRole, setUserRole] = useState(() => getUserRole());
  const [user, setUser] = useState(() => {
    const authed = checkAuthenticated();
    if (authed) {
      const email = getUserEmail();
      const accountId = getUserAccountId();
      const role = getUserRole();
      return {
        id: accountId,
        email: email,
        role: role,
        fullName: email ? email.split('@')[0] : 'User',
        permissions: role === 'ADMIN' ? ['PERM_ROOT_ACCESS'] : []
      };
    }
    return null;
  });
  const [loading, setLoading] = useState(true);

  const fetchProfile = async () => {
    const authed = checkAuthenticated();
    if (authed) {
      const accountId = getUserAccountId();
      const role = getUserRole();
      if (accountId) {
        try {
          const profileRes = await getUserProfile(accountId);
          if (profileRes && profileRes.success && profileRes.data) {
            setUser(prevUser => ({
              ...prevUser,
              ...profileRes.data,
              role: role,
              permissions: role === 'ADMIN' ? ['PERM_ROOT_ACCESS'] : []
            }));
          }
        } catch (error) {
          console.error("Failed to load user profile in AuthProvider:", error);
        }
      }
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchProfile();
    
    const handleStorageChange = () => {
      const authed = checkAuthenticated();
      setIsAuthenticated(authed);
      const role = getUserRole();
      setUserRole(role);
      if (authed) {
        const email = getUserEmail();
        const accountId = getUserAccountId();
        setUser({
          id: accountId,
          email: email,
          role: role,
          fullName: email ? email.split('@')[0] : 'User',
          permissions: role === 'ADMIN' ? ['PERM_ROOT_ACCESS'] : []
        });
        fetchProfile();
      } else {
        setUser(null);
        setLoading(false);
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const login = (data) => {
    saveAuthData(data);
    setIsAuthenticated(true);
    setUserRole(data.role || "");
    const email = data.email || "";
    const role = data.role || "";
    setUser({
      id: data.accessToken ? getUserAccountId() : null,
      email: email,
      role: role,
      fullName: email ? email.split('@')[0] : 'User',
      permissions: role === 'ADMIN' ? ['PERM_ROOT_ACCESS'] : []
    });
    fetchProfile();
  };

  const logout = () => {
    clearAuthData();
    setIsAuthenticated(false);
    setUserRole(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ 
      user, 
      userRole, 
      isAuthenticated, 
      login, 
      logout,
      loading,
      refreshProfile: fetchProfile 
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
