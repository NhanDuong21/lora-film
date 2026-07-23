import apiClient from "@/services/apiClient";

/**
 * Create a new booking from active seat reservations
 * @param {Object} data - Create booking request
 * @param {string} data.showtimePublicId - Showtime UUID
 * @param {Array<string>} data.reservationPublicIds - List of active reservation UUIDs
 * @returns {Promise<Object>} The booking response
 */
export const createBooking = async ({ showtimePublicId, reservationPublicIds }) => {
  const response = await apiClient.post("/api/bookings", {
    showtimePublicId,
    reservationPublicIds
  });
  return response.data.data;
};

/**
 * Get booking details by publicId
 * @param {string} bookingId - Booking UUID
 * @returns {Promise<Object>} Booking detail response
 */
export const getBookingDetails = async (bookingId) => {
  const response = await apiClient.get(`/api/bookings/${bookingId}`);
  return response.data.data;
};

/**
 * Find booking details by booking code
 * @param {string} bookingCode - The booking code string
 * @returns {Promise<Object>} Booking detail response
 */
export const getBookingByCode = async (bookingCode) => {
  const response = await apiClient.get(`/api/bookings/code/${bookingCode}`);
  return response.data.data;
};

/**
 * Get current user's bookings with pagination and status filters
 * @param {Object} params - Query params
 * @param {number} [params.page] - Page index (0-based)
 * @param {number} [params.size] - Page size
 * @param {string} [params.status] - BookingStatus filter
 * @param {string} [params.fromDate] - ISO start date
 * @param {string} [params.toDate] - ISO end date
 * @param {string} [params.sort] - Sort order (e.g. "createdAt,desc")
 * @returns {Promise<Object>} Paginated bookings list
 */
export const getBookingHistory = async ({ page = 0, size = 10, status, fromDate, toDate, sort = "createdAt,desc" }) => {
  const params = { page, size, sort };
  if (status) params.status = status;
  if (fromDate) params.fromDate = fromDate;
  if (toDate) params.toDate = toDate;

  const response = await apiClient.get("/api/bookings", { params });
  return response.data.data;
};

/**
 * Cancel a pending payment booking
 * @param {string} bookingId - Booking UUID
 * @param {string} [reason] - Reason for cancellation
 * @returns {Promise<Object>} Cancelled booking response
 */
export const cancelBooking = async (bookingId, reason = "") => {
  const response = await apiClient.delete(`/api/bookings/${bookingId}`, {
    data: { reason }
  });
  return response.data.data;
};

/**
 * Initiate payment for a pending booking
 * @param {string} bookingId - Booking UUID
 * @param {Object} payload - Payment payload
 * @param {string} payload.paymentMethod - e.g., "MOMO", "VNPAY"
 * @param {string} [payload.channel] - Channel description
 * @returns {Promise<Object>} Payment response containing paymentUrl
 */
export const initiatePayment = async (bookingId, { paymentMethod, channel = "Web Client" }) => {
  const response = await apiClient.post(`/api/bookings/${bookingId}/payment`, {
    paymentMethod,
    channel
  });
  return response.data.data;
};
