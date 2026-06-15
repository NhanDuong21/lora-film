import axios from "axios";
import { setAuthData, clearAuthData } from "../utils/authStorage";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const register = async (userData) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/api/auth/register`, userData);
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.", { cause: error });
    }
};

export const verifyOtp = async (accountId, otpCode) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/api/auth/verify`, {
            accountId: Number(accountId),
            otp: otpCode
        });
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.", { cause: error });
    }
};

export const login = async (email, password) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/api/auth/login`, {
            email,
            password
        });
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.", { cause: error });
    }
};

export const refreshToken = async (tokenValue) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/api/auth/refresh-token`, {
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
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.", { cause: error });
    }
};
