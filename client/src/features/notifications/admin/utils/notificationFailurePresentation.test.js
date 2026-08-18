import { describe, expect, it } from 'vitest';
import { getNotificationFailurePresentation } from './notificationFailurePresentation';

describe('getNotificationFailurePresentation', () => {
    it('translates known SMTP failures into actionable Vietnamese', () => {
        expect(getNotificationFailurePresentation('SMTP_CONNECTION_FAILED')).toEqual({
            code: 'SMTP_CONNECTION_FAILED',
            title: 'Không thể kết nối máy chủ SMTP',
            description: expect.stringContaining('MAIL_HOST'),
        });
    });

    it('does not expose an untranslated provider message for unknown failures', () => {
        expect(getNotificationFailurePresentation('NEW_PROVIDER_FAILURE')).toEqual({
            code: 'NEW_PROVIDER_FAILURE',
            title: 'Gửi thông báo thất bại',
            description: expect.stringContaining('tra log notification-service'),
        });
    });

    it('explains Gmail policy rejection in Vietnamese', () => {
        expect(getNotificationFailurePresentation('SMTP_POLICY_REJECTED')).toEqual({
            code: 'SMTP_POLICY_REJECTED',
            title: 'Gmail chặn thư theo chính sách gửi mail',
            description: expect.stringContaining('chính sách chống spam'),
        });
    });
});
