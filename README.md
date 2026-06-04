# Movie Booking System

Welcome to the online movie booking system project by Group 3. This repository contains the complete source code of the project including the Frontend (Client), Backend (Microservices Server), and API Gateway.

## Technology Stack

**Frontend (Client):**
- React 19 (with Vite)
- React Router DOM
- Axios

**Backend (Server & API Gateway):**
- Java 21
- Spring Boot (Microservices)
- Spring Cloud Gateway
- Maven 3.9+
- Database: MySQL 8+

## Project Structure

```text
hcm26_cpl_java_05_group3/
├── client/                 # Frontend application (React/Vite)
├── server/                 # Backend services (Java/Spring Boot)
│   ├── auth-service/       # User authentication service (JWT/OAuth2)
│   ├── movie-service/      # Service managing movies, cinemas, schedules
│   ├── booking-service/    # Booking processing service
│   ├── payment-service/    # Payment integration service
│   ├── notification-service/# Email/SMS notification service
│   ├── user-service/       # User profiles and management service
│   ├── analytics-service/  # Data insights and reporting service
│   ├── promotion-service/  # Coupons and discount service
│   └── score-service/      # Loyalty points and movie rating service
├── api-gateway/            # Intermediate API Gateway routing requests
└── docs/                   # Project documentation (workflow, structure...)
```
*(For more details, see [Project Structure Documentation](docs/project-structure.md))*

## 🛠 Clone Instructions

1. Clone the repository to your local machine:
   ```bash
   git clone <repository_url>
   cd hcm26_cpl_java_05_group3
   ```
2. Checkout the `develop` branch (main integration branch):
   ```bash
   git checkout develop
   ```

## Frontend Run Instructions

Open a new terminal and run the following commands:

```bash
cd client
npm install
npm run dev
```

## Backend Run Instructions

The backend uses a Microservices architecture. Ensure that you have MySQL installed and the corresponding databases created (`auth_db`, `movie_db`, `booking_db`, `payment_db`, `notification_db`, `user_db`, `promotion_db`, `score_db`, `analytics_db`).

Open a terminal in each service directory and run them independently. You can use the commands:

```bash
mvn clean compile
mvn spring-boot:run
```

**Recommended Startup Order:**
1. `cd server/auth-service && mvn spring-boot:run` (Port: `8081`)
2. `cd server/movie-service && mvn spring-boot:run` (Port: `8082`)
3. `cd server/booking-service && mvn spring-boot:run` (Port: `8083`)
4. `cd server/payment-service && mvn spring-boot:run` (Port: `8084`)
5. `cd server/notification-service && mvn spring-boot:run` (Port: `8085`)
6. `cd server/user-service && mvn spring-boot:run` (Port: `8086`)
7. `cd server/promotion-service && mvn spring-boot:run` (Port: `8087`)
8. `cd server/score-service && mvn spring-boot:run` (Port: `8088`)
9. `cd server/analytics-service && mvn spring-boot:run` (Port: `8089`)
10. `cd api-gateway && mvn spring-boot:run` (Port: `8080`)

1. `auth-service` (8081)
2. `movie-service` (8082)
3. `booking-service` (8083)
4. `payment-service` (8084)
5. `notification-service` (8085)
6. `user-service` (8086)
7. `promotion-service` (8087)
8. `score-service` (8088)
9. `analytics-service` (8089)
10. `api-gateway` (8080)

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
Check the health status via the `GET /health` endpoint for each service (e.g., `http://localhost:8081/health`, `http://localhost:8086/health`). 

The Gateway routes requests using the following static routes:
- `/api/auth/**` -> `http://localhost:8081` (auth-service)
- `/api/movies/**` -> `http://localhost:8082` (movie-service)
- `/api/bookings/**` -> `http://localhost:8083` (booking-service)
- `/api/payments/**` -> `http://localhost:8084` (payment-service)
- `/api/notifications/**` -> `http://localhost:8085` (notification-service)
- `/api/users/**` -> `http://localhost:8086` (user-service)
- `/api/promotions/**` -> `http://localhost:8087` (promotion-service)
- `/api/scores/**` -> `http://localhost:8088` (score-service)
- `/api/analytics/**` -> `http://localhost:8089` (analytics-service)

## Basic Branching Rules

The project adopts a branch management process inspired by Git Flow:
- **`main`**: Production branch, contains the most stable source code. No direct pushing allowed.
- **`develop`**: Main integration branch. All feature branches must branch off from here.
- **Development Branch Prefixes**:
  - `feature/<issue-id>-<description>`: Develop a new feature.
  - `fix/<issue-id>-<description>`: Fix a bug.
  - `docs/<issue-id>-<description>`: Update documentation.
  - `setup/<issue-id>-<description>`: Configuration setup, CI/CD.
  - `test/<issue-id>-<description>`: Add/edit test cases.

## Merge Request (MR) Process

All code intended for `develop` must go through a Merge Request (MR).
1. Complete the code on your local branch, ensuring it runs well and passes tests.
2. Push the branch to GitLab.
3. Create an MR with a clear title (following Conventional Commits standards), with `develop` as the target branch.
4. Write a detailed description for the MR and link the corresponding Issue (e.g., `Closes #1`).
5. Assign the Team Leader/Reviewer (Thành) to review the code.
6. Once the MR is approved and passes the pipeline, it will be merged into `develop`.

*(For more details, see [GitLab Workflow Guidelines](docs/gitlab-workflow.md))*

## Team Members

- **Phan Tuấn Thành** - Team Leader / Developer
- **Dương Thiện Nhân** - Member / Developer
- **Trần Hiển Vinh** - Member / Developer
- **Trương Hoàng Khang** - Member / Developer
- **Trần Lương Thiện Hoàn** - Member / Developer
---
**Note:** See detailed rules regarding Git, commits, and workflows in [docs/gitlab-workflow.md](docs/gitlab-workflow.md).
