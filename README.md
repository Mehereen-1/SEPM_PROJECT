# Binimoy (Bibliophile)

**A location-aware community platform for exchanging physical books with trusted workflows for request, delivery, and completion.**

## 1. Project Title & Tagline

### Project Title
Binimoy (Bibliophile)

### Tagline
A location-aware community platform for exchanging physical books with trusted workflows for request, delivery, and completion.

## 2. Executive Summary

Binimoy is a full-stack, role-aware book exchange platform that allows readers to list books, request exchanges, and complete the physical handoff through a delivery partner workflow. The system is built as a production-style Spring Boot application with a server-rendered frontend, role-based access control, delivery cost computation, and an event-driven notification subsystem.

This platform is designed for:
- Student and campus communities
- Local book-sharing networks
- Teams evaluating end-to-end software engineering execution (product, architecture, testing, and DevOps)

Why it matters:
- It solves trust and logistics gaps in informal book sharing
- It introduces operational accountability via status-driven exchange and delivery lifecycles
- It demonstrates production-oriented architecture choices: layered backend design, explicit domain modeling, modular notification design, and CI-backed quality gates

Key differentiator:
- The exchange lifecycle is tightly integrated with geolocation-based delivery orchestration and event-driven notifications, while keeping modules loosely coupled through Observer + Strategy patterns.

## 3. Product Overview

### Real-World Problem
Book sharing is often fragmented across informal channels. Typical pain points include:
- No standardized request/approval workflow
- Poor visibility into ownership, request status, and fulfillment progress
- No operational mechanism for physical handoff
- Weak moderation and governance controls

### Limitations of Common Alternatives
Most lightweight listing solutions stop at discovery. They rarely provide:
- Multi-role system behavior (reader, delivery partner, admin)
- Request-to-delivery lifecycle controls
- Route-aware delivery cost visibility
- Notification and moderation infrastructure

### How This System Solves It
Binimoy combines a catalog + offer marketplace with an exchange workflow and delivery execution model:
1. Users register as readers or delivery partners
2. Readers create offers from the shared book catalog
3. Exchange requests are sent and resolved by offer owners
4. Accepted exchanges generate delivery offers
5. Delivery partners execute a tracked two-pickup lifecycle and complete the final handoff
6. Notification events keep participants informed throughout

### High-Level Workflow
- Discovery: Browse catalog and active offers
- Negotiation: Request, accept, or reject exchange
- Fulfillment: Accept delivery, perform pickups, complete delivery
- Communication: Notification summary, unread tracking, and mark-read operations
- Governance: Admin controls for books, offers, users, and exchange approvals

## 4. Key Features

### Core Features
- Role-aware onboarding and authentication for `BOOK_FRIEND`, `DELIVERY_PARTNER`, and `ADMIN`
- Reader dashboard with profile metrics and quick-action workspace
- Catalog browsing from persisted `books.csv` bootstrap data
- Offer creation, listing, update, deletion, and image upload (`uploads/offers/{offerId}`)
- Exchange request lifecycle: create, accept, reject, and personal request inboxes

### Advanced Features
- Delivery lifecycle with explicit operational stages:
  - `AVAILABLE` -> `PENDING` (accepted by delivery partner)
  - `PICKUP_STARTED` (first pickup done)
  - `BOOK_PICKED` (second pickup done)
  - `COMPLETED` (final drop-off)
- Geospatial route support using user coordinates
- Backend-synchronized route metrics (`distanceKm`, `deliveryCost`, `costPerKm`)
- Interactive map flows with Leaflet + OSRM routing + Nominatim reverse geocoding
- Admin module for moderation and platform governance:
  - Manage books
  - Block users
  - Block/delete offers
  - Review and approve exchange requests

### Intelligent / AI Features
- No AI/ML module is implemented in this version.
- Pricing and workflow behavior are deterministic and rule-based.

### System / Engineering Features
- Layered backend architecture (Controller -> Service -> Repository -> Entity)
- Event-driven notification subsystem with:
  - Observer pattern (`NotificationSubject`, `NotificationObserver`)
  - Strategy pattern per event type (`Exchange`, `Delivery`, `System`)
- Duplicate prevention at domain and persistence layers:
  - Unique exchange pair constraint (`requester_offer_id`, `target_offer_id`)
  - Notification deduplication via `(event_key, recipient_user_id)`
- Security model:
  - Spring Security role-based authorization
  - Form login + JWT token support for API clients
  - BCrypt encoding with controlled backward compatibility handling
- CI pipeline on GitHub Actions (`.github/workflows/ci.yml`) for build + test gates

## 5. System Architecture

### Architectural Style
This project is implemented as a modular layered monolith:
- Presentation layer: Thymeleaf templates + vanilla JS
- API layer: Spring MVC REST controllers
- Business layer: domain services and module-specific orchestrators
- Data layer: Spring Data JPA repositories on PostgreSQL

### Major Components and Responsibilities
- `controller/`: user-facing and API endpoints for auth, offers, exchange, delivery, books
- `admin/`: admin controllers, DTOs, services, facade, strategy-based actions
- `notification/`: event models, strategies, observers, repository, service, web controllers
- `service/`: shared domain services such as `UserService`, `BookService`, `DeliveryPricingService`
- `security/`: authentication, JWT filter/utility, security helper methods
- `repository/`: query contracts with fetch-optimized read paths
- `loader/`: startup CSV ingestion for catalog bootstrap

### Data Flow
1. Client UI (Thymeleaf page + JS) issues request
2. Security filter chain enforces route-level and role-level access
3. Controller validates payload and identity context
4. Service executes domain rules and persistence orchestration
5. Repository handles query/write operations
6. For eventful actions, notification event is published and persisted via strategy resolution
7. Frontend polls notification summary endpoints and updates UI state

### Scalability-Relevant Design Choices
- Clear module boundaries reduce coupling inside the monolith
- Event strategy registry allows new notification types without controller rewrites
- Query methods use focused joins for reduced N+1 risk in high-read screens
- Route metrics are normalized and persisted for backend/frontend consistency

![System Architecture](./docs/architecture.png)

## 6. Technology Stack

### Frontend
- Thymeleaf templates
- Vanilla JavaScript (module-per-page pattern)
- Tailwind CSS (CDN usage in templates)
- Custom CSS (`admin-dashboard.css`, `notifications.css`, `theme.scss`)
- Leaflet + Leaflet Control Geocoder (map UI)

### Backend
- Java 17
- Spring Boot 4.0.3
- Spring MVC
- Spring Security
- Spring Validation
- Spring Data JPA

### Database
- PostgreSQL (primary runtime database)
- H2 (test profile)

### DevOps / Deployment
- Maven Wrapper (`mvnw`, `mvnw.cmd`)
- Docker multi-stage build (`Dockerfile`)
- Docker Compose orchestration (`compose.yaml`)
- GitHub Actions CI (`.github/workflows/ci.yml`)

### AI / ML
- Not applicable in current implementation

### Tools and Libraries
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- Apache Commons CSV
- Mockito, MockMvc, Spring Security Test, Data JPA Test

## 7. Project Structure (REAL)

```text
/root
├── .github/
│   └── workflows/
│       └── ci.yml
├── docs/
│   ├── project-prd.md
│   ├── NOTIFICATION_SYSTEM_DOCUMENTATION.md
│   ├── DELIVERY_TESTS_README.md
│   ├── BOOK_OFFER_TESTING_REPORT.md
│   ├── ADMIN_TESTING_README.md
│   └── auth-testing.md
├── src/
│   ├── main/
│   │   ├── java/com/example/project/
│   │   │   ├── admin/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── facade/
│   │   │   │   ├── service/
│   │   │   │   └── strategy/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── entity/
│   │   │   ├── loader/
│   │   │   ├── notification/
│   │   │   │   ├── event/
│   │   │   │   ├── model/
│   │   │   │   ├── observer/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── strategy/
│   │   │   │   └── web/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── ProjectApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── books.csv
│   │       ├── static/
│   │       │   ├── js/
│   │       │   └── styles/
│   │       └── templates/
│   │           ├── fragments/
│   │           └── error/
│   └── test/
│       ├── java/com/example/project/
│       │   ├── admin/
│       │   ├── controller/
│       │   ├── notification/
│       │   ├── repository/
│       │   └── service/
│       └── resources/
│           └── application-test.yaml
├── uploads/
│   └── offers/
├── compose.yaml
├── Dockerfile
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .env
└── .env.local
```

### Directory Responsibilities
- `src/main/java/com/example/project/controller`: core user and domain endpoint orchestration
- `src/main/java/com/example/project/admin`: moderation/governance module with facade and strategy abstractions
- `src/main/java/com/example/project/notification`: decoupled notification event-processing engine
- `src/main/resources/templates`: server-rendered views for reader, delivery, admin, auth, and notifications
- `src/main/resources/static/js`: per-page client interaction logic and route map handling
- `src/test/java`: unit, repository, and integration tests by domain area
- `docs`: PRD and focused test/module documentation

### Key Files
- `pom.xml`: dependency graph, Java/Maven build definition
- `compose.yaml`: PostgreSQL + app service composition variables
- `Dockerfile`: multi-stage JAR build and runtime image
- `src/main/resources/application.yaml`: runtime datasource/JPA/server/admin config
- `.github/workflows/ci.yml`: CI build and test automation

## 8. Core Workflows

### User Onboarding Flow
1. User opens `/auth` and selects role context
2. Registration form captures identity + profile + coordinates/address
3. `UserService` resolves/creates role and stores BCrypt-encoded credentials
4. Login redirects users by role:
   - Admin -> `/admin/dashboard`
   - Delivery partner -> `/delivery/dashboard`
   - Reader -> `/reader/dashboard`

### Book Exchange Lifecycle
1. Reader creates an offer from catalog (`POST /offers`)
2. Offer owner and condition/note become visible in browse pages
3. Another reader sends request (`POST /exchange-requests`)
4. Target owner accepts/rejects (`PUT /exchange-requests/{id}/accept|reject`)
5. On acceptance:
   - Exchange request becomes `ACCEPTED`
   - Both offers are marked `RESERVED`
   - Delivery offer is created if absent
   - Notifications are published

### Delivery Lifecycle
1. Delivery partner accepts task (`POST /delivery/accept/{id}`)
2. First pickup starts (`POST /delivery/pickup-start/{id}`)
3. Second pickup and handoff progress (`POST /delivery/book-picked/{id}`)
4. Final drop-off completes task (`POST /delivery/complete/{id}`)
5. Route metrics (`distanceKm`, `deliveryCost`) are resolved server-side and synced to map/dashboard views

### Notifications and Events
1. Domain action occurs (exchange/delivery/system)
2. `NotificationService` publishes `NotificationEvent`
3. `DefaultNotificationSubject` dispatches event to observers
4. `PersistingNotificationObserver` resolves matching strategy
5. Strategy generates recipient-specific drafts with event keys
6. Repository deduplicates and persists notifications
7. UI consumes:
   - `GET /notifications`
   - `GET /notifications/summary`
   - `PATCH /notifications/{id}`
   - `PATCH /notifications`

## 9. Diagrams Section

![Class Diagram](./docs/class-diagram.png)

![DFD](./docs/dfd.png)

![Use Case Diagram](./docs/use-case-diagram.png)

![Activity Diagram](./docs/activity-diagram.png)

![Architecture Diagram](./docs/architecture.png)

## 10. Screenshots

### Landing Page
![Screenshot](./docs/screens/screen1.png)

### Dashboard
![Screenshot](./docs/screens/screen2.png)

### Key Features UI
![Screenshot](./docs/screens/screen3.png)

## 11. Demo / Walkthrough

- Demo video: `Add demo link here`

### Suggested Demo Highlights
- Reader onboarding and profile location setup
- Offer creation with image upload
- Exchange request send/accept flow
- Delivery partner lifecycle (pickup -> completion)
- Notification center updates and mark-read actions
- Admin moderation actions

## 12. Setup & Installation

### Prerequisites
- Java 17
- Docker + Docker Compose (recommended for PostgreSQL)
- Git
- Internet access for map tile/geocoding services used in UI

### Installation Steps

#### 1. Clone the Repository
```bash
git clone <your-repository-url>
cd SEPM_PROJECT
```

#### 2. Configure Environment Variables
Use `.env` (or adapt values from `.env.local`) in project root.

Minimum required for local development:
```env
DB_NAME=se_project_db
DB_USER=se_project_user
DB_PASSWORD=securepass
DB_PORT=5432
APP_PORT=8080
SPRING_PROFILES_ACTIVE=dev
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
```

#### 3. Start Database
Option A: Start PostgreSQL via Docker Compose
```bash
docker compose up -d db
```

Option B: Use a local PostgreSQL instance and set `DB_URL`, `DB_USER`, `DB_PASSWORD` accordingly.

#### 4. Run Backend (Serves Frontend Too)
Windows:
```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:
```bash
./mvnw spring-boot:run
```

#### 5. Open Application
- Main app: `http://localhost:8080`
- Reader entry: `/reader/dashboard`
- Delivery entry: `/delivery/dashboard`
- Admin entry: `/admin/dashboard`

### Build and Test Commands
Windows:
```powershell
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd test
```

Linux/macOS:
```bash
./mvnw clean package -DskipTests
./mvnw test
```

### Running Frontend
There is no separate frontend build/runtime process. Thymeleaf templates and static JS/CSS are served by the Spring Boot application.

## 13. Environment Variables

| Variable | Required | Purpose |
|---|---|---|
| `DB_URL` | No (default provided) | Full JDBC URL override (`jdbc:postgresql://...`) |
| `DB_USER` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `DB_NAME` | Yes (compose) | PostgreSQL database name in Compose setup |
| `DB_PORT` | Yes (compose) | Host port mapping for PostgreSQL |
| `DB_CONTAINER_NAME` | No | Docker container naming for DB |
| `APP_PORT` | No | Spring Boot server port (default `8080`) |
| `APP_CONTAINER_NAME` | No | Docker container naming for app |
| `SPRING_PROFILES_ACTIVE` | No | Active Spring profile (for example `dev`) |
| `ADMIN_EMAIL` | Recommended | Bootstrap admin user email at startup |
| `ADMIN_PASSWORD` | Recommended | Bootstrap admin user password at startup |
| `APP_JWT_SECRET` | Optional but recommended for production | JWT signing secret (maps to `app.jwt.secret`) |
| `APP_JWT_EXPIRATION_MS` | Optional | JWT token expiration in milliseconds |

## 14. API Overview

### Authentication
- `POST /api/auth/register` - API registration
- `POST /api/auth/login` - JWT issuance
- `POST /api/auth/logout` - context logout

### Books
- `GET /books/browse` - List catalog books
- `GET /books/{id}` - Fetch a single book
- `POST /books` - Admin creates catalog book

### Offers
- `GET /offers` - Browse active offers
- `GET /offers/my-active` - Current user active offers
- `GET /offers/my` - Current user offer details
- `POST /offers` - Create offer
- `POST /offers/{offerId}/images` - Upload offer images
- `PUT /offers/{offerId}` - Update own offer
- `DELETE /offers/{offerId}` - Delete own offer

### Exchange Requests
- `POST /exchange-requests` - Create request
- `PUT /exchange-requests/{id}/accept` - Accept request
- `PUT /exchange-requests/{id}/reject` - Reject request
- `GET /exchange-requests/my-requests` - Sent and received requests

### Delivery
- `POST /delivery/accept/{id}` - Accept delivery task
- `POST /delivery/pickup-start/{id}` - Complete first pickup
- `POST /delivery/book-picked/{id}` - Complete second pickup
- `POST /delivery/complete/{id}` - Complete final delivery
- `GET /delivery/location-data/{id}` - Delivery route metrics payload

### Notifications
- `GET /notifications` - Notification list (`unread`, `limit`)
- `GET /notifications/summary` - Unread count + recent items
- `PATCH /notifications/{id}` - Mark one as read
- `PATCH /notifications` - Mark all as read

### Admin
- `GET /admin/books`, `POST /admin/books`, `PUT /admin/books/{id}`, `DELETE /admin/books/{id}`
- `GET /admin/offers`, `PUT /admin/offers/{id}/block`, `DELETE /admin/offers/{id}`
- `GET /admin/users`, `PUT /admin/users/{id}/block`
- `GET /admin/exchange-requests`, `PUT /admin/exchange-requests/{id}/approve`

### Example Requests

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "reader@example.com",
  "password": "secret123"
}
```

#### Create Offer
```http
POST /offers
Content-Type: application/json

{
  "bookId": "book-uuid-or-id",
  "condition": "Good",
  "note": "Minimal highlighting"
}
```

#### Create Exchange Request
```http
POST /exchange-requests
Content-Type: application/json

{
  "requesterOfferId": 10,
  "targetOfferId": 24
}
```

#### Mark All Notifications Read
```http
PATCH /notifications
```

## 15. Scalability & Design Decisions

### Why This Architecture
- A layered monolith was chosen to keep domain consistency high while implementing multiple complex business modules (offers, exchange, delivery, admin, notifications).
- Separation into dedicated packages and modules (`admin`, `notification`, `security`) keeps growth manageable without early distributed-system complexity.

### Key Trade-offs
- Pros:
  - Faster iteration and easier debugging in a single deployable unit
  - Strong transactional consistency around exchange and delivery state changes
  - Lower infrastructure overhead for academic/early-stage product contexts
- Cons:
  - Independent scaling of subsystems is limited
  - Notification handling is synchronous in-process today
  - File uploads are local filesystem-based, not cloud object storage

### Future Scalability Path
- Extract notification publishing to asynchronous broker-backed processing
- Move media storage to object storage (S3-compatible)
- Introduce caching for heavy browse/query endpoints
- Split admin/reporting concerns as standalone services if traffic or governance load increases

## 16. Future Improvements / Roadmap

- Add explicit API versioning and OpenAPI documentation
- Introduce audit logs for all moderation and state transition actions
- Add rate limiting and abuse controls on critical endpoints
- Add dedicated delivery partner assignment heuristics (proximity, workload)
- Add optional realtime updates (SSE/WebSocket) for notifications and delivery status
- Provide observability stack (metrics, tracing, alerting)
- Add license file and contributor covenant artifacts for open-source readiness

## 17. Contribution Guidelines

### How to Contribute
1. Fork the repository
2. Create a feature branch from `dev`
3. Implement changes with tests
4. Run local checks (`mvnw test`)
5. Open a pull request to `dev`

### Suggested Branching Strategy
- `dev`: integration branch (CI currently targets this branch)
- `feature/<short-topic>`: new features
- `fix/<short-topic>`: bug fixes
- `docs/<short-topic>`: documentation-only changes

### Contribution Standards
- Keep controller logic thin; place business rules in services
- Add/extend tests for every behavior change
- Preserve API response contracts used by existing frontend scripts
- Keep README and `docs/` aligned with implementation changes

## 18. License

No explicit license file is currently present in this repository.

Until a license is added, treat the codebase as proprietary/all-rights-reserved by default.
For open-source distribution, add a `LICENSE` file (for example MIT, Apache-2.0, or GPL) and update this section accordingly.
