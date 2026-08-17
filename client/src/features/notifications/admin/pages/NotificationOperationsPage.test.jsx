import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NotificationOperationsPage from './NotificationOperationsPage';
import { notificationAdminService } from '../services/notificationAdminService';

vi.mock('../services/notificationAdminService', () => ({
    notificationAdminService: {
        requests: vi.fn(),
        dashboard: vi.fn(),
        deadLetters: vi.fn(),
        coverage: vi.fn(),
        request: vi.fn(),
        retryDelivery: vi.fn(),
    },
}));

describe('NotificationOperationsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        notificationAdminService.dashboard.mockResolvedValue({
            templateRegistry: { headCommit: 'active-revision' },
        });
        notificationAdminService.requests.mockResolvedValue({
            content: [], totalPages: 0, number: 0, first: true, last: true,
        });
        notificationAdminService.deadLetters.mockResolvedValue({
            content: [], totalElements: 0, totalPages: 0, number: 0, first: true, last: true,
        });
        notificationAdminService.coverage.mockResolvedValue({ items: [] });
    });

    it('defaults history to real operations in the last seven days', async () => {
        render(<MemoryRouter><NotificationOperationsPage mode="history" /></MemoryRouter>);

        expect(await screen.findByRole('heading', { name: 'Lịch sử gửi' })).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: 'Lọc dữ liệu thật hoặc gửi thử' }))
            .toHaveValue('false');
        expect(screen.getByRole('combobox', { name: 'Khoảng thời gian' })).toHaveValue('168');
        await waitFor(() => expect(notificationAdminService.requests).toHaveBeenCalledWith(
            expect.objectContaining({ test: false, from: expect.any(String) }),
        ));
    });

    it('shows blocked configuration beside exhausted deliveries in the action center', async () => {
        notificationAdminService.coverage.mockResolvedValue({
            items: [{
                templateKey: 'REGISTER_OTP', displayName: 'Xác thực đăng ký bằng OTP',
                sourceService: 'auth-service', eventTypes: ['AUTH_REGISTRATION_OTP'],
                channels: ['EMAIL'], locale: 'vi-VN', readiness: 'BLOCKED',
            }],
        });
        render(<MemoryRouter><NotificationOperationsPage mode="attention" /></MemoryRouter>);

        expect(await screen.findByText('REGISTER_OTP chưa có template đang hoạt động'))
            .toBeInTheDocument();
        expect(screen.getByText('Cấu hình 1')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Tạo template' }))
            .toHaveAttribute('href', '/admin/notification-templates?contract=REGISTER_OTP');
    });
});
