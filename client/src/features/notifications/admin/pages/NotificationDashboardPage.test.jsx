import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NotificationDashboardPage from './NotificationDashboardPage';
import { notificationAdminService } from '../services/notificationAdminService';

vi.mock('../services/notificationAdminService', () => ({
    notificationAdminService: {
        dashboard: vi.fn(),
    },
}));

describe('NotificationDashboardPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders delivery telemetry and Git registry provenance returned by the API', async () => {
        notificationAdminService.dashboard.mockResolvedValue({
            totalRequests: 12,
            totalDeliveries: 18,
            accepted: 15,
            confirmed: 4,
            delivered: 15,
            failed: 1,
            pending: 2,
            deadLetters: 0,
            deliveryRate: 83.3,
            deliveryStatuses: {
                DELIVERED: 15,
                RETRY_SCHEDULED: 2,
                FAILED: 1,
            },
            templateRegistry: {
                available: true,
                provider: 'JGit',
                branch: 'main',
                repository: 'NhanDuong21/template-mail',
                headCommit: '1234567890abcdef',
                remoteHeadCommit: 'abcdef1234567890',
                lastSyncedAt: '2026-08-18T01:00:00Z',
            },
            coverage: {
                totalRequirements: 5,
                readyRequirements: 4,
                blockedRequirements: 1,
                items: [{
                    templateKey: 'REGISTER_OTP',
                    sourceService: 'auth-service',
                    eventTypes: ['REGISTER_OTP'],
                    channels: ['EMAIL'],
                    locale: 'vi-VN',
                    readiness: 'BLOCKED',
                }],
            },
        });

        render(
            <MemoryRouter>
                <NotificationDashboardPage />
            </MemoryRouter>,
        );

        expect(await screen.findByRole('heading', {
            name: 'Trung tâm điều phối thông báo',
        })).toBeInTheDocument();
        expect(screen.getByText('83.3%')).toBeInTheDocument();
        expect(screen.getByText('1234567890')).toBeInTheDocument();
        expect(screen.getByText('Đang thử lại')).toBeInTheDocument();
        expect(screen.getByText('REGISTER_OTP chưa có template đang hoạt động'))
            .toBeInTheDocument();
        expect(notificationAdminService.dashboard).toHaveBeenCalledWith({
            hours: 24,
            includeTest: false,
        });
    });

    it('shows a recoverable error state when the operations API is unavailable', async () => {
        notificationAdminService.dashboard.mockRejectedValue(
            new Error('notification API unavailable'),
        );

        render(
            <MemoryRouter>
                <NotificationDashboardPage />
            </MemoryRouter>,
        );

        expect(await screen.findByText('Không thể tải dữ liệu thông báo'))
            .toBeInTheDocument();
        expect(screen.getByText('notification API unavailable')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument();
    });
});
