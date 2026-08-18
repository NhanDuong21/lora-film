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

    it('never pre-fills the application password and validates before calling the backend', () => {
        render(<EmailProviderConfigurationPanel configuration={configuration} />);

        expect(screen.getByLabelText('Email người gửi')).toHaveValue('old@example.com');
        expect(screen.getByLabelText('Mật khẩu ứng dụng mới')).toHaveAttribute('type', 'password');
        expect(screen.getByLabelText('Mật khẩu ứng dụng mới')).toHaveValue('');

        fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra và áp dụng' }));

        expect(screen.getByText('Vui lòng nhập Mật khẩu ứng dụng hợp lệ của tài khoản email mới.'))
            .toBeInTheDocument();
        expect(notificationAdminService.updateEmailProvider).not.toHaveBeenCalled();
    });

    it('normalizes the application password, saves it once and clears it from the form', async () => {
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
        fireEvent.change(screen.getByLabelText('Mật khẩu ứng dụng mới'), {
            target: { value: 'abcd efgh ijkl mnop' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra và áp dụng' }));

        await waitFor(() => expect(notificationAdminService.updateEmailProvider)
            .toHaveBeenCalledWith({
                senderEmail: 'new@example.com',
                appPassword: 'abcdefghijklmnop',
                fromName: 'LoraFilm',
            }));
        expect(screen.getByLabelText('Mật khẩu ứng dụng mới')).toHaveValue('');
        expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ source: 'ADMIN' }));
    });

    it('shows a clear Vietnamese authentication error', async () => {
        notificationAdminService.testEmailProvider.mockRejectedValue({
            errorCode: 'SMTP_AUTHENTICATION_FAILED',
        });
        render(<EmailProviderConfigurationPanel configuration={configuration} />);

        fireEvent.change(screen.getByLabelText('Mật khẩu ứng dụng mới'), {
            target: { value: 'invalid-app-password' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra kết nối' }));

        expect(await screen.findByText(
            'Email hoặc Mật khẩu ứng dụng không hợp lệ. Gmail đã từ chối đăng nhập SMTP.',
        )).toBeInTheDocument();
    });
});
