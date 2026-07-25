# Showtime Pricing Management Contract

Status: Approved for implementation
Version: `showtime-snapshot-v1`
Last updated: 2026-07-23

## Purpose

This document is the authoritative V1 contract for configuring Showtime ticket
prices. It replaces hardcoded runtime pricing with effective-dated policies while
preserving immutable prices once a Showtime leaves `DRAFT`.

The Movie Service owns price resolution and Showtime price snapshots. Booking
persists the selected price lines it receives from Movie Service. Payment always
uses the amount and currency returned by Booking and never accepts a client
amount.

## Domain model

### Price policy

A policy is an immutable published version. It contains:

- a public ID, name, cinema, inclusive effective date range, currency, and
  priority;
- a stored status of `DRAFT`, `ACTIVE`, or `INACTIVE`;
- optional lineage through `supersedesPolicy`;
- activation and deactivation actor/time metadata;
- an optimistic-lock version;
- a complete collection of rules.

`EXPIRED` is a display state. It is returned when a stored `ACTIVE` policy has an
`effectiveTo` before the current date in the cinema timezone. It is never stored.

V1 accepts only `VND`.

### Price policy rule

A rule targets one SeatType and can optionally be scoped to either one
Auditorium or one ScreenType. Both scopes cannot be set on the same rule.

Rules define:

- `dayType`: `ALL_DAYS`, `WEEKDAY`, or `WEEKEND`;
- an optional time band;
- a positive price;
- whether the rule is active.

A null start and null end mean all day. Exactly one null endpoint or equal
endpoints is invalid. Normal bands are `[start, end)`. A start later than the end
is an overnight band and matches `[start, 24:00)` plus `[00:00, end)`.

The day type always uses the cinema-local date of the Showtime start, including
the after-midnight part of an overnight band.

### Showtime price snapshot

There is one snapshot row for each distinct active SeatType used by an
`ACTIVE` or `MAINTENANCE` seat in the Showtime Auditorium. A snapshot records:

- SeatType foreign key plus name and code snapshots;
- positive price and three-letter currency;
- source: `POLICY`, `LEGACY`, or `MANUAL_OVERRIDE`;
- optional source policy and rule;
- resolution time and cinema timezone.

The `(showtime_id, seat_type_id)` pair remains unique.

## Lifecycle

| Stored status | Metadata/rules mutable | Activate | Deactivate | Copy |
| --- | --- | --- | --- | --- |
| `DRAFT` | Yes, with expected version | Yes | No | Yes |
| `ACTIVE` | No | No | Yes, with expected version and reason | Yes |
| `INACTIVE` | No | No | No | Yes |

Activation is the publishing action. It validates the whole policy, performs
cross-policy ambiguity checks, and records the actor and timestamp. Inactive
policies are terminal. Changes to an active or inactive policy require copying it
to a new draft linked through `supersedesPolicy`.

Activating, expiring, deactivating, editing, or copying a policy never changes
existing Showtime snapshots.

## Resolution

The cinema IANA timezone is mandatory and authoritative. A missing or invalid
timezone is an error; resolution never falls back to UTC.

For each required SeatType:

1. Convert `Showtime.startTime` to the Cinema `ZoneId`.
2. Select stored `ACTIVE` policies for the Cinema whose inclusive effective
   range contains the local date.
3. Select active rules matching SeatType, Auditorium/ScreenType scope, day type,
   and local time.
4. Rank every candidate using the complete rank tuple:

   `(scopeRank, dayRank, timeRank, policyPriority)`

   where:

   - `scopeRank`: Auditorium `3`, ScreenType `2`, Cinema-wide `1`;
   - `dayRank`: matching `WEEKDAY`/`WEEKEND` `2`, `ALL_DAYS` `1`;
   - `timeRank`: bounded band `2`, all-day `1`;
   - `policyPriority`: the policy integer priority, higher first.

5. Use the candidate only when exactly one candidate has the highest complete
   tuple.
6. If more than one candidate has the highest complete tuple, return
   `PRICING_AMBIGUOUS`.
7. If no candidate matches, return that SeatType as missing.
8. Persist a snapshot batch only when every required SeatType resolves.

Creation time, database ID, and public ID are never silent tie breakers. Public
IDs are used only to produce stable diagnostics.

The resolver uses the Showtime local start date, not Auto Schedule
`serviceDate`. This keeps manual and automatic creation equivalent.

## Overlap and ambiguity validation

Effective date intervals are inclusive and a null end is unbounded. Time
intervals are half-open, so touching endpoints do not overlap.

Two rules can create a forbidden equal-rank ambiguity only when all of these are
true:

- they target the same SeatType;
- their policy effective date ranges intersect;
- their policy priorities are equal;
- they have the same scope rank and their concrete scopes intersect;
- they have the same day rank and their day sets intersect;
- they have the same time rank and their time sets intersect.

Scope/day/time truth table:

| Pair | Equal rank | Matching set intersects | Result |
| --- | --- | --- | --- |
| Cinema / Auditorium | No | Yes | Allowed override |
| Screen / Auditorium | No | May | Allowed override |
| Same Auditorium / Same Auditorium | Yes | Yes | Continue overlap check |
| Different Auditoriums | Yes | No | Allowed |
| Same ScreenType / Same ScreenType | Yes | Yes | Continue overlap check |
| Different ScreenTypes | Yes | No | Allowed |
| `ALL_DAYS` / `WEEKDAY` | No | Yes | Allowed override |
| `WEEKDAY` / `WEEKDAY` | Yes | Yes | Continue overlap check |
| `WEEKDAY` / `WEEKEND` | Yes | No | Allowed |
| All-day / Bounded | No | Yes | Allowed override |
| Bounded / Bounded, disjoint | Yes | No | Allowed |
| Bounded / Bounded, intersecting | Yes | Yes | Conflict |
| Equal ranks, different priority | Complete tuple differs | Yes | Allowed overlay |
| Equal complete rank and coverage | Yes | Yes | Conflict |

Overnight bands are split into two half-open intervals for overlap checks.
Draft save returns ordered conflict diagnostics. Activation fails while any
conflict remains.

## Showtime integration and mutability

- Manual creation saves a `DRAFT`, attempts resolution, and returns derived
  pricing completeness and diagnostics.
- Auto Schedule apply uses the same resolver and has no hardcoded fallback.
- A `DRAFT` edit that changes Cinema, Auditorium, or start time deletes the
  provisional batch and resolves it again in the same transaction.
- An explicit resolve operation replaces only a `DRAFT` provisional batch.
- Missing or ambiguous pricing leaves the Showtime in `DRAFT` with no partial
  batch. It does not roll back scheduling.
- Once a Showtime leaves `DRAFT`, every price mutation is forbidden.
- Snapshot refresh and the `OPEN_FOR_BOOKING` transition lock the Showtime row.

The `OPEN_FOR_BOOKING` gate requires exactly one positive snapshot per required
SeatType, one supported currency across the batch, no duplicates, and required
metadata for newly created rows. Complete migrated `LEGACY` rows remain valid.
The current policy status is not rechecked.

Customer and internal booking reads fail on missing prices. They never return a
zero fallback.

## Booking and payment consistency

Movie Service returns the authoritative selected seat lines and aggregate.
Booking validates:

- each requested Seat ID appears exactly once;
- every unit price is positive;
- every line uses the response currency;
- the line sum equals `ticketAmount`.

In the Booking creation transaction, Booking persists one
`booking_price_snapshots` row with the Showtime numeric/public IDs, capture time,
currency, selected Seat ID/label/type/unit-price lines, authoritative total, and
`pricing_engine_version = showtime-snapshot-v1`.

`booking_price_snapshots.booking_id` is unique.

The internal payment context uses the persisted Booking amount/currency. Payment
results with a different amount or currency are rejected. Replayed provider event
IDs are idempotent. Payment and analytics events continue carrying stored
amount/currency and do not expose policy details.

Frontend totals are estimates. The Booking/Payment response is final.

## Admin API contract

Policy endpoints:

- `GET /api/admin/pricing/policies`
- `POST /api/admin/pricing/policies`
- `GET /api/admin/pricing/policies/{publicId}`
- `PUT /api/admin/pricing/policies/{publicId}`
- `POST /api/admin/pricing/policies/{publicId}/activate`
- `POST /api/admin/pricing/policies/{publicId}/deactivate`
- `POST /api/admin/pricing/policies/{publicId}/copy`
- `GET /api/admin/pricing/policies/{publicId}/usage`
- `POST /api/admin/pricing/resolve-preview`

Showtime endpoints:

- `GET /api/admin/showtimes/{publicId}/pricing`
- `POST /api/admin/showtimes/{publicId}/pricing/resolve`
- `GET /api/admin/showtimes/{publicId}/prices` remains a compatibility alias.
- `PUT /api/admin/showtimes/{publicId}/prices` is an audited, complete-batch,
  `DRAFT`-only manual override and is not exposed by the new UI.

All mutation requests carry an expected version where an existing aggregate is
changed. Errors use `PRICE_POLICY_OVERLAP`, `PRICE_POLICY_IMMUTABLE`,
`PRICING_AMBIGUOUS`, `PRICING_INCOMPLETE`, or
`PRICING_CONCURRENT_MODIFICATION` with structured details.

## Compatibility and deferred work

Migrated snapshots are marked `LEGACY` and are not linked to seeded policies.
They remain readable and can open a Showtime when complete.

V1 deliberately excludes holiday calendars, generic expressions, promotions,
surge/demand/personalized pricing, multi-currency sales, approval workflows,
automatic or bulk repricing, manual-override UI, policy deletion, and changes to
Auto Schedule candidate generation or service-date semantics.
