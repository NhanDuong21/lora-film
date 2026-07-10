# Movie Service

This is the Movie microservice for the Lorafilm project.

## Architecture Guidelines

We are following **Package by Feature / Domain-Driven Design (DDD) Lite**.

- **Domain logic** goes into its respective feature package (`movie`, `cinema`, `auditorium`, `seat`, `showtime`, `pricing`).
- Each feature package should contain standard layers:
  - `domain`: Entities, value objects, and domain interfaces.
  - `repository`: Spring Data JPA repositories.
  - `service`: Business logic interfaces and implementations.
  - `dto`: Data Transfer Objects (Requests/Responses).
  - `controller`: REST APIs.
- Use the common classes provided in the `common` package for consistent API responses (`ApiResponse`, `PageResponse`) and unified error handling (`BusinessException`, `ErrorCode`).
- Note: Infrastructure and Database schemas are managed globally at the workspace root, not within this service.
