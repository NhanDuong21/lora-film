import axios from "axios";
import { mockRegister } from "../data/mock/authMock";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

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
        throw new Error("Network error occurred. Please try again later.", { cause: error });
    }
};

export const register = async (userData) => {
    // Temporarily toggled to use mock register as backend endpoint refactoring is ongoing
    return mockRegister(userData);
};
