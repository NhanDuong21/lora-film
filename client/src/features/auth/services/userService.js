import apiClient from "@/services/apiClient";

export const getUserProfile = async (accountId) => {
    const response = await apiClient.get(`/api/users/${accountId}`);
    return response.data.data;
};

export const getUserProfiles = async (accountIds = []) => {
    if (!accountIds.length) return [];
    const response = await apiClient.get('/api/users/admin/batch', {
        params: { accountIds: accountIds.join(',') }
    });
    return response.data.data || [];
};

export const searchUserProfiles = async (query, limit = 20) => {
    if (!query?.trim()) return [];
    const response = await apiClient.get('/api/users/admin/search', {
        params: { query: query.trim(), limit }
    });
    return response.data.data || [];
};
