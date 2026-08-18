import apiClient from '@/services/apiClient';
import queryCache from '@/utils/queryCache';
import { getUserAccountId } from '@/utils/authStorage';

const templateBase = '/api/v1/admin/notification-templates';
const operationBase = '/api/v1/admin/notifications';
const emailProviderBase = '/api/v1/admin/notification-settings/email-provider';
const unwrap = response => response?.data?.data;
const cacheNamespace = () => `notification-admin:${getUserAccountId() || 'anonymous'}`;
const paramsKey = params => Object.entries(params || {})
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${String(value)}`)
    .join('&');
const cachedGet = (name, url, params, options = {}) => queryCache.fetchQuery(
    `${cacheNamespace()}:${name}:${paramsKey(params)}`,
    async () => unwrap(await (params ? apiClient.get(url, { params }) : apiClient.get(url))),
    {
        staleTime: options.staleTime ?? 15_000,
        forceRefresh: options.forceRefresh ?? false,
        maxRetries: 0,
    },
);
const invalidate = (...resources) => resources.forEach(resource =>
    queryCache.invalidateQueries(`${cacheNamespace()}:${resource}`));

export const notificationAdminService = {
    async dashboard(params = {}, options = {}) {
        return cachedGet('dashboard', `${operationBase}/dashboard`, params, options);
    },
    async requests(params = {}) {
        return unwrap(await apiClient.get(operationBase, { params }));
    },
    async request(publicId) {
        return unwrap(await apiClient.get(`${operationBase}/${publicId}`));
    },
    async retryDelivery(deliveryPublicId) {
        const result = unwrap(await apiClient.post(`${operationBase}/deliveries/${deliveryPublicId}/retry`));
        invalidate('dashboard', 'requests', 'dead-letters');
        return result;
    },
    async deadLetters(params = {}) {
        return unwrap(await apiClient.get(`${operationBase}/dead-letters`, { params }));
    },
    async templates(params = {}, options = {}) {
        return cachedGet('templates', templateBase, params, { staleTime: 30_000, ...options });
    },
    async coverage(options = {}) {
        return cachedGet('coverage', `${templateBase}/coverage`, undefined, {
            staleTime: 30_000,
            ...options,
        });
    },
    async emailProvider(options = {}) {
        return cachedGet('email-provider', emailProviderBase, undefined, {
            staleTime: 30_000,
            ...options,
        });
    },
    async testEmailProvider(command) {
        return unwrap(await apiClient.post(`${emailProviderBase}/test`, command));
    },
    async updateEmailProvider(command) {
        const result = unwrap(await apiClient.put(emailProviderBase, command));
        invalidate('email-provider');
        return result;
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
        const result = unwrap(await apiClient.post(`${templateBase}/${templateKey}/publish`, {
            draftId,
            expectedCommitSha,
        }));
        invalidate('dashboard', 'templates', 'coverage');
        return result;
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
        const result = unwrap(await apiClient.post(`${templateBase}/${templateKey}/rollback`, {
            channel,
            locale,
            version,
        }));
        invalidate('dashboard', 'templates', 'coverage');
        return result;
    },
};
