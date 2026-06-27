import apiClient from "./apiClient";

export const getUserProfile = async (accountId) => {
    try {
        const response = await apiClient.get(`/api/users/${accountId}`);
        return response.data;
    } catch (error) {
        throw error;
    }
};
