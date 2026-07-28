import apiClient from "@/services/apiClient";
import { setAuthData, clearAuthData } from "@/utils/authStorage";

export const register = async (userData) => {
    const response = await apiClient.post(`/api/auth/register`, userData);
    return response.data;
};


export const verifyOtp = async (email, otpCode, purpose = "REGISTRATION") => {
    const response = await apiClient.post(`/api/auth/verify`, {
        email,
        otp: otpCode,
        purpose
    });
    return response.data;
};

export const login = async (email, password) => {
    const response = await apiClient.post(`/api/auth/login`, {
        email,
        password
    });
    return response.data;
};

export const resendOtp = async (email, purpose = "REGISTRATION") => {
    const response = await apiClient.post(`/api/auth/send-otp`, {
        email,
        purpose
    });
    return response.data;
};

export const refreshToken = async (tokenValue) => {
    try {
        const response = await apiClient.post(`/api/auth/refresh-token`, {
            refreshToken: tokenValue
        });
        const resData = response.data;
        if (resData && resData.success && resData.data) {
            setAuthData(resData.data);
        }
        return resData;
    } catch (error) {
        clearAuthData();
        window.location.href = "/login";
        throw error;
    }
};

export const forgotPassword = async (email) =>
    (await apiClient.post("/api/auth/forgot-password", { email })).data;

export const resetPassword = async (token, newPassword) =>
    (await apiClient.post("/api/auth/reset-password", { token, newPassword })).data;

export const changePassword = async (oldPassword, newPassword) =>
    (await apiClient.post("/api/auth/change-password", { oldPassword, newPassword })).data;

export const logout = async () => {
    try {
        await apiClient.post('/api/auth/logout');
    } finally {
        clearAuthData();
    }
};

export const getSessions = async () =>
    (await apiClient.get("/api/auth/sessions")).data?.data || [];

export const revokeSession = async (sessionId) =>
    (await apiClient.delete(`/api/auth/sessions/${encodeURIComponent(sessionId)}`)).data;

export const revokeAllSessions = async () =>
    (await apiClient.delete("/api/auth/sessions")).data;
