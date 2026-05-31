# Sequence Diagrams

This document describes the sequence diagrams used in the Movie Theater Management System.

---

# Register Flow

## Description

This sequence diagram describes the account registration process in the Movie Theater Management System.

## Participants

* User
* React Frontend
* API Gateway
* Auth Service
* MySQL

## Main Flow

### 1. User enters registration information

The user opens the registration page and enters the required information, including:

* Username
* Password
* Confirm Password
* Full Name
* Email
* Phone Number

After completing the form, the user submits the registration request.

### 2. Frontend sends request to API Gateway

The React Frontend performs basic client-side validation and sends the registration request to the API Gateway.

**Request**

```http
POST /api/auth/register
```

### 3. API Gateway forwards request to Auth Service

The API Gateway receives the request and routes it to the Auth Service responsible for user authentication and account management.

### 4. Auth Service validates request

Auth Service validates all required fields and verifies that the request data meets business rules.

Validation includes:

* Required fields are present
* Email format is valid
* Password meets security requirements
* Confirm password matches password

### 5. Auth Service checks email in MySQL

Auth Service queries MySQL to determine whether the provided email address already exists.

**Database Query**

```sql
SELECT * FROM users
WHERE email = ?
```

If an existing account is found, the registration process is stopped and an error response is returned.

### 6. Auth Service hashes password

If the email does not exist, Auth Service hashes the password using BCrypt before storing it in the database.

The original plain-text password is never stored.

### 7. Auth Service stores new user in MySQL

Auth Service creates a new user record and saves it to MySQL.

Stored information includes:

* Username
* Hashed Password
* Full Name
* Email
* Phone Number
* Account Status
* Created Date

**Database Insert**

```sql
INSERT INTO users (...)
VALUES (...)
```

### 8. Registration success response

MySQL confirms that the user record has been created successfully.

Auth Service returns:

```http
HTTP/1.1 201 Created
```

The response is forwarded through API Gateway to the React Frontend.

### 9. Frontend redirects user

The React Frontend displays a success message and redirects the user to the Login page.

---

## Alternative Flow

### Email Already Exists

1. Auth Service checks the email address.
2. MySQL returns an existing record.
3. Auth Service returns:

```http
HTTP/1.1 409 Conflict
```

4. Frontend displays an error message:

```text
Email already exists.
```

---

## Sequence Diagram

![Register Sequence](./diagrams/register-sequence.png)

---

## Diagram Flow Summary

```text
User
→ React Frontend
→ API Gateway
→ Auth Service
→ MySQL
→ Auth Service
→ API Gateway
→ React Frontend
→ User
```
