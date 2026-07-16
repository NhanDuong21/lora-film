import apiClient from "@/services/apiClient";

export const getUserProfile = async (accountId) => {
    const response = await apiClient.get(`/api/users/${accountId}`);
    return response.data.data;
};
