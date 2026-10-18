# Technical diagrams

Diagrams of the BG Sistemas Informáticos backend, written in [Mermaid](https://mermaid.js.org/) so GitHub draws them from the text (BGSI-168).
PNG versions for slides are in [`docs/diagrams/`](diagrams/).

1. [Architecture](#1-architecture)
2. [Entity-relationship diagram](#2-entity-relationship-diagram) (BGSI-6)
3. [Domain class diagram](#3-domain-class-diagram) (BGSI-7)
4. [Layers of one feature: Contact](#4-layers-of-one-feature-contact) (BGSI-7)
5. [Strategy pattern: export to Excel / PDF](#5-strategy-pattern-export-to-excel--pdf) (BGSI-7)

## 1. Architecture

The Vue app calls the REST API with a JWT. Spring Security protects `/api/admin/**`, and every feature follows the same layers down to MySQL (Docker).

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
        sec["Spring Security<br/>JWT filter · /api/admin/** = ADMIN"] --> ctrl["Controllers + DTOs"]
        ctrl --> service["Services (interfaces + Impl)"]
        service --> jpa["Spring Data JPA repositories"]
        service --> mail["EmailNotificationService @Async"]
        service --> files["Exporters (Excel / PDF)"]
    end
    jpa --> db[("MySQL 8 · Docker")]
    mail --> smtp["Gmail SMTP"]
```

## 2. Entity-relationship diagram

Eight MySQL tables. Only two relations use a foreign key:

- `brand` 1 — 0..N `service` (`service.brand_id`). A brand with products cannot be deleted (409).
- `rate_question` 1 — 1..N `rate_option` (`rate_option.question_id`). Deleting a question deletes its options.

`contact.interested_service` stores the product **name** as text on purpose: the lead keeps its history even if the product is deleted.

```mermaid
erDiagram
    BRAND ||--o{ SERVICE : "offers"
    RATE_QUESTION ||--|{ RATE_OPTION : "has"

    BRAND {
        BIGINT id PK
        VARCHAR name
        VARCHAR description
        VARCHAR category
        VARCHAR logo_url
        BIT active
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
        BIT featured
        BIGINT brand_id FK
    }
    CLIENT {
        BIGINT id PK
        VARCHAR name
        VARCHAR business_type
        VARCHAR testimonial "max 1000"
        VARCHAR logo_url
        INT rating "1 to 5"
        BIT published
    }
    CONTACT {
        BIGINT id PK
        VARCHAR name
        VARCHAR email
        VARCHAR phone
        VARCHAR interested_service "text, not FK"
        VARCHAR message "max 2000"
        BIT gdpr_consent
        ENUM status "NEW CONTACTED ARCHIVED"
        DATETIME created_at
        VARCHAR note "max 1000"
    }
    RATE_QUESTION {
        BIGINT id PK
        VARCHAR title "max 40"
        VARCHAR question "max 150"
        VARCHAR hint
        INT position
        BIT active
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
    }
    SITE_SETTING {
        VARCHAR setting_key PK "max 100"
        VARCHAR setting_value
    }
```

## 3. Domain class diagram

JPA entities (`@Entity`) and their enums.

- `Service → Brand`: unidirectional many-to-one association (`@ManyToOne`).
- `RateQuestion ◆— RateOption`: bidirectional composition (`@OneToMany(cascade = ALL, orphanRemoval = true)`).

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
        -Brand brand
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
    Brand "1" <-- "0..*" Service : brand
    RateQuestion "1" *-- "1..*" RateOption : options
    Contact --> ContactStatus
    RateOption --> AdjustmentType
    User --> Role
```

## 4. Layers of one feature: Contact

Every feature (`brand`, `service`, `client`, `contact`, `rate`, `setting`...) uses the same layers:
**Controller → Mapper / DTO → Service (interface) → ServiceImpl → Repository → Entity**.
Controllers depend on the interface, and Spring injects the implementation through the constructor.

```mermaid
classDiagram
    direction TB
    class ContactController {
        <<RestController /api/contacts>>
        +createContact(ContactRequestDTO) ResponseEntity
    }
    class AdminContactController {
        <<RestController /api/admin/contacts>>
        +getContacts(status, page, size) PageResponseDTO
        +updateContact(id, ContactUpdateRequestDTO) ContactResponseDTO
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
        +getContacts(ContactStatus, Pageable) Page
        +updateContact(Long, ContactStatus, String) Contact
    }
    class ContactServiceImpl {
        <<Service>>
        -ContactRepository contactRepository
        -NotificationService notificationService
    }
    class ContactRepository {
        <<interface>>
        +findByStatus(ContactStatus, Pageable) Page
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
