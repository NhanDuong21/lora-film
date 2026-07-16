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
    const response = await apiClient.post(`/api/auth/resend-otp`, {
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
