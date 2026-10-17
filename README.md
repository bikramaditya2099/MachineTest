# JWT-Based User Authentication System

A full-stack application with **Java Spring Boot** backend featuring JWT authentication, role-based access control, in-memory user storage, and a **Vanilla HTML/CSS/JS** frontend with dark glassmorphism UI.

---

## 📁 Project Structure

```
Assignment/
├── backend/                          # Spring Boot Application
│   ├── pom.xml
│   └── src/main/java/com/blueocean/auth/
│       ├── AuthApplication.java       # Main entry point
│       ├── config/
│       │   ├── SecurityConfig.java    # Spring Security + JWT filter chain
│       │   └── SwaggerConfig.java     # OpenAPI 3 / Swagger UI
│       ├── controller/
│       │   ├── AuthController.java    # /api/auth/**
│       │   ├── DashboardController.java # /api/dashboard
│       │   ├── SettingsController.java  # /api/settings (Admin only)
│       │   └── ProductController.java   # /api/products/**
│       ├── dto/                       # Request/Response DTOs
│       ├── exception/                 # Custom exceptions + GlobalExceptionHandler
│       ├── filter/
│       │   └── JwtAuthFilter.java     # JWT validation per request
│       ├── model/
│       │   ├── User.java              # In-memory user (implements UserDetails)
│       │   ├── Product.java           # JPA entity for PostgreSQL
│       │   └── enums/UserType.java
│       ├── repository/
│       │   └── ProductRepository.java
│       ├── service/
│       │   ├── CountryValidationService.java
│       │   ├── JwtBlacklistService.java
│       │   └── impl/
│       │       ├── UserServiceImpl.java
│       │       └── ProductServiceImpl.java
│       └── util/
│           └── JwtUtil.java
└── frontend/                          # Vanilla HTML/CSS/JS
    ├── index.html                     # Login page
    ├── register.html                  # Registration + OTP verification
    ├── dashboard.html                 # User dashboard + products
    ├── settings.html                  # Admin-only settings
    ├── css/style.css                  # Dark glassmorphism theme
    └── js/
        ├── auth.js                    # Login/register/logout logic
        └── dashboard.js               # Product CRUD + pagination
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| PostgreSQL | 14+ |

### 1. Set Up PostgreSQL

```sql
CREATE DATABASE authdb;
-- User: postgres, Password: postgres (or update application.yml)
```

### 2. Configure Application

Edit `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: postgres
    password: postgres

app:
  dev-mode: true   # Set true to bypass IP country check for localhost testing
```

### 3. Build & Run Backend

```bash
cd Assignment/backend
mvn clean install -DskipTests
mvn spring-boot:run
```

Backend starts at: **http://localhost:8080**

### 4. Open Frontend

Simply open any HTML file in your browser:
```bash
open Assignment/frontend/index.html
# or double-click index.html
```

---

## 📡 API Endpoints

### Auth (Public)

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/api/auth/register` | Register user |
| `POST` | `/api/auth/verify-otp` | Verify OTP |
| `POST` | `/api/auth/login` | Login → JWT token |
| `POST` | `/api/auth/logout` | Logout (blacklist token) |

### Protected (JWT Required)

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| `GET` | `/api/dashboard` | Any | `"Welcome <username>"` |
| `GET` | `/api/settings` | **ADMINISTRATOR** | Admin settings |
| `GET` | `/api/products` | Any | List products (paginated) |
| `GET` | `/api/products/{id}` | Any | Get product |
| `POST` | `/api/products` | Any | Create product |
| `PUT` | `/api/products/{id}` | Any | Update product |
| `DELETE` | `/api/products/{id}` | Any | Delete product |

### Swagger UI

**http://localhost:8080/swagger-ui.html**

---

## 🔑 Authentication Flow

```
1. POST /api/auth/register  →  OTP returned in response (dev mode)
2. POST /api/auth/verify-otp  →  Account activated
3. POST /api/auth/login  →  JWT token returned
4. Use: Authorization: Bearer <token>  →  Access protected endpoints
5. POST /api/auth/logout  →  Token blacklisted
```

---

## ✅ Validation Rules

| Field | Rule |
|-------|------|
| Username | 8–50 characters |
| Email | Must contain `@` and `.` |
| Password | 8–12 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special char |
| UserType | `STANDARD_USER` or `ADMINISTRATOR` |

---

## 🌍 Restricted Countries

Users from these countries cannot register:

| Country | ISO Code |
|---------|----------|
| Syria | SY |
| Afghanistan | AF |
| Iran | IR |
| North Korea | KP |
| Cuba | CU |
| Sudan | SD |
| Myanmar | MM |
| Libya | LY |
| Somalia | SO |
| Yemen | YE |

> Set `app.dev-mode: true` in `application.yml` to bypass IP check during local development.

---

## 🔒 Security Features

- **BCrypt password hashing** (strength 10)
- **JWT HS256** signed tokens with 24h expiration
- **Token blacklisting** on logout (in-memory `HashSet`)
- **Role-based access**: `ADMINISTRATOR` vs `STANDARD_USER`
- **IP-based country blocking** via ip-api.com
- **Stateless sessions** (no server-side session storage)
- **CORS configured** for frontend communication

---

## 📦 Product Management

- **Pagination**: `?page=0&size=10`
- **Sorting**: `?sortBy=name&sortDir=asc`
- **Filtering**: `?name=mouse` (partial, case-insensitive)

---

## 🧪 Testing with Postman

### 1. Register
```json
POST http://localhost:8080/api/auth/register
{
  "username": "adminuser",
  "email": "admin@example.com",
  "password": "Admin@123",
  "userType": "ADMINISTRATOR"
}
```

### 2. Verify OTP (use OTP from register response)
```json
POST http://localhost:8080/api/auth/verify-otp
{
  "username": "adminuser",
  "otp": "482910"
}
```

### 3. Login
```json
POST http://localhost:8080/api/auth/login
{
  "username": "adminuser",
  "password": "Admin@123"
}
```

### 4. Access Dashboard (use token from login)
```
GET http://localhost:8080/api/dashboard
Authorization: Bearer <your-jwt-token>
```

### 5. Access Settings (Admin only)
```
GET http://localhost:8080/api/settings
Authorization: Bearer <admin-jwt-token>
```

---

## 🧑‍💻 Technology Stack

### Backend
- Java 17
- Spring Boot 3.2
- Spring Security 6
- JJWT 0.11.5 (JWT)
- Spring Data JPA
- PostgreSQL
- Lombok
- SpringDoc OpenAPI 2.3 (Swagger)

### Frontend
- HTML5 / CSS3 / Vanilla JavaScript
- Inter font (Google Fonts)
- Dark glassmorphism design system

---

## 📝 Notes

- Passwords are **never stored in plain text** — BCrypt is used
- OTP is returned in the API response **for development only** — in production, it would be emailed
- JWT blacklist is **in-memory** — restarts clear logged-out tokens. Use Redis for production
- Country validation uses [ip-api.com](http://ip-api.com) free tier (1,500 requests/minute)
