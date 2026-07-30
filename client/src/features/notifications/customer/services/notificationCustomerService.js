import apiClient from '@/services/apiClient';

const baseUrl = '/api/v1/notifications';
const unwrap = response => response?.data?.data;

export const NOTIFICATIONS_CHANGED_EVENT = 'lorafilm:notifications-changed';

export const notificationCustomerService = {
  async list({ page = 0, size = 20 } = {}) {
    return unwrap(await apiClient.get(baseUrl, { params: { page, size } }));
  },

  async unreadCount() {
    return Number(unwrap(await apiClient.get(`${baseUrl}/unread-count`))?.count || 0);
  },

  async markRead(publicId) {
    return unwrap(await apiClient.patch(`${baseUrl}/${publicId}/read`));
  },

  async markAllRead() {
    return Number(unwrap(await apiClient.patch(`${baseUrl}/read-all`))?.count || 0);
  }
};

export const announceNotificationChange = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(NOTIFICATIONS_CHANGED_EVENT));
  }
};
