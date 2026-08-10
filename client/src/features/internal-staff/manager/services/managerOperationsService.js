import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data;

const withCinema = (cinemaPublicId, params = {}) => ({ cinemaPublicId, ...params });

const managerOperationsService = {
  getBookings: async (cinemaPublicId, params = {}) => unwrap(await apiClient.get(
    '/api/manager/bookings',
    { params: withCinema(cinemaPublicId, params) },
  )),

  getBookingSummary: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/bookings/summary',
    { params: { cinemaPublicId } },
  )),

  getBookingDetail: async (cinemaPublicId, bookingPublicId) => unwrap(await apiClient.get(
    `/api/manager/bookings/${bookingPublicId}`,
    { params: { cinemaPublicId } },
  )),

  getBookingFoods: async (cinemaPublicId, bookingPublicId) => unwrap(await apiClient.get(
    `/api/manager/bookings/${bookingPublicId}/foods`,
    { params: { cinemaPublicId } },
  )),

  cancelBookingHold: async (cinemaPublicId, bookingPublicId, reason) => unwrap(await apiClient.put(
    `/api/manager/bookings/${bookingPublicId}/cancel-hold`,
    { reason },
    { params: { cinemaPublicId } },
  )),

  getPayments: async (cinemaPublicId, params = {}) => unwrap(await apiClient.get(
    '/api/manager/payments',
    { params: withCinema(cinemaPublicId, params) },
  )),

  getPaymentSummary: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/payments/summary',
    { params: { cinemaPublicId } },
  )),

  getPaymentDetail: async (cinemaPublicId, paymentPublicId) => unwrap(await apiClient.get(
    `/api/manager/payments/${paymentPublicId}`,
    { params: { cinemaPublicId } },
  )),

  getRefundRequests: async (cinemaPublicId, params = {}) => unwrap(await apiClient.get(
    '/api/manager/payments/refund-requests',
    { params: withCinema(cinemaPublicId, params) },
  )),

  approveRefund: async (cinemaPublicId, refundPublicId, note) => unwrap(await apiClient.post(
    `/api/manager/payments/refund-requests/${refundPublicId}/approve`,
    { note },
    { params: { cinemaPublicId } },
  )),

  rejectRefund: async (cinemaPublicId, refundPublicId, note) => unwrap(await apiClient.post(
    `/api/manager/payments/refund-requests/${refundPublicId}/reject`,
    { note },
    { params: { cinemaPublicId } },
  )),
};

export default managerOperationsService;
