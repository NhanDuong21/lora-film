import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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

        expect(await screen.findByText('Không thể gửi OTP đăng ký tài khoản'))
            .toBeInTheDocument();
        expect(screen.getByText('Cấu hình 1')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Tạo mẫu còn thiếu' }))
            .toHaveAttribute('href', '/admin/notification-templates?contract=REGISTER_OTP');
    });

    it('explains SMTP failures in Vietnamese and keeps the code for diagnostics', async () => {
        notificationAdminService.requests.mockResolvedValue({
            content: [{
                publicId: 'request-1', eventType: 'AUTH_REGISTRATION_OTP', templateKey: 'REGISTER_OTP',
                sourceService: 'auth-service', status: 'PROCESSING', createdAt: '2026-08-18T06:21:00Z',
            }],
            totalPages: 1, number: 0, first: true, last: true,
        });
        notificationAdminService.request.mockResolvedValue({
            publicId: 'request-1', eventType: 'AUTH_REGISTRATION_OTP', templateKey: 'REGISTER_OTP',
            sourceService: 'auth-service', status: 'PROCESSING', locale: 'vi-VN',
            createdAt: '2026-08-18T06:21:00Z', updatedAt: '2026-08-18T06:23:00Z',
            deliveries: [{
                publicId: 'delivery-1', channel: 'EMAIL', provider: 'smtp', status: 'RETRY_SCHEDULED',
                failureCategory: 'TRANSIENT', failureCode: 'SMTP_SEND_FAILED',
                failureMessage: 'Email provider is temporarily unavailable', attemptCount: 3,
                nextRetryAt: '2026-08-18T06:34:00Z',
                attempts: [{
                    attemptNumber: 3, provider: 'smtp', outcome: 'FAILURE', failureCategory: 'TRANSIENT',
                    failureCode: 'SMTP_SEND_FAILED', durationMs: 2608, createdAt: '2026-08-18T06:23:52Z',
                }],
            }],
        });
        render(<MemoryRouter><NotificationOperationsPage mode="history" /></MemoryRouter>);

        fireEvent.click(await screen.findByText('OTP đăng ký tài khoản'));

        expect((await screen.findAllByText('Không thể gửi email qua SMTP')).length).toBeGreaterThan(0);
        expect(screen.getByText(/Email · lần thử 3: Không thể gửi email qua SMTP/)).toBeInTheDocument();
        expect(screen.getAllByText('Mã kỹ thuật: SMTP_SEND_FAILED').length).toBeGreaterThan(0);
        expect(screen.queryByText('Email provider is temporarily unavailable')).not.toBeInTheDocument();
    });
});
