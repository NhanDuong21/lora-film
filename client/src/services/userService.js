import axios from "axios";
import { getAuthToken } from "../utils/authStorage";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const getUserProfile = async (accountId) => {
    try {
        const token = getAuthToken();
        const response = await axios.get(`${API_BASE_URL}/api/users/${accountId}`, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            }
        });
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau.", { cause: error });
    }
};
