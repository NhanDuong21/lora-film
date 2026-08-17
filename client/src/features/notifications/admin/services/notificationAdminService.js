import apiClient from '@/services/apiClient';

const templateBase = '/api/v1/admin/notification-templates';
const operationBase = '/api/v1/admin/notifications';
const unwrap = response => response?.data?.data;

export const notificationAdminService = {
    async dashboard(params = {}) {
        return unwrap(await apiClient.get(`${operationBase}/dashboard`, { params }));
    },
    async requests(params = {}) {
        return unwrap(await apiClient.get(operationBase, { params }));
    },
    async request(publicId) {
        return unwrap(await apiClient.get(`${operationBase}/${publicId}`));
    },
    async retryDelivery(deliveryPublicId) {
        return unwrap(await apiClient.post(`${operationBase}/deliveries/${deliveryPublicId}/retry`));
    },
    async deadLetters(params = {}) {
        return unwrap(await apiClient.get(`${operationBase}/dead-letters`, { params }));
    },
    async templates(params = {}) {
        return unwrap(await apiClient.get(templateBase, { params }));
    },
    async coverage() {
        return unwrap(await apiClient.get(`${templateBase}/coverage`));
    },
    async published(templateKey, channel, locale) {
        return unwrap(await apiClient.get(`${templateBase}/${templateKey}`, {
            params: { channel, locale },
        }));
    },
    async createDraft(templateKey, content) {
        return unwrap(await apiClient.post(`${templateBase}/drafts`, { templateKey, content }));
    },
    async draft(templateKey, draftId) {
        return unwrap(await apiClient.get(`${templateBase}/${templateKey}/drafts/${draftId}`));
    },
    async updateDraft(templateKey, draftId, expectedCommitSha, changeSummary, content) {
        return unwrap(await apiClient.put(
            `${templateBase}/${templateKey}/drafts/${draftId}`,
            { changeSummary, content },
            { headers: { 'If-Match': `"${expectedCommitSha}"` } },
        ));
    },
    async deleteDraft(templateKey, draftId) {
        return apiClient.delete(`${templateBase}/${templateKey}/drafts/${draftId}`);
    },
    async validate(templateKey, draftId) {
        return unwrap(await apiClient.post(`${templateBase}/${templateKey}/validate`, null, {
            params: { draftId },
        }));
    },
    async preview(templateKey, draftId, sampleData) {
        return unwrap(await apiClient.post(`${templateBase}/${templateKey}/preview`, sampleData, {
            params: { draftId },
        }));
    },
    async previewPublished(templateKey, channel, locale, sampleData) {
        return unwrap(await apiClient.post(
            `${templateBase}/${templateKey}/preview-published`,
            sampleData,
            { params: { channel, locale } },
        ));
    },
    async publish(templateKey, draftId, expectedCommitSha) {
        return unwrap(await apiClient.post(`${templateBase}/${templateKey}/publish`, {
            draftId,
            expectedCommitSha,
        }));
    },
    async testSend(templateKey, command) {
        return unwrap(await apiClient.post(`${templateBase}/${templateKey}/test-send`, command));
    },
    async versions(templateKey, channel, locale) {
        return unwrap(await apiClient.get(`${templateBase}/${templateKey}/versions`, {
            params: { channel, locale },
        }));
    },
    async version(templateKey, version, channel, locale) {
        return unwrap(await apiClient.get(`${templateBase}/${templateKey}/versions/${version}`, {
            params: { channel, locale },
        }));
    },
    async diff(templateKey, from, to, channel, locale) {
        return unwrap(await apiClient.get(`${templateBase}/${templateKey}/versions/diff`, {
            params: { from, to, channel, locale },
        }));
    },
    async rollback(templateKey, channel, locale, version) {
        return unwrap(await apiClient.post(`${templateBase}/${templateKey}/rollback`, {
            channel,
            locale,
            version,
        }));
    },
};
