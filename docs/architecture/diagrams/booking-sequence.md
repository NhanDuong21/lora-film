# Booking Ticket Flow

## Description

This sequence diagram describes the draft booking ticket flow in the Movie Theater Management System.

The flow shows how a user selects a movie, showtime, and seats, then the system temporarily locks the selected seats, processes payment, confirms the booking, publishes a booking event, and sends a confirmation notification.

> This is a draft flow for architecture documentation and may be refined in later sprints.

---

## Participants

* User
* React Frontend
* API Gateway
* Booking Service
* Payment Service
* MySQL
* Redis
* Kafka
* Notification Service

---

## Main Flow

### 1. User selects movie, showtime, and seats

The user selects a movie, chooses a showtime, and selects one or more seats from the React Frontend.

At this stage, the booking is not confirmed yet.

---

### 2. Frontend sends seat lock request

The React Frontend sends a request to API Gateway to temporarily lock the selected seats.

**Request**

```http
POST /api/bookings/seat-lock
```

---

### 3. API Gateway forwards request to Booking Service

API Gateway receives the request and routes it to Booking Service.

---

### 4. Booking Service checks seat availability

Booking Service checks MySQL to verify whether the selected seats are still available.

If one or more seats are already booked, the system returns an error response.

---

### 5. Booking Service locks seats in Redis

If the selected seats are available, Booking Service creates temporary seat locks in Redis.

Redis is used to prevent multiple users from booking the same seats at the same time.

The seat lock should have an expiration time, for example:

```text
seat_lock:{showtimeId}:{seatId}
```

---

### 6. Frontend shows payment screen

After the seats are locked successfully, the frontend displays the payment screen to the user.

---

### 7. User confirms payment

The user reviews the booking information and confirms payment.

---

### 8. Payment Service processes payment

The React Frontend sends the payment request through API Gateway.

API Gateway routes the request to Payment Service.

Payment Service validates the booking information and processes the payment.

---

### 9. Booking Service saves booking to MySQL

If payment is successful, Booking Service confirms the booking and saves the booking record into MySQL.

Booking Service also updates the selected seats as booked.

---

### 10. Booking Service releases Redis seat lock

After the booking is confirmed, Booking Service removes the temporary seat locks from Redis.

---

### 11. Booking Service publishes booking event to Kafka

Booking Service publishes a `BookingConfirmed` event to Kafka.

Kafka is used for asynchronous communication between services.

---

### 12. Notification Service sends confirmation

Notification Service consumes the `BookingConfirmed` event from Kafka and sends a booking confirmation message to the user.

---

### 13. Frontend displays booking result

The React Frontend displays the booking success result to the user.

---

## Alternative Flow

### Seat Already Booked

If the selected seats are already booked in MySQL, Booking Service returns an error response and the frontend asks the user to select different seats.

### Seat Temporarily Locked

If the selected seats are already locked in Redis by another user, Booking Service returns an error response and the frontend asks the user to choose other seats.

### Seat Lock Expired

If the user takes too long to complete payment, the Redis seat lock may expire. The user must select seats again before continuing the booking process.

### Payment Failed

If payment fails, the booking is not confirmed and the frontend displays a payment failed message.

---

## Sequence Diagram

![Booking Sequence](./diagrams/booking-sequence.png)

---

## Diagram Flow Summary

```text
User
→ React Frontend
→ API Gateway
→ Booking Service
→ MySQL
→ Redis
→ Payment Service
→ Booking Service
→ MySQL
→ Redis
→ Kafka
→ Notification Service
→ User
```