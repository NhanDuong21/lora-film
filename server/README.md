# Movie Booking Microservices Backend

This directory contains the standalone Spring Boot microservices backend for a movie booking system.

## Repository Structure

```text
server/
├── auth-service/
├── movie-service/
├── booking-service/
├── payment-service/
├── notification-service/
├── user-service/
├── promotion-service/
├── score-service/
├── analytics-service/
└── README.md
```

## Requirements

- Java 21
- Maven 3.9+
- MySQL 8+

## Service Ports

- `api-gateway` -> `8080` (Located at root)
- `auth-service` -> `8081`
- `movie-service` -> `8082`
- `booking-service` -> `8083`
- `payment-service` -> `8084`
- `notification-service` -> `8085`
- `user-service` -> `8086`
- `promotion-service` -> `8087`
- `score-service` -> `8088`
- `analytics-service` -> `8089`

## How To Run

Run each module independently from its own folder:

```bash
cd server/auth-service
mvn spring-boot:run
```

```bash
cd server/movie-service
mvn spring-boot:run
```

```bash
cd server/booking-service
mvn spring-boot:run
```

```bash
cd server/payment-service
mvn spring-boot:run
```

```bash
cd server/notification-service
mvn spring-boot:run
```

```bash
cd server/user-service
mvn spring-boot:run
```

```bash
cd server/promotion-service
mvn spring-boot:run
```

```bash
cd server/score-service
mvn spring-boot:run
```

```bash
cd server/analytics-service
mvn spring-boot:run
```

To start the gateway:
```bash
cd api-gateway
mvn spring-boot:run
```

## Health Check

Each service exposes `GET /health` and returns a response in the form:

```json
{
  "service": "auth-service",
  "status": "UP"
}
```

## Notes

- Start MySQL before launching the services.
- Create the referenced databases if you want JPA to connect successfully (`auth_db`, `movie_db`, `booking_db`, `payment_db`, `notification_db`, `user_db`, `promotion_db`, `score_db`, `analytics_db`).
- The API gateway routes requests to the backend services using the configured local ports.
