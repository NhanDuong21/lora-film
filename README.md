# Movie Booking Microservices Backend

This repository contains a Spring Boot microservices backend for a movie booking system. Each service is a standalone Maven project with its own `application.properties`, health endpoint, and unique port.

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

## Configuration

Each service includes an `application.properties` file with:

- `server.port`
- `spring.application.name`
- MySQL datasource settings
- JPA settings for backend services

The gateway includes Spring Cloud Gateway route definitions for the backend services.

## Health Checks

Each service exposes:

- `GET /health`

Example response:

```json
{
  "service": "auth-service",
  "status": "UP"
}
```

## How To Run

Open a terminal in each service directory and run the service independently.

### API Gateway

```bash
cd server/api-gateway
mvn spring-boot:run
```

### Auth Service

```bash
cd server/auth-service
mvn spring-boot:run
```

### Movie Service

```bash
cd server/movie-service
mvn spring-boot:run
```

### Booking Service

```bash
cd server/booking-service
mvn spring-boot:run
```

### Payment Service

```bash
cd server/payment-service
mvn spring-boot:run
```

### Notification Service

```bash
cd server/notification-service
mvn spring-boot:run
```

## Notes

- Make sure MySQL is running locally before starting the backend services.
- Create the referenced databases if you want JPA to connect successfully:
  - `auth_db`
  - `movie_db`
  - `booking_db`
  - `payment_db`
  - `notification_db`
- The gateway routes requests to the backend services using the configured local ports.

## Example Gateway Routes

- `/auth/**` -> `http://localhost:8081`
- `/movies/**` -> `http://localhost:8082`
- `/bookings/**` -> `http://localhost:8083`
- `/payments/**` -> `http://localhost:8084`
- `/notifications/**` -> `http://localhost:8085`

## Build Verification

From each module directory you can verify compilation with:

```bash
mvn clean compile
```

If you want to run the full backend, start the services in this order:

1. `auth-service`
2. `movie-service`
3. `booking-service`
4. `payment-service`
5. `notification-service`
6. `api-gateway`

## Docker Compose Local Development

A base local environment is available at the repository root with:

- MySQL 8
- Redis
- Zookeeper
- Kafka

### Prerequisites

- Docker Desktop or Docker Engine installed
- Docker Compose support enabled
- `docker` and `docker compose` available in your terminal

### Setup

1. Copy the sample environment file:

```bash
cp .env.example .env
```

2. Edit `.env` and add your values for:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

### Start services

```bash
docker compose up -d
```

### Stop services

```bash
docker compose down
```

### Check logs

```bash
docker compose logs -f
```

Or check logs for a single service:

```bash
docker compose logs -f mysql
```

### Remove containers and volumes

```bash
docker compose down -v
```

### Verify services are running

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Zookeeper: `localhost:2181`
- Kafka: `localhost:9092`

You can also check Docker status directly:

```bash
docker compose ps
```
