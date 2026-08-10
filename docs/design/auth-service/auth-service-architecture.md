# Auth Service Architecture Specification

**Document ID:** AUTH-ARCH-

---

# Table of Contents

1. Overview
2. Service Responsibility
3. Business Capability
4. Architecture
5. Design Principles
6. Dependencies

---

# 1. Overview

## 1.1 Purpose

Auth Service is responsible for authentication and authorization for the Cinema Management System.

It provides a centralized identity platform that authenticates users, issues JWT tokens, validates permissions, manages sessions, and secures access to all backend services.

The service does **not** manage customer information, employee information, payroll, movies, bookings, or payments.

---

## 1.2 Scope

### Included

- User Registration
- Email Verification
- Login
- Logout
- Refresh Token
- Forgot Password
- Reset Password
- Change Password
- JWT Authentication
- OAuth2 Login
- Role Management
- Permission Management
- Session Management
- Security Audit

### Excluded

- Customer Profile
- Employee Information
- Payroll
- Movie Management
- Booking
- Payment
- Promotion
- Loyalty Point
- Rating

These responsibilities belong to other services.

---

# 2. Service Responsibility

Auth Service is the identity provider of the system.

Its responsibilities include:

## Authentication

- Register
- Verify Email
- Login
- Logout

---

## Authorization

- Roles
- Permissions
- RBAC Validation

---

## Token Management

- JWT Access Token
- Refresh Token
- Token Validation
- Token Rotation

---

## Session Management

- Active Sessions
- Device Tracking
- Logout Current Session
- Logout All Sessions

---

## Security

- Password Encryption
- OTP Verification
- Login Attempt Validation
- Account Locking
- Audit Logging

---

## OAuth2

Supported providers

- Google
- GitHub
- Facebook

---

# 3. Business Capability

```text
                    Auth Service

         +-----------------------------+

           Authentication

           Authorization

           Token Management

           Session Management

           OAuth2

           Security

           Audit Logging

         +-----------------------------+
```

---

# 4. Architecture

```text
                    API Gateway
                         │
                         ▼
                 Auth Controller
                         │
                         ▼
                 Authentication Layer
                         │
         ┌───────────────┼────────────────┐
         ▼               ▼                ▼
 Authentication   Authorization     Session Service
         │               │                │
         └───────────────┼────────────────┘
                         ▼
                  Repository Layer
                         ▼
                      MySQL
```

---

# 5. Design Principles

## Single Responsibility

Auth Service only handles identity and access management.

Business data is managed by dedicated services.

---

## Stateless Authentication

Access Tokens are stateless.

Authentication information is stored inside JWT claims.

---

## Secure by Default

Every endpoint requires authentication unless explicitly marked as public.

Examples:

Public:

- Register
- Login
- Verify Email
- Refresh Token

Protected:

- Logout
- Change Password
- Session Management
- Role Management

---

## Role-Based Access Control

Authorization is implemented using RBAC.

Example:

```text
ADMIN

↓

Manage Roles

Manage Permissions

Manage Accounts
```

```text
CUSTOMER

↓

Login

Logout

Update Password
```

---

# 6. External Dependencies

| Dependency      | Purpose            |
| --------------- | ------------------ |
| MySQL           | Persistent Storage |
| Redis           | OTP & Cache        |
| Kafka           | Event Publishing   |
| Spring Security | Authentication     |
| JWT             | Access Token       |
| BCrypt          | Password Hashing   |
| OAuth2 Client   | Social Login       |

---

# 7. Service Interfaces

### REST API

Used for:

- Register
- Login
- Refresh Token
- Logout

---

### Kafka Producer

Publish Events:

- ACCOUNT_REGISTERED
- ACCOUNT_VERIFIED
- ACCOUNT_STATUS_CHANGED
- PASSWORD_CHANGED

---

### Kafka Consumer

Currently none.

Future versions may consume:

- USER_DELETED
- EMPLOYEE_DISABLED

---

# 8. High-Level Request Flow

```text
Client
    │
    ▼
API Gateway
    │
    ▼
Auth Controller
    │
    ▼
Authentication Service
    │
    ▼
Repository
    │
    ▼
MySQL
```

---

# 9. Authentication Module

The Authentication Module is responsible for validating user identities and issuing authentication credentials.

It includes the following business functions:

- User Registration
- Email Verification
- Login
- Logout
- Forgot Password
- Reset Password
- Change Password

---

# 10. User Registration

## 10.1 Purpose

Allow new users to create an account in the Cinema Management System.

A newly registered account remains in **PENDING_VERIFICATION** status until the email address is verified.

---

## 10.2 Business Flow

```text
Client

↓

POST /api/auth/register

↓

Validate Request

↓

Check Email Exists

↓

Hash Password

↓

Create Account

↓

Generate OTP

↓

Save OTP (Redis)

↓

Publish ACCOUNT_REGISTERED

↓

Notification Service

↓

Send Verification Email

↓

Return Success
```

---

## 10.3 Business Rules

| Rule ID     | Description                                               |
| ----------- | --------------------------------------------------------- |
| AUTH-BR-001 | Email must be unique                                      |
| AUTH-BR-002 | Password must satisfy security policy                     |
| AUTH-BR-003 | Account status is PENDING_VERIFICATION after registration |
| AUTH-BR-004 | OTP expires after configured time                         |
| AUTH-BR-005 | Verification email must be sent asynchronously            |

---

## 10.4 Validation Rules

### Email

- Required
- Valid email format
- Maximum 255 characters
- Unique

---

### Password

- Required
- Minimum 8 characters
- Maximum 100 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character

---

### Full Name

- Required
- Maximum 100 characters

---

# 11. Email Verification

## Purpose

Activate a registered account after successful OTP verification.

---

## Flow

```text
Client

↓

POST /api/auth/verify-email

↓

Validate OTP

↓

Load Account

↓

Update Status

↓

ACTIVE

↓

Delete OTP

↓

Publish ACCOUNT_VERIFIED

↓

Return Success
```

---

## Business Rules

| Rule        | Description                                         |
| ----------- | --------------------------------------------------- |
| AUTH-BR-006 | OTP must exist                                      |
| AUTH-BR-007 | OTP must not expire                                 |
| AUTH-BR-008 | OTP can only be used once                           |
| AUTH-BR-009 | Only PENDING_VERIFICATION accounts can be activated |

---

# 12. Login

## Purpose

Authenticate users and issue JWT credentials.

---

## Login Flow

```text
Client

↓

POST /api/auth/login

↓

Validate Email

↓

Load Account

↓

Check Status

↓

Verify Password

↓

Generate Access Token

↓

Generate Refresh Token

↓

Store Session

↓

Return JWT
```

---

## Account Status Validation

Supported statuses

| Status               | Login    |
| -------------------- | -------- |
| ACTIVE               | Allowed  |
| PENDING_VERIFICATION | Rejected |
| LOCKED               | Rejected |
| DISABLED             | Rejected |

---

## Failed Login

If password validation fails:

- Increase failed login counter
- Write audit log
- Return Unauthorized

---

# 13. Logout

## Purpose

Invalidate the current authenticated session.

---

## Flow

```text
Client

↓

POST /api/auth/logout

↓

Validate JWT

↓

Remove Session

↓

Blacklist Token (optional)

↓

Return Success
```

---

## Business Rules

| Rule        | Description                         |
| ----------- | ----------------------------------- |
| AUTH-BR-010 | Only authenticated users can logout |
| AUTH-BR-011 | Session must exist                  |

---

# 14. Forgot Password

## Purpose

Allow users to request password reset.

---

## Flow

```text
Client

↓

POST /api/auth/forgot-password

↓

Validate Email

↓

Generate Reset OTP

↓

Save Redis

↓

Publish PASSWORD_RESET_REQUESTED

↓

Notification Service

↓

Send Email
```

---

## Business Rules

| Rule        | Description                              |
| ----------- | ---------------------------------------- |
| AUTH-BR-012 | Account must exist                       |
| AUTH-BR-013 | OTP expires after configured duration    |
| AUTH-BR-014 | Only ACTIVE accounts can reset passwords |

---

# 15. Reset Password

## Flow

```text
Client

↓

POST /api/auth/reset-password

↓

Validate OTP

↓

Hash Password

↓

Update Password

↓

Delete OTP

↓

Publish PASSWORD_CHANGED

↓

Return Success
```

---

## Business Rules

| Rule        | Description                                    |
| ----------- | ---------------------------------------------- |
| AUTH-BR-015 | OTP must be valid                              |
| AUTH-BR-016 | New password must satisfy password policy      |
| AUTH-BR-017 | New password cannot match the current password |

---

# 16. Change Password

## Purpose

Allow authenticated users to change their password.

---

## Flow

```text
Client

↓

POST /api/auth/change-password

↓

Validate JWT

↓

Verify Current Password

↓

Validate New Password

↓

Hash Password

↓

Update Database

↓

Publish PASSWORD_CHANGED

↓

Return Success
```

---

## Business Rules

| Rule        | Description                                    |
| ----------- | ---------------------------------------------- |
| AUTH-BR-018 | Current password must be correct               |
| AUTH-BR-019 | New password must satisfy password policy      |
| AUTH-BR-020 | New password must differ from current password |

---

# 17. Authentication Sequence

## Login Sequence

```text
Client
 │
 │ Login
 ▼
Gateway
 │
 ▼
Auth Controller
 │
 ▼
Authentication Service
 │
 │ Validate Account
 │ Verify Password
 │ Generate JWT
 │ Save Session
 ▼
Database
 │
 ▼
Response
```

---

# 18. Authentication Error Codes

| Code     | Description               |
| -------- | ------------------------- |
| AUTH_001 | Email already exists      |
| AUTH_002 | Account not found         |
| AUTH_003 | Invalid password          |
| AUTH_004 | Invalid OTP               |
| AUTH_005 | OTP expired               |
| AUTH_006 | Account locked            |
| AUTH_007 | Account disabled          |
| AUTH_008 | Account not verified      |
| AUTH_009 | Password policy violation |
| AUTH_010 | Session not found         |

---

\

# 19. JWT Architecture

## 19.1 Overview

JWT (JSON Web Token) is the primary authentication mechanism used by the Cinema Management System.

Two token types are supported:

| Token         | Purpose                   | Storage  |
| ------------- | ------------------------- | -------- |
| Access Token  | Authenticate API requests | Client   |
| Refresh Token | Request new Access Token  | Database |

---

## 19.2 Authentication Flow

```text
                    Login

                      │

                      ▼

            Generate JWT Token

                      │

         ┌────────────┴────────────┐

         ▼                         ▼

 Access Token              Refresh Token

(15 Minutes)                (7 Days)

         │                         │

         ▼                         ▼

 Client Storage           Database Storage

```

---

## 19.3 JWT Claims

```json
{
  "sub": "1",
  "email": "admin@cinema.com",
  "role": "ADMIN",
  "iat": 1753512000,
  "exp": 1753512900
}
```

---

## 19.4 JWT Validation

Every protected request follows this flow.

```text
Client

↓

Authorization Header

↓

API Gateway

↓

Validate JWT

↓

Forward Request

↓

Service
```

If validation fails

```text
Return

401 Unauthorized
```

---

# 20. Refresh Token

## Purpose

Refresh Token allows clients to obtain a new Access Token without requiring users to log in again.

---

## Refresh Flow

```text
Client

↓

POST /auth/refresh

↓

Validate Refresh Token

↓

Load Session

↓

Generate New JWT

↓

Update Refresh Token

↓

Return Token
```

---

## Business Rules

| Rule        | Description                             |
| ----------- | --------------------------------------- |
| AUTH-BR-021 | Refresh Token must exist                |
| AUTH-BR-022 | Refresh Token must not expire           |
| AUTH-BR-023 | Refresh Token belongs to one account    |
| AUTH-BR-024 | Expired Refresh Tokens cannot be reused |

---

## Refresh Token Rotation

For security reasons, every successful refresh request rotates the Refresh Token.

```text
Old Refresh Token

↓

Invalidate

↓

Generate New Refresh Token

↓

Save Database

↓

Return New Token
```

---

# 21. Session Management

## Purpose

Track authenticated user sessions.

Each login creates one active session.

---

## Session Lifecycle

```text
Login

↓

Create Session

↓

User Active

↓

Logout

↓

Remove Session
```

---

## Session Information

| Field         | Description               |
| ------------- | ------------------------- |
| Session Id    | Unique Session Identifier |
| Account Id    | Owner                     |
| Refresh Token | Current Refresh Token     |
| Device        | Client Device             |
| IP Address    | Login Address             |
| Created At    | Login Time                |
| Expired At    | Expiration                |

---

## Session Flow

```text
Client

↓

Login

↓

Authentication

↓

Create Session

↓

Save Database

↓

Return JWT
```

---

## Logout Flow

```text
Logout

↓

Delete Session

↓

Invalidate Refresh Token

↓

Success
```

---

# 22. OAuth2 Authentication

## Purpose

Support social login providers.

Supported providers

- Google
- GitHub
- Facebook

---

## OAuth2 Flow

```text
Client

↓

Google Login

↓

Google OAuth

↓

Authorization Code

↓

Auth Service

↓

Validate Provider

↓

Create Account (First Login)

↓

Generate JWT

↓

Return Token
```

---

## Account Linking

Existing users may connect additional providers.

Example

```text
Account

↓

Google

GitHub

Facebook
```

All providers authenticate the same account.

---

# 23. Device Management

Each successful login records device information.

Stored information

- Browser
- Operating System
- Device Name
- Login Time
- IP Address

---

## Device Flow

```text
Login

↓

Read Device

↓

Create Session

↓

Store Device Information
```

---

# 24. Audit Logging

Every important security action is recorded.

Examples

- Register
- Login
- Logout
- Password Changed
- Password Reset
- Role Updated
- Permission Updated

---

## Audit Flow

```text
Business Action

↓

Audit Service

↓

Create Log

↓

Database
```

---

## Audit Information

| Field      | Description      |
| ---------- | ---------------- |
| Action     | Business Action  |
| Account Id | Actor            |
| Timestamp  | Execution Time   |
| IPAddress  | Client IP        |
| Result     | SUCCESS / FAILED |

---

# 25. Security Components

## Password Hashing

Algorithm

```
BCrypt
```

---

## Email Verification

Authentication requires email verification before login.

---

## Session Validation

Only active sessions are accepted.

---

## Role Validation

Authorization is checked before business execution.

---

## Input Validation

Every API validates

- Required Fields
- Length
- Format
- Business Rules

---

# 26. Module Dependency

```text
                Auth Controller

                       │

        ┌──────────────┼──────────────┐

        ▼              ▼              ▼

 Authentication   Authorization   Session

        │              │              │

        └──────────────┼──────────────┘

                       ▼

                  Repository

                       ▼

                     MySQL
```

---

# 27. Component Diagram

```text
+--------------------------------------+

           Auth Service

+--------------------------------------+

Controller Layer

↓

Service Layer

↓

Repository Layer

↓

Database

+--------------------------------------+
```

---

# 28. Internal Modules

```text
auth-service

├── Authentication

├── Authorization

├── JWT

├── Session

├── OAuth2

├── Security

├── Audit

├── Event Publisher
```

---

# 29. Summary

The Auth Service provides a centralized identity platform for the Cinema Management System.

Core responsibilities include:

- Authentication
- Authorization
- JWT Management
- Refresh Token Rotation
- Session Tracking
- OAuth2 Authentication
- Audit Logging
- Security Validation

Business data such as customer profiles, employees, payroll, bookings, and payments are delegated to their respective services.

---

# 30. Database Architecture

## 30.1 Overview

The Auth Service owns its own database (`auth_db`) and is the single source of truth for authentication and authorization data.

No other microservice is allowed to access `auth_db` directly.

Communication with other services must be performed through:

- REST API
- Kafka Events

---

# 31. Database Schema

```text
auth_db

├── accounts
├── roles
├── permissions
├── role_permissions
├── refresh_tokens
├── account_providers
├── sessions
└── audit_logs
```

---

# 32. Entity Relationship Diagram

```text
                    +-------------+
                    |    roles    |
                    +-------------+
                           |
                           |
                    role_id FK
                           |
                           ▼
+------------------------------------------------+
|                   accounts                     |
+------------------------------------------------+
| id                                             |
| email                                          |
| password_hash                                  |
| role_id                                        |
| status                                         |
| created_at                                     |
| updated_at                                     |
+------------------------------------------------+
     │               │               │
     │               │               │
     ▼               ▼               ▼

refresh_tokens   sessions    account_providers

                     │

                     ▼

               audit_logs
```

---

# 33. accounts

Stores login credentials.

| Column        | Type         | Constraint |
| ------------- | ------------ | ---------- |
| id            | BIGINT       | PK         |
| email         | VARCHAR(255) | UNIQUE     |
| password_hash | VARCHAR(255) | NOT NULL   |
| role_id       | BIGINT       | FK         |
| status        | ENUM         | NOT NULL   |
| created_at    | TIMESTAMP    |            |
| updated_at    | TIMESTAMP    |            |

---

## Account Status

```text
PENDING_VERIFICATION

ACTIVE

LOCKED

DISABLED
```

---

# 34. roles

System roles.

| Column      | Type         |
| ----------- | ------------ |
| id          | BIGINT       |
| name        | VARCHAR(50)  |
| description | VARCHAR(255) |

---

## Default Roles

```text
ADMIN

MANAGER

EMPLOYEE

CUSTOMER
```

---

# 35. permissions

Stores every permission supported by the system.

| Column      | Type         |
| ----------- | ------------ |
| id          | BIGINT       |
| code        | VARCHAR(100) |
| name        | VARCHAR(150) |
| description | TEXT         |

---

Example

```text
USER_READ

USER_UPDATE

PAYROLL_READ

PAYROLL_UPDATE

ROLE_MANAGE

ACCOUNT_DISABLE
```

---

# 36. role_permissions

Many-to-many relationship between roles and permissions.

| Column        | Description |
| ------------- | ----------- |
| role_id       | FK          |
| permission_id | FK          |

Composite Primary Key

```text
(role_id, permission_id)
```

---

# 37. refresh_tokens

Stores active Refresh Tokens.

| Column     | Type      |
| ---------- | --------- |
| id         | BIGINT    |
| account_id | FK        |
| token      | TEXT      |
| expires_at | TIMESTAMP |
| revoked    | BOOLEAN   |
| created_at | TIMESTAMP |

---

Business Rules

- One token belongs to one account.
- Expired tokens are invalid.
- Revoked tokens cannot be reused.

---

# 38. sessions

Stores active login sessions.

| Column           | Type         |
| ---------------- | ------------ |
| id               | BIGINT       |
| account_id       | FK           |
| refresh_token_id | FK           |
| device_name      | VARCHAR(150) |
| ip_address       | VARCHAR(50)  |
| user_agent       | TEXT         |
| last_active      | TIMESTAMP    |
| created_at       | TIMESTAMP    |

---

Purpose

- Session Tracking
- Multi-device Login
- Logout Session
- Login History

---

# 39. account_providers

Stores OAuth2 providers linked to an account.

| Column           | Type         |
| ---------------- | ------------ |
| id               | BIGINT       |
| account_id       | FK           |
| provider         | ENUM         |
| provider_user_id | VARCHAR(255) |
| created_at       | TIMESTAMP    |

---

Supported Providers

```text
GOOGLE

GITHUB

FACEBOOK
```

---

# 40. audit_logs

Records important security actions.

| Column     | Type         |
| ---------- | ------------ |
| id         | BIGINT       |
| account_id | FK           |
| action     | VARCHAR(100) |
| ip_address | VARCHAR(50)  |
| user_agent | TEXT         |
| result     | ENUM         |
| created_at | TIMESTAMP    |

---

Audit Actions

```text
LOGIN_SUCCESS

LOGIN_FAILED

REGISTER

VERIFY_EMAIL

PASSWORD_CHANGED

RESET_PASSWORD

ROLE_UPDATED

PERMISSION_UPDATED
```

---

# 41. Foreign Keys

```text
accounts.role_id
    ↓
roles.id

refresh_tokens.account_id
    ↓
accounts.id

sessions.account_id
    ↓
accounts.id

sessions.refresh_token_id
    ↓
refresh_tokens.id

account_providers.account_id
    ↓
accounts.id

audit_logs.account_id
    ↓
accounts.id
```

---

# 42. Recommended Indexes

accounts

```sql
idx_account_email
```

refresh_tokens

```sql
idx_refresh_token

idx_refresh_account
```

sessions

```sql
idx_session_account

idx_session_last_active
```

audit_logs

```sql
idx_audit_account

idx_audit_created
```

---

# 43. Audit Columns

Every table should contain

```sql
created_at

updated_at
```

Optional

```sql
created_by

updated_by
```

---

# 44. Naming Convention

Tables

```text
snake_case
```

Examples

```text
refresh_tokens

account_providers

role_permissions
```

Columns

```text
snake_case
```

Primary Key

```text
id
```

Foreign Key

```text
account_id

role_id
```

Boolean

```text
is_active

is_deleted

revoked
```

---

# 45. Database Best Practices

- Every table uses InnoDB.
- UTF8MB4 character set.
- Soft delete should be avoided in Auth Service.
- Passwords are always stored using BCrypt hashes.
- Refresh Tokens should never be stored in plain text in production.
- Foreign key constraints must be enabled.
- Frequently queried columns should be indexed.

---

# 46. REST API Specification

Base URL

```
/api/auth
```

Authentication

```
Bearer JWT
```

Content-Type

```
application/json
```

---

# 47. Authentication APIs

| Method | Endpoint         | Description            | Authentication |
| ------ | ---------------- | ---------------------- | -------------- |
| POST   | /register        | Register Account       | Public         |
| POST   | /verify-email    | Verify OTP             | Public         |
| POST   | /login           | User Login             | Public         |
| POST   | /refresh         | Refresh Access Token   | Public         |
| POST   | /logout          | Logout Current Session | JWT            |
| POST   | /forgot-password | Request Password Reset | Public         |
| POST   | /reset-password  | Reset Password         | Public         |
| POST   | /change-password | Change Password        | JWT            |

---

# 48. Session APIs

| Method | Endpoint       | Description         |
| ------ | -------------- | ------------------- |
| GET    | /sessions      | Current Sessions    |
| DELETE | /sessions/{id} | Logout Session      |
| DELETE | /sessions      | Logout All Sessions |

Authentication

```
Bearer JWT
```

---

# 49. Role APIs

Accessible only by ADMIN.

| Method | Endpoint    |
| ------ | ----------- |
| GET    | /roles      |
| POST   | /roles      |
| PUT    | /roles/{id} |
| DELETE | /roles/{id} |

---

# 50. Permission APIs

| Method | Endpoint          |
| ------ | ----------------- |
| GET    | /permissions      |
| POST   | /permissions      |
| PUT    | /permissions/{id} |
| DELETE | /permissions/{id} |

---

# 51. Register API

## Endpoint

```
POST /api/auth/register
```

## Request

```json
{
  "email": "john@example.com",
  "password": "Password@123",
  "fullName": "John Doe"
}
```

## Success Response

HTTP 201

```json
{
  "success": true,
  "message": "Registration successful. Please verify your email."
}
```

---

## Error Response

```json
{
  "success": false,
  "code": "AUTH_001",
  "message": "Email already exists"
}
```

---

# 52. Verify Email API

## Endpoint

```
POST /api/auth/verify-email
```

## Request

```json
{
  "email": "john@example.com",
  "otp": "123456"
}
```

## Success

```json
{
  "success": true,
  "message": "Email verified successfully."
}
```

---

# 53. Login API

## Endpoint

```
POST /api/auth/login
```

## Request

```json
{
  "email": "john@example.com",
  "password": "Password@123"
}
```

---

## Success Response

```json
{
  "accessToken": "xxxxx",
  "refreshToken": "xxxxx",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

---

## Error

```json
{
  "code": "AUTH_003",
  "message": "Invalid email or password."
}
```

---

# 54. Refresh Token API

## Endpoint

```
POST /api/auth/refresh
```

## Request

```json
{
  "refreshToken": "xxxxxxxxxxxx"
}
```

---

## Response

```json
{
  "accessToken": "xxxxx",
  "refreshToken": "xxxxx",
  "expiresIn": 900
}
```

---

# 55. Logout API

## Endpoint

```
POST /api/auth/logout
```

Authentication

```
Bearer JWT
```

Response

```json
{
  "success": true
}
```

---

# 56. Forgot Password API

## Endpoint

```
POST /api/auth/forgot-password
```

Request

```json
{
  "email": "john@example.com"
}
```

Response

```json
{
  "success": true,
  "message": "Password reset email has been sent."
}
```

---

# 57. Reset Password API

## Endpoint

```
POST /api/auth/reset-password
```

Request

```json
{
  "email": "john@example.com",
  "otp": "123456",
  "newPassword": "Password@123"
}
```

Response

```json
{
  "success": true
}
```

---

# 58. Change Password API

Authentication Required

```
Bearer JWT
```

Endpoint

```
POST /api/auth/change-password
```

Request

```json
{
  "currentPassword": "OldPassword@123",
  "newPassword": "NewPassword@123"
}
```

Response

```json
{
  "success": true
}
```

---

# 59. Session APIs

## Get Current Sessions

```
GET /api/auth/sessions
```

Response

```json
[
  {
    "id": 1,
    "device": "Chrome",
    "ipAddress": "192.168.1.10",
    "lastActive": "2026-07-26T15:00:00"
  }
]
```

---

## Logout Specific Session

```
DELETE /api/auth/sessions/{id}
```

Response

```json
{
  "success": true
}
```

---

## Logout All Sessions

```
DELETE /api/auth/sessions
```

Response

```json
{
  "success": true
}
```

---

# 60. Standard Response Format

## Success

```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully."
}
```

---

## Error

```json
{
  "success": false,
  "code": "AUTH_004",
  "message": "Invalid OTP."
}
```

---

# 61. HTTP Status Codes

| Status | Meaning               |
| ------ | --------------------- |
| 200    | OK                    |
| 201    | Created               |
| 204    | No Content            |
| 400    | Bad Request           |
| 401    | Unauthorized          |
| 403    | Forbidden             |
| 404    | Not Found             |
| 409    | Conflict              |
| 422    | Validation Error      |
| 500    | Internal Server Error |

---

# 62. Validation Rules

| Field           | Rule                           |
| --------------- | ------------------------------ |
| Email           | Required, Valid Format, Unique |
| Password        | Min 8 Characters               |
| OTP             | 6 Digits                       |
| Role Name       | Unique                         |
| Permission Code | Unique                         |

---

# 63. Global Error Codes

| Code     | Description           |
| -------- | --------------------- |
| AUTH_001 | Email Already Exists  |
| AUTH_002 | Account Not Found     |
| AUTH_003 | Invalid Credentials   |
| AUTH_004 | Invalid OTP           |
| AUTH_005 | OTP Expired           |
| AUTH_006 | Account Locked        |
| AUTH_007 | Account Disabled      |
| AUTH_008 | Account Not Verified  |
| AUTH_009 | Refresh Token Invalid |
| AUTH_010 | Session Not Found     |
| AUTH_011 | Permission Denied     |
| AUTH_012 | Validation Failed     |

---

# 64. Project Structure

The Auth Service follows a layered architecture based on Spring Boot best practices.

```text
auth-service/
├── src/main/java/com/project/authservice/
│   ├── client/
│   ├── common/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── event/
│   ├── exception/
│   ├── kafka/
│   ├── mapper/
│   ├── repository/
│   ├── scheduler/
│   ├── security/
│   ├── service/
│   └── util/
├── src/main/resources/
│   ├── application.example.properties
│   └── application.properties          # local, Git ignored
└── pom.xml
```

Schema được quản lý thủ công tại
`docs/database/mysql/auth-service-schema.sql`; service không dùng Flyway hay
Liquibase và Hibernate chỉ chạy `ddl-auto=validate`.

---

# 65. Package Responsibilities

## config

Responsible for Spring configurations.

Contains

- SecurityConfig
- KafkaConfig
- RedisConfig
- SwaggerConfig
- JwtConfig
- WebMvcConfig

---

## controller

REST Controllers.

Example

```text
AuthController

RoleController

PermissionController

SessionController
```

---

## service

Business logic.

Example

```text
AuthenticationService

JwtService

SessionService

RoleService

PermissionService
```

---

## repository

Spring Data JPA repositories.

Example

```text
AccountRepository

RoleRepository

PermissionRepository

RefreshTokenRepository

SessionRepository
```

---

## entity

JPA entities.

```text
Account

Role

Permission

RefreshToken

Session

AuditLog
```

---

## dto

Contains

Request DTO

Response DTO

Internal DTO

---

## mapper

Entity ↔ DTO conversion.

Recommended

```
MapStruct
```

---

## security

Contains

- JwtAuthenticationFilter
- AuthenticationEntryPoint
- AccessDeniedHandler
- SecurityConfiguration

---

## oauth2

Contains

- OAuth2UserService
- OAuth2SuccessHandler
- OAuth2FailureHandler

---

## kafka

Contains

Producer

Consumer

Topic Configuration

---

## redis

Contains

OTP Cache

Redis Repository

---

## util

Shared helper classes.

---

# 66. Redis Design

Redis is used for temporary authentication data.

## Keys

```text
otp:{email}

login_attempt:{email}

blacklist:{jwt}

refresh:{accountId}
```

---

## TTL

| Key           | TTL              |
| ------------- | ---------------- |
| OTP           | 5 Minutes        |
| Login Attempt | 30 Minutes       |
| JWT Blacklist | Token Expiration |
| Refresh Cache | 7 Days           |

---

# 67. Kafka Events

## Published Events

```text
ACCOUNT_REGISTERED

ACCOUNT_VERIFIED

ACCOUNT_LOCKED

ACCOUNT_DISABLED

PASSWORD_CHANGED

ROLE_UPDATED

PERMISSION_UPDATED
```

---

## Event Structure

Example

```json
{
  "eventId": "UUID",
  "eventType": "ACCOUNT_VERIFIED",
  "occurredAt": "2026-07-26T15:00:00",
  "accountId": 15,
  "email": "john@example.com"
}
```

---

# 68. Security Configuration

Spring Security protects every endpoint by default.

Public Endpoints

```text
POST /auth/register

POST /auth/login

POST /auth/verify-email

POST /auth/refresh

POST /auth/forgot-password

POST /auth/reset-password
```

Protected Endpoints

```text
All Remaining APIs
```

---

# 69. Exception Handling

Global Exception Handler returns a standardized response.

Example

```json
{
  "success": false,
  "code": "AUTH_003",
  "message": "Invalid credentials",
  "timestamp": "2026-07-26T15:10:00Z",
  "path": "/api/auth/login"
}
```

---

# 70. Logging Strategy

Every request should generate:

- Request ID
- User ID (if authenticated)
- IP Address
- Execution Time
- Response Status

Sensitive information such as passwords and refresh tokens must never be written to logs.

---

# 71. Monitoring

Recommended metrics:

- Login Success Rate
- Failed Login Attempts
- Active Sessions
- JWT Validation Time
- Refresh Token Usage
- OTP Verification Success Rate
- Kafka Publish Success Rate
- Redis Cache Hit Ratio

---

# 72. Coding Standards

- Java 21
- Spring Boot 3.x
- Constructor Injection
- DTO Pattern
- Repository Pattern
- Service Layer Pattern
- Global Exception Handling
- Validation using Jakarta Validation
- OpenAPI 3 Documentation

---

# 73. Deployment Configuration

Environment variables

```text
JWT_SECRET

JWT_EXPIRED

REFRESH_TOKEN_EXPIRED

REDIS_HOST

REDIS_PORT

MYSQL_HOST

MYSQL_PORT

MYSQL_DATABASE

MYSQL_USERNAME

MYSQL_PASSWORD

KAFKA_BOOTSTRAP_SERVERS
```

---

# 74. Scalability

The Auth Service is stateless.

Multiple instances can run simultaneously.

```text
               API Gateway

                    │

        ┌───────────┼───────────┐

        ▼           ▼           ▼

     Auth-1      Auth-2      Auth-3

          │         │         │

          └─────────┼─────────┘

                 MySQL

                  │

                Redis

                  │

                 Kafka
```

---

# 75. Future Improvements

Planned enhancements:

- Multi-Factor Authentication (MFA)
- Passkey / WebAuthn Support
- Biometric Authentication
- Device Trust Management
- Single Sign-On (SSO)
- Account Risk Detection
- Login Notification
- Suspicious Activity Detection
- Fine-Grained Permission Management
- Audit Dashboard

---

# 76. Conclusion

The Auth Service provides a secure and scalable Identity and Access Management (IAM) solution for the Cinema Management System.

Key responsibilities include:

- User Authentication
- Authorization
- JWT Management
- Refresh Token Rotation
- Session Management
- OAuth2 Integration
- Security Audit
- Event Publishing

The service follows a stateless architecture, communicates with other services through REST APIs and Kafka events, and maintains an isolated database to ensure loose coupling and independent scalability.

---
