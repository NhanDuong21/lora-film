/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import { 
  isAuthenticated as checkAuthenticated,
  getUserEmail,
  getUserRole,
  getUserAccountId,
  getAuthToken,
  getRefreshToken,
  clearAuthData,
  setAuthData as saveAuthData
} from '@/utils/authStorage';
import { getUserProfile } from '@/features/auth/services/userService';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => checkAuthenticated());
  const [userRole, setUserRole] = useState(() => getUserRole());
  const [accountId, setAccountId] = useState(() => getUserAccountId());
  const [email, setEmail] = useState(() => getUserEmail());
  const [accessToken, setAccessToken] = useState(() => getAuthToken());
  const [refreshToken, setRefreshToken] = useState(() => getRefreshToken());
  
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profilePending, setProfilePending] = useState(false);
  const [profileError, setProfileError] = useState(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // loadProfile with bounded retry strategy for eventual consistency
  const loadProfile = useCallback(async function loadProfile(targetAccountId, retryCount = 0) {
    const accId = targetAccountId || getUserAccountId();
    if (!accId) {
      setProfileLoading(false);
      return null;
    }

    setProfileLoading(true);
    setProfilePending(false);
    setProfileError(null);

    try {
      const profileData = await getUserProfile(accId);
      if (profileData) {
        setProfile(profileData);
        setProfilePending(false);
        setProfileLoading(false);
        return profileData;
      }
      throw new Error('Profile response did not contain data');
    } catch (error) {
      const errorCode = error?.errorCode || error?.code || error?.error;
      const status = error?.status || error?.response?.status;
      const isUserNotFound = errorCode === "USER_NOT_FOUND" || status === 404;

      if (isUserNotFound && retryCount < 3) {
        // Wait 1000ms and retry
        await new Promise(resolve => setTimeout(resolve, 1000));
        return loadProfile(accId, retryCount + 1);
      } else {
        setProfileLoading(false);
        if (isUserNotFound) {
          setProfilePending(true);
        } else {
          setProfileError('Không thể tải hồ sơ. Vui lòng thử lại.');
        }
        setProfile(null);
      }
    }
    return null;
  }, []);

  const initializeAuth = useCallback(async () => {
    const authed = checkAuthenticated();
    setIsAuthenticated(authed);
    if (authed) {
      const storedAccountId = getUserAccountId();
      const storedRole = getUserRole();
      const storedEmail = getUserEmail();
      const storedToken = getAuthToken();
      const storedRefresh = getRefreshToken();

      setAccountId(storedAccountId);
      setUserRole(storedRole);
      setEmail(storedEmail);
      setAccessToken(storedToken);
      setRefreshToken(storedRefresh);

      if (storedAccountId) {
        // Fire and forget: don't block app initialization on profile fetch
        loadProfile(storedAccountId).catch(console.error);
      }
    }
    setIsInitializing(false);
  }, [loadProfile]);

  useEffect(() => {
    initializeAuth();
    
    const handleStorageChange = () => {
      const authed = checkAuthenticated();
      setIsAuthenticated(authed);
      if (authed) {
        setAccountId(getUserAccountId());
        setUserRole(getUserRole());
        setEmail(getUserEmail());
        setAccessToken(getAuthToken());
        setRefreshToken(getRefreshToken());
        loadProfile(getUserAccountId());
      } else {
        setAccountId(null);
        setUserRole(null);
        setEmail(null);
        setAccessToken(null);
        setRefreshToken(null);
        setProfile(null);
        setProfilePending(false);
        setProfileError(null);
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [initializeAuth, loadProfile]);

  const login = async (authSessionData) => {
    saveAuthData(authSessionData);
    
    setIsAuthenticated(true);
    const storedAccountId = authSessionData.accountId || getUserAccountId();
    const storedRole = authSessionData.role || getUserRole();
    const storedEmail = authSessionData.email || getUserEmail();
    const storedToken = authSessionData.accessToken || getAuthToken();
    const storedRefresh = authSessionData.refreshToken || getRefreshToken();

    setAccountId(storedAccountId);
    setUserRole(storedRole);
    setEmail(storedEmail);
    setAccessToken(storedToken);
    setRefreshToken(storedRefresh);

    if (storedAccountId) {
      // Fire and forget: don't block login transition on profile fetch
      loadProfile(storedAccountId).catch(console.error);
    }
  };

  const logout = () => {
    clearAuthData();
    setIsAuthenticated(false);
    setAccountId(null);
    setUserRole(null);
    setEmail(null);
    setAccessToken(null);
    setRefreshToken(null);
    setProfile(null);
    setProfilePending(false);
    setProfileError(null);
  };

  const updateUser = (updatedFields) => {
    setProfile(prev => {
      if (!prev) return updatedFields;
      return {
        ...prev,
        ...updatedFields
      };
    });
  };

  // Backwards compatible combined user object
  const user = useMemo(() => {
    if (!isAuthenticated) return null;
    return {
      id: accountId,
      email: email,
      role: userRole,
      fullName: profile?.fullName || email?.split('@')[0] || 'User',
      permissions: (userRole || '').replace(/^ROLE_/, '') === 'ADMIN'
        ? ['PERM_ROOT_ACCESS']
        : (userRole || '').replace(/^ROLE_/, '') === 'ACCOUNTANT'
          ? ['PERM_VIEW_FINANCE']
          : [],
      profilePending: profilePending,
      profileLoading: profileLoading,
      ...profile
    };
  }, [isAuthenticated, accountId, email, userRole, profile, profilePending, profileLoading]);

  return (
    <AuthContext.Provider value={{ 
      user, 
      userRole, 
      isAuthenticated,
      accessToken,
      refreshToken,
      accountId,
      email,
      profile,
      profileLoading,
      profilePending,
      profileError,
      isInitializing,
      loading: isInitializing, // legacy support for 'loading'
      login, 
      logout,
      updateUser,
      loadProfile,
      refreshProfile: () => loadProfile(accountId)
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
