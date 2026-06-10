import axios from "axios";

const API_BASE_URL = import.meta.env.DEV 
    ? "" 
    : (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080");

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
        throw new Error("Network error occurred. Please try again later.");
    }
};
