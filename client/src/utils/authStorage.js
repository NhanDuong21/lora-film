import { jwtDecode } from "jwt-decode";

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
    const token = data.accessToken || data.token || "";
    localStorage.setItem("authToken", token);
    localStorage.setItem("refreshToken", data.refreshToken || "");
    localStorage.setItem("tokenType", data.tokenType || "Bearer");
    localStorage.setItem("userEmail", data.email || "");
    localStorage.setItem("userRole", data.role || "");
    
    if (data.expiresIn) {
        localStorage.setItem("expiresIn", data.expiresIn.toString());
    }

    if (data.accountId) {
        localStorage.setItem("userAccountId", data.accountId.toString());
    } else if (token) {
        try {
            const decoded = jwtDecode(token);
            if (decoded && decoded.userId) {
                localStorage.setItem("userAccountId", decoded.userId.toString());
            }
        } catch {
            // Ignore parse errors safely
        }
    }
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

export const getUserAccountId = () => {
    return localStorage.getItem("userAccountId");
};

export const clearAuthData = () => {
    localStorage.removeItem("authToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("tokenType");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userAccountId");
    localStorage.removeItem("expiresIn");
};

export const isAuthenticated = () => {
    return !!localStorage.getItem("authToken");
};

// Consolidated Storage APIs required by the integration prompt
export const getAuthSession = () => {
    return {
        accessToken: getAuthToken(),
        refreshToken: getRefreshToken(),
        tokenType: localStorage.getItem("tokenType") || "Bearer",
        accountId: getUserAccountId(),
        email: getUserEmail(),
        role: getUserRole(),
        expiresIn: localStorage.getItem("expiresIn")
    };
};

export const saveAuthSession = (data) => {
    setAuthData(data);
};

export const updateAuthTokens = (accessToken, refreshToken) => {
    localStorage.setItem("authToken", accessToken);
    if (refreshToken) {
        localStorage.setItem("refreshToken", refreshToken);
    }
};

export const clearAuthSession = () => {
    clearAuthData();
};

export const getAccessToken = () => {
    return getAuthToken();
};
