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

export const login = async (email, password, rememberMe = false) => {
    const response = await apiClient.post(`/api/auth/login`, {
        email,
        password,
        rememberMe
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

export const refreshToken = async (tokenValue, { redirectOnFailure = true } = {}) => {
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
        if (redirectOnFailure) {
            window.location.href = "/login";
        }
        throw error;
    }
};

export const forgotPassword = async (email) =>
    (await apiClient.post("/api/auth/forgot-password", { email })).data;

export const resetPassword = async (token, newPassword, email) =>
    (await apiClient.post("/api/auth/reset-password", {
        token,
        newPassword,
        ...(email ? { email } : {})
    })).data;

export const changePassword = async (oldPassword, newPassword) =>
    (await apiClient.post("/api/auth/change-password", { oldPassword, newPassword })).data;

export const requestChangeEmail = async (newEmail, password) =>
    (await apiClient.post("/api/auth/change-email/request", { newEmail, currentPassword: password })).data;

// Compatibility wrapper for the existing admin account screen. The backend owns
// the OTP verification workflow; callers receive the request response unchanged.
export const changeEmail = requestChangeEmail;

export const verifyChangeEmail = async (otp) =>
    (await apiClient.post("/api/auth/change-email/verify", { otp })).data;

export const getCurrentAccount = async () =>
    (await apiClient.get("/api/auth/me")).data?.data;

export const logout = async () => {
    try {
        await apiClient.post('/api/auth/logout');
    } finally {
        clearAuthData();
    }
};

export const logoutAll = async () => {
    try {
        await apiClient.post('/api/auth/logout-all');
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
