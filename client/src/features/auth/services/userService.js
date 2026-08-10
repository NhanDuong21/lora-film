import apiClient from "@/services/apiClient";

export const getUserProfile = async (accountId) => {
    const response = await apiClient.get(`/api/users/${accountId}`);
    return response.data.data;
};

export const updateUserProfile = async (payload) => {
    const response = await apiClient.put("/api/users/profile", payload);
    return response.data.data;
};

export const uploadAvatar = async (file) => {
    const formData = new FormData();
    formData.append("file", file);
    const response = await apiClient.post("/api/users/profile/avatar", formData, {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    });
    return response.data.data;
};

export const deleteAvatar = async () => {
    await apiClient.delete("/api/users/profile/avatar");
};

export const getUserProfiles = async (accountIds = []) => {
    if (!accountIds.length) return [];
    const response = await apiClient.get('/api/users/admin/batch', {
        params: { accountIds: accountIds.join(',') }
    });
    return response.data.data || [];
};

export const getAccountDisplayNames = async (accountIds = []) => {
    if (!accountIds.length) return [];
    const response = await apiClient.get('/api/users/directory/display-names', {
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
