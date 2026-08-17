import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import { notificationAdminService } from './notificationAdminService';

vi.mock('@/services/apiClient', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

describe('notificationAdminService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('loads production dashboard metrics in the requested time window', async () => {
        apiClient.get.mockResolvedValue({ data: { data: { totalRequests: 4 } } });

        await expect(notificationAdminService.dashboard({ hours: 168, includeTest: false }))
            .resolves.toEqual({ totalRequests: 4 });

        expect(apiClient.get).toHaveBeenCalledWith(
            '/api/v1/admin/notifications/dashboard',
            { params: { hours: 168, includeTest: false } },
        );
    });

    it('requests a sanitized preview for the published locale variant', async () => {
        apiClient.post.mockResolvedValue({ data: { data: { renderedHtml: '<p>Preview</p>' } } });

        await notificationAdminService.previewPublished(
            'ACCOUNT_LOCKED',
            'EMAIL',
            'vi-VN',
            { user_name: 'Nguyễn Văn An' },
        );

        expect(apiClient.post).toHaveBeenCalledWith(
            '/api/v1/admin/notification-templates/ACCOUNT_LOCKED/preview-published',
            { user_name: 'Nguyễn Văn An' },
            { params: { channel: 'EMAIL', locale: 'vi-VN' } },
        );
    });

    it('sends the expected Git commit as an If-Match header when saving a draft', async () => {
        apiClient.put.mockResolvedValue({
            data: { data: { commitSha: 'next-sha' } },
        });

        const result = await notificationAdminService.updateDraft(
            'TICKET_PURCHASED',
            'draft-42',
            'expected-sha',
            'Update email copy',
            { channel: 'EMAIL', locale: 'vi-VN' },
        );

        expect(result).toEqual({ commitSha: 'next-sha' });
        expect(apiClient.put).toHaveBeenCalledWith(
            '/api/v1/admin/notification-templates/TICKET_PURCHASED/drafts/draft-42',
            {
                changeSummary: 'Update email copy',
                content: { channel: 'EMAIL', locale: 'vi-VN' },
            },
            { headers: { 'If-Match': '"expected-sha"' } },
        );
    });

    it('requests a version diff using explicit channel and locale coordinates', async () => {
        apiClient.get.mockResolvedValue({
            data: { data: { changed: true } },
        });

        await expect(notificationAdminService.diff(
            'TICKET_PURCHASED',
            'v000001',
            'v000002',
            'EMAIL',
            'vi-VN',
        )).resolves.toEqual({ changed: true });

        expect(apiClient.get).toHaveBeenCalledWith(
            '/api/v1/admin/notification-templates/TICKET_PURCHASED/versions/diff',
            {
                params: {
                    from: 'v000001',
                    to: 'v000002',
                    channel: 'EMAIL',
                    locale: 'vi-VN',
                },
            },
        );
    });
});
