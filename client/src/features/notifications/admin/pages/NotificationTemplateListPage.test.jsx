import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NotificationTemplateListPage from './NotificationTemplateListPage';
import { notificationAdminService } from '../services/notificationAdminService';

vi.mock('../services/notificationAdminService', () => ({
    notificationAdminService: {
        templates: vi.fn(),
        coverage: vi.fn(),
        createDraft: vi.fn(),
    },
}));

const publishedTemplate = {
    templateKey: 'LEGACY_NOTICE', displayName: 'Legacy notice', category: 'OPERATIONAL',
    channel: 'EMAIL', locale: 'vi-VN', status: 'PUBLISHED', commitSha: '1234567890abcdef',
};

describe('NotificationTemplateListPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        notificationAdminService.templates.mockResolvedValue([publishedTemplate]);
        notificationAdminService.coverage.mockResolvedValue({ items: [] });
    });

    it('treats a published template without a producer contract as unlinked, not warning', async () => {
        render(<MemoryRouter><NotificationTemplateListPage /></MemoryRouter>);

        expect(await screen.findByRole('heading', { name: 'Legacy notice' })).toBeInTheDocument();
        expect(screen.getByText('Chưa liên kết')).toBeInTheDocument();
        expect(screen.queryByText('Có cảnh báo')).not.toBeInTheDocument();
        expect(screen.getByText('Đã phát hành')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Tạo mẫu còn thiếu' }));
        expect(screen.getByText('Không có yêu cầu tích hợp đang thiếu template.'))
            .toBeInTheDocument();
    });

    it('prefills and locks technical fields when opened from a blocked contract', async () => {
        notificationAdminService.templates.mockResolvedValue([]);
        notificationAdminService.coverage.mockResolvedValue({
            items: [{
                templateKey: 'REGISTER_OTP', displayName: 'Xác thực đăng ký bằng OTP',
                sourceService: 'auth-service', eventTypes: ['AUTH_REGISTRATION_OTP'],
                channels: ['EMAIL'], locale: 'vi-VN', readiness: 'BLOCKED',
            }],
        });
        render(
            <MemoryRouter initialEntries={['/admin/notification-templates?contract=REGISTER_OTP']}>
                <NotificationTemplateListPage />
            </MemoryRouter>,
        );

        const code = await screen.findByLabelText('Mã template');
        expect(code).toHaveValue('REGISTER_OTP');
        expect(code).toHaveAttribute('readonly');
        expect(screen.getAllByLabelText('Kênh').find(control => control.disabled)).toBeDisabled();
        expect(screen.getAllByLabelText('Ngôn ngữ').find(control => control.disabled)).toBeDisabled();
    });
});
