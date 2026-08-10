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
                headCommit: '1234567890abcdef',
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
        expect(screen.getByText('Chờ gửi lại')).toBeInTheDocument();
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
