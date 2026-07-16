import axios from "axios";
import { getAuthToken, getRefreshToken, setAuthData, clearAuthData } from "../utils/authStorage";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        "Content-Type": "application/json"
    }
});

// Request Interceptor
apiClient.interceptors.request.use(
    (config) => {
        // Do not attach Authorization if it's CCCD or other external URLs
        if (config.url && !config.url.includes("/api/cccd")) {
            const token = getAuthToken();
            if (token) {
                config.headers["Authorization"] = `Bearer ${token}`;
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response Interceptor for handling token refresh
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        // Skip token refresh for auth endpoints
        const isAuthEndpoint = originalRequest.url && (
            originalRequest.url.includes("/api/auth/login") ||
            originalRequest.url.includes("/api/auth/register") ||
            originalRequest.url.includes("/api/auth/verify") ||
            originalRequest.url.includes("/api/auth/send-otp") ||
            originalRequest.url.includes("/api/auth/refresh-token")
        );

        if (error.response && error.response.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                .then((token) => {
                    originalRequest.headers["Authorization"] = `Bearer ${token}`;
                    return apiClient(originalRequest);
                })
                .catch((err) => {
                    return Promise.reject(err);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const refreshVal = getRefreshToken();
            if (!refreshVal) {
                clearAuthData();
                window.location.href = "/login";
                return Promise.reject(error);
            }

            try {
                // Call refresh-token API using raw axios to avoid interceptor recursion
                const response = await axios.post(`${API_BASE_URL}/api/auth/refresh-token`, {
                    refreshToken: refreshVal
                });

                if (response.data && response.data.success && response.data.data) {
                    const newTokens = response.data.data;
                    setAuthData(newTokens);
                    const newAccessToken = newTokens.accessToken;

                    apiClient.defaults.headers.common["Authorization"] = `Bearer ${newAccessToken}`;
                    originalRequest.headers["Authorization"] = `Bearer ${newAccessToken}`;

                    processQueue(null, newAccessToken);
                    isRefreshing = false;
                    return apiClient(originalRequest);
                } else {
                    throw new Error("Token refresh response did not return valid data");
                }
            } catch (refreshError) {
                processQueue(refreshError, null);
                isRefreshing = false;
                clearAuthData();
                window.location.href = "/login";
                return Promise.reject(refreshError);
            }
        }

        // Return standard error data if available
        if (error.response && error.response.data) {
            return Promise.reject(error.response.data);
        }
        return Promise.reject(error);
    }
);

export default apiClient;
