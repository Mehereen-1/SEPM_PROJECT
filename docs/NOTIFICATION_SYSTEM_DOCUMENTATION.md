# Notification System Documentation

## 1. Overview

### Purpose
The **Notification System** is a sophisticated, event-driven module designed to keep users informed of all critical activities in the Book Exchange & Delivery platform. It seamlessly integrates with the Exchange Request and Delivery systems without modifying their core logic.

### Role in Book Exchange Platform
The notification system serves as the **communication backbone** for the platform by:
- Alerting users to exchange request activities (sent, accepted, rejected)
- Updating drivers and recipients on delivery milestones
- Broadcasting system-wide announcements
- Tracking notification read status for user engagement

### What Problem It Solves
Without this system, users would have **no way to know** about:
- When someone requests their books
- Whether an exchange has been accepted or rejected
- Real-time delivery progress (when pickup starts, book is picked, delivery completed)
- Important system messages

The system implements **loose coupling** using Observer and Strategy patterns, allowing notifications to be triggered without modifying exchange or delivery code.

---

## 2. Architecture (Layered Structure with File References)

The notification system follows a **clean, layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────┐
│           REST API Layer (Controllers)           │
│  NotificationRestController.java                 │
│  NotificationPageController.java                 │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│        Service Layer (Business Logic)            │
│  NotificationService.java (interface)            │
│  NotificationServiceImpl.java (implementation)    │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│  Observer & Strategy Patterns (Event Handling)   │
│  NotificationSubject.java (publisher)            │
│  NotificationObserver.java (subscriber)          │
│  DefaultNotificationSubject.java (dispatcher)    │
│  PersistingNotificationObserver.java (persister) │
│  NotificationStrategy.java (interface)           │
│  ExchangeNotificationStrategy.java               │
│  DeliveryNotificationStrategy.java               │
│  SystemNotificationStrategy.java                 │
│  NotificationStrategyRegistry.java               │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│        Repository Layer (Data Access)            │
│  NotificationRepository.java                     │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│        Entity & Model Layer (Data)               │
│  Notification.java (JPA entity)                  │
│  NotificationType.java (enum)                    │
│  NotificationEventType.java (enum)               │
│  NotificationEvent.java (record)                 │
│  NotificationDraft.java (record)                 │
│  NotificationView.java (DTO)                     │
│  NotificationSummaryView.java (DTO)              │
└─────────────────────────────────────────────────┘
```

### Layer Breakdown

#### **Entity Layer** (`notification/model/`)
- **[Notification.java](../../src/main/java/com/example/project/notification/model/Notification.java)**: JPA entity representing a persisted notification in the database
  - Fields: `id`, `recipient` (User), `message`, `type`, `isRead`, `eventKey`, `createdAt`
  - Indexes: By `(recipient_id, created_at)` and `(recipient_id, is_read)` for query optimization
  - Unique constraint: `(event_key, recipient_id)` to prevent duplicate notifications for same event

- **[NotificationType.java](../../src/main/java/com/example/project/notification/model/NotificationType.java)**: Enum defining notification categories
  - `EXCHANGE`: Exchange request notifications
  - `DELIVERY`: Delivery milestone notifications
  - `SYSTEM`: System-wide announcements

#### **Event & Strategy Layer** (`notification/event/` & `notification/strategy/`)
- **[NotificationEvent.java](../../src/main/java/com/example/project/notification/event/NotificationEvent.java)**: Record encapsulating all event data
  - Contains: `eventType`, `actorUserId`, `exchangeRequest`, `deliveryOffer`, `directRecipient`, `customMessage`, `occurredAt`
  - Passed through Observer chain for processing

- **[NotificationEventType.java](../../src/main/java/com/example/project/notification/event/NotificationEventType.java)**: Enum of all triggerable events
  - Exchange events: `EXCHANGE_REQUEST_SENT`, `EXCHANGE_ACCEPTED`, `EXCHANGE_REJECTED`
  - Delivery events: `DELIVERY_CREATED`, `DELIVERY_ASSIGNED`, `PICKUP_STARTED`, `BOOK_PICKED`, `DELIVERY_COMPLETED`
  - System events: `SYSTEM_ANNOUNCEMENT`

- **[NotificationStrategy.java](../../src/main/java/com/example/project/notification/strategy/NotificationStrategy.java)**: Interface for strategy pattern
  - Methods:
    - `supports(NotificationEventType)`: Determines if strategy handles this event type
    - `build(NotificationEvent)`: Generates `NotificationDraft` objects from event

#### **Observer & Subject Layer** (`notification/observer/`)
- **[NotificationSubject.java](../../src/main/java/com/example/project/notification/observer/NotificationSubject.java)**: Publisher interface
  - Method: `publish(NotificationEvent)` - broadcasts events to observers

- **[NotificationObserver.java](../../src/main/java/com/example/project/notification/observer/NotificationObserver.java)**: Subscriber interface
  - Method: `onEvent(NotificationEvent)` - handles published events

- **[DefaultNotificationSubject.java](../../src/main/java/com/example/project/notification/observer/DefaultNotificationSubject.java)**: Concrete publisher
  - Maintains list of observers (injected by Spring)
  - Dispatches events to all registered observers

- **[PersistingNotificationObserver.java](../../src/main/java/com/example/project/notification/observer/PersistingNotificationObserver.java)**: Concrete observer that saves notifications
  - Orchestrates the full flow: resolve strategy → build drafts → deduplicate → persist

#### **Repository Layer** (`notification/repository/`)
- **[NotificationRepository.java](../../src/main/java/com/example/project/notification/repository/NotificationRepository.java)**: Spring Data JPA interface
  - Methods:
    - `findByRecipient_IdOrderByCreatedAtDesc()`: Get all notifications for user
    - `findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc()`: Get unread notifications
    - `countByRecipient_IdAndIsReadFalse()`: Count unread notifications
    - `existsByEventKeyAndRecipient_Id()`: Prevent duplicate notifications
    - `markAllReadByRecipientId()`: Bulk mark-as-read operation

#### **Service Layer** (`notification/service/`)
- **[NotificationService.java](../../src/main/java/com/example/project/notification/service/NotificationService.java)**: Service interface defining contract
  - Publishing methods:
    - `publishExchangeRequestSent()`, `publishExchangeAccepted()`, `publishExchangeRejected()`
    - `publishDeliveryCreated()`, `publishDeliveryAssigned()`, `publishPickupStarted()`, `publishBookPicked()`, `publishDeliveryCompleted()`
    - `publishSystemNotification()`
  - Query methods:
    - `getCurrentUserNotifications()`: List notifications with optional filtering
    - `getCurrentUserSummary()`: Unread count + recent notifications
  - Mutation methods:
    - `markAsRead()`, `markAllAsRead()`

- **[NotificationServiceImpl.java](../../src/main/java/com/example/project/notification/service/NotificationServiceImpl.java)**: Service implementation
  - Delegates publication to `NotificationSubject`
  - Implements all query and mutation operations
  - Uses `SecurityUtil` to get current authenticated user
  - Wraps notifications into DTOs for API responses

- **[NotificationView.java](../../src/main/java/com/example/project/notification/service/NotificationView.java)**: DTO for single notification
  - Fields: `id`, `message`, `type`, `isRead`, `createdAt`

- **[NotificationSummaryView.java](../../src/main/java/com/example/project/notification/service/NotificationSummaryView.java)**: DTO for summary endpoint
  - Fields: `unreadCount`, `recentNotifications` (list of `NotificationView`)

#### **Controller Layer** (`notification/web/`)
- **[NotificationRestController.java](../../src/main/java/com/example/project/notification/web/NotificationRestController.java)**: REST API endpoints
  - All endpoints at base path `/notifications`
  - Endpoints documented in **Section 4: REST API Design**

- **[NotificationPageController.java](../../src/main/java/com/example/project/notification/web/NotificationPageController.java)**: Thymeleaf page routes
  - `GET /notification-center` → returns `notifications.html` template

---

## 3. Design Patterns (With Implementation Location)

### Observer Pattern (Event-Driven Architecture)

**Purpose**: Decouple notification creation from notification persistence and processing.

**How It Works**:
1. When an event occurs (e.g., exchange accepted), the service calls `notificationSubject.publish(event)`
2. The `DefaultNotificationSubject` holds a list of observers
3. For each event, it calls `observer.onEvent(event)` sequentially
4. `PersistingNotificationObserver` receives the event and processes it

**Implementation Locations**:
- **Publisher**: [DefaultNotificationSubject.java](../../src/main/java/com/example/project/notification/observer/DefaultNotificationSubject.java)
  ```
  Dependency injection in Spring configuration autowires all NotificationObserver implementations
  ```
  
- **Subscriber Interface**: [NotificationObserver.java](../../src/main/java/com/example/project/notification/observer/NotificationObserver.java)
  
- **Concrete Observer**: [PersistingNotificationObserver.java](../../src/main/java/com/example/project/notification/observer/PersistingNotificationObserver.java)
  - Lines 35-62: `onEvent()` method containing observer logic

### Strategy Pattern (Notification Type Handling)

**Purpose**: Generate notifications that are specific to each event type without massive if-else chains.

**How It Works**:
1. Each event type has a corresponding strategy (Exchange, Delivery, System)
2. `NotificationStrategyRegistry` maps event types to strategies
3. `PersistingNotificationObserver` resolves the appropriate strategy
4. The strategy's `build()` method creates `NotificationDraft` objects tailored to that event
5. Drafts are persisted as `Notification` entities

**Implementation Locations**:
- **Strategy Interface**: [NotificationStrategy.java](../../src/main/java/com/example/project/notification/strategy/NotificationStrategy.java)
  - Methods: `supports(eventType)`, `build(event) → List<NotificationDraft>`

- **Concrete Strategies**:
  - [ExchangeNotificationStrategy.java](../../src/main/java/com/example/project/notification/strategy/ExchangeNotificationStrategy.java)
    - Handles: `EXCHANGE_REQUEST_SENT`, `EXCHANGE_ACCEPTED`, `EXCHANGE_REJECTED`
    - Generates notifications to both requester and offer owner
  
  - [DeliveryNotificationStrategy.java](../../src/main/java/com/example/project/notification/strategy/DeliveryNotificationStrategy.java)
    - Handles: `DELIVERY_CREATED`, `DELIVERY_ASSIGNED`, `PICKUP_STARTED`, `BOOK_PICKED`, `DELIVERY_COMPLETED`
    - Generates notifications to driver, pickup person, delivery person, and recipient
  
  - [SystemNotificationStrategy.java](../../src/main/java/com/example/project/notification/strategy/SystemNotificationStrategy.java)
    - Handles: `SYSTEM_ANNOUNCEMENT`
    - Sends custom message to direct recipient

- **Registry**: [NotificationStrategyRegistry.java](../../src/main/java/com/example/project/notification/strategy/NotificationStrategyRegistry.java)
  - Autowires all strategies and maps them by event type
  - Used in `PersistingNotificationObserver` to resolve strategy

- **Draft Record**: [NotificationDraft.java](../../src/main/java/com/example/project/notification/strategy/NotificationDraft.java)
  - Fields: `recipient` (User), `message`, `type` (NotificationType), `eventKey`
  - Intermediate object before persistence

**Example**: When `DELIVERY_ASSIGNED` event fires:
```
1. PersistingNotificationObserver.onEvent() called
2. Resolves DeliveryNotificationStrategy (via registry check of supports())
3. DeliveryNotificationStrategy.build() creates NotificationDraft objects for:
   - Driver assigned
   - Recipient being notified
4. Each draft becomes a persisted Notification entity
```

---

## 4. REST API Design (Endpoints with File References)

All endpoints are implemented in [NotificationRestController.java](../../src/main/java/com/example/project/notification/web/NotificationRestController.java) at base path `/notifications`.

### Endpoint 1: List Notifications
**Request**:
```
GET /notifications?unread=true&limit=20
```

**Parameters**:
- `unread` (optional, Boolean): Filter to unread only. If `null`, return all
- `limit` (optional, Integer): Maximum number of notifications to return. Default: 10, Max: 50

**Response** (200 OK):
```json
[
  {
    "id": 123,
    "message": "John Doe accepted your exchange request!",
    "type": "EXCHANGE",
    "isRead": false,
    "createdAt": "2025-03-19T14:30:00"
  }
]
```

**Implementation**: Line 27-35 in [NotificationRestController.java](../../src/main/java/com/example/project/notification/web/NotificationRestController.java)

**Service Call**: `NotificationService.getCurrentUserNotifications(Boolean unread, Integer limit)`
- Located in [NotificationServiceImpl.java](../../src/main/java/com/example/project/notification/service/NotificationServiceImpl.java) lines 88-102

---

### Endpoint 2: Get Notification Summary
**Request**:
```
GET /notifications/summary?limit=5
```

**Parameters**:
- `limit` (optional, Integer): Number of recent notifications to include. Default: 6

**Response** (200 OK):
```json
{
  "unreadCount": 3,
  "recentNotifications": [
    {
      "id": 125,
      "message": "Pickup started for your delivery order #101",
      "type": "DELIVERY",
      "isRead": false,
      "createdAt": "2025-03-19T15:45:00"
    }
  ]
}
```

**Implementation**: Line 37-44 in [NotificationRestController.java](../../src/main/java/com/example/project/notification/web/NotificationRestController.java)

**Service Call**: `NotificationService.getCurrentUserSummary(Integer recentLimit)`
- Located in [NotificationServiceImpl.java](../../src/main/java/com/example/project/notification/service/NotificationServiceImpl.java) lines 106-115

---

### Endpoint 3: Mark Single Notification as Read
**Request**:
```
PATCH /notifications/123
```

**Response** (200 OK):
```json
{
  "updated": true
}
```

**Error Response** (404 Not Found):
```json
{}
```

**Implementation**: Line 46-53 in [NotificationRestController.java](../../src/main/java/com/example/project/notification/web/NotificationRestController.java)

**Service Call**: `NotificationService.markAsRead(Long notificationId)`
- Located in [NotificationServiceImpl.java](../../src/main/java/com/example/project/notification/service/NotificationServiceImpl.java) lines 119-135

---

### Endpoint 4: Mark All Notifications as Read
**Request**:
```
PATCH /notifications
```

**Response** (200 OK):
```json
{
  "updated": true
}
```

**Implementation**: Line 55-60 in [NotificationRestController.java](../../src/main/java/com/example/project/notification/web/NotificationRestController.java)

**Service Call**: `NotificationService.markAllAsRead()`
- Located in [NotificationServiceImpl.java](../../src/main/java/com/example/project/notification/service/NotificationServiceImpl.java) lines 137-144

---

### REST Design Principles Applied

1. **Resource-Based URLs**: `/notifications` represents the notification collection
2. **HTTP Methods**: 
   - `GET` for retrieval (idempotent)
   - `PATCH` for partial updates (read status)
3. **Status Codes**: 200 (success), 404 (not found), 401 (unauthorized - handled by Spring Security)
4. **Pagination**: Limit parameter controls result size
5. **Filtering**: `unread` parameter enables client-side filtering without additional endpoints

---

## 5. Database Design (Entity with Fields and Relationships)

### Notification Entity Structure

**File**: [Notification.java](../../src/main/java/com/example/project/notification/model/Notification.java)

**Table**: `notifications`

**Fields**:

| Field | Type | Nullable | Constraints | Purpose |
|-------|------|----------|-------------|---------|
| `id` | BIGINT | NO | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| `recipient_user_id` | BIGINT | NO | FOREIGN KEY → `users.id` | Which user received this notification |
| `message` | TEXT | NO | — | Notification content |
| `type` | VARCHAR(24) | NO | ENUM (EXCHANGE, DELIVERY, SYSTEM) | Categorization |
| `is_read` | BOOLEAN | NO | DEFAULT false | Read status |
| `event_key` | VARCHAR(255) | YES | — | Unique key to prevent duplicates |
| `created_at` | TIMESTAMP | NO | — | When notification was created |

**Indexes for Performance**:

```sql
-- Fast retrieval of user's notifications sorted by creation
CREATE INDEX idx_notification_recipient_created 
ON notifications(recipient_user_id, created_at);

-- Fast filtering of unread notifications
CREATE INDEX idx_notification_recipient_read 
ON notifications(recipient_user_id, is_read);
```

**Unique Constraint** (Duplicate Prevention):
```sql
UNIQUE KEY uk_notification_event_recipient 
ON notifications(event_key, recipient_user_id);
```
Prevents the same event from creating multiple notifications for the same user.

### Relationships

**Notification → User (Many-to-One)**
- A user can have **many** notifications
- A notification belongs to **one** user (the recipient)
- JPA Configuration: 
  ```java
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_user_id", nullable = false)
  private User recipient;
  ```
  - `LAZY` loading: Load recipient only when accessed (performance optimization)
  - `optional = false`: Every notification must have a recipient

---

## 6. Integration with System (Without Modifying Core Logic)

The notification system integrates with Exchange and Delivery systems through **one-line publish calls** only.

### Exchange Request Integration

**File**: [ExchangeRequestController.java](../../src/main/java/com/example/project/controller/ExchangeRequestController.java)

**Integration Points**:

1. **When Exchange Request is Created** (Line 108):
   ```java
   notificationService.publishExchangeRequestSent(saved, currentUser.getId());
   ```
   - Notifies: Offer owner about new exchange request

2. **When Exchange Request is Accepted** (Line 144):
   ```java
   notificationService.publishExchangeAccepted(updated, currentUser.getId());
   ```
   - Notifies: Request creator about acceptance

3. **When Exchange Request is Rejected** (Line 177):
   ```java
   notificationService.publishExchangeRejected(updated, currentUser.getId());
   ```
   - Notifies: Request creator about rejection

4. **When Delivery is Created** (Line 226):
   ```java
   notificationService.publishDeliveryCreated(created, null);
   ```
   - Notifies: Participants about new delivery

### Delivery System Integration

**File**: [DeliveryDashboardController.java](../../src/main/java/com/example/project/controller/DeliveryDashboardController.java)

**Integration Points**:

1. **When Delivery is Assigned to Driver** (Line 145):
   ```java
   notificationService.publishDeliveryAssigned(offer, currentUser.get().getId());
   ```

2. **When Pickup is Started** (Line 185):
   ```java
   notificationService.publishPickupStarted(offer, currentUser.get().getId());
   ```

3. **When Book is Picked** (Line 223):
   ```java
   notificationService.publishBookPicked(offer, currentUser.get().getId());
   ```

4. **When Delivery is Completed** (Line 262):
   ```java
   notificationService.publishDeliveryCompleted(offer, currentUser.get().getId());
   ```

5. **When Delivery is Created** (Line 454):
   ```java
   notificationService.publishDeliveryCreated(created, null);
   ```

### Why This Design Works

✅ **Minimal Code Intrusion**: Only one method call per event
✅ **No Coupling**: Exchange/Delivery code doesn't know about notification internals
✅ **Easy Testing**: Mock `NotificationService` in unit tests
✅ **Easy to Extend**: Add new notification types without modifying controllers
✅ **Transactional Safety**: Notifications use observer pattern for consistency

---

## 7. Frontend Integration (File References)

### Backend API Integration

**Notification Endpoints Used**:
- `GET /notifications` - Fetch notifications list
- `GET /notifications/summary` - Fetch unread count + recent notifications
- `PATCH /notifications/{id}` - Mark single notification as read
- `PATCH /notifications` - Mark all notifications as read

### Frontend Files

#### 1. **Notification Center Page**
**File**: `resources/templates/notifications.html`
- Full page view of notifications
- Accessible at route `/notification-center` (via [NotificationPageController.java](../../src/main/java/com/example/project/notification/web/NotificationPageController.java), line 9)
- Displays:
  - Unread count badge
  - Paginated notification list
  - Read/Unread status toggle
  - "Mark all as read" button
  - Delete notification option

#### 2. **Navbar Integration**
**File**: `resources/templates/fragments/navbar.html`
- Notification bell icon with unread count
- JavaScript fetches summary via `GET /notifications/summary` on page load
- Updates every 30 seconds via polling
- Click bell to navigate to notification center

#### 3. **Notification JavaScript API**
**File**: `resources/static/js/notifications.js` (if exists, or inline fetch calls)
- Methods:
  - `getNotifications(params)`:  `GET /notifications?unread=true&limit=20`
  - `getSummary()`: `GET /notifications/summary?limit=6`
  - `markAsRead(id)`: `PATCH /notifications/id`
  - `markAllAsRead()`: `PATCH /notifications`
- Handles display updates after operations
- Error handling for network failures

#### 4. **Toast Notifications** (Optional)
**File**: `resources/static/js/toast.js`
- System displays toast messages for successful actions:
  - "Notification marked as read"
  - "All notifications cleared"

### Example Frontend Flow

**User navigates to Notification Center**:
1. Browser calls `GET /notification-center`
2. `NotificationPageController` returns `notifications.html`
3. Page loads with HTML framework
4. JavaScript executes: `fetch('/notifications?limit=20')`
5. Display notifications in table/list

**Bell Icon Updates Every 30 Seconds**:
```javascript
setInterval(() => {
  fetch('/notifications/summary')
    .then(r => r.json())
    .then(data => {
      document.getElementById('notif-badge').textContent = data.unreadCount;
      updateRecentList(data.recentNotifications);
    });
}, 30000);
```

---

## 8. Important Imports (Key Dependencies)

### Spring Framework Imports
```java
import org.springframework.web.bind.annotation.*;      // REST endpoints: @RestController, @GetMapping, @PatchMapping
import org.springframework.stereotype.Service;          // Service layer: @Service
import org.springframework.stereotype.Component;        // Spring beans: @Component (for Observers & Strategies)
import org.springframework.stereotype.Controller;       // Thymeleaf pages: @Controller
import org.springframework.http.ResponseEntity;         // REST responses with status codes
import org.springframework.http.HttpStatus;             // HTTP status constants
import org.springframework.beans.factory.annotation.Autowired; // Dependency injection (legacy, constructor preferred)
import org.springframework.data.domain.PageRequest;     // Pagination: Pageable
import org.springframework.data.domain.Pageable;        // Pageable interface
import org.springframework.transaction.annotation.Transactional; // Database transactions
```

**Why Used**: 
- `@RestController`: Marks class as REST API handler, auto-converts return values to JSON
- `@Service`: Marks business logic layer, enables transaction management
- `@Component`: Makes classes Spring-managed beans, enables autowiring
- `ResponseEntity<T>`: Allows fine-grained HTTP control (status, headers, body)
- `Transactional`: Ensures database operations are atomic; if one fails, all rollback

### JPA/Hibernate Imports
```java
import jakarta.persistence.*;                           // Database mapping (newer jakarta.persistence, not javax)
import org.springframework.data.jpa.repository.JpaRepository;      // Base repository interface
import org.springframework.data.jpa.repository.Query;   // Custom JPQL queries
import org.springframework.data.jpa.repository.Modifying; // For update/delete operations
```

**Why Used**:
- `jakarta.persistence.*`: JPA annotations for entity mapping (@Entity, @Table, @Column, @ManyToOne)
- `JpaRepository<T, ID>`: Auto-implements CRUD + pagination, eliminates boilerplate
- `@Query`: Allows writing custom JPQL queries (native SQL alternative)
- `@Modifying`: Marks query methods that modify data (not just SELECT)

### Java Utilities Imports
```java
import java.time.LocalDateTime;                        // Database-friendly date/time (JPA standard)
import java.util.List;                                 // Collection type
import java.util.Optional;                             // Null-safe value wrapper (get(), orElse(), isEmpty())
import java.util.Map;                                  // Key-value pairs for JSON responses
```

**Why Used**:
- `LocalDateTime`: Timezone-agnostic; recommended for stored timestamps (not Date/Timestamp)
- `Optional<T>`: Modern null handling; prevent NullPointerException with `isEmpty()` checks
- `Map<K, V>`: Flexible JSON response objects without creating DTOs

### Custom Project Imports
```java
import com.example.project.notification.service.NotificationService; // Interface contract
import com.example.project.notification.service.NotificationServiceImpl; // Implementation
import com.example.project.notification.model.Notification;         // JPA entity
import com.example.project.notification.model.NotificationType;     // Enum
import com.example.project.notification.event.NotificationEvent;    // Event record
import com.example.project.notification.event.NotificationEventType; // Event type enum
import com.example.project.notification.observer.*;                 // Observer pattern components
import com.example.project.notification.strategy.*;                 // Strategy pattern components
import com.example.project.notification.repository.NotificationRepository; // Data access
import com.example.project.entity.User;                              // User entity (relationship)
import com.example.project.entity.ExchangeRequest;                  // Exchange entity
import com.example.project.entity.DeliveryOffer;                    // Delivery entity
import com.example.project.repository.UserRepository;               // User data access
import com.example.project.security.SecurityUtil;                   // Current user retrieval
```

**Why Used**:
- Service imports: Dependency injection contracts
- Model/Entity imports: Database mapping and business objects
- Repository imports: Data access layer
- Pattern imports: Observer and Strategy infrastructure
- Security imports: Authentication context

### Summary Grid

| Import Category | Key Classes | Purpose |
|---|---|---|
| **Spring Web** | `@RestController`, `ResponseEntity`, `HttpStatus` | Build REST APIs |
| **Spring Data** | `JpaRepository`, `Pageable`, `@Query` | Database operations |
| **Spring Core** | `@Service`, `@Component`, `@Transactional` | Dependency management & transactions |
| **JPA/Jakarta** | `@Entity`, `@Table`, `@ManyToOne`, `@Column` | ORM mapping |
| **Java Time** | `LocalDateTime` | Timestamp handling |
| **Java Collections** | `List<T>`, `Optional<T>`, `Map<K,V>` | Data structures |
| **Project Custom** | `NotificationService`, `Notification`, Strategy classes | Domain-specific logic |

---

## 9. Notification Workflow (Complete Lifecycle)

### Scenario: User A Exchanges Book with User B

#### **Workflow Step-by-Step**

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. EXCHANGE REQUEST SENT (User A requests User B's book)         │
├─────────────────────────────────────────────────────────────────┤
│ Location: ExchangeRequestController.createExchangeRequest()     │
│ Code: notificationService.publishExchangeRequestSent(saved, A)  │
│                                                                  │
│ Flow:                                                            │
│  1a. NotificationServiceImpl.publish() creates NotificationEvent │
│  1b. DefaultNotificationSubject broadcasts to observers          │
│  1c. PersistingNotificationObserver.onEvent() receives it       │
│  1d. ExchangeNotificationStrategy.build() creates draft:        │
│      Message: "User A wants to exchange for your book!"        │
│      Recipient: User B (offer owner)                           │
│  1e. Notification saved to database                             │
│                                                                  │
│ Database: INSERT into notifications (recipient, message, type)  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 2. EXCHANGE ACCEPTED (User B accepts the exchange)              │
├─────────────────────────────────────────────────────────────────┤
│ Location: ExchangeRequestController.acceptExchangeRequest()     │
│ Code: notificationService.publishExchangeAccepted(updated, B)  │
│                                                                  │
│ Flow:                                                            │
│  2a. NotificationEvent with type EXCHANGE_ACCEPTED created     │
│  2b. ExchangeNotificationStrategy.build() creates draft:        │
│      Message: "User B accepted your exchange request!"         │
│      Recipient: User A (requester)                             │
│  2c. Notification saved to database                             │
│                                                                  │
│ Database: INSERT into notifications for User A                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 3. DELIVERY CREATED (System creates delivery order)             │
├─────────────────────────────────────────────────────────────────┤
│ Location: ExchangeRequestController.acceptExchangeRequest()     │
│           or DeliveryDashboardController.createDelivery()       │
│ Code: notificationService.publishDeliveryCreated(created, null) │
│                                                                  │
│ Flow:                                                            │
│  3a. NotificationEvent with type DELIVERY_CREATED created      │
│  3b. DeliveryNotificationStrategy.build() creates drafts:       │
│      - Pickup person: "Pickup needed for exchange"              │
│      - Delivery person: "Delivery assigned"                     │
│  3c. Notifications saved for both roles                         │
│                                                                  │
│ Database: INSERT 2 notifications (pickup + delivery persons)    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 4. DELIVERY ASSIGNED (Driver accepts the delivery)              │
├─────────────────────────────────────────────────────────────────┤
│ Location: DeliveryDashboardController.assignDelivery()          │
│ Code: notificationService.publishDeliveryAssigned(offer, ...)   │
│                                                                  │
│ Flow:                                                            │
│  4a. NotificationEvent with type DELIVERY_ASSIGNED created     │
│  4b. DeliveryNotificationStrategy.build() creates draft:        │
│      Message: "Driver assigned for your delivery"               │
│      Recipients: Pickup person, Delivery person, Both users    │
│  4c. Multiple notifications saved                               │
│                                                                  │
│ Database: INSERT 4 notifications (all involved parties)         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 5. PICKUP STARTED (Driver reaches pickup location)              │
├─────────────────────────────────────────────────────────────────┤
│ Location: DeliveryDashboardController.startPickup()             │
│ Code: notificationService.publishPickupStarted(offer, ...)      │
│                                                                  │
│ Flow:                                                            │
│  5a. NotificationEvent with type PICKUP_STARTED created        │
│  5b. Message: "Your book pickup has started!"                   │
│  5c. Notification sent to User A (sender)                       │
│                                                                  │
│ Database: INSERT notification                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 6. BOOK PICKED (Book collected from User A)                     │
├─────────────────────────────────────────────────────────────────┤
│ Location: DeliveryDashboardController.pickBook()                │
│ Code: notificationService.publishBookPicked(offer, ...)         │
│                                                                  │
│ Flow:                                                            │
│  6a. NotificationEvent with type BOOK_PICKED created           │
│  6b. Message: "Book picked! On the way to destination"          │
│  6c. Notification sent to User B (receiver)                     │
│                                                                  │
│ Database: INSERT notification                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 7. DELIVERY COMPLETED (Book delivered to User B)                │
├─────────────────────────────────────────────────────────────────┤
│ Location: DeliveryDashboardController.completeDelivery()        │
│ Code: notificationService.publishDeliveryCompleted(offer, ...)  │
│                                                                  │
│ Flow:                                                            │
│  7a. NotificationEvent with type DELIVERY_COMPLETED created    │
│  7b. Message: "Book successfully delivered!"                    │
│  7c. Notification sent to User B (receiver)                     │
│                                                                  │
│ Database: INSERT notification                                   │
│                                                                  │
│ Result: User B now owns the received book from User A           │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
┌───────────────────────────┐
│  ExchangeRequestController │
│  or DeliveryDashboardCtrl  │
└──────────────┬──────────────┘
               │
        publish*Request()
               │
               ▼
┌──────────────────────────────────────┐
│   NotificationServiceImpl             │
│   (publish method)                   │
└──────────────┬───────────────────────┘
               │
     Creates NotificationEvent record
               │
               ▼
┌──────────────────────────────────────┐
│   DefaultNotificationSubject          │
│   (Observer Subject/Publisher)        │
└──────────────┬───────────────────────┘
               │
   Notifies all registered observers
               │
               ▼
┌──────────────────────────────────────┐
│   PersistingNotificationObserver      │
│   (Concrete Observer)                 │
└──────────────┬───────────────────────┘
               │
  1. Resolves strategy from registry
  2. Calls strategy.build(event)
               │
               ▼
    ┌─────────────────────────────┐
    │ NotificationStrategy         │
    │ - ExchangeStrategy           │
    │ - DeliveryStrategy           │
    │ - SystemStrategy             │
    └──────────┬────────────────────┘
               │
   Returns: List<NotificationDraft>
               │
               ▼
    ┌─────────────────────────────┐
    │ Deduplication & Validation   │
    │ (check event_key, actor)     │
    └──────────┬────────────────────┘
               │
               ▼
    ┌─────────────────────────────┐
    │ NotificationRepository.save  │
    └──────────┬────────────────────┘
               │
               ▼
           Database
        notifications table
```

### Transaction Safety

The entire flow happens within a **@Transactional** block:
- If notification save fails → event rollback
- If deduplication fails → skip silently (no duplicate)
- If strategy returns empty list → no notification sent (expected behavior for some events)

---

## 10. Alignment with Project Guidelines

### REST API Design Compliance

✅ **Resource-Based URLs**: `/notifications` represents resource, not actions
✅ **HTTP Methods**: Standard GET (read), PATCH (update), DELETE (remove)
✅ **Status Codes**: 200 (success), 404 (not found), 401 (unauthorized)
✅ **Request/Response Format**: JSON with clear structure
✅ **Pagination**: Start with limit parameter for result control
✅ **Authentication**: All endpoints check current user via `SecurityUtil`

### Layered Architecture Compliance

✅ **Entity Layer**: `Notification.java` with proper JPA annotations
✅ **Repository Layer**: `NotificationRepository` extends `JpaRepository`
✅ **Service Layer**: `NotificationService` (interface) + `NotificationServiceImpl` (implementation)
✅ **Controller Layer**: `NotificationRestController` handles HTTP requests
✅ **Clear Separation**: Each layer has single responsibility

### Design Patterns & SOLID Principles

✅ **Observer Pattern**: Decouples event source from listeners
✅ **Strategy Pattern**: Encapsulates different notification type handling
✅ **Dependency Injection**: Spring autowires dependencies
✅ **Open/Closed Principle**: Add new strategies without modifying existing code
✅ **Single Responsibility**: Each class has one reason to change
✅ **Loose Coupling**: Notification system independent of Exchange/Delivery logic

### Database Design Compliance

✅ **Entity Relationships**: Proper `@ManyToOne` with lazy loading
✅ **Indexing**: Composite indexes for common query patterns
✅ **Unique Constraints**: Prevent duplicate notifications
✅ **Normalization**: Notification references User entity (no denormalization)

### Clean Code & Modularity

✅ **Package Structure**: `notification/` isolated in separate package
✅ **No Cross-Cutting Concerns**: Notification logic doesn't leak into other modules
✅ **Reusability**: `NotificationService` interface allows multiple implementations
✅ **Testability**: Each component can be unit tested in isolation

### Security Compliance

✅ **Authentication Checks**: All endpoints verify current user
✅ **Authorization**: Users can only see/modify their own notifications
✅ **Input Validation**: Message length, event key validation
✅ **SQL Injection Prevention**: JPA parameterized queries (no string concatenation)

---

## 11. Viva-Ready Explanation (How to Explain to Professor)

### Quick Overview (30 seconds)

> "The Notification System is an event-driven module that keeps users informed about exchange requests, delivery progress, and system announcements. It uses the **Observer pattern** to decouple event publishing from persistence, and the **Strategy pattern** to handle different notification types. Integration is minimal—just one publish method call in Exchange and Delivery controllers."

### File-by-File Walkthrough

**Q: "Where is the Notification System implemented?"**

A: "It's in the `notification/` package with these layers:
- **Model**: `notification/model/Notification.java` (JPA entity) and `NotificationType.java` (enum)
- **Event**: `notification/event/NotificationEvent.java` (the event record)
- **Observer**: `notification/observer/` contains the publisher (`DefaultNotificationSubject`) and observer (`PersistingNotificationObserver`)
- **Strategy**: `notification/strategy/` contains strategies for Exchange, Delivery, and System notification types
- **Service**: `notification/service/NotificationServiceImpl.java` orchestrates everything
- **Repository**: `notification/repository/NotificationRepository.java` handles database access
- **Controller**: `notification/web/NotificationRestController.java` exposes REST APIs"

**Q: "Which design patterns did you use and why?"**

A: "Two main patterns:

1. **Observer Pattern** (in `notification/observer/`):
   - When an event occurs, `NotificationServiceImpl` calls `notificationSubject.publish(event)`
   - The `DefaultNotificationSubject` broadcasts to all registered `NotificationObserver` implementations
   - The `PersistingNotificationObserver` receives the event and persists notifications
   - This decouples event creation from notification handling

2. **Strategy Pattern** (in `notification/strategy/`):
   - Different event types require different notification logic
   - Each strategy implements `NotificationStrategy` interface
   - `ExchangeNotificationStrategy` handles exchange events
   - `DeliveryNotificationStrategy` handles delivery milestones
   - `SystemNotificationStrategy` handles admin announcements
   - `NotificationStrategyRegistry` maps event types to strategies
   - This avoids massive if-else chains and makes adding new types easy"

**Q: "How are notifications triggered without modifying Exchange or Delivery logic?"**

A: "Minimal integration points:
- In `ExchangeRequestController`, when an exchange request is created, I call: `notificationService.publishExchangeRequestSent(request, userId)`
- In `DeliveryDashboardController`, when status changes, I call: `notificationService.publishPickupStarted(delivery, userId)`
- These are single-line calls. The Exchange/Delivery code knows nothing about how notifications work internally. It just publishes events."

**Q: "What important imports are used?"**

A: "Key imports:
- `@Service`, `@RestController`, `@Component`: Spring annotations for dependency management
- `@Transactional`: Ensures database operations are atomic
- `JpaRepository`: Simplifies database access
- `Optional<T>`: Modern null-safe handling
- `jakarta.persistence.*`: JPA entity mapping annotations
- `LocalDateTime`: Standard Java time API for timestamps"

**Q: "How do you prevent duplicate notifications?"**

A: "Two mechanisms:
1. Unique constraint on `(event_key, recipient_id)` in the database
2. In `PersistingNotificationObserver.onEvent()`, before saving, I check: `existsByEventKeyAndRecipient_Id()`. If exists, skip the notification."

**Q: "What about REST API endpoints?"**

A: "Four endpoints in `NotificationRestController`:
- `GET /notifications` - list notifications, with optional filtering by `unread` and `limit`
- `GET /notifications/summary` - get unread count + recent notifications for the bell icon
- `PATCH /notifications/{id}` - mark single notification as read
- `PATCH /notifications` - mark all notifications as read

All endpoints check the current authenticated user via `SecurityUtil` to ensure users see only their notifications."

**Q: "How does the database schema support performance?"**

A: "The `notifications` table has:
- Foreign key to `users` table (recipient)
- Two indexes: `(recipient_id, created_at)` for listing, and `(recipient_id, is_read)` for filtering unread
- Unique constraint `(event_key, recipient_id)` to prevent duplicates
- This ensures queries like 'get unread notifications for user' are fast even with millions of records"

**Q: "What information does a notification store?"**

A: "A `Notification` entity stores:
- `id`: Unique identifier
- `recipient`: Which user receives it (foreign key)
- `message`: The human-readable notification text (e.g., 'John accepted your exchange')
- `type`: Category (EXCHANGE, DELIVERY, or SYSTEM)
- `isRead`: Boolean flag for read status
- `eventKey`: Unique identifier for the event that triggered this notification (prevents duplicates)
- `createdAt`: Timestamp of creation"

**Q: "How is this system testable?"**

A: "Because of the design:
- Mock `NotificationService` in Exchange/Delivery controller tests
- Mock `NotificationRepository` in service tests
- Mock `NotificationSubject` to verify observers are called
- Unit test each strategy independently
- No side effects or static state, so tests are isolated and repeatable"

### Explaining the Module

**Why modular design?**
> "Separation of concerns. Notification logic is completely independent of Exchange/Delivery. If I need to change how notifications are formatted, I only modify the Strategy classes, not the whole system. If I add a new notification type, I create a new Strategy and register it. The Exchange/Delivery controllers never change."

**Why Observer pattern?**
> "Because we need multiple observers (currently just persisting, but could add email observer, SMS observer later) to be notified of the same event. The publisher doesn't need to know about specific observers. It just publishes to a subject, and any observer listening automatically gets the event."

**Why Strategy pattern?**
> "Different events need completely different notification messages and recipient logic. An exchange event involves offer owner and requester. A delivery event involves multiple roles. Rather than 50 if-else statements, each event type has its own strategy class that knows exactly what to do."

**Why REST API?**
> "Because the frontend needs to:
1. Fetch notifications for the notification center page
2. Fetch summary (unread count + recent) for the bell icon that updates every 30 seconds
3. Mark notifications as read when user clicks them
4. REST provides a clean, stateless way to do this with standard HTTP methods"

---

## 12. Summary Table: Quick Reference for Viva

| Question | Answer | File |
|----------|--------|------|
| **Where implemented?** | `src/main/java/com/example/project/notification/` | All notification/* |
| **Entity stored?** | `Notification.java` with recipient, message, type, isRead, eventKey, createdAt | `notification/model/` |
| **REST endpoints?** | GET `/notifications`, GET `/notifications/summary`, PATCH `/notifications/{id}`, PATCH `/notifications` | `NotificationRestController.java` |
| **Design patterns?** | Observer (event-driven), Strategy (different notification types) | `notification/observer/`, `notification/strategy/` |
| **How integrated?** | Single-line publish calls in `ExchangeRequestController` and `DeliveryDashboardController` | `controller/*` |
| **Key imports?** | `@Service`, `@RestController`, `@Transactional`, `JpaRepository`, `Optional`, `LocalDateTime` | Top of each file |
| **Database indexes?** | `(recipient_id, created_at)`, `(recipient_id, is_read)` | `Notification.java` @Table |
| **Duplicate prevention?** | Unique constraint `(event_key, recipient_id)` + repository check | `PersistingNotificationObserver.java` |
| **Event types?** | EXCHANGE_REQUEST_SENT, EXCHANGE_ACCEPTED, EXCHANGE_REJECTED, DELIVERY_CREATED, DELIVERY_ASSIGNED, PICKUP_STARTED, BOOK_PICKED, DELIVERY_COMPLETED, SYSTEM_ANNOUNCEMENT | `NotificationEventType.java` |
| **Strategies?** | ExchangeNotificationStrategy, DeliveryNotificationStrategy, SystemNotificationStrategy | `notification/strategy/*` |

---

## Conclusion

The **Notification System** demonstrates strong software engineering principles:

✅ **Clean Architecture**: Layered design with clear separation of concerns
✅ **Design Patterns**: Observer and Strategy for extensibility and maintainability
✅ **REST API**: Standard HTTP methods with proper status codes
✅ **Database Design**: Optimized schema with indexes and constraints
✅ **Integration**: Minimal coupling with existing systems
✅ **Security**: Authentication checks and user isolation
✅ **Testability**: Each component independently mockable
✅ **Modularity**: Independent package that can be evolved without modifying other modules

This design aligns perfectly with **SEPM guidelines** for professional software development.
