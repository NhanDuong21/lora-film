# Booking lifecycle sequence

This document is normative for Booking Service as of 2026-07-27.

## Ownership

- Movie Service owns Showtime/seat identity, sellability, and authoritative seat prices.
- Booking Service owns the Booking aggregate, immutable price snapshot, checkout amount lock, hold deadline, and seat lifecycle.
- MySQL `seat_reservations` plus `uk_active_seat_reservation` are the only long-lived capacity authority.
- Redis only reduces contention during the short creation transaction. Its keys are released after commit or rollback.
- Payment Service owns payment attempts, provider integration, callbacks, and refund settlement.
- Payment `SUCCESS` is the only command that confirms a Booking.

## Create and hold

```text
Customer
  -> Gateway: POST /api/bookings
     { showtimePublicId, seatPublicIds }
  -> Booking: validate customer/idempotency/configured seat limit
  -> Movie: validate public Showtime + exact public seat set + prices
  -> Booking: deadline = min(receipt time + hold duration, Showtime start)
  -> Redis: acquire deterministically sorted seat mutexes with one owner token
  -> MySQL transaction:
       lock relevant reservation/customer rows
       expire stale linked or compatibility holds
       create PENDING_PAYMENT Booking
       create immutable Booking price/presentation snapshots
       create linked HELD seat_reservations
       create history/audit/outbox/idempotency response
       flush active-seat unique constraint
  -> Redis (finally): compare-and-delete only locks owned by this request
  -> Customer: Booking response with public IDs, server amount, and UTC deadline
```

After the response there is no Redis seat key to maintain. Clearing or restarting
Redis cannot release a `HELD` or `BOOKED` database reservation.

## Checkout and payment handoff

```text
Customer -> Booking: mutate F&B while amountLockedAt is null
Customer -> Booking: POST /api/bookings/{bookingPublicId}/finalize-checkout
Booking -> MySQL: set amountLockedAt once; keep the original expiresAt
Customer -> Payment: POST /api/payments
                    { bookingPublicId, paymentMethod }
Payment -> Booking: GET /internal/bookings/{bookingPublicId}/payment-context
Booking -> Payment: locked amount/currency, amountLockedAt, expiresAt (UTC)
```

The browser never supplies amount, currency, status, or expiry to Payment.
Finalization, retry, failed attempts, and idempotent replay never extend the
Booking deadline.

## Payment result

```text
Payment -> Booking:
  POST /internal/bookings/{bookingPublicId}/payment-results
  X-Internal-Token: dedicated Payment-to-Booking token

Booking transaction on SUCCESS received before expiresAt:
  persist normalized event receipt
  PENDING_PAYMENT -> CONFIRMED
  HELD reservations -> BOOKED
  create tickets/history/audit/outbox
```

- `FAILED`, `CANCELLED`, `TIMEOUT`, and `PENDING` attempts are audit records;
  while the original deadline remains live the Booking stays retryable.
- Exact duplicate `eventId` plus normalized payload returns the stored result
  idempotently.
- Reusing an `eventId` with a changed payload returns HTTP 409.
- Late, amount/currency-mismatched, non-payable, or conflicting `SUCCESS`
  persists both the receipt and a reconciliation task, then returns HTTP 409.
- Provider `occurredAt` is audit data. Server receipt time decides lateness.

## Cancellation and expiration

```text
Customer/Admin cancellation of PENDING_PAYMENT:
  Booking -> CANCELLED
  HELD -> RELEASED

Scheduler or command-time stale check:
  Booking -> EXPIRED
  HELD -> EXPIRED
```

Both flows are MySQL-only and make the generated active-seat key null. A new
Booking can then reserve the seat. A confirmed `BOOKED` reservation remains
unavailable through completion and refund.

## Refund result

```text
Payment -> Booking:
  POST /internal/bookings/{bookingPublicId}/refund-results

REFUND_SUCCESS for CONFIRMED:
  Booking -> REFUNDED
  tickets -> refunded
  seat reservations remain BOOKED
```

Direct confirmation and direct refund mutation routes are tombstones. Admin can
cancel a pending order or complete a confirmed order, but cannot manufacture
Payment success, refund settlement, or expiry.
