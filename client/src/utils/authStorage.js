let pendingAccountId = null;

export const setPendingAccountId = (id) => {
    pendingAccountId = id;
};

export const getPendingAccountId = () => {
    return pendingAccountId;
};

export const clearPendingAccountId = () => {
    pendingAccountId = null;
};

export const setAuthData = (data) => {
    if (!data) return;
    localStorage.setItem("authToken", data.accessToken || data.token || "");
    localStorage.setItem("refreshToken", data.refreshToken || "");
    localStorage.setItem("tokenType", data.tokenType || "Bearer");
    localStorage.setItem("userEmail", data.email || "");
    localStorage.setItem("userRole", data.role || "");
};

export const getAuthToken = () => {
    return localStorage.getItem("authToken");
};

export const getRefreshToken = () => {
    return localStorage.getItem("refreshToken");
};

export const getUserRole = () => {
    return localStorage.getItem("userRole");
};

export const getUserEmail = () => {
    return localStorage.getItem("userEmail");
};

export const clearAuthData = () => {
    localStorage.removeItem("authToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("tokenType");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userRole");
};

export const isAuthenticated = () => {
    return !!localStorage.getItem("authToken");
};
