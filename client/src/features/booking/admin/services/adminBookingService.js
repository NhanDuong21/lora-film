import apiClient from "@/services/apiClient";

/**
 * Fetch and search bookings across the platform (Admin only)
 * @param {Object} filter - Filter parameters
 * @param {string} [filter.bookingCode] - Search by booking code
 * @param {number} [filter.userId] - Filter by user ID
 * @param {string} [filter.status] - Filter by BookingStatus enum
 * @param {string} [filter.fromDate] - Filter from ISO date
 * @param {string} [filter.toDate] - Filter to ISO date
 * @param {number} [filter.page] - Page number (0-based)
 * @param {number} [filter.size] - Page size
 * @returns {Promise<Object>} Paginated bookings list
 */
export const getBookings = async (filter = {}) => {
  const response = await apiClient.get("/api/admin/bookings", { params: filter });
  return response.data.data;
};

/**
 * Get full booking detail including snapshot and status histories (Admin only)
 * @param {string} publicId - Booking UUID
 * @returns {Promise<Object>} Booking detail response
 */
export const getBookingDetail = async (publicId) => {
  const response = await apiClient.get(`/api/admin/bookings/${publicId}`);
  return response.data.data;
};

/**
 * Update the status of a booking manually (Admin only)
 * @param {string} publicId - Booking UUID
 * @param {string} status - New target status
 * @param {string} [reason] - Transition reason
 * @returns {Promise<Object>} Updated booking response
 */
export const updateBookingStatus = async (publicId, status, reason = "Admin manual override") => {
  const response = await apiClient.put(`/api/admin/bookings/${publicId}/status`, {
    status,
    reason
  });
  return response.data.data;
};

/**
 * Get food order for a booking (Admin only)
 * @param {string} bookingId - Booking UUID (publicId)
 * @returns {Promise<Object>} Food order response
 */
export const getBookingFoods = async (bookingId) => {
  const response = await apiClient.get(`/api/admin/bookings/${bookingId}/foods`);
  return response.data.data;
};

/**
 * Get booking monitoring summary statistics (Admin only)
 * @returns {Promise<Object>} Monitoring summary response
 */
export const getBookingMonitoringSummary = async () => {
  const response = await apiClient.get("/api/admin/monitoring/summary");
  return response.data.data;
};
