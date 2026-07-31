# Analytics input event contracts

This document complements `payment-analytics-events.md` and
`booking-cancelled-event.md`. The consumer implementation is in
`server/analytics-service`.

## `PAYMENT_REFUNDED` v1

Topic: `payment-refunded.v1`  
Owner: `payment-service`  
Kafka key: `bookingPublicId`

```json
{
  "eventId": "stable-uuid",
  "schemaVersion": "1.0",
  "eventType": "PAYMENT_REFUNDED",
  "sourceService": "payment-service",
  "paymentPublicId": "payment-uuid",
  "bookingPublicId": "booking-uuid",
  "refundAmount": 100000,
  "currency": "VND",
  "refundedAt": "2026-07-29T08:00:00Z"
}
```

`refundAmount` must be positive. A replay must preserve `eventId`. Analytics uses
`refundedAt` converted to `Asia/Ho_Chi_Minh` as the KPI date. Refunds are the only
input allowed to reduce revenue.

## Optional enrichment for `payment-success.v1`

The existing v1 contract remains valid. These additive fields make all KPI
dimensions calculable without cross-service reads:

```json
{
  "userPublicId": "account-uuid",
  "membershipTier": "GOLD",
  "cinemaName": "LoraFilm Hải Châu",
  "auditoriumPublicId": "auditorium-uuid",
  "promotionPublicId": "promotion-uuid",
  "promotionName": "Summer 2026",
  "availableSeats": 120,
  "paymentMethod": "VNPAY"
}
```

The values must be snapshots captured by the owning service at transaction time.
Analytics never looks them up later. If a producer omits them, ingestion still
succeeds but the data-quality score identifies which downstream dimensions are
incomplete.

## Consumer guarantees

- malformed or semantically invalid input goes directly to `<topic>.dlq`;
- transient database/Kafka failures retry with bounded exponential backoff;
- `eventId` is unique across all three input streams;
- facts are immutable and no consumer calculates or updates KPI tables;
- there are no outbound commands from Analytics to Booking, Payment, Movie,
  Promotion, or User services.
