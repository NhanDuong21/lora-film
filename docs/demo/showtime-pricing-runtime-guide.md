# Showtime Pricing Runtime and Demo Guide

This guide is the release checklist for the managed Showtime pricing feature.
The authoritative domain and API rules are in
`docs/design/movie-service/showtime-pricing-management.md`.

## Deployment order

1. Back up the Movie and Booking databases and retain the preflight query
   output.
2. Run `20260723_add_showtime_price_policies.sql` against the Movie database.
3. Run `20260723_add_booking_price_snapshot_unique.sql` against the Booking
   database.
4. Run every post-migration query in
   `showtime-pricing-migration-preflight.md`.
5. Deploy Movie Service, Booking Service, Payment Service, API Gateway, and the
   frontend, in that order.
6. Do not enable admin policy mutations until all services are healthy.

The new Movie Service binary cannot start against the old schema because
Hibernate validation is enabled.

## Service smoke checks

Use an authenticated administrator session for admin calls.

1. Open `/admin/pricing` and verify that each cinema has an ACTIVE
   `Legacy Default` policy.
2. Create a DRAFT policy containing a cinema-wide, all-day rule for every
   active SeatType in a test auditorium.
3. Add a WEEKEND or bounded-time override and run the form's resolution
   preview.
4. Activate the policy. An equal-rank conflict must be rejected with
   `PRICE_POLICY_OVERLAP`.
5. Create a manual DRAFT Showtime. Open its pricing diagnostics and verify one
   complete snapshot batch with policy/rule provenance and the cinema
   timezone.
6. Apply an Auto Schedule preview and verify that its created Showtimes use the
   same policy resolver—there must be no seat-code defaulting in application
   code.
7. Create a deliberately incomplete policy situation. Showtime creation must
   remain successful and DRAFT, but OPEN must fail with
   `PRICING_INCOMPLETE`.
8. Change a DRAFT Showtime's auditorium or start time. Its provisional snapshot
   must be replaced as one batch. Activating or deactivating a policy alone
   must not mutate an existing snapshot.

## Booking and payment consistency check

1. Open a fully priced Showtime and request its customer seat layout.
2. Create a Booking for at least two differently priced seats.
3. Verify exactly one `booking_price_snapshots` row exists for the Booking and
   that its line sum equals the Booking `ticket_amount`.
4. Request `GET /internal/bookings/{numericBookingId}/payment-context`.
   The amount and currency must match the persisted Booking.
5. Create a Payment without sending a client amount.
6. Post a success result through
   `POST /internal/bookings/{numericBookingId}/payment-results`.
7. Repost the same payment event and verify idempotent success.
8. Post a different event with a mismatched amount or currency and verify it is
   rejected.

## Evidence to retain

- Preflight and post-migration query output.
- Policy list/detail screenshots, including activation metadata.
- Resolution preview and Showtime provenance screenshots.
- One incomplete-pricing OPEN rejection payload.
- Booking, Booking price snapshot, and Payment attempt IDs with matching
  amount/currency.
- Test reports for Movie, Booking, Payment, gateway configuration, and the
  frontend.

Never copy production customer details, provider secrets, or payment tokens
into demo evidence.

## Rollback

The normal rollback is an application rollback: deploy the previous binaries
and leave the additive pricing tables and columns intact.

Physical rollback is permitted only before any new policy or provenance writes
have occurred, after restoring the legacy Auto Schedule price behavior. Once
new writes exist, restore from the database backups instead of dropping tables,
columns, or foreign keys.
