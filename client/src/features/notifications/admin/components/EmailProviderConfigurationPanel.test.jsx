import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EmailProviderConfigurationPanel from './EmailProviderConfigurationPanel';
import { notificationAdminService } from '../services/notificationAdminService';

vi.mock('../services/notificationAdminService', () => ({
    notificationAdminService: {
        testEmailProvider: vi.fn(),
        updateEmailProvider: vi.fn(),
    },
}));

const configuration = {
    source: 'ENV',
    senderEmail: 'old@example.com',
    senderEmailMasked: 'o***@example.com',
    fromName: 'LoraFilm',
    smtpHost: 'smtp.gmail.com',
    smtpPort: 587,
    connectionStatus: 'CHƯA KIỂM TRA',
};

describe('EmailProviderConfigurationPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('never pre-fills the App Password and validates before calling the backend', () => {
        render(<EmailProviderConfigurationPanel configuration={configuration} />);

        expect(screen.getByLabelText('Email người gửi')).toHaveValue('old@example.com');
        expect(screen.getByLabelText('App Password mới')).toHaveAttribute('type', 'password');
        expect(screen.getByLabelText('App Password mới')).toHaveValue('');

        fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra và lưu' }));

        expect(screen.getByText('Vui lòng nhập App Password hợp lệ của tài khoản email mới.'))
            .toBeInTheDocument();
        expect(notificationAdminService.updateEmailProvider).not.toHaveBeenCalled();
    });

    it('normalizes the App Password, saves it once and clears it from the form', async () => {
        const onUpdated = vi.fn();
        notificationAdminService.updateEmailProvider.mockResolvedValue({
            ...configuration,
            source: 'ADMIN',
            senderEmail: 'new@example.com',
            senderEmailMasked: 'n***@example.com',
            connectionStatus: 'CONNECTED',
        });
        render(
            <EmailProviderConfigurationPanel
                configuration={configuration}
                onUpdated={onUpdated}
            />,
        );

        fireEvent.change(screen.getByLabelText('Email người gửi'), {
            target: { value: 'new@example.com' },
        });
        fireEvent.change(screen.getByLabelText('App Password mới'), {
            target: { value: 'abcd efgh ijkl mnop' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra và lưu' }));

        await waitFor(() => expect(notificationAdminService.updateEmailProvider)
            .toHaveBeenCalledWith({
                senderEmail: 'new@example.com',
                appPassword: 'abcdefghijklmnop',
                fromName: 'LoraFilm',
            }));
        expect(screen.getByLabelText('App Password mới')).toHaveValue('');
        expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ source: 'ADMIN' }));
    });

    it('shows a clear Vietnamese authentication error', async () => {
        notificationAdminService.testEmailProvider.mockRejectedValue({
            errorCode: 'SMTP_AUTHENTICATION_FAILED',
        });
        render(<EmailProviderConfigurationPanel configuration={configuration} />);

        fireEvent.change(screen.getByLabelText('App Password mới'), {
            target: { value: 'invalid-app-password' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra kết nối' }));

        expect(await screen.findByText(
            'Email hoặc App Password không hợp lệ. Gmail đã từ chối đăng nhập SMTP.',
        )).toBeInTheDocument();
    });
});
