import apiClient from "@/services/apiClient";
import {
  clearAllBookingCreationAttempts,
  clearBookingCreationAttempt,
} from "../utils/bookingCreationIdempotency";

export const BOOKING_CHANGED_EVENT = "lorafilm:booking-changed";
const BOOKING_SYNC_STORAGE_KEY = "lorafilm:booking-sync";
const BOOKING_SYNC_LISTENER_FLAG = "__lorafilmBookingSyncListenerAttached";
const CHECKOUT_PHASE_STORAGE_PREFIX = "lorafilm:checkout-phase:";

export const CHECKOUT_PHASES = Object.freeze({
  ADD_ONS: "ADD_ONS",
  PAYMENT: "PAYMENT",
});

const checkoutPhaseStorageKey = (bookingId) =>
  bookingId ? `${CHECKOUT_PHASE_STORAGE_PREFIX}${bookingId}` : null;

export const getStoredCheckoutPhase = (bookingId) => {
  const storageKey = checkoutPhaseStorageKey(bookingId);
  if (!storageKey || typeof window === "undefined") return null;
  try {
    const storedPhase = window.localStorage.getItem(storageKey);
    return Object.values(CHECKOUT_PHASES).includes(storedPhase)
      ? storedPhase
      : null;
  } catch {
    return null;
  }
};

export const storeCheckoutPhase = (bookingId, phase) => {
  const storageKey = checkoutPhaseStorageKey(bookingId);
  if (
    !storageKey ||
    typeof window === "undefined" ||
    !Object.values(CHECKOUT_PHASES).includes(phase)
  ) {
    return;
  }
  try {
    window.localStorage.setItem(storageKey, phase);
  } catch {
    // Checkout remains usable when browser storage is unavailable.
  }
};

export const clearStoredCheckoutPhase = (bookingId) => {
  const storageKey = checkoutPhaseStorageKey(bookingId);
  if (!storageKey || typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(storageKey);
  } catch {
    // Nothing else is required when browser storage is unavailable.
  }
};

const dispatchBookingChanged = (detail) => {
  window.dispatchEvent(new CustomEvent(BOOKING_CHANGED_EVENT, { detail }));
};

const emitBookingChanged = (detail) => {
  if (typeof window !== "undefined") {
    dispatchBookingChanged(detail);
    try {
      window.localStorage.setItem(
        BOOKING_SYNC_STORAGE_KEY,
        JSON.stringify({ detail, emittedAt: Date.now() }),
      );
    } catch {
      // Cross-tab sync is progressive enhancement when storage is unavailable.
    }
  }
};

if (typeof window !== "undefined" && !window[BOOKING_SYNC_LISTENER_FLAG]) {
  window[BOOKING_SYNC_LISTENER_FLAG] = true;
  window.addEventListener("storage", (event) => {
    if (event.key !== BOOKING_SYNC_STORAGE_KEY || !event.newValue) return;
    try {
      const payload = JSON.parse(event.newValue);
      dispatchBookingChanged(payload?.detail || {});
    } catch {
      // Ignore malformed values written by older app versions.
    }
  });
}

const normalizeCustomerBooking = (booking) => {
  if (!booking) return booking;
  const presentation = booking.presentation || booking.snapshot || {};
  const seats = Array.isArray(presentation.seats) ? presentation.seats : [];
  const food = booking.food || booking.foodOrder || null;
  const foodItems = Array.isArray(food?.items) ? food.items : [];

  return {
    ...booking,
    snapshot: presentation,
    foodOrder: food,
    movieTitle: presentation.movieTitle,
    posterUrl: presentation.moviePosterUrl || presentation.moviePoster,
    cinemaName: presentation.cinemaName,
    auditoriumName: presentation.auditoriumName,
    showtimeStart: presentation.showtimeStart,
    showtimeEnd: presentation.showtimeEnd,
    seatNames: seats
      .map((seat) => seat.label)
      .filter(Boolean)
      .join(", "),
    foodNames: foodItems
      .map((item) => `${item.name || item.productName} x${item.quantity}`)
      .join(", "),
  };
};

const uuidv4 = () => {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
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
export const createBooking = async ({
  showtimePublicId,
  seatPublicIds,
  reservationPublicIds,
  counterCustomerAccountId,
  counterCustomerName,
  counterCustomerPhone,
  counterCustomerEmail,
  idempotencyKey = uuidv4(),
}) => {
  const payload = { showtimePublicId };
  if (Array.isArray(seatPublicIds) && seatPublicIds.length > 0)
    payload.seatPublicIds = seatPublicIds;
  if (Array.isArray(reservationPublicIds) && reservationPublicIds.length > 0)
    payload.reservationPublicIds = reservationPublicIds;
  if (counterCustomerAccountId)
    payload.counterCustomerAccountId = counterCustomerAccountId;
  if (counterCustomerName?.trim())
    payload.counterCustomerName = counterCustomerName.trim();
  if (counterCustomerPhone?.trim())
    payload.counterCustomerPhone = counterCustomerPhone.trim();
  if (counterCustomerEmail?.trim())
    payload.counterCustomerEmail = counterCustomerEmail.trim();
  const response = await apiClient.post(
    "/api/bookings",
    {
      ...payload,
    },
    {
      headers: { "Idempotency-Key": idempotencyKey },
    },
  );
  const booking = response.data.data;
  clearBookingCreationAttempt(showtimePublicId);
  emitBookingChanged({ action: "CREATED", publicId: booking?.publicId });
  return booking;
};

export const getOrCreateScoreRedemptionKey = (bookingId, points) => {
  const storageKey = `booking:score-redemption:${bookingId}:${points}`;
  const existing = sessionStorage.getItem(storageKey);
  if (existing) return existing;
  const created = uuidv4();
  sessionStorage.setItem(storageKey, created);
  return created;
};

export const finalizeCheckout = async (
  bookingId,
  {
    scorePoints = 0,
    scoreIdempotencyKey = null,
    selectedUserPromotionPublicIds = [],
    selectedPromotionPublicIds = [],
    couponCode = null,
    paymentMethod = null,
  } = {},
) => {
  const response = await apiClient.post(
    `/api/bookings/${bookingId}/finalize-checkout`,
    {
      scorePoints,
      ...(scorePoints > 0 && scoreIdempotencyKey
        ? { scoreIdempotencyKey }
        : {}),
      selectedUserPromotionPublicIds,
      selectedPromotionPublicIds,
      ...(couponCode ? { couponCode } : {}),
      ...(paymentMethod ? { paymentMethod } : {}),
    },
  );
  const booking = response.data.data;
  emitBookingChanged({
    action: "FINALIZED",
    publicId: booking?.publicId || bookingId,
  });
  return booking;
};

export const previewBookingPromotions = async (
  bookingId,
  {
    selectedUserPromotionPublicIds = [],
    selectedPromotionPublicIds = [],
    evaluationUserPromotionPublicIds = [],
    evaluationPromotionPublicIds = [],
    couponCode = null,
    paymentMethod = null,
  } = {},
) => {
  const response = await apiClient.post(
    `/api/bookings/${bookingId}/promotions/preview`,
    {
      selectedUserPromotionPublicIds,
      selectedPromotionPublicIds,
      evaluationUserPromotionPublicIds,
      evaluationPromotionPublicIds,
      ...(couponCode ? { couponCode } : {}),
      ...(paymentMethod ? { paymentMethod } : {}),
    },
  );
  return response.data.data;
};

/**
 * Get booking details by publicId
 * @param {string} bookingId - Booking UUID
 * @returns {Promise<Object>} Booking detail response
 */
export const getBookingDetails = async (bookingId) => {
  const response = await apiClient.get(`/api/bookings/${bookingId}`);
  return normalizeCustomerBooking(response.data.data);
};

/**
 * Return the current customer's unexpired PENDING_PAYMENT booking for a Showtime.
 * The server is authoritative; this endpoint is not a client-side uniqueness guard.
 */
export const getActiveBookingForShowtime = async (showtimePublicId) => {
  const response = await apiClient.get("/api/bookings/active", {
    params: { showtimePublicId },
  });
  return response.data?.data || null;
};

/**
 * Find booking details by booking code
 * @param {string} bookingCode - The booking code string
 * @returns {Promise<Object>} Booking detail response
 */
export const getBookingByCode = async (bookingCode) => {
  const response = await apiClient.get(`/api/bookings/code/${bookingCode}`);
  return normalizeCustomerBooking(response.data.data);
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
 * Resend the booking confirmation email
 * @param {string} bookingId - Booking UUID
 * @returns {Promise<Object>} Success response
 */
export const resendBookingEmail = async (bookingId) => {
  const response = await apiClient.post(
    `/api/bookings/${bookingId}/resend-email`,
  );
  return response.data;
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
export const getBookingHistory = async ({
  page = 0,
  size = 10,
  status,
  fromDate,
  toDate,
  sort = "createdAt,desc",
}) => {
  const params = { page, size, sort };
  if (status) params.status = status;
  if (fromDate)
    params.fromDate = new Date(`${fromDate}T00:00:00`).toISOString();
  if (toDate) params.toDate = new Date(`${toDate}T23:59:59.999`).toISOString();

  const response = await apiClient.get("/api/bookings", { params });
  const responsePage = response.data.data;
  return {
    ...responsePage,
    content: (responsePage?.content || []).map(normalizeCustomerBooking),
  };
};

/**
 * Get the authenticated customer's successful paid spending for one calendar year.
 * Booking Service owns this aggregation so pagination cannot make the total incomplete.
 */
export const getBookingSpendingSummary = async (
  year = new Date().getFullYear(),
) => {
  const response = await apiClient.get("/api/bookings/spending-summary", {
    params: { year },
  });
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
      reasonDetail: reason,
    },
    headers: { "Idempotency-Key": idempotencyKey },
  });
  const booking = response.data.data;
  clearAllBookingCreationAttempts();
  clearStoredCheckoutPhase(bookingId);
  emitBookingChanged({ action: "CANCELLED", publicId: bookingId });
  return booking;
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
  const error = new Error("Hệ thống thanh toán chưa sẵn sàng.");
  error.code = "PAYMENT_SERVICE_HANDOFF_REQUIRED";
  throw error;
};
