# Booking readiness for Payment Service

Status: implemented in Booking contract v1 (`schemaVersion = 1.0`), 2026-07-27.

## 1. Stable public identity

Canonical cross-service routes use `bookingPublicId` and Payment results accept
`paymentPublicId`. Legacy numeric Booking routes remain compatibility adapters
until Payment Service finishes migration. Browser requests never use internal
Booking IDs.

## 2. UTC time and immutable deadline

Payment context and result DTOs use ISO-8601 UTC instants (`...Z`).
`amountLockedAt` is the checkout boundary. `expiresAt` is created once by
Booking and cannot be extended by finalization, retry, replay, or failed payment.
Server receipt time, not provider `occurredAt`, determines whether success is
late.

## 3. Server-owned financial context

`GET /internal/bookings/{bookingPublicId}/payment-context` is payable only when:

- Booking is `PENDING_PAYMENT`;
- `amountLockedAt` is present;
- locked amount/currency are valid;
- original deadline is live;
- every linked reservation is live `HELD`.

Payment receives the stored amount/currency and immutable analytics snapshot.
The browser sends only `bookingPublicId` and `paymentMethod` to Payment Service.

## 4. Normalized result receipt and idempotency

`POST /internal/bookings/{bookingPublicId}/payment-results` requires a UUID
`eventId`, schema `1.0`, Payment identity, result, positive amount, currency,
and UTC `occurredAt`.

Booking hashes the complete normalized payload. Exact replay returns the stored
response with `idempotent=true`. Reusing the event ID with any changed
normalized field returns `PAYMENT_EVENT_ID_REUSED` without lifecycle effects.
The validation also runs for alternate event delivery, not only HTTP.

## 5. Ordering and reconciliation

- `FAILED/CANCELLED/TIMEOUT/PENDING` does not release seats or extend deadline.
- `FAILED -> SUCCESS` is accepted while the original Booking remains payable.
- Failure after accepted success cannot downgrade the Booking.
- late/non-payable/conflicting success and amount/currency mismatch persist the
  full receipt plus one reconciliation task and return 409.
- exact replay of a rejected event returns the same conflict and task reference.

## 6. Confirmation and refund lifecycle

Accepted Payment success is one transaction:

```text
PENDING_PAYMENT -> CONFIRMED
HELD -> BOOKED
create tickets/history/audit/outbox
```

Refund settlement uses
`POST /internal/bookings/{bookingPublicId}/refund-results`.
Only `REFUND_SUCCESS` for `CONFIRMED` changes Booking to `REFUNDED`; tickets are
refunded and reservations remain `BOOKED`. Direct confirmation and refund routes
return tombstone errors and never mutate state.

## 7. Service authentication

Payment authority routes require the dedicated
`PAYMENT_TO_BOOKING_INTERNAL_TOKEN`. The general internal token cannot call
Payment context/result/refund-result routes, and the Payment token cannot call
ordinary internal Booking operations. Missing/invalid tokens return stable
machine-readable errors.

## 8. Admin and customer boundaries

- Admin UI/API exposes only `PENDING_PAYMENT -> CANCELLED` and
  `CONFIRMED -> COMPLETED`.
- Admin cannot force confirmation, refund, expiry, or arbitrary status changes.
- Admin screens render Booking API values and neutral “Chưa ghi nhận” states;
  they do not invent payment/provider/movie data.
- Customer checkout finalizes Booking first, creates a stable Payment attempt
  idempotency key, then hands off only public Booking identity and method.
- Booking-side provider simulation controls are removed. Customer-facing
  failures use Vietnamese application modals rather than browser alerts.

## Canonical endpoint matrix

| Method | Route | Authority |
| --- | --- | --- |
| `POST` | `/api/bookings/{bookingPublicId}/finalize-checkout` | Booking owner |
| `GET` | `/internal/bookings/{bookingPublicId}/payment-context` | Payment token |
| `POST` | `/internal/bookings/{bookingPublicId}/payment-results` | Payment token |
| `POST` | `/internal/bookings/{bookingPublicId}/refund-results` | Payment token |
| `POST` | `/internal/bookings/{bookingPublicId}/confirm` | 410 tombstone |
| `POST` | `/internal/bookings/{bookingPublicId}/refund` | 410 tombstone |

Internal endpoints are not Gateway routes.
