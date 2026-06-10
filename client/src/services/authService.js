import axios from "axios";

// Dynamically resolve the API base URL. Use relative URL in development to route through Vite's proxy and bypass CORS.
const API_BASE_URL = import.meta.env.DEV 
    ? "" 
    : (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080");

/**
 * Authenticates a user by sending credentials to the API Gateway.
 * 
 * @param {string} email - User email address
 * @param {string} password - User password
 * @returns {Promise<object>} Response data containing status and payload
 */
export const login = async (email, password) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/api/auth/login`, {
            email,
            password
        });
        return response.data;
    } catch (error) {
        // Propagate the specific error details for user-friendly handling
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Network error occurred. Please try again later.");
    }
};
