# ![favicon-32x32.png](src/main/resources/static/images/favicons/favicon-32x32.png) OnlineShop

**A full-featured e-commerce platform**   
It demonstrates the complete software development lifecycle — from requirements analysis to containerized delivery.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Server--Side-005F0F)
![Flyway](https://img.shields.io/badge/Flyway-V1--V7-red)
![License](https://img.shields.io/badge/License-Educational-lightgrey)

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Quick Start (Docker)](#-quick-start-docker)
- [Test Credentials](#-test-credentials)
- [Database Schema (Flyway)](#-database-schema-flyway)
- [Testing](#-testing)
- [Environment Variables](#-environment-variables)
- [Project Documentation](#-project-documentation)
- [Author](#-author)

---

## ✨ Features

### 👤 Customer Area (USER)
- ✅ User registration with BCrypt password hashing
- ✅ Role-based redirect after login (USER → product list, ADMIN → dashboard)
- ✅ OAuth2 login via Google (optional, additional to session-based auth)
- ✅ Product catalog with pagination, search, and category filtering
- ✅ Product detail page with image, price, and stock availability
- ✅ Shopping cart: add / update quantity / remove items / total calculation
- ✅ Checkout with stock validation and price snapshot at order time
- ✅ Order history and detailed order view
- ✅ Favorites list (toggle logic, no duplicates via UNIQUE constraint)
- ✅ User profile with editable personal data and delivery address

### 🛡 Admin Area (ADMIN)
- ✅ Dashboard with quick access to all management sections
- ✅ Product CRUD with image upload (max 5 MB, MIME validation)
- ✅ Category CRUD with auto-generated URL-friendly slugs (German umlaut support)
- ✅ Order management: view details, change status (NEW → CONFIRMED → SHIPPED / CANCELLED)
- ✅ User management: block / unblock accounts, change roles
- ✅ Analytics dashboard: monthly revenue chart (Chart.js), Top-10 products, order status distribution

### 🔒 Security & Quality
- ✅ Spring Security with RBAC and session-based authentication
- ✅ SQL-injection protection (JPA Prepared Statements)
- ✅ Path traversal protection on file uploads
- ✅ Global exception handler with dedicated error pages (403 / 404 / 500)
- ✅ Bean Validation (`@NotBlank`, `@Email`, `@PositiveOrZero`)
- ✅ Soft-delete for products (preserves order history)
- ✅ Optimistic locking (`@Version`) and pessimistic locking (`PESSIMISTIC_WRITE`) during checkout

---

## 🛠 Tech Stack

| Category | Technology |
|---|---|
| **Language** | Java 21 LTS |
| **Framework** | Spring Boot 4.0.6 |
| **Security** | Spring Security 6, OAuth2 Client, BCrypt |
| **Web / UI** | Spring MVC, Thymeleaf, Bootstrap 5, Chart.js |
| **ORM / DB** | Spring Data JPA, Hibernate, MySQL 8.0 |
| **Migrations** | Flyway (V1 – V7) |
| **Validation** | Jakarta Bean Validation |
| **Logging** | SLF4J + Logback (`@Slf4j`) |
| **Testing** | JUnit 5, Mockito, AssertJ, MockMvc, H2 |
| **Containerization** | Docker, Docker Compose |
| **Build Tool** | Maven 3.9+ |

---

## 🏗 Architecture

The project follows a classic **MVC + Service + Repository** layered architecture with a clear separation of concerns:

```
com.karpenko.onlineshop
├── config/              # SecurityConfig, WebConfig, DataInitializer
├── controller/
│   ├── admin/           # AdminDashboard, Product, Category, Order, User, Analytics
│   ├── auth/            # AuthController (login / register)
│   └── shop/            # Product, Cart, Checkout, Order, Favorite, Profile
├── dto/                 # CartDto, ProductDto, UserRegistrationDto, analytics DTOs
├── entity/              # User, Product, Category, Cart, Order, Favorite, OrderItem
├── exception/           # GlobalExceptionHandler + custom exceptions
├── mapper/              # CartMapper, ProductMapper
├── repository/          # Spring Data JPA repositories + AnalyticsRepository
├── security/            # CustomUserDetails, OAuth2UserService, AuthSuccessHandler
├── service/             # Interfaces
│   └── impl/            # Implementations with @Transactional
├── specification/       # ProductSpecification (dynamic queries)
└── util/                # SlugUtil
```

---

## 🚀 Quick Start (Docker)

### 1. Clone the repository
```bash
git clone <https://github.com/karpvicmit/onlineshop>
```

### 2. Create the `.env` file
Copy `.env.example` to `.env` and adjust values if needed:
```bash
cp .env.example .env
```
All required variables are pre-filled in `.env.example` — you can run the app as-is.

### 3. Start the application
```bash
docker-compose up --build
```

### 4. Open in your browser
- 🌐 Shop: **http://localhost:8080**
- 🛡 Admin panel: **http://localhost:8080/admin/dashboard**
- 🏥 Healthcheck: **http://localhost:8080/actuator/health**

> ⏱ First startup takes ~60–90 seconds (image build + MySQL init + Flyway migrations).

### Stop the application
```bash
docker-compose down          # stop containers
docker-compose down -v       # stop and remove the DB volume
```

---

## 🔑 Test Credentials

### 👑 Administrator (auto-created on first startup)
| Field | Value |
|---|---|
| Email | `admin@onlineshop.de` |
| Password | `AdminSecure2026!` |

> ⚠️ **Important:** change the password after the first login.

### 👤 Regular user
Register a new user via the **/register** form, or use the test account:
- Email: `user@test.de`
- Password: `SecurePass123`

---

## 🗄 Database Schema (Flyway)

Migrations run automatically on application startup under the `prod` profile.

| File | Description |
|---|---|
| `V1__create_users.sql` | `users` table with indexes on `email` and `provider_id` |
| `V2__create_categories.sql` | `categories` table with unique slug |
| `V3__create_products.sql` | `products` table (soft-delete, optimistic lock) |
| `V4__create_orders_and_order_items.sql` | `orders` and `order_items` tables (price snapshot at order time) |
| `V5__create_cart_and_cart_items.sql` | `cart` (1:1 with user) and `cart_item` tables |
| `V6__create_favorites.sql` | `favorites` table with UNIQUE(user_id, product_id) |
| `V7__insert_initial_data.sql` | Initial categories and sample products |

---

## 🧪 Testing

The project is covered by unit and integration tests:

```bash
# Run all tests
./mvnw test

# Service unit tests only
./mvnw test -Dtest=*ServiceTest

# Integration tests
./mvnw test -Dtest=FullFlowIntegrationTest
```

### Test coverage
- ✅ `AuthSuccessHandler` — role-based redirect
- ✅ `SecurityConfig` — access control for `/admin/**`, public and protected endpoints
- ✅ `ProductRepository` — search, filtering, pagination, soft-delete
- ✅ `OrderRepository` — JPQL queries with eager loading
- ✅ `CartService` — add, update, stock validation
- ✅ `OrderService` — checkout, atomic stock decrease, status transitions, price snapshot
- ✅ `FavoriteService` — toggle logic, race condition handling
- ✅ `UserService` — registration, email normalization, blocking
- ✅ `FileUploadService` — MIME validation, size limit, path traversal protection
- ✅ `SlugUtil` — slug generation with German umlaut support (ä → ae, ß → ss)
- ✅ `FullFlowIntegrationTest` — end-to-end scenario: register → cart → checkout → verify order

---

## ⚙️ Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_NAME` | Database name | `onlineshop_db` |
| `DB_USER` | Database user | `onlineshop_user` |
| `DB_PASSWORD` | Database user password | — |
| `DB_ROOT_PASSWORD` | MySQL root password | — |
| `DB_PORT` | MySQL port | `3306` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `ADMIN_EMAIL` | Administrator email (first startup) | `admin@onlineshop.de` |
| `ADMIN_PASSWORD` | Administrator password (min. 8 chars) | `AdminSecure2026!` |
| `APP_UPLOAD_DIR` | Path for uploaded images | `/app/uploads` |
| `GOOGLE_CLIENT_ID` | OAuth2 Google Client ID *(optional)* | — |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Google Client Secret *(optional)* | — |

---

## 📄 Project Documentation

Full project documentation (Lastenheft, Pflichtenheft) is located in the repository root:
- `Lastenheft_OnlineShop.docx` — customer requirements specification
- `Pflichtenheft_OnlineShop.docx` — technical implementation specification

---

## 👨‍💻 Author

**Karpenko Viktor**  
Developed as part of the *Softwareentwicklung Weiterbildung* course (IBB), 2026.

---