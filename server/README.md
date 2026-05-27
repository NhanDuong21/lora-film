# Movie Booking Microservices Backend

This directory contains the standalone Spring Boot microservices backend for a movie booking system.

## Repository Structure

```text
server/
├── api-gateway/
├── auth-service/
├── movie-service/
├── booking-service/
├── payment-service/
├── notification-service/
└── README.md
```

## Requirements

- Java 21
- Maven 3.9+
- MySQL 8+

## Service Ports

- `api-gateway` -> `8080`
- `auth-service` -> `8081`
- `movie-service` -> `8082`
- `booking-service` -> `8083`
- `payment-service` -> `8084`
- `notification-service` -> `8085`

## How To Run

Run each module independently from its own folder:

```bash
cd api-gateway
mvn spring-boot:run
```

```bash
cd auth-service
mvn spring-boot:run
```

```bash
cd movie-service
mvn spring-boot:run
```

```bash
cd booking-service
mvn spring-boot:run
```

```bash
cd payment-service
mvn spring-boot:run
```

```bash
cd notification-service
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
- Create the referenced databases if you want JPA to connect successfully.
- The API gateway routes requests to the backend services using the configured local ports.
