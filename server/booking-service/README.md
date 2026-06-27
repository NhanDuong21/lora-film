# Booking Service

This is the booking service for the movie booking system.

## API Documentation & Testing (Swagger UI)

Swagger UI is available at:
```text
http://localhost:8083/swagger-ui/index.html
```

### How to use JWT Authorization in Swagger UI:
1. Obtain a valid JWT access token from the Authentication Service (e.g. via `POST http://localhost:8081/api/auth/login`).
2. Copy the value of `data.accessToken` from the login response.
3. Open Swagger UI and click the **Authorize** button at the top-right of the page.
4. Paste only the raw access token (e.g. `eyJhbGciOi...`) into the **Value** input field.
5. Do **not** include the `Bearer ` prefix (Swagger will automatically prepend it to the Authorization header).
6. Click **Authorize** and close the dialog.
7. Protected API endpoints (marked with lock icons) can now be executed directly from the browser.
