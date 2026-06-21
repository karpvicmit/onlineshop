# UML Diagramme - OnlineShop

🛒 **UML Diagramme - OnlineShop**  
**Projekt:** OnlineShop | **Autor:** Karpenko Viktor

**Technologie-Stack:** Java 21 · Spring Boot 4.0.6 · Spring Security 6 · Thymeleaf · MySQL 8.0 · Flyway V1–V7 · Docker

---

## 1. Entity-Klassen und ihre Beziehungen

Dieses Diagramm zeigt alle Daten-Entitäten des Online-Shops und deren Beziehungen. Neu seit letzter Version: OAuth2-Felder in `User`, Soft-Delete + Optimistic Locking in `Product`.

```mermaid
classDiagram
direction TB

class User {
    -id: Long
    -email: String
    -passwordHash: String
    -role: Role
    -status: UserStatus
    -firstName: String
    -lastName: String
    -address: String
    -createdAt: LocalDateTime
    -authProvider: AuthProvider
    -providerId: String
}
class AuthProvider {
    <<enumeration>>
    LOCAL
    GOOGLE
}
class Role {
    <<enumeration>>
    USER
    ADMIN
}
class UserStatus {
    <<enumeration>>
    ACTIVE
    BLOCKED
}
class Category {
    -id: Long
    -name: String
    -description: String
    -slug: String
    -createdAt: LocalDateTime
}
class Product {
    -id: Long
    -name: String
    -description: String
    -price: BigDecimal
    -stock: Integer
    -imageUrl: String
    -category: Category
    -deleted: boolean
    -version: Integer
    -createdAt: LocalDateTime
    -updatedAt: LocalDateTime
}
class Cart {
    -id: Long
    -user: User
    -items: List~CartItem~
}
class CartItem {
    -id: Long
    -cart: Cart
    -product: Product
    -quantity: Integer
}
class Order {
    -id: Long
    -user: User
    -orderDate: LocalDateTime
    -totalAmount: BigDecimal
    -status: OrderStatus
    -deliveryAddress: String
    -orderItems: List~OrderItem~
}
class OrderItem {
    -id: Long
    -order: Order
    -product: Product
    -quantity: Integer
    -unitPrice: BigDecimal
}
class OrderStatus {
    <<enumeration>>
    NEW
    CONFIRMED
    SHIPPED
    CANCELLED
}
class Favorite {
    -id: Long
    -user: User
    -product: Product
    -createdAt: LocalDateTime
}
```

---

## 2. Service-Schicht (Business-Logik)

Alle Services sind zustandslos (Stateless) und über `@Transactional` abgesichert. `OrderService.checkout()` ruft `CartService`, `ProductRepository` und `PriceCalculatorService` auf.

```mermaid
classDiagram
direction TB

class UserService {
    +register(dto): User
    +getCurrentUser(): User
    +getAllUsers(Pageable): Page~User~
    +updateUserRole(id, role): void
    +updateUserStatus(id, status): void
}
class ProductService {
    +findProducts(name, category, pageable): Page~ProductDto~
    +getProductById(id): ProductDto
    +saveProduct(product, image): Product
    +deleteProduct(id): void
}
class CategoryService {
    +getAllCategories(): List~Category~
    +saveCategory(category): Category
    +deleteCategory(id): void
}
class CartService {
    +getCartDtoForUser(userId): CartDto
    +addItemToCart(userId, productId, qty): void
    +updateItemQuantity(userId, productId, qty): void
    +removeItemFromCart(userId, productId): void
    +clearCart(userId): void
}
class OrderService {
    +checkout(user): Order
    +getOrderHistory(user): List~Order~
    +getOrderDetails(id, user): Order
    +updateOrderStatus(id, status): void
}
class FavoriteService {
    +toggleFavorite(user, productId): void
    +getFavoritesByUserId(userId): List~Favorite~
}
class ProfileService {
    +updateProfile(user, dto): void
}
class PriceCalculatorService {
    +calculateTotal(cart): BigDecimal
    +calculateTotalWithLockedPrices(items, lockedProducts): BigDecimal
}
class AnalyticsService {
    +getMonthlyRevenue(): List~MonthlyRevenueDto~
    +getTop10Products(): List~TopProductDto~
    +getOrdersByStatus(): List~OrderStatusCountDto~
}
class FileUploadService {
    +storeFile(MultipartFile): String
}

OrderService --> CartService : leert Warenkorb nach Checkout
OrderService --> ProductRepository : findByIdWithLock (PESSIMISTIC_WRITE)
OrderService --> PriceCalculatorService : berechnet totalAmount
CartService --> ProductRepository : prüft Lagerbestand
CartService --> CartMapper : Entity → DTO
ProductService --> FileUploadService : speichert Produktbild
ProductService --> ProductMapper : Entity → DTO
CategoryService --> SlugUtil : generiert URL-Slug
FavoriteService --> ProductRepository : prüft Existenz
```

---

## 3. Schichtenarchitektur (Layered Architecture)

Klassische 3-Schichten-Architektur mit klaren Verantwortlichkeiten. Cross-Cutting Concerns (Security, Exception Handling, Logging) sind horizontal integriert.

```mermaid
graph TD
    A["🌐 Browser (Thymeleaf + Bootstrap 5)"] --> B["🎮 Controller-Schicht"]
    B --> C["⚙️ Service-Schicht"]
    C --> D["💾 Repository-Schicht (Spring Data JPA)"]
    D --> E["🗄️ MySQL 8.0 + Flyway V1–V7"]

    B -.->|"Spring Security 6, BCrypt, OAuth2"| F["🔐 Security"]
    C -.->|"Transactional, Slf4j"| G["🚨 Cross-Cutting"]
    C -.->|"GlobalExceptionHandler"| H["⚠️ Exception Handling"]

    style A fill:#FFFFff,stroke:#2E75B6,stroke-width:2px,color:#000000
    style B fill:#E8F4FD,stroke:#2E75B6,stroke-width:2px,color:#000000
    style C fill:#D6E4F0,stroke:#1F5C99,stroke-width:2px,color:#000000
    style D fill:#EBF5FB,stroke:#2E75B6,stroke-width:2px,color:#000000
    style E fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style F fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style G fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style H fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
```

---

## 4. Prozess: Bestellaufgabe (Checkout) mit Pessimistic Locking

Der Checkout läuft in einer einzigen `@Transactional`-Transaktion ab. Neu: Pessimistic Locking (`PESSIMISTIC_WRITE`) verhindert Overselling bei parallelen Bestellungen.

```mermaid
sequenceDiagram
    participant Browser
    participant CheckoutC as CheckoutController
    participant OrderService
    participant ProductRepo
    participant CartService
    participant PriceCalc
    participant OrderRepo
    participant DB as MySQL

    Browser->>CheckoutC: POST /shop/checkout/confirm
    CheckoutC->>OrderService: checkout(user)
    activate OrderService
    Note over OrderService: Transactional START

    OrderService->>OrderService: Prüfe: user.address vorhanden?
    OrderService->>OrderService: Lade Cart mit Items

    loop Für jedes CartItem
        OrderService->>ProductRepo: findByIdWithLock(id)
        Note over ProductRepo: SELECT ... FOR UPDATE
        ProductRepo->>DB: PESSIMISTIC_WRITE Lock
        DB-->>ProductRepo: locked Product
        ProductRepo-->>OrderService: Product

        alt stock < required
            OrderService-->>CheckoutC: ProductOutOfStockException
            Note over OrderService: ROLLBACK
        else stock OK
            OrderService->>OrderService: stock -= quantity
            OrderService->>OrderService: Erstelle OrderItem (unitPrice = locked price)
        end
    end

    OrderService->>PriceCalc: calculateTotalWithLockedPrices(items, lockedProducts)
    PriceCalc-->>OrderService: totalAmount

    OrderService->>OrderRepo: save(new Order)
    OrderRepo->>DB: INSERT INTO orders
    OrderRepo->>DB: INSERT INTO order_items (N)

    OrderService->>CartService: clearCart(userId)
    CartService->>DB: DELETE FROM cart_item

    alt Fehler / Exception
        OrderService->>DB: ROLLBACK
        OrderService-->>CheckoutC: Exception
    else Erfolg
        OrderService->>DB: COMMIT
        OrderService-->>CheckoutC: Order
    end

    deactivate OrderService
    CheckoutC-->>Browser: redirect /shop/orders (mit successMessage)
```

---

## 5. Prozess: Produktsuche und Filterung (Specification Pattern)

Dynamische Queries via `ProductSpecification` kombinierbar mit `Specification.where(...).and(...)`. Soft-Delete-Filter immer aktiv.

```mermaid
sequenceDiagram
    participant Browser
    participant ProductC as ProductController
    participant ProductService
    participant Spec as ProductSpecification
    participant ProductR as ProductRepository
    participant DB as MySQL

    Browser->>ProductC: GET /shop/products?q=iphone&category=elektronik&page=0
    ProductC->>ProductService: findProducts("iphone", "elektronik", Pageable)

    ProductService->>Spec: hasNameAndCategory("iphone", "elektronik")
    Spec->>Spec: WHERE LOWER(name) LIKE '%iphone%' AND category.slug = 'elektronik'
    Spec->>Spec: and(isNotDeleted())
    Spec->>Spec: WHERE deleted = false
    Spec-->>ProductService: Specification~Product~

    ProductService->>ProductR: findAll(spec, PageRequest.of(0, 12, Sort))
    ProductR->>DB: SELECT * FROM products WHERE ... LIMIT 12 OFFSET 0
    DB-->>ProductR: List~Product~
    ProductR-->>ProductService: Page~Product~
    ProductService->>ProductService: map(product -> productMapper.toDto)
    ProductService-->>ProductC: Page~ProductDto~
    ProductC->>ProductC: model.addAttribute("products", ...)
    ProductC-->>Browser: render shop/products/list.html
```

---

## 6. Use-Case Diagramm

Übersicht aller Funktionalitäten aus Sicht der Benutzer. Neu: OAuth2-Login (Google) als optionale Alternative.

```mermaid
graph TD
    USER["👥 Endkunde (USER)"]
    ADMIN["👥 Administrator"]

    USER --> UC1["Registrieren (E-Mail + Passwort)"]
    USER --> UC2["Login / Logout (Session)"]
    USER --> UC2a["OAuth2 Login (Google)"]
    USER --> UC3["Produktliste durchsuchen"]
    USER --> UC4["Produktdetails anzeigen"]
    USER --> UC5["Warenkorb verwalten"]
    USER --> UC6["Bestellung aufgeben (Checkout)"]
    USER --> UC7["Bestellhistorie einsehen"]
    USER --> UC8["Favoriten verwalten (Toggle)"]
    USER --> UC9["Profil bearbeiten"]

    ADMIN --> UC2
    ADMIN --> UC10["Produkte verwalten (CRUD + Bild)"]
    ADMIN --> UC11["Kategorien verwalten"]
    ADMIN --> UC12["Bestellungen verwalten"]
    ADMIN --> UC13["Benutzer verwalten (sperren/Rolle)"]
    ADMIN --> UC14["Analysen einsehen (Chart.js)"]
    ADMIN --> UC15["CSV-Export"]

    style USER fill:#EBF5FB,stroke:#2E75B6,stroke-width:2px,color:#000000
    style ADMIN fill:#EBF5FB,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC10 fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC11 fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC12 fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC13 fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC14 fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC15 fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style UC2a fill:#D4EDDA,stroke:#198754,stroke-width:2px,color:#000000
```

⭐ = Wunschkriterium (realisiert)

---

## 7. Datenbankschema (Entity-Relationship)

Flyway-Migrationen V1–V7 erstellen diese Tabellen automatisch beim Applikationsstart.

```mermaid
erDiagram
    USERS ||--|| CART : "1:1"
    USERS ||--o{ ORDERS : "1:N"
    USERS ||--o{ FAVORITES : "1:N"
    CATEGORIES ||--o{ PRODUCTS : "1:N"
    PRODUCTS ||--o{ CART_ITEMS : "1:N"
    CART ||--o{ CART_ITEMS : "1:N"
    PRODUCTS ||--o{ ORDER_ITEMS : "1:N"
    ORDERS ||--o{ ORDER_ITEMS : "1:N"
    PRODUCTS ||--o{ FAVORITES : "1:N"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar role "USER|ADMIN"
        varchar status "ACTIVE|BLOCKED"
        varchar first_name
        varchar last_name
        text address
        datetime created_at
        varchar auth_provider "LOCAL|GOOGLE"
        varchar provider_id
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
        text description
        varchar slug UK
        datetime created_at
    }
    PRODUCTS {
        bigint id PK
        varchar name
        text description
        decimal price
        int stock
        varchar image_url
        bigint category_id FK
        boolean deleted "Soft-Delete"
        int version "Optimistic Lock"
        datetime created_at
        datetime updated_at
    }
    CART {
        bigint id PK
        bigint user_id FK
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
    }

    ORDERS {
        bigint id PK
        bigint user_id FK
        datetime order_date
        decimal total_amount
        varchar status "NEW|CONFIRMED|SHIPPED|CANCELLED"
        text delivery_address
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        decimal unit_price "Preis zum Bestellzeitpunkt"
    }
    FAVORITES {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        datetime created_at
    }
```

---

## 8. Security-Flow (Spring Security 6 + OAuth2)

Zwei parallele Authentifizierungswege: klassischer Form-Login mit BCrypt und optionaler OAuth2-Flow über Google.

```mermaid
graph LR
    A["🌐 Browser"] -->|"1. Login-Seite"| B["SecurityFilterChain"]

    subgraph "Pfad A: Form-Login"
        B -->|"2a. username + password"| C["AuthenticationManager"]
        C -->|"3a. loadUserByUsername"| D["CustomUserDetailsService"]
        D -->|"4a. SELECT * FROM users WHERE email=?"| DBX[("MySQL users")]
        DBX -->|"5a. User Entity"| D
        D -->|"6a. CustomUserDetails"| C
        C -->|"7a. BCrypt.matches()"| E["BCryptPasswordEncoder (Stärke 12)"]
    end

    subgraph "Pfad B: OAuth2 Google"
        B -->|"2b. /oauth2/authorization/google"| F["OAuth2LoginFilter"]
        F -->|"3b. Authorization Code Flow"| G["Google Authorization Server"]
        G -->|"4b. Access Token + User Info"| H["CustomOAuth2UserService"]
        H -->|"5b. findByEmail / createUser"| DBX
        H -->|"6b. CustomUserDetails + attributes"| C
    end

    E -->|"matches"| I["SessionRepository"]
    I -->|"JSESSIONID Cookie"| A
    C -->|"Auth OK"| I

    A -->|"nächster Request + Cookie"| B
    B -->|"check Session"| I
    I -->|"valid"| J["Controller (RBAC)"]
    J -->|"ROLE_ADMIN → /admin/**"| K["Admin-Seiten"]
    J -->|"ROLE_USER → /shop/**"| L["Shop-Seiten"]
    J -->|"anonymous → /login"| M["Login-Seite"]

    style B fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style C fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style E fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style F fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style H fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style I fill:#E8F4FD,stroke:#2E75B6,stroke-width:2px,color:#000000
    style J fill:#E8F4FD,stroke:#2E75B6,stroke-width:2px,color:#000000
```

---

## 9. Exception Handling (GlobalExceptionHandler)

`@ControllerAdvice` fängt alle Exceptions zentral ab und rendert passende Fehlerseiten. Neu: HTTP 409 für Out-of-Stock und Email-Exists.

```mermaid
graph TD
    A["Controller / Service"] -->|"throws"| B["ResourceNotFoundException"]
    A -->|"throws"| C["ProductOutOfStockException"]
    A -->|"throws"| D["EmailAlreadyExistsException"]
    A -->|"throws"| E["CartNotFoundException"]
    A -->|"throws"| F["IllegalStateException / IllegalArgumentException"]
    A -->|"throws"| G["DataIntegrityViolationException"]
    A -->|"throws"| H["Exception (catch-all)"]

    B -->|"@ExceptionHandler 404"| I["error/404.html"]
    C -->|"@ExceptionHandler 409"| J["error/409.html"]
    D -->|"@ExceptionHandler 409"| J
    E -->|"@ExceptionHandler 404"| I
    F -->|"@ExceptionHandler 400"| K["error/400.html"]
    G -->|"@ExceptionHandler 409"| J
    H -->|"@ExceptionHandler 500"| L["error/500.html"]

    I --> M["SLF4J / Logback"]
    J --> M
    K --> M
    L --> M

    style B fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style C fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style D fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style E fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style F fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style G fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
    style H fill:#FFE8E8,stroke:#C00000,stroke-width:2px,color:#000000
```

---

## 10. Order Status State Machine

Der `OrderService` validiert Statusübergänge. Nicht alle Transitionen sind erlaubt (z.B. CANCELLED → CONFIRMED ist verboten).

```mermaid
stateDiagram-v2
    [*] --> NEW : Bestellung aufgegeben (checkout)

    NEW --> CONFIRMED : Admin bestätigt
    NEW --> CANCELLED : Admin storniert

    CONFIRMED --> SHIPPED : Admin versendet
    CONFIRMED --> CANCELLED : Admin storniert

    SHIPPED --> CANCELLED : Admin storniert (Rückabwicklung)

    CANCELLED --> [*] : Endzustand (keine weiteren Übergänge)
    SHIPPED --> [*] : Endzustand

    note right of NEW
        isValidTransition(NEW, SHIPPED) = false
        Man muss über CONFIRMED gehen!
    end note

    note right of CANCELLED
        Kein Übergang mehr möglich!
        isValidTransition(CANCELLED, *) = false
    end note
```

---

## 11. OAuth2 Login Flow (Google)

Detaillierter Ablauf des OAuth2-Logins. `CustomOAuth2UserService` erstellt automatisch neue User oder verknüpft bestehende.

```mermaid
sequenceDiagram
    participant User
    participant Browser
    participant App as Spring Security
    participant Google
    participant DB as MySQL

    User->>Browser: Klick "Mit Google anmelden"
    Browser->>App: GET /oauth2/authorization/google
    App->>Google: Redirect zu Google Consent Screen
    Google->>User: Login + Berechtigung erteilen
    User->>Google: "Allow"
    Google->>App: Callback mit Authorization Code
    App->>Google: Exchange Code → Access Token
    Google-->>App: Access Token + User Info (email, name, sub)

    App->>App: CustomOAuth2UserService.loadUser()
    App->>DB: SELECT * FROM users WHERE email = ?

    alt User existiert bereits
        DB-->>App: Existing User
        App->>App: Prüfe status != BLOCKED
        App->>App: Verknüpfe providerId (falls neu)
    else Neuer User
        App->>App: Erstelle User (ROLE_USER, AuthProvider.GOOGLE)
        App->>App: Random password (wird nie genutzt)
        App->>DB: INSERT INTO users
        DB-->>App: Saved User
    end

    App->>App: Erstelle CustomUserDetails(user, attributes)
    App->>App: AuthSuccessHandler.onAuthenticationSuccess()

    alt ROLE_ADMIN
        App-->>Browser: Redirect /admin/dashboard
    else ROLE_USER
        App-->>Browser: Redirect /shop/products
    end
```

---

## 12. Favorite Toggle mit Race-Condition-Handling

Der `FavoriteService` nutzt eine "check-then-act"-Logik. Ein UNIQUE-Constraint auf DB-Ebene fängt Duplikate ab, falls zwei Requests parallel eintreffen.

```mermaid
sequenceDiagram
    participant User
    participant FavC as FavoriteController
    participant FavService
    participant FavRepo
    participant DB as MySQL

    User->>FavC: POST /shop/favorites/toggle/{productId}
    FavC->>FavService: toggleFavorite(user, productId)
    activate FavService

    FavService->>FavRepo: findByUserIdAndProductId(userId, productId)
    FavRepo->>DB: SELECT ... WHERE user_id=? AND product_id=?
    DB-->>FavRepo: Optional~Favorite~

    alt Favorite existiert bereits
        FavRepo-->>FavService: Optional.of(existing)
        FavService->>FavRepo: delete(existing)
        FavRepo->>DB: DELETE FROM favorites WHERE id=?
        Note over FavService: Produkt aus Favoriten entfernt
    else Favorite existiert NICHT
        FavRepo-->>FavService: Optional.empty()
        FavService->>FavRepo: findById(productId)
        FavService->>FavService: Erstelle new Favorite(user, product)

        FavService->>FavRepo: save(favorite)
        FavRepo->>DB: INSERT INTO favorites

        alt Erfolg
            DB-->>FavRepo: OK
            Note over FavService: Produkt zu Favoriten hinzugefügt
        else DataIntegrityViolationException (Race Condition!)
            DB-->>FavRepo: Duplicate entry
            FavRepo-->>FavService: Exception
            Note over FavService: Transactional noRollbackFor
            Note over FavService: catch + log.warn("Duplicate")
            Note over FavService: Keine Exception an Caller!
        end
    end

    deactivate FavService
    FavC-->>User: Redirect /shop/products
```

---

## 13. Docker Deployment

Orchestrierung via `docker-compose.yml` mit Healthchecks, Named Volumes und isoliertem Bridge-Network.

```mermaid
graph TB
    A["💻 Entwicklungs-PC"] -->|"docker-compose up --build"| DC["🐳 Docker Compose"]

    subgraph DC["Docker Compose Stack"]
        B["Spring Boot App<br/>Java 21 · Port 8080<br/>Healthcheck: /actuator/health"]
        C["MySQL 8.0<br/>Port 3306<br/>Healthcheck: mysql ping"]
        D["Flyway Migrations V1 – V7<br/>(auto at startup)"]
        E["Docker Network<br/>onlineshop-network (bridge)"]
    end

    F[("Docker Volumes<br/>mysql_data<br/>app_uploads")]
    G["🌐 Browser<br/>http://localhost:8080"]
    H["📁 .env File<br/>DB_PASSWORD<br/>ADMIN_EMAIL<br/>GOOGLE_CLIENT_ID"]

    B -->|"JDBC"| C
    C -->|"executes at startup"| D
    B -.->|"mount /app/uploads"| F
    C -.->|"mount /var/lib/mysql"| F
    G -->|"HTTP"| B
    E -.-> B
    E -.-> C
    H -.->|"env vars"| B
    H -.->|"env vars"| C

    style DC fill:#F0F0F0,stroke:#000000,stroke-width:3px,color:#000000
    style B fill:#E8F4FD,stroke:#2E75B6,stroke-width:2px,color:#000000
    style C fill:#FFF2CC,stroke:#2E75B6,stroke-width:2px,color:#000000
    style D fill:#EBF5FB,stroke:#2E75B6,stroke-width:2px,color:#000000
    style F fill:#D4EDDA,stroke:#198754,stroke-width:2px,color:#000000
    style H fill:#F8D7DA,stroke:#C00000,stroke-width:2px,color:#000000
```

🔐 **Sicherheit im Docker-Deployment:**

- Alle sensiblen Daten (Passwörter, OAuth2-Credentials) kommen aus der `.env`-Datei — nie im Code hartcodiert.
- `.env` ist in `.gitignore` eingetragen.
- Admin-Credentials werden beim ersten Start automatisch angelegt (`DataInitializer`).

---

## 14. Flyway Migrations-Übersicht

```mermaid
graph LR
    V1["V1: users"] --> V2["V2: categories"]
    V2 --> V3["V3: products"]
    V3 --> V4["V4: orders + order_items"]
    V4 --> V5["V5: cart + cart_item"]
    V5 --> V6["V6: favorites"]
    V6 --> V7["V7: initial data"]

    style V1 fill:#E8F4FD,stroke:#2E75B6,color:#000000
    style V2 fill:#E8F4FD,stroke:#2E75B6,color:#000000
    style V3 fill:#E8F4FD,stroke:#2E75B6,color:#000000
    style V4 fill:#E8F4FD,stroke:#2E75B6,color:#000000
    style V5 fill:#E8F4FD,stroke:#2E75B6,color:#000000
    style V6 fill:#E8F4FD,stroke:#2E75B6,color:#000000
    style V7 fill:#D4EDDA,stroke:#198754,color:#000000
```

---

**Projekt Level 2** | **Karpenko Viktor** | **22.06.2026**

Alle Diagramme wurden mit [Mermaid](https://mermaid.js.org/) generiert.

**Letzte Aktualisierung:** Umsetzung von OAuth2-Login, Pessimistic Locking, Soft-Delete und Flyway V7 berücksichtigt.