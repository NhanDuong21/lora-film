import apiClient from "@/services/apiClient";

const uuidv4 = () => {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

/**
 * Create a new booking from active seat reservations
 * @param {Object} data - Create booking request
 * @param {string} data.showtimePublicId - Showtime UUID
 * @param {Array<string>} data.seatPublicIds - Canonical public seat UUIDs
 * @param {string} [data.idempotencyKey] - UUID idempotency key for this logical booking request
 * @returns {Promise<Object>} The booking response
 */
export const createBooking = async ({ showtimePublicId, seatPublicIds, reservationPublicIds, idempotencyKey = uuidv4() }) => {
  const payload = { showtimePublicId };
  if (Array.isArray(seatPublicIds) && seatPublicIds.length > 0) payload.seatPublicIds = seatPublicIds;
  if (Array.isArray(reservationPublicIds) && reservationPublicIds.length > 0) payload.reservationPublicIds = reservationPublicIds;
  const response = await apiClient.post("/api/bookings", {
    ...payload
  }, {
    headers: { "Idempotency-Key": idempotencyKey }
  });
  return response.data.data;
};

export const finalizeCheckout = async (bookingId) => {
  const response = await apiClient.post(`/api/bookings/${bookingId}/finalize-checkout`);
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
 * Get tickets issued for a confirmed booking.
 * @param {string} bookingId - Booking UUID
 * @returns {Promise<Array>} Ticket list
 */
export const getBookingTickets = async (bookingId) => {
  const response = await apiClient.get(`/api/bookings/${bookingId}/tickets`);
  return response.data.data || [];
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
  const idempotencyKey = uuidv4();
  const response = await apiClient.delete(`/api/bookings/${bookingId}`, {
    data: {
      reasonCode: "USER_CANCEL",
      reasonDetail: reason
    },
    headers: { "Idempotency-Key": idempotencyKey }
  });
  return response.data.data;
};

/**
 * Initiate payment for a pending booking
 * @param {string} bookingId - Booking UUID
 * @param {Object} payload - Payment payload
 * @param {string} payload.paymentMethod - e.g., "MOMO", "VNPAY"
 * @param {string} [payload.paymentProvider] - Payment provider
 * @returns {Promise<Object>} Payment response containing paymentUrl
 */
// Payment provider initiation is intentionally owned by Payment Service.
export const initiatePayment = async () => {
  const error = new Error("PAYMENT_SERVICE_HANDOFF_REQUIRED");
  error.code = "PAYMENT_SERVICE_HANDOFF_REQUIRED";
  throw error;
};
