# Technical diagrams

Diagrams of the BG Sistemas Informáticos backend, written in [Mermaid](https://mermaid.js.org/) so GitHub draws them from the text (BGSI-168).
PNG versions for slides are in [`docs/diagrams/`](diagrams/). Updated with the security, search, database and storage work of October 2026.

1. [Architecture](#1-architecture)
2. [Entity-relationship diagram](#2-entity-relationship-diagram) (BGSI-6)
3. [Domain class diagram](#3-domain-class-diagram) (BGSI-7)
4. [Layers of one feature: Contact](#4-layers-of-one-feature-contact) (BGSI-7)
5. [Strategy pattern: export to Excel / PDF](#5-strategy-pattern-export-to-excel--pdf) (BGSI-7)
6. [Image storage: local folder or Supabase](#6-image-storage-local-folder-or-supabase) (BGSI-327)
7. [Login with two-step verification](#7-login-with-two-step-verification) (BGSI-274, BGSI-276)
8. [Password confirmation for sensitive actions](#8-password-confirmation-for-sensitive-actions) (BGSI-275)

## 1. Architecture

The Vue app calls the REST API with a JWT. Spring Security protects `/api/admin/**`, and every feature follows the same layers down to the database: MySQL in Docker for development, PostgreSQL on Supabase for production (same code, only the Spring profile changes). The images go to the `uploads/` folder or to Supabase Storage, and two nightly tasks delete what is no longer useful.

```mermaid
flowchart LR
    visitor([Visitor]) --> spa
    admin([Admin]) --> spa
    subgraph front["Frontend · Vue 3 + Vite :5173"]
        spa["Views + components"] --> comp["Composables"]
        comp --> svc["Services + mappers + models"]
        svc --> repo["Repositories (axios)"]
        store["Pinia auth store (JWT)"] -.-> repo
    end
    repo -- "REST JSON · Bearer JWT" --> sec
    subgraph back["Backend · Spring Boot 4 :8080"]
        sec["Spring Security<br/>JWT · 2FA · login lock<br/>/api/admin/** = ADMIN"] --> ctrl["Controllers + DTOs"]
        ctrl --> service["Services (interfaces + Impl)"]
        service --> jpa["Spring Data JPA repositories"]
        service --> mail["EmailNotificationService @Async"]
        service --> files["Exporters (Excel / PDF)"]
        service --> storage["ImageStorageService"]
        jobs["Nightly cleanups @Scheduled"] --> jpa
        jobs --> storage
    end
    jpa --> db[("MySQL 8 · Docker (dev)<br/>PostgreSQL · Supabase (prod)")]
    storage --> img[("uploads/ folder<br/>or Supabase Storage")]
    mail --> smtp["Gmail SMTP"]
```

## 2. Entity-relationship diagram

Eleven tables. Foreign keys:

- `brand` 1 — 0..N `service` (`service.brand_id`). A brand with products cannot be deleted (409).
- `rate_question` 1 — 1..N `rate_option` (`rate_option.question_id`). Deleting a question deletes its options.
- `users` 1 — 0..N `password_reset_token`, `email_change_token` and `recovery_codes` (`user_id`). They are deleted with the admin.

Notes:

- `contact.interested_service` stores the product **name** as text on purpose: the lead keeps its history even if the product is deleted.
- Secrets are never stored in clear: passwords and recovery codes as BCrypt hashes, link tokens as SHA-256 hashes, and the 2FA secret encrypted with AES-256-GCM (the key is only in `.env`).
- Columns added later are nullable so the existing rows keep working: `service.active` null = active, `users.status` null = ACTIVE, `users.totp_enabled` null = off.
- Indexes (BGSI-326): `contact (status, created_at)` and `contact (created_at)` for the admin list; `expires_at` in both link tables for the nightly cleanup. Unique: `users.email`, `token_hash` of both link tables.
- In production (PostgreSQL on Supabase) the tables live in their own schema `app`, not in the public one.

```mermaid
erDiagram
    BRAND ||--o{ SERVICE : "offers"
    RATE_QUESTION ||--|{ RATE_OPTION : "has"
    USERS ||--o{ PASSWORD_RESET_TOKEN : "asks"
    USERS ||--o{ EMAIL_CHANGE_TOKEN : "asks"
    USERS ||--o{ RECOVERY_CODES : "keeps"

    BRAND {
        BIGINT id PK
        VARCHAR name
        VARCHAR description
        VARCHAR category
        VARCHAR logo_url
        BOOLEAN active
    }
    SERVICE {
        BIGINT id PK
        VARCHAR name
        VARCHAR description
        VARCHAR long_description "max 2000"
        VARCHAR features "list as text"
        VARCHAR sectors "list as text"
        VARCHAR image_url
        VARCHAR official_url
        BOOLEAN featured
        BOOLEAN active "null = active"
        BIGINT brand_id FK
    }
    CLIENT {
        BIGINT id PK
        VARCHAR name
        VARCHAR business_type
        VARCHAR testimonial "max 1000"
        VARCHAR logo_url
        INT rating "1 to 5"
        BOOLEAN published
    }
    CONTACT {
        BIGINT id PK
        VARCHAR name
        VARCHAR email
        VARCHAR phone
        VARCHAR interested_service "text, not FK"
        VARCHAR message "max 2000"
        BOOLEAN gdpr_consent
        ENUM status "NEW CONTACTED ARCHIVED"
        DATETIME created_at "indexed"
        VARCHAR note "max 1000"
    }
    RATE_QUESTION {
        BIGINT id PK
        VARCHAR title "max 40"
        VARCHAR question "max 150"
        VARCHAR hint
        INT position
        BOOLEAN active
    }
    RATE_OPTION {
        BIGINT id PK
        BIGINT question_id FK
        VARCHAR icon "max 16"
        VARCHAR label "max 100"
        VARCHAR description
        DECIMAL amount "10,2 euros"
        ENUM adjustment_type "BASE FACTOR ADDITION"
        INT position
    }
    USERS {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password "BCrypt hash"
        ENUM role "ADMIN"
        ENUM status "ACTIVE PENDING BLOCKED"
        TIMESTAMP password_changed_at "older JWTs rejected"
        VARCHAR totp_secret "AES-256-GCM"
        BOOLEAN totp_enabled
        BIGINT totp_last_step "a code is used once"
        TIMESTAMP last_login_at
    }
    PASSWORD_RESET_TOKEN {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token_hash UK "SHA-256"
        TIMESTAMP created_at
        TIMESTAMP expires_at "indexed"
        TIMESTAMP used_at
    }
    EMAIL_CHANGE_TOKEN {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR new_email
        VARCHAR token_hash UK "SHA-256"
        TIMESTAMP created_at
        TIMESTAMP expires_at "indexed"
        TIMESTAMP used_at
    }
    RECOVERY_CODES {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR code_hash "BCrypt"
        TIMESTAMP used_at
    }
    SITE_SETTING {
        VARCHAR setting_key PK "max 100"
        VARCHAR setting_value "hero photo, mail settings"
    }
```

## 3. Domain class diagram

JPA entities (`@Entity`) and their enums.

- `Service → Brand`: unidirectional many-to-one association (`@ManyToOne(fetch = LAZY)`).
- `RateQuestion ◆— RateOption`: bidirectional composition (`@OneToMany(cascade = ALL, orphanRemoval = true)`).
- `PasswordResetToken`, `EmailChangeToken`, `RecoveryCode → User`: unidirectional many-to-one (`@ManyToOne(fetch = LAZY)`). `User` does not know them, so loading an admin never loads its links.

```mermaid
classDiagram
    direction LR
    class Brand {
        -Long id
        -String name
        -String description
        -String category
        -String logoUrl
        -boolean active
    }
    class Service {
        -Long id
        -String name
        -String description
        -String longDescription
        -List~String~ features
        -List~String~ sectors
        -String imageUrl
        -String officialUrl
        -boolean featured
        -Boolean active
        -Brand brand
        +isActive() boolean
    }
    class Client {
        -Long id
        -String name
        -String businessType
        -String testimonial
        -String logoUrl
        -Integer rating
        -boolean published
    }
    class Contact {
        -Long id
        -String name
        -String email
        -String phone
        -String interestedService
        -String message
        -boolean gdprConsent
        -ContactStatus status
        -LocalDateTime createdAt
        -String note
    }
    class RateQuestion {
        -Long id
        -String title
        -String question
        -String hint
        -int position
        -boolean active
        -List~RateOption~ options
    }
    class RateOption {
        -Long id
        -RateQuestion question
        -String icon
        -String label
        -String description
        -BigDecimal amount
        -AdjustmentType adjustmentType
        -int position
    }
    class User {
        -Long id
        -String email
        -String password
        -Role role
        -UserStatus status
        -Instant passwordChangedAt
        -String totpSecret
        -Boolean totpEnabled
        -Long totpLastStep
        -Instant lastLoginAt
        +isActive() boolean
        +isTotpEnabled() boolean
    }
    class PasswordResetToken {
        -Long id
        -User user
        -String tokenHash
        -Instant createdAt
        -Instant expiresAt
        -Instant usedAt
        +isUsable(Instant) boolean
    }
    class EmailChangeToken {
        -Long id
        -User user
        -String newEmail
        -String tokenHash
        -Instant createdAt
        -Instant expiresAt
        -Instant usedAt
    }
    class RecoveryCode {
        -Long id
        -User user
        -String codeHash
        -Instant usedAt
    }
    class SiteSetting {
        -String key
        -String value
    }
    class ContactStatus {
        <<enumeration>>
        NEW
        CONTACTED
        ARCHIVED
    }
    class AdjustmentType {
        <<enumeration>>
        BASE
        FACTOR
        ADDITION
    }
    class Role {
        <<enumeration>>
        ADMIN
    }
    class UserStatus {
        <<enumeration>>
        ACTIVE
        PENDING
        BLOCKED
    }
    Brand "1" <-- "0..*" Service : brand
    RateQuestion "1" *-- "1..*" RateOption : options
    User "1" <-- "0..*" PasswordResetToken : user
    User "1" <-- "0..*" EmailChangeToken : user
    User "1" <-- "0..*" RecoveryCode : user
    Contact --> ContactStatus
    RateOption --> AdjustmentType
    User --> Role
    User --> UserStatus
```

## 4. Layers of one feature: Contact

Every feature (`brand`, `service`, `client`, `contact`, `rate`, `setting`...) uses the same layers:
**Controller → Mapper / DTO → Service (interface) → ServiceImpl → Repository → Entity**.
Controllers depend on the interface, and Spring injects the implementation through the constructor.
The admin lists search with one JPQL query with optional filters (`:q IS NULL OR ... LIKE :q ESCAPE '!'`, BGSI-302), the same in MySQL and PostgreSQL.

```mermaid
classDiagram
    direction TB
    class ContactController {
        <<RestController /api/contacts>>
        +createContact(ContactRequestDTO) ResponseEntity
    }
    class AdminContactController {
        <<RestController /api/admin/contacts>>
        +getContacts(status, q, page, size) PageResponseDTO
        +updateContact(id, ContactUpdateRequestDTO) ContactResponseDTO
        +deleteContact(id) ResponseEntity  @RequiresConfirmation
    }
    class ContactMapper {
        <<Component>>
        +toEntity(ContactRequestDTO) Contact
        +toResponse(Contact) ContactResponseDTO
    }
    class ContactRequestDTO {
        <<record>>
    }
    class ContactResponseDTO {
        <<record>>
    }
    class ContactService {
        <<interface>>
        +createContact(Contact) Contact
        +getContacts(ContactStatus, String search, Pageable) Page
        +updateContact(Long, ContactStatus, String) Contact
        +deleteContact(Long) void
    }
    class ContactServiceImpl {
        <<Service>>
        -ContactRepository contactRepository
        -NotificationService notificationService
        +getContacts() @Transactional(readOnly)
    }
    class ContactRepository {
        <<interface>>
        +search(status, q, Pageable) Page  @Query JPQL
        +countByStatus(ContactStatus) long
        +findTop5ByOrderByCreatedAtDesc() List
    }
    class JpaRepository~Contact, Long~ {
        <<interface>>
    }
    class NotificationService {
        <<interface>>
        +notifyNewContact(Contact) void
        +sendTest() void
    }
    class EmailNotificationService {
        <<Service>>
        +notifyNewContact(Contact) void  @Async
    }
    class Contact {
        <<Entity>>
    }

    ContactController --> ContactService
    ContactController --> ContactMapper
    AdminContactController --> ContactService
    AdminContactController --> ContactMapper
    ContactMapper ..> ContactRequestDTO
    ContactMapper ..> ContactResponseDTO
    ContactMapper ..> Contact
    ContactService <|.. ContactServiceImpl
    ContactServiceImpl --> ContactRepository
    ContactServiceImpl --> NotificationService
    NotificationService <|.. EmailNotificationService
    JpaRepository <|-- ContactRepository
    ContactRepository ..> Contact
```

## 5. Strategy pattern: export to Excel / PDF

`ExportService` receives every `Exporter` bean and picks one by format. A new format only needs a new class that implements `Exporter`.

```mermaid
classDiagram
    direction LR
    class AdminExportController {
        <<RestController /api/admin/*/export>>
    }
    class ExportService {
        <<Service>>
        -Map~ExportFormat, Exporter~ exporters
        +download(ExportTable, ExportFormat, fileName) ResponseEntity
    }
    class Exporter {
        <<interface>>
        +format() ExportFormat
        +export(ExportTable) byte[]
    }
    class ExcelExporter {
        <<Component>>
        Apache POI
    }
    class PdfExporter {
        <<Component>>
        OpenPDF
    }
    class ExportTable {
        <<record>>
        +String title
        +List~String~ headers
        +List~List~String~~ rows
    }
    class ExportFormat {
        <<enumeration>>
        PDF
        XLSX
    }
    AdminExportController --> ExportService
    ExportService o-- Exporter
    Exporter <|.. ExcelExporter
    Exporter <|.. PdfExporter
    Exporter ..> ExportTable
    Exporter ..> ExportFormat
```

## 6. Image storage: local folder or Supabase

Dependency inversion: the upload controller and the nightly cleanup only know the `ImageStorageService` interface. `app.storage.type` (`STORAGE_TYPE` in `.env`) decides which implementation Spring creates: `local` (default, the `uploads/` folder) or `supabase` (a public bucket of Supabase Storage, served by its CDN). The database only keeps the URL of each image, never the image.

```mermaid
classDiagram
    direction LR
    class AdminUploadController {
        <<RestController /api/admin/uploads>>
        +uploadImage(MultipartFile) ResponseEntity
    }
    class ImageStorageService {
        <<interface>>
        +store(MultipartFile) String
        +publicUrl(String) String
        +list() List~StoredImage~
        +delete(String) void
    }
    class LocalImageStorageService {
        <<Component · storage.type=local>>
        -Path uploadDir
    }
    class SupabaseImageStorageService {
        <<Component · storage.type=supabase>>
        -RestClient client
        -String bucket
    }
    class ImageFiles {
        <<final>>
        +newName(MultipartFile) String
        +isGeneratedName(String) boolean
    }
    class StoredImage {
        <<record>>
        +String name
        +Instant savedAt
    }
    class UnusedImageCleanup {
        <<Component @Scheduled 03:45>>
        +deleteUnusedImages() int
    }
    class ImageReferences {
        <<Component>>
        +fileNamesInUse() Set~String~
    }
    class ExpiredTokenCleanup {
        <<Component @Scheduled 03:30>>
        +deleteExpiredTokens() int
    }
    AdminUploadController --> ImageStorageService
    UnusedImageCleanup --> ImageStorageService
    UnusedImageCleanup --> ImageReferences
    ImageStorageService <|.. LocalImageStorageService
    ImageStorageService <|.. SupabaseImageStorageService
    LocalImageStorageService ..> ImageFiles
    SupabaseImageStorageService ..> ImageFiles
    ImageStorageService ..> StoredImage
```

The nightly cleanups (`app.cleanup.cron` and `app.cleanup.images-cron`):

| Task | When | What it deletes |
|---|---|---|
| `ExpiredTokenCleanup` | 03:30 | Reset, invitation and email change links that expired more than a day ago |
| `UnusedImageCleanup` | 03:45 | Images older than a day that no brand, client, product or setting uses (old logos, photos of deleted products, images of cancelled forms). Nothing is deleted if the database cannot be read |

## 7. Login with two-step verification

The login is HTTP Basic (email + password) and the answer is a JWT (HS512, 8 hours). After 5 wrong passwords the email is locked for 15 minutes (BGSI-274). With two-step verification on (BGSI-276), the first step only gives a pending token for 5 minutes, and the JWT comes after the 6-digit code of the authenticator app (or a recovery code).

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant F as Vue app
    participant LF as LoginAttemptFilter
    participant S as Spring Security<br/>(BasicAuthenticationFilter)
    participant AC as AuthController
    participant TF as TwoFactorLoginController
    participant DB as Database

    A->>F: email + password
    F->>LF: POST /api/auth/login (Authorization: Basic)
    alt email locked (5 wrong passwords)
        LF-->>F: 429 Too Many Requests (try again in 15 min)
    else
        LF->>S: continues
        S->>DB: find user by email + BCrypt check
        alt wrong password or blocked admin
            S-->>F: 401 (the failure is counted)
        else 2FA off
            S->>AC: authenticated user
            AC->>DB: save last_login_at
            AC-->>F: 200 { token: JWT }
        else 2FA on
            S->>AC: authenticated user
            AC-->>F: 202 { twoFactorRequired, pendingToken } (5 min)
            A->>F: 6-digit code
            F->>TF: POST /api/auth/login/2fa { pendingToken, code }
            TF->>DB: decrypt secret (AES) + check TOTP once (or a recovery code)
            TF-->>F: 200 { token: JWT }
        end
    end
    F->>F: keeps the JWT in the Pinia store
    Note over F,DB: Next requests: Authorization: Bearer JWT.<br/>A JWT issued before the last password change is rejected (PasswordChangedJwtValidator).
```

## 8. Password confirmation for sensitive actions

Every delete (brands, products, clients, contacts, rate questions and options), managing admins (invite, block, unblock, remove 2FA, delete), the mail settings, the email change and turning off 2FA are marked with `@RequiresConfirmation`. The server checks it, so calling the API directly without the confirmation also fails (BGSI-275).

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant F as Vue app (axios interceptor)
    participant CI as ConfirmationInterceptor
    participant AC as AccountController
    participant C as Controller (@RequiresConfirmation)

    A->>F: "Borrar" a brand
    F->>CI: DELETE /api/admin/brands/7
    CI-->>F: 403 { reason: CONFIRMATION_REQUIRED }
    F->>A: dialog "Confirma tu contraseña"
    A->>F: password (+ 2FA code if on)
    F->>AC: POST /api/admin/account/confirm
    AC-->>F: 200 { confirmToken } (valid 5 min, only for this admin)
    F->>CI: DELETE /api/admin/brands/7 (X-Confirm-Token)
    CI->>C: token valid
    C-->>F: 204 No Content
```
