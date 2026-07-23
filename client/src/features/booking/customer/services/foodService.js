import apiClient from "@/services/apiClient";

/**
 * Fetch list of concessions / food & beverages available from catalog
 * @returns {Promise<Array>} List of concession items
 */
export const getConcessions = async () => {
  const response = await apiClient.get("/api/customer/concessions");
  return response.data.data;
};

/**
 * Get current food order for a booking
 * @param {string} bookingId - Booking UUID
 * @returns {Promise<Object>} The food order response
 */
export const getBookingFoodOrder = async (bookingId) => {
  const response = await apiClient.get(`/api/bookings/${bookingId}/foods`);
  return response.data.data;
};

/**
 * Add a food item to the booking
 * @param {string} bookingId - Booking UUID
 * @param {Object} data - Food item data
 * @param {number} data.productId - Concession product ID
 * @param {number} data.quantity - Selected quantity
 * @returns {Promise<Object>} The updated food order response
 */
export const addFoodItem = async (bookingId, { productId, quantity }) => {
  const response = await apiClient.post(`/api/bookings/${bookingId}/foods`, {
    productId,
    quantity
  });
  return response.data.data;
};

/**
 * Update the quantity of an existing food item in a booking
 * @param {string} bookingId - Booking UUID
 * @param {number} foodItemId - Internal ID of the food item in the order
 * @param {number} quantity - New quantity
 * @returns {Promise<Object>} The updated food order response
 */
export const updateFoodQuantity = async (bookingId, foodItemId, quantity) => {
  const response = await apiClient.put(`/api/bookings/${bookingId}/foods/${foodItemId}`, {
    quantity
  });
  return response.data.data;
};

/**
 * Remove a food item from a booking
 * @param {string} bookingId - Booking UUID
 * @param {number} foodItemId - Internal ID of the food item in the order
 * @returns {Promise<Void>} Empty response
 */
export const removeFoodItem = async (bookingId, foodItemId) => {
  await apiClient.delete(`/api/bookings/${bookingId}/foods/${foodItemId}`);
};
