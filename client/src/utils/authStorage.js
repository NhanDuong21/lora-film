import { jwtDecode } from "jwt-decode";

const AUTH_KEYS = [
    "authToken",
    "refreshToken",
    "tokenType",
    "userEmail",
    "userRole",
    "userAccountId",
    "userPermissions",
    "expiresIn",
    "authRememberMe"
];

let pendingAccountId = null;

const readValue = (key) => sessionStorage.getItem(key) ?? localStorage.getItem(key);

const removeFromStorage = (storage) => {
    AUTH_KEYS.forEach((key) => storage.removeItem(key));
};

const decodeToken = (token) => {
    if (!token) return null;
    try {
        return jwtDecode(token);
    } catch {
        return null;
    }
};

export const setPendingAccountId = (id) => {
    pendingAccountId = id;
};

export const getPendingAccountId = () => pendingAccountId;

export const clearPendingAccountId = () => {
    pendingAccountId = null;
};

export const getRememberMe = () => readValue("authRememberMe") === "true";

export const setAuthData = (data) => {
    if (!data) return;

    const current = getAuthSession();
    const rememberMe = data.rememberMe ?? getRememberMe();
    const storage = rememberMe ? localStorage : sessionStorage;
    const secondaryStorage = rememberMe ? sessionStorage : localStorage;
    const token = data.accessToken || data.token || current.accessToken || "";
    const decoded = decodeToken(token);

    removeFromStorage(secondaryStorage);
    storage.setItem("authToken", token);
    storage.setItem("refreshToken", data.refreshToken || current.refreshToken || "");
    storage.setItem("tokenType", data.tokenType || current.tokenType || "Bearer");
    storage.setItem("userEmail", data.email || decoded?.sub || current.email || "");
    storage.setItem("userRole", data.role || decoded?.role || current.role || "");
    storage.setItem("authRememberMe", String(Boolean(rememberMe)));

    const permissions = data.permissions || decoded?.permissions || current.permissions || [];
    storage.setItem("userPermissions", JSON.stringify(Array.isArray(permissions) ? permissions : []));

    const accountId = data.accountId ?? decoded?.userId ?? current.accountId;
    if (accountId != null && accountId !== "") {
        storage.setItem("userAccountId", String(accountId));
    }

    const expiresIn = data.expiresIn ?? current.expiresIn;
    if (expiresIn != null && expiresIn !== "") {
        storage.setItem("expiresIn", String(expiresIn));
    }
};

export const getAuthToken = () => readValue("authToken");

export const getRefreshToken = () => readValue("refreshToken");

export const getUserRole = () => readValue("userRole");

export const getUserEmail = () => readValue("userEmail");

export const getUserAccountId = () => readValue("userAccountId");

export const getUserPermissions = () => {
    const decodedPermissions = decodeToken(getAuthToken())?.permissions;
    if (Array.isArray(decodedPermissions)) return decodedPermissions;
    try {
        const stored = JSON.parse(readValue("userPermissions") || "[]");
        return Array.isArray(stored) ? stored : [];
    } catch {
        return [];
    }
};

export const clearAuthData = () => {
    removeFromStorage(localStorage);
    removeFromStorage(sessionStorage);
};

export const isAuthenticated = () => {
    const decoded = decodeToken(getAuthToken());
    return Boolean(decoded?.exp && decoded.exp * 1000 > Date.now());
};

export const hasRefreshToken = () => Boolean(getRefreshToken());

export const getAuthSession = () => ({
    accessToken: getAuthToken(),
    refreshToken: getRefreshToken(),
    tokenType: readValue("tokenType") || "Bearer",
    accountId: getUserAccountId(),
    email: getUserEmail(),
    role: getUserRole(),
    permissions: getUserPermissions(),
    expiresIn: readValue("expiresIn"),
    rememberMe: getRememberMe()
});

export const saveAuthSession = (data) => {
    setAuthData(data);
};

export const updateAuthTokens = (accessToken, refreshToken) => {
    setAuthData({ accessToken, refreshToken });
};

export const clearAuthSession = () => {
    clearAuthData();
};

export const getAccessToken = () => getAuthToken();
