import apiClient from "@/services/apiClient";

/**
 * Request to hold specific seats for a showtime
 * @param {Object} data - Hold seat request payload
 * @param {number} data.showtimeId - Internal showtime database ID
 * @param {Array<number>} data.seatIds - List of internal seat database IDs
 * @param {string} [idempotencyKey] - Optional UUID to guarantee request idempotency
 * @returns {Promise<Object>} The hold seat response containing reservation public IDs and expiration
 */
export const holdSeats = async ({ showtimeId, seatIds }, idempotencyKey = "") => {
  const headers = {};
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }
  const response = await apiClient.post("/api/seat-reservations", {
    showtimeId,
    seatIds
  }, { headers });
  return response.data;
};

/**
 * Release previously held seats
 * @param {Object} data - Release seat request payload
 * @param {number} data.showtimeId - Internal showtime database ID
 * @param {Array<number>} data.seatIds - List of internal seat database IDs
 * @returns {Promise<Void>} Empty response
 */
export const releaseSeats = async ({ showtimeId, seatIds }) => {
  await apiClient.delete("/api/seat-reservations", {
    data: { showtimeId, seatIds }
  });
};

/**
 * Extend active seat reservation lease time
 * @param {string} reservationPublicId - Reservation public UUID
 * @returns {Promise<Object>} The extension response with new expiration time
 */
export const extendReservation = async (reservationPublicId) => {
  const response = await apiClient.post(`/api/seat-reservations/${reservationPublicId}/extend`);
  return response.data;
};
