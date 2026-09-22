# BuildingOS
## Detailed Business Requirements Document (BRD) + Agentic Development Blueprint

**Product:** BuildingOS – Building & Rental Management System  
**Target Market:** Multi-flat residential buildings in Bangladesh  
**Primary Clients:** Android/iOS/Web-ready Flutter application  
**Frontend Stack:** Flutter, GetX, Clean Architecture, Drift  
**Backend Stack:** Java + Spring Boot Microservices, API Gateway, Kafka, PostgreSQL/Supabase-compatible infrastructure  
**Architecture Style:** Microservices + Clean Architecture + Repository Pattern + Use Case Pattern + Feature-First Package Structure  
**Document Purpose:** Product, UX, backend, data, API, event, security, offline-sync, and agentic-development source of truth.

---

# 1. Product Vision

BuildingOS is a multi-tenant SaaS building operations and rental-management platform for apartment buildings, committees, owners, managers, tenants, and BuildingOS platform operators. A single user identity may participate in many buildings and may own multiple units across multiple buildings.

The system centralizes:

- Building administration
- Unit ownership
- Tenant lifecycle
- Lease management
- Rent collection
- Maintenance-fee collection
- Expense management
- Building work tracking
- Contractor management
- Announcements
- Meetings
- Reports
- Receipts
- Assets
- Notifications
- Audit history
- Offline-first mobile operations

The platform must support buildings where one owner can own multiple units, one manager can collect money on behalf of owners, a tenant may change over time, flats may become vacant, and the committee separately manages common building finances.

---

# 2. Business Goals

1. Provide transparent maintenance and rent accounting.
2. Reduce cash-payment disputes using immutable transaction history and receipts.
3. Provide owner visibility across multiple flats.
4. Give tenants clear due/payment information.
5. Create structured building-management workflows.
6. Maintain historical ownership, tenancy, rent, and expense data.
7. Support offline field usage and safe later synchronization.
8. Operate as a SaaS platform supporting many independently managed buildings, building-level subscriptions, and users who participate in or own units across multiple buildings.
9. Establish reliable auditability for financial and administrative actions.
10. Keep backend services independently deployable and independently scalable.

---

# 3. Scope

## 3.1 In Scope

- User authentication
- Building application and self-service onboarding
- Back-office building review, approval, rejection, suspension, and activation
- Assisted onboarding by authorized BuildingOS staff
- Building-level subscription and entitlement management
- Cross-building user/property portfolio
- Role assignment
- Unit management
- Ownership
- Tenants
- Leases
- Rent invoices
- Rent payments
- Partial payments
- Advance payments
- Maintenance invoices
- Maintenance payments
- Common expense management
- Work orders
- Contractor management
- Meeting records
- Announcements
- Notifications
- Receipts
- Reports
- Assets
- Contact directory
- Offline operation
- Sync engine
- Audit trail
- Multi-building SaaS tenancy and subscription enforcement

## 3.2 Future / Optional Scope

- Automated subscription billing/payment collection
- Integrated payment gateway
- bKash/Nagad merchant APIs
- Utility billing
- Visitor management
- Gate access
- QR rent payment
- Accounting integrations
- OCR receipt extraction
- AI financial assistant
- Predictive maintenance
- Vendor marketplace
- Tenant issue chat
- Service marketplace

---

# 4. Actors and Roles

## 4.1 System Roles

### SUPER_ADMIN
Highest BuildingOS platform authority. This is a platform-scoped role and is not tied to one building.

Permissions:
- Review/approve/reject building applications
- Create, activate, suspend, and archive buildings
- Manage building subscriptions, plans, and entitlements
- Assign/remove Building Admins
- Assign onboarding/support staff
- View platform-wide operational dashboards
- View platform audit/security events subject to policy
- Configure platform-level policies

### PLATFORM_ADMIN
Day-to-day BuildingOS back-office operator. Permissions are narrower than `SUPER_ADMIN` and must be explicitly granted.

Typical permissions:
- Review building applications
- Manage building lifecycle
- Manage building subscription state
- Assign Building Admins
- View operational building metadata
- Add internal back-office notes

### ONBOARDING_AGENT
BuildingOS staff member who assists customers during initial setup through a temporary, scoped onboarding assignment.

Typical permissions:
- View assigned onboarding cases
- Configure approved building setup data within granted scope
- Bulk-create floors/units
- Assist with owner/member invitations
- Configure initial building settings
- Mark onboarding tasks complete

Restrictions:
- No implicit access to unrelated buildings
- No permanent building membership unless separately granted
- No unrestricted financial access
- Every assisted action must be audited

### SUPPORT_AGENT
BuildingOS support operator with temporary, reason-bound, auditable assistance access.

Typical permissions:
- View support cases assigned to the agent
- Request/start scoped support sessions
- Assist users with configuration and navigation

Restrictions:
- No silent or unrestricted impersonation
- Sensitive financial/admin actions are blocked or require elevated approval
- Support access expires automatically

### SUBSCRIPTION_ADMIN
Optional platform role for SaaS commercial operations.

Typical permissions:
- Manage plans and plan availability
- Assign/change building plans
- Manage trial, grace-period, suspension, cancellation, and reactivation states
- View subscription history and entitlement state

### BUILDING_ADMIN
Highest building-level authority.

Permissions:
- Configure building
- Manage committee
- Manage managers
- Manage owners
- Manage units
- Access all building data
- Financial reporting
- Audit access

### COMMITTEE
Building governance role.

Permissions:
- Maintenance financials
- Common expenses
- Work management
- Contractor management
- Meetings
- Announcements
- Reports

### OWNER
Unit owner.

Permissions:
- View owned units
- View rent
- View tenant
- View lease
- View rent payments
- View maintenance dues
- View selected building reports
- Receive notices

### PROPERTY_MANAGER / RENT_MANAGER
Operates rental collection.

Permissions:
- Tenant onboarding
- Lease creation
- Rent collection
- Payment recording
- Receipt generation
- Due follow-up
- Move-in/move-out

### TENANT
Occupant.

Permissions:
- View own lease
- View current rent
- View dues
- View payment history
- Download receipt
- Receive announcements
- View permitted contacts

### ACCOUNTANT
Optional finance role.

Permissions:
- Payment entry
- Expense entry
- Reports
- Reconciliation
- No ownership/role administration

### VIEWER / AUDITOR
Read-only access.

---

# 5. Role Authorization Model

Authorization must distinguish **platform context** from **building context**.

Platform-scoped roles (`SUPER_ADMIN`, `PLATFORM_ADMIN`, `ONBOARDING_AGENT`, `SUPPORT_AGENT`, `SUBSCRIPTION_ADMIN`) are managed separately from building memberships.

A user can have different roles in different buildings. A user's ownership of a unit is a separate domain relationship and must not be inferred only from the `OWNER` role.

Example:

```text
User A
 ├─ Building 1 → OWNER
 ├─ Building 2 → COMMITTEE
 └─ Building 3 → TENANT
```

Use separate authorization relationships:

```text
platform_user_role
user_building_role
```

Ownership is modeled separately:

```text
user -> ownership -> unit -> building
```

One user may therefore own multiple units in one building and units across many buildings while also holding different operational roles in each building.

Each request must include active building context.

Recommended token claims:

```json
{
  "sub": "user-uuid",
  "buildingId": "building-uuid",
  "roles": ["OWNER", "COMMITTEE"],
  "permissions": [
    "rent.read",
    "maintenance.read",
    "announcement.create"
  ]
}
```

Authorization should use permission checks instead of hard-coding role names in controller logic.

---

# 6. High-Level Backend Architecture

```text
Flutter App
    |
    v
API Gateway
    |
    +---------------------------------------------------+
    |         |         |         |         |          |
    v         v         v         v         v          v
Identity   Building   Rental    Finance   Work      Communication
Service    Service    Service   Service   Service   Service
    |         |         |         |         |          |
    +---------+---------+---------+---------+----------+
                          |
                        Kafka
                          |
            +-------------+-------------+
            |             |             |
            v             v             v
      Notification    Reporting      Audit
       Service         Service       Service
```

Each microservice owns its domain and persistence.

No service may directly access another service's database.

Cross-service communication:

- Synchronous: REST through API Gateway or internal service API
- Asynchronous: Kafka events

Prefer asynchronous event propagation where eventual consistency is acceptable.

---

# 7. Required Backend Components

## 7.1 API Gateway

Recommended responsibilities:

- Routing
- Authentication token validation
- Correlation IDs
- Request tracing
- Rate limiting
- API versioning
- CORS
- Tenant/building context validation
- Request logging
- Circuit breaking
- Response normalization where appropriate

Suggested technology:

- Spring Cloud Gateway

Do not place domain/business logic in the gateway.

---

# 8. Proposed Microservices

## 8.1 Identity & Access Service

Responsibilities:

- User profile
- Login identity mapping
- Phone authentication integration
- Google authentication integration
- Building memberships
- Roles
- Permissions
- Invitations
- Platform roles
- Temporary support/onboarding access grants
- Device tokens
- Session security

Core entities:

- User
- UserProfile
- BuildingMembership
- Role
- Permission
- RolePermission
- UserRole
- Invitation
- PlatformRole
- PlatformUserRole
- SupportAccessGrant
- DeviceRegistration

Publishes:

- `user.created`
- `user.updated`
- `building.member.added`
- `building.member.removed`
- `role.assigned`
- `platform.role.assigned`
- `support.access.granted`
- `support.access.revoked`
- `device.registered`

---

## 8.2 Building Service

Responsibilities:

- Building application
- Building approval/lifecycle state
- Building onboarding progress
- Assisted onboarding assignment metadata
- Building
- Floor
- Flat/unit
- Parking
- Storage
- Ownership
- Ownership history
- Unit metadata
- Building settings
- Contact directory metadata

Core entities:

- BuildingApplication
- BuildingApplicationReview
- BuildingOnboarding
- AssistedOnboardingSession
- Building
- Floor
- Unit
- ParkingSpace
- StorageUnit
- Ownership
- OwnershipHistory
- UnitAssignment

Unit types:

```text
FLAT
PARKING
STORAGE
COMMERCIAL
COMMON
OTHER
```

Publishes:

- `building.application.created`
- `building.application.submitted`
- `building.application.info_requested`
- `building.application.approved`
- `building.application.rejected`
- `building.onboarding.started`
- `building.onboarding.completed`
- `building.activated`
- `building.suspended`
- `building.created`
- `building.updated`
- `unit.created`
- `unit.updated`
- `ownership.assigned`
- `ownership.transferred`

---

## 8.3 Rental Service

Responsibilities:

- Tenant profile
- Lease
- Rent configuration
- Rent billing
- Rent due state
- Tenant lifecycle
- Move-in
- Notice
- Move-out
- Rent adjustments
- Advance rent ledger reference

Core entities:

- Tenant
- Lease
- LeaseTenant
- RentSchedule
- RentInvoice
- RentAdjustment
- MoveIn
- MoveOut
- Notice

Publishes:

- `tenant.created`
- `lease.created`
- `lease.activated`
- `rent.invoice.created`
- `rent.invoice.overdue`
- `tenant.notice.submitted`
- `lease.terminated`
- `unit.vacated`

Consumes:

- `unit.created`
- `ownership.transferred`
- `payment.allocated`

---

## 8.4 Finance Service

Responsibilities:

- Maintenance fees
- Payments
- Receipts
- Allocation
- Partial payments
- Advance balances
- Common expenses
- Categories
- Financial periods
- Payment channels
- Reconciliation
- Owner income summaries

Core entities:

- MaintenanceInvoice
- Payment
- PaymentAllocation
- Receipt
- AdvanceLedger
- Expense
- ExpenseCategory
- FinancialPeriod
- PaymentMethod
- ReconciliationRecord

Payment types:

```text
RENT
MAINTENANCE
SECURITY_DEPOSIT
ADVANCE_RENT
OTHER
```

Payment methods:

```text
CASH
BKASH
NAGAD
BANK_TRANSFER
CARD
CHEQUE
OTHER
```

Publishes:

- `payment.recorded`
- `payment.allocated`
- `payment.reversed`
- `receipt.generated`
- `maintenance.invoice.created`
- `maintenance.invoice.overdue`
- `expense.created`
- `expense.approved`
- `advance.balance.changed`

Consumes:

- `rent.invoice.created`
- `lease.activated`
- `unit.created`

---

## 8.5 Work & Maintenance Service

Responsibilities:

- Work requests
- Planned work
- Maintenance work
- Contractors
- Quotes
- Assignment
- Deadlines
- Status
- Work expenses reference
- Work attachments

Entities:

- WorkOrder
- WorkTask
- Contractor
- ContractorContact
- WorkAssignment
- Quote
- WorkAttachment
- WorkComment

Statuses:

```text
DRAFT
PLANNED
ASSIGNED
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

Publishes:

- `work.created`
- `work.assigned`
- `work.started`
- `work.overdue`
- `work.completed`
- `contractor.created`

---

## 8.6 Communication Service

Responsibilities:

- Announcements
- Meeting records
- Agenda
- Decisions
- Attachments
- Audience targeting
- Read acknowledgment
- Optional comments

Entities:

- Announcement
- AnnouncementAudience
- AnnouncementRead
- Meeting
- AgendaItem
- MeetingDecision
- MeetingParticipant
- Comment

Publishes:

- `announcement.published`
- `meeting.created`
- `meeting.updated`
- `meeting.decision.recorded`

---

## 8.7 Notification Service

Responsibilities:

- FCM notification
- Notification preference
- Scheduled reminders
- Notification inbox
- Email/SMS future adapters
- Delivery tracking
- Retry

Consumes events:

- `rent.invoice.overdue`
- `maintenance.invoice.overdue`
- `payment.recorded`
- `announcement.published`
- `meeting.created`
- `work.assigned`
- `work.overdue`

Entities:

- Notification
- NotificationTemplate
- NotificationPreference
- NotificationDelivery

---

## 8.8 Document Service

Responsibilities:

- Receipt files
- Lease files
- NID file references
- Expense receipts
- Work attachments
- Meeting files
- Signed URLs
- File metadata

Entities:

- Document
- DocumentVersion
- DocumentReference

Suggested object storage:

- Supabase Storage / S3-compatible object storage

---

## 8.9 Reporting Service

Responsibilities:

- Aggregated read models
- Building dashboard projections
- Owner dashboard
- Manager dashboard
- Committee dashboard
- Monthly statements
- Six-month reports
- Export-ready views

The reporting service should generally consume Kafka events and maintain denormalized read projections.

Do not perform complex cross-service report joins at runtime whenever avoidable.

---

## 8.10 Audit Service

Responsibilities:

- Immutable audit events
- Financial action history
- Administrative action history
- User action trace
- Before/after metadata

Consumes important domain events.

Audit records must not be editable by ordinary application users.

---

## 8.11 Sync Service

Recommended for offline-first behavior.

Responsibilities:

- Device sync cursors
- Mutation ingestion
- Idempotency keys
- Delta synchronization
- Conflict policies
- Device acknowledgment

Alternative:
Each domain service can expose `/sync`, but a dedicated sync orchestration service provides a cleaner mobile contract.

---

## 8.12 Subscription & Entitlement Service

BuildingOS is a SaaS product. Subscription state is a platform concern and must not be embedded in rental or finance-domain entities.

Responsibilities:

- Subscription plans
- Building subscription lifecycle
- Trial management
- Plan changes
- Grace period / suspension state
- Feature entitlements
- Usage-limit enforcement metadata
- Subscription history
- Future billing-provider integration

Core entities:

- SubscriptionPlan
- BuildingSubscription
- SubscriptionChange
- Entitlement
- BuildingEntitlement
- SubscriptionUsageSnapshot

Subscription states:

```text
TRIAL
ACTIVE
PAST_DUE
GRACE_PERIOD
SUSPENDED
CANCELLED
EXPIRED
```

Rules:

- Subscription is building-level by default.
- A user's membership in multiple buildings does not merge those buildings' subscriptions.
- Subscription controls product access/entitlements; it does not define building roles or ownership.
- Back-office admins may change subscription state only through audited use cases.
- Automated charging/payment gateway integration may be added later without changing the subscription domain model.

Publishes:

- `subscription.started`
- `subscription.plan_changed`
- `subscription.trial_ending`
- `subscription.past_due`
- `subscription.suspended`
- `subscription.reactivated`
- `subscription.cancelled`
- `building.entitlements.changed`

---

# 9. Microservice Data Ownership

| Domain | Owning Service | Database |
|---|---|---|
| Users / memberships | Identity | identity_db |
| Buildings / units / ownership | Building | building_db |
| Tenants / leases / rent invoice | Rental | rental_db |
| Payments / expenses / maintenance | Finance | finance_db |
| Work / contractors | Work | work_db |
| Announcements / meetings | Communication | communication_db |
| Notification state | Notification | notification_db |
| Document metadata | Document | document_db |
| Read models | Reporting | reporting_db |
| Audit records | Audit | audit_db |
| Sync metadata | Sync | sync_db |
| Plans / subscriptions / entitlements | Subscription | subscription_db |

Database-per-service is mandatory.

---

# 10. Spring Boot Internal Architecture

Every service must use Clean Architecture.

```text
feature/
  <feature-name>/
    domain/
      model/
      valueobject/
      exception/
      repository/
      service/
    application/
      port/
        in/
        out/
      usecase/
      command/
      query/
      dto/
      mapper/
    infrastructure/
      persistence/
        entity/
        repository/
        mapper/
      messaging/
      client/
      config/
    presentation/
      rest/
      request/
      response/
```

Example:

```text
finance-service/
└─ src/main/java/com/buildingos/finance/
   ├─ payment/
   │  ├─ domain/
   │  │  ├─ model/Payment.java
   │  │  ├─ model/PaymentAllocation.java
   │  │  └─ repository/PaymentRepository.java
   │  ├─ application/
   │  │  ├─ port/in/RecordPaymentUseCase.java
   │  │  ├─ port/out/PaymentEventPublisher.java
   │  │  ├─ command/RecordPaymentCommand.java
   │  │  └─ usecase/RecordPaymentService.java
   │  ├─ infrastructure/
   │  │  ├─ persistence/
   │  │  └─ messaging/
   │  └─ presentation/
   │     └─ rest/PaymentController.java
   ├─ maintenanceinvoice/
   ├─ expense/
   └─ receipt/
```

This is **feature-first** first, layer-second.

Avoid:

```text
controllers/
services/
repositories/
entities/
```

at the global service root.

---

# 11. Repository Pattern

Domain defines repository interfaces.

Example:

```java
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(PaymentId id);
    List<Payment> findByLeaseId(LeaseId leaseId);
}
```

Infrastructure implements:

```java
@Component
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final JpaPaymentRepository jpaRepository;
}
```

Domain must not depend on Spring Data JPA.

---

# 12. Use Case Pattern

Each business action is an explicit use case.

Examples:

```text
CreateBuildingUseCase
CreateUnitUseCase
AssignOwnerUseCase
TransferOwnershipUseCase
CreateTenantUseCase
CreateLeaseUseCase
ActivateLeaseUseCase
GenerateRentInvoiceUseCase
RecordPaymentUseCase
AllocatePaymentUseCase
ReversePaymentUseCase
CreateMaintenanceInvoiceUseCase
CreateExpenseUseCase
CreateWorkOrderUseCase
PublishAnnouncementUseCase
CreateMeetingUseCase
MoveOutTenantUseCase
```

Controller responsibilities:

1. Validate transport-level request.
2. Convert request to command/query.
3. Invoke use case.
4. Return response.

Controller must not contain business logic.

---

# 13. CQRS Guidance

Full CQRS is not mandatory.

Use a lightweight command/query separation.

Command examples:

```text
CreateLeaseCommand
RecordPaymentCommand
TransferOwnershipCommand
CompleteWorkOrderCommand
```

Query examples:

```text
GetOwnerDashboardQuery
GetCurrentRentDueQuery
GetUnitOccupancyQuery
GetFinancialSummaryQuery
```

Reporting-heavy views should use Reporting Service projections.

---

# 14. Kafka Architecture

## 14.1 Rules

1. Services publish domain events after successful local transaction.
2. Use Transactional Outbox Pattern.
3. Consumer handlers must be idempotent.
4. Every event must include unique `eventId`.
5. Every event must include `occurredAt`.
6. Every event should include `buildingId` when relevant.
7. Events are versioned.

Base event:

```json
{
  "eventId": "uuid",
  "eventType": "payment.recorded",
  "eventVersion": 1,
  "occurredAt": "2026-09-21T10:00:00Z",
  "producer": "finance-service",
  "buildingId": "uuid",
  "correlationId": "uuid",
  "payload": {}
}
```

## 14.2 Recommended Topics

```text
building.events
identity.events
rental.events
finance.events
work.events
communication.events
document.events
notification.commands
audit.events
```

Initially use event-type header or envelope.

For larger scale, split high-volume events into dedicated topics.

---

# 15. Transactional Outbox

Every service that publishes Kafka events must use an outbox table.

Example:

```text
outbox_event
- id
- aggregate_type
- aggregate_id
- event_type
- event_version
- payload
- created_at
- published_at
- status
```

Local transaction:

```text
business entity update
+
outbox insert
=
same DB transaction
```

Background publisher sends unpublished outbox rows to Kafka.

This prevents:

```text
DB commit succeeds
Kafka publish fails
```

from creating inconsistent state.

---

# 16. Idempotency

Financial mutations require idempotency.

Request header:

```text
Idempotency-Key: <uuid>
```

Critical APIs:

- Record payment
- Reverse payment
- Create expense
- Generate invoice
- Sync mutation
- Lease activation

Repeated identical request must not create duplicate transaction.

---

# 17. Distributed Transaction Policy

Do not use two-phase commit.

Use Saga / event-driven workflows.

Example: lease activation

```text
Rental Service
  -> activates lease
  -> emits lease.activated

Finance Service
  -> creates initial rent obligations
  -> emits rent.invoice.created

Reporting Service
  -> updates owner dashboard
```

If downstream processing fails, retry via Kafka.

Where compensation is needed, define explicit compensating action.

---

# 18. API Standards

Base:

```text
/api/v1
```

Example:

```text
POST   /api/v1/buildings
GET    /api/v1/buildings/{buildingId}
POST   /api/v1/buildings/{buildingId}/units
GET    /api/v1/units/{unitId}

POST   /api/v1/tenants
POST   /api/v1/leases
POST   /api/v1/leases/{leaseId}/activate
POST   /api/v1/leases/{leaseId}/notice
POST   /api/v1/leases/{leaseId}/move-out

GET    /api/v1/rent-invoices
POST   /api/v1/payments
POST   /api/v1/payments/{id}/reverse

GET    /api/v1/maintenance-invoices
POST   /api/v1/expenses

POST   /api/v1/work-orders
PATCH  /api/v1/work-orders/{id}/status

POST   /api/v1/announcements
POST   /api/v1/meetings

POST   /api/v1/building-applications
GET    /api/v1/building-applications/{id}
POST   /api/v1/building-applications/{id}/submit
GET    /api/v1/buildings/{buildingId}/onboarding
PATCH  /api/v1/buildings/{buildingId}/onboarding

GET    /api/v1/platform/building-applications
POST   /api/v1/platform/building-applications/{id}/approve
POST   /api/v1/platform/building-applications/{id}/reject
POST   /api/v1/platform/building-applications/{id}/request-information
POST   /api/v1/platform/buildings/{buildingId}/assign-admin
POST   /api/v1/platform/buildings/{buildingId}/assign-onboarding-agent

GET    /api/v1/platform/subscriptions
GET    /api/v1/platform/buildings/{buildingId}/subscription
POST   /api/v1/platform/buildings/{buildingId}/subscription/change-plan
POST   /api/v1/platform/buildings/{buildingId}/subscription/suspend
POST   /api/v1/platform/buildings/{buildingId}/subscription/reactivate
```

Platform endpoints require platform-scoped permissions and must never rely only on an active `buildingId` claim.

Standard response:

```json
{
  "success": true,
  "data": {},
  "meta": {},
  "traceId": "uuid"
}
```

Standard validation error:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fields": {
    "amount": "Amount must be greater than zero"
  },
  "traceId": "uuid"
}
```

---

# 19. Core Domain Rules

## 19.1 Ownership

- One owner may own many units.
- One unit may optionally support multiple co-owners.
- Ownership has effective dates.
- Ownership history must never be overwritten.
- Transfer creates a new ownership period.

Suggested:

```text
ownership
- id
- unit_id
- owner_user_id
- share_percentage
- valid_from
- valid_to
- status
```

---

# 20. Tenant and Lease Rules

- A unit may have zero or one active primary lease at a time.
- A lease may include multiple occupants.
- Historical leases remain immutable except safe corrections.
- Move-out closes tenancy.
- Security deposit is recorded separately from rent.
- Rent amount is versioned or tied to lease period.
- Lease files are documents, not blobs stored inside the Rental DB.

Lease states:

```text
DRAFT
PENDING
ACTIVE
NOTICE_GIVEN
EXPIRED
TERMINATED
CANCELLED
```

---

# 21. Rent Invoice Rules

A monthly rent invoice includes:

```text
base rent
parking charge
service charge
other recurring charge
adjustments
late fee
discount
previous balance reference
```

Status:

```text
DRAFT
ISSUED
PARTIALLY_PAID
PAID
OVERDUE
VOID
```

Due:

```text
due = invoice total - allocated payment
```

Do not calculate due purely from raw payment totals.

Use allocations.

---

# 22. Payment Allocation Model

Example:

Tenant owes:

```text
January  = 20,000
February = 20,000
```

Pays:

```text
30,000
```

Payment:

```text
Payment P1 = 30,000
```

Allocations:

```text
P1 -> January  = 20,000
P1 -> February = 10,000
```

February remaining:

```text
10,000
```

This design supports:

- Partial payments
- Bulk payment
- Advance payment
- Accurate invoice settlement
- Reconciliation
- Reversal

---

# 23. Advance Rent

Advance money is a ledger balance, not a negative invoice.

Example:

```text
Tenant pays 50,000
Current invoice = 20,000

Allocate 20,000
Remaining 30,000 -> advance ledger
```

Next invoice can consume advance through an explicit allocation.

---

# 24. Payment Reversal

Never delete financial payment records.

Use:

```text
status = REVERSED
reversal_reason
reversed_by
reversed_at
```

Reverse allocations and append audit event.

---

# 25. Maintenance Fee Rules

Maintenance fees can be configured by:

- Flat
- Unit size
- Fixed building rate
- Custom rate
- Owner category

System must support recurring generation.

Maintenance invoice:

```text
unit_id
billing_month
base_fee
adjustment
previous_due
due_date
status
```

---

# 26. Expense Rules

Expense fields:

```text
id
building_id
category_id
title
description
amount
expense_date
payment_method
vendor
work_order_id optional
receipt_document_id optional
created_by
approved_by optional
status
```

Statuses:

```text
DRAFT
SUBMITTED
APPROVED
REJECTED
PAID
VOID
```

Approval workflow can be building-configurable.

---

# 27. Work Order Rules

Work order can represent:

- Generator repair
- Pump maintenance
- Lift maintenance
- Painting
- Plumbing
- Electrical repair
- Cleaning contract
- Security equipment
- Structural repair

Fields:

```text
title
description
priority
status
planned_start
due_date
completed_at
contractor_id
estimated_cost
actual_cost_reference
attachments
```

Priority:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

---

# 28. Announcement Rules

Audience examples:

```text
ALL
OWNERS
TENANTS
COMMITTEE
MANAGERS
SPECIFIC_UNITS
SPECIFIC_USERS
```

Announcement fields:

```text
title
body
priority
publish_at
expires_at
requires_acknowledgement
```

---

# 29. Meeting Management

Meeting:

```text
title
meeting_type
scheduled_at
location
agenda
participants
minutes
decisions
attachments
```

Decision:

```text
decision_text
owner
deadline
status
```

Meeting decisions can optionally create work orders.

---

# 30. Asset Management

Assets:

- Generator
- Water pump
- Lift
- CCTV
- Fire extinguisher
- Transformer
- Solar equipment

Fields:

```text
asset_code
name
category
purchase_date
warranty_expiry
supplier
status
maintenance_interval
last_service_date
next_service_date
```

---

# 31. Offline-First Architecture

Flutter local DB:

```text
Drift
```

Server remains source of truth.

Device stores:

- Cached building data
- Units
- Tenant records needed by role
- Invoices
- Payment history
- Announcements
- Work orders
- Pending offline mutations
- Sync cursor

---

# 32. Offline Mutation Queue

Every offline write creates:

```text
local_mutation
- mutation_id
- entity_type
- entity_id
- operation
- payload
- created_at
- retry_count
- sync_status
- idempotency_key
```

Statuses:

```text
PENDING
SYNCING
SYNCED
FAILED
CONFLICT
```

---

# 33. Offline Financial Restrictions

For financial safety:

Offline recording may be allowed for authorized managers, but:

- Generate local temporary receipt number.
- Mark as `PENDING_SYNC`.
- Prevent editing after sync submission.
- Backend idempotency key prevents duplicates.
- Final server receipt replaces provisional receipt reference.
- Never silently resolve amount conflicts.

---

# 34. Sync API

Example:

```text
POST /api/v1/sync/push
GET  /api/v1/sync/pull?cursor=<cursor>
```

Push:

```json
{
  "deviceId": "uuid",
  "mutations": [
    {
      "mutationId": "uuid",
      "idempotencyKey": "uuid",
      "entityType": "PAYMENT",
      "operation": "CREATE",
      "payload": {}
    }
  ]
}
```

Pull:

```json
{
  "cursor": "next-cursor",
  "changes": [],
  "deleted": []
}
```

---

# 35. Conflict Resolution

Use entity-specific policy.

Examples:

### Announcement
Last-write-wins may be acceptable.

### Tenant contact
Server version comparison + user conflict resolution.

### Lease
Reject conflicting concurrent mutation.

### Payment
Never auto-merge.

### Ownership
Reject conflict and require refresh.

Use optimistic locking:

```text
version
```

or ETag.

---

# 36. Flutter App Architecture

Recommended:

```text
lib/
├─ app/
│  ├─ routes/
│  ├─ bindings/
│  ├─ theme/
│  └─ app.dart
├─ core/
│  ├─ network/
│  ├─ database/
│  ├─ sync/
│  ├─ auth/
│  ├─ widgets/
│  ├─ utils/
│  └─ errors/
└─ features/
   ├─ authentication/
   ├─ dashboard/
   ├─ buildings/
   ├─ units/
   ├─ owners/
   ├─ tenants/
   ├─ leases/
   ├─ rent/
   ├─ maintenance_fee/
   ├─ payments/
   ├─ expenses/
   ├─ work_orders/
   ├─ contractors/
   ├─ announcements/
   ├─ meetings/
   ├─ assets/
   ├─ contacts/
   ├─ reports/
   └─ notifications/
```

Per feature:

```text
feature/
├─ presentation/
│  ├─ pages/
│  ├─ widgets/
│  ├─ controllers/
│  └─ bindings/
├─ domain/
│  ├─ entities/
│  ├─ repositories/
│  └─ usecases/
└─ data/
   ├─ models/
   ├─ datasources/
   ├─ repositories/
   └─ mappers/
```

---

# 37. Navigation Structure

Recommended bottom navigation by permission:

```text
Dashboard
Finance
Units
Work
More
```

Tenant:

```text
Home
Payments
Notices
Profile
```

Owner:

```text
Dashboard
Properties
Payments
Notices
More
```

Committee:

```text
Dashboard
Finance
Work
Notices
More
```

---

# 38. Screen Specification Conventions

Each screen below contains:

- Purpose
- Data
- Main actions
- Buttons
- Validation
- Permission
- Backend owner

---

# 39. Splash Screen

Purpose:
Initialize app.

Actions:
- Check auth token
- Initialize Drift
- Load cached user
- Attempt sync
- Resolve active building

UI:
- Logo
- Loading indicator

Navigation:
- Logged in with one building and no global action required -> Dashboard
- Logged in with multiple buildings, pending invitations, building applications, or cross-building ownership -> My Buildings / Portfolio
- Logged out -> Login

---

# 40. Login Screen

Fields:
- Phone
- Country code
- Google login

Buttons:
- `Continue with Phone`
- `Continue with Google`

Links:
- Terms
- Privacy

Backend:
Identity Service

---

# 41. OTP Verification Screen

Fields:
- 6-digit OTP

Buttons:
- `Verify`
- `Resend OTP`
- `Change Number`

Rules:
- Expiration
- Retry limits
- Rate limit

---

# 42. My Buildings / Building Selector Screen

Purpose:
Provide the user's global entry point across all buildings before entering one active building context.

A single user account may:
- belong to many buildings
- have different roles in each building
- own multiple units in one building
- own units across multiple buildings
- be a tenant in one building and an owner/admin in another

Data per building card:
- Building name
- Address
- Building lifecycle status
- User roles in that building
- Number of units owned by the user
- Pending alerts
- Subscription/access warning when relevant and user is authorized to see it

Sections:
- `My Buildings`
- `My Properties` — aggregated owned units across buildings
- `Pending Invitations`
- `Building Applications` — applications submitted by the user

Buttons:
- `Open Building`
- `Add Building`
- `Join Building` / `Accept Invitation`
- `Continue Onboarding` when the user administers a building still being configured
- `View Application` for pending building applications

Rules:
- Selecting a building establishes `activeBuildingId` and resolves that building's roles/permissions.
- Global portfolio screens must not require pretending that all ownership belongs to one active building.
- `OWNER` role alone is not proof of ownership; property cards are derived from active Ownership records.

Backend:
Identity + Building + Reporting

---

# 43. Main Dashboard Screen

Dynamic by role.

Common cards:
- Current dues
- Collected this month
- Outstanding amount
- Occupied/vacant
- Open maintenance works
- Upcoming meeting
- Latest announcement

Buttons:
- `Collect Rent`
- `Add Expense`
- `Create Work`
- `Post Announcement`
- `View Report`

Visibility depends on permission.

Backend:
Reporting Service

---

# 44. Owner Dashboard

Cards:
- Total units owned
- Monthly expected rent
- Rent collected
- Outstanding rent
- Maintenance due
- Occupancy

Sections:
- Unit performance
- Recent payment
- Tenant alerts
- Lease expiry

Buttons:
- `View Properties`
- `View Rent Ledger`
- `View Maintenance`
- `Download Statement`

Backend:
Reporting

---

# 45. Manager Dashboard

Cards:
- Due today
- Overdue
- Today's collections
- Pending sync payments

Sections:
- Collection queue
- Overdue tenants
- Lease expiry
- Vacant units

Buttons:
- `Collect Rent`
- `Add Tenant`
- `Create Lease`
- `Record Move-Out`
- `Sync Now`

---

# 46. Tenant Dashboard

Cards:
- Current rent due
- Maintenance if tenant-visible
- Next due date
- Advance balance

Sections:
- Latest receipts
- Notices
- Lease summary

Buttons:
- `View Payment History`
- `Download Receipt`
- `View Lease`
- `Contact Manager`

---

# 47. Committee Dashboard

Cards:
- Maintenance collection
- Outstanding maintenance
- Monthly expenses
- Cash/bank totals
- Work orders open
- Budget variance

Buttons:
- `Add Expense`
- `Generate Maintenance Fees`
- `Create Work Order`
- `Post Announcement`
- `Create Meeting`
- `Open Financial Report`

---

# 48. Unit List Screen

Filters:
- Type
- Floor
- Occupancy
- Owner
- Due status

Rows:
- Unit number
- Owner
- Tenant
- Occupancy
- Rent
- Maintenance due

Buttons:
- `+ Add Unit`
- Filter
- Search
- Sort

Tap row -> Unit Detail.

Backend:
Building + Reporting

---

# 49. Add/Edit Unit Screen

Fields:
- Unit number
- Floor
- Unit type
- Area
- Bedroom optional
- Default maintenance rate
- Notes

Buttons:
- `Save`
- `Save & Add Another`
- `Cancel`

Validation:
- Unique unit number inside building
- Positive area
- Valid unit type

Backend:
Building

---

# 50. Unit Detail Screen

Header:
- Flat 4B
- Occupied / Vacant
- Owner(s)

Tabs:
- Overview
- Ownership
- Tenant
- Lease
- Rent
- Maintenance
- Documents
- History

Buttons:
- `Assign Owner`
- `Add Tenant`
- `Create Lease`
- `Transfer Ownership`
- `Mark Vacant`
- More menu

Backend:
Building + Rental + Finance

---

# 51. Ownership Assignment Screen

Fields:
- Owner
- Share %
- Effective date
- Notes

Buttons:
- `Assign`
- `Add Co-owner`
- `Cancel`

Validation:
Total active ownership shares <= 100%.

Backend:
Building

---

# 52. Ownership Transfer Screen

Fields:
- Current owner
- New owner
- Effective date
- Share transferred
- Transfer reference
- Document

Buttons:
- `Transfer Ownership`
- `Cancel`

Confirmation modal required.

Backend:
Building

Kafka:
`ownership.transferred`

---

# 53. Owner List Screen

Data:
- Name
- Phone
- Number of units
- Outstanding maintenance
- Rent summary if permitted

Buttons:
- `+ Add Owner`
- Search
- Filter

---

# 54. Owner Detail Screen

Sections:
- Profile
- Owned units
- Rent income
- Maintenance liabilities
- Documents
- Activity

Buttons:
- `Edit`
- `Add Unit Ownership`
- `View Statement`
- `Send Notification`

---

# 55. Tenant List Screen

Filters:
- Active
- Notice
- Moved out
- Unit
- Rent overdue

Data:
- Tenant
- Flat
- Rent
- Due
- Status

Buttons:
- `+ Add Tenant`
- Search
- Filter
- Export

Backend:
Rental + Reporting

---

# 56. Add Tenant Screen

Fields:
- Full name
- Phone
- Email optional
- NID optional
- Emergency contact
- Permanent address
- Occupants count
- Photo optional
- Notes

Buttons:
- `Save Tenant`
- `Save & Create Lease`
- `Cancel`

Validation:
- Name required
- Phone format
- NID optional but validate if supplied

---

# 57. Tenant Detail Screen

Tabs:
- Profile
- Lease
- Rent
- Payments
- Documents
- History

Buttons:
- `Edit`
- `Create Lease`
- `Record Payment`
- `Give Notice`
- `Move Out`

---

# 58. Lease List Screen

Filters:
- Active
- Expiring
- Notice
- Expired
- Unit

Actions:
- Search
- Add lease
- Export

Row:
- Unit
- Tenant
- Start
- End
- Rent
- Status

---

# 59. Create Lease Screen

Fields:
- Unit
- Tenant
- Start date
- End date
- Monthly rent
- Security deposit
- Advance rent
- Due day
- Late fee policy
- Parking charge
- Other recurring charges
- Agreement document

Buttons:
- `Save Draft`
- `Save & Activate`
- `Cancel`

Backend:
Rental

Activation may trigger finance events.

---

# 60. Lease Detail Screen

Sections:
- Lease summary
- Tenant
- Unit
- Rent schedule
- Deposit
- Advance
- Documents
- Invoice history

Buttons:
- `Activate`
- `Edit Draft`
- `Give Notice`
- `Terminate`
- `Renew`
- `Download Agreement`

---

# 61. Rent Overview Screen

Cards:
- Expected this month
- Collected
- Outstanding
- Advance balance total

Tabs:
- Due
- Paid
- Partial
- Overdue

Filters:
- Month
- Owner
- Unit
- Tenant

Buttons:
- `Collect Rent`
- `Generate Invoices`
- `Export`

Backend:
Reporting + Rental + Finance

---

# 62. Rent Invoice Detail Screen

Displays:
- Invoice no
- Billing period
- Tenant
- Unit
- Charges
- Discounts
- Paid
- Outstanding
- Allocations

Buttons:
- `Record Payment`
- `Add Adjustment`
- `Download Statement`
- `Send Reminder`

---

# 63. Collect Rent Screen

Fields:
- Tenant
- Unit
- Selected invoices
- Amount received
- Payment date
- Method
- Reference
- Notes
- Receipt attachment optional

Allocation UI:
- Auto allocate oldest first
- Manual allocation toggle

Buttons:
- `Save Payment`
- `Save & Share Receipt`
- `Cancel`

Confirmation summary:
- Received
- Allocated
- Advance created
- Remaining due

Backend:
Finance

Idempotency mandatory.

---

# 64. Payment Detail Screen

Displays:
- Receipt number
- Payer
- Amount
- Method
- Time
- Collector
- Invoice allocations
- Sync status
- Audit reference

Buttons:
- `Download Receipt`
- `Share Receipt`
- `Reverse Payment` permission controlled

No delete.

---

# 65. Payment Reversal Screen

Fields:
- Reason
- Notes

Displays:
- Original payment
- Impact on invoices
- Advance effect

Buttons:
- `Confirm Reversal`
- `Cancel`

Require confirmation and permission.

---

# 66. Maintenance Fee Dashboard

Cards:
- Billed
- Collected
- Outstanding
- Collection %

Tabs:
- Due
- Paid
- Overdue

Buttons:
- `Generate Monthly Fees`
- `Record Payment`
- `Export Report`

---

# 67. Generate Maintenance Fees Screen

Fields:
- Billing month
- Due date
- Unit scope
- Rate policy
- Exclusions

Preview:
- Unit
- Amount

Buttons:
- `Preview`
- `Generate`
- `Cancel`

Prevent duplicate generation for same billing period unless explicit adjustment mode.

---

# 68. Maintenance Payment Screen

Fields:
- Owner
- Unit(s)
- Invoice(s)
- Amount
- Payment method
- Reference
- Date

Buttons:
- `Save Payment`
- `Save & Receipt`

Backend:
Finance

---

# 69. Expense List Screen

Filters:
- Date
- Category
- Status
- Vendor
- Work order

Cards:
- This month
- Approved
- Pending

Buttons:
- `+ Add Expense`
- `Export`
- Search

---

# 70. Add Expense Screen

Fields:
- Title
- Category
- Amount
- Date
- Vendor
- Method
- Work order optional
- Description
- Receipt photo/document

Buttons:
- `Save Draft`
- `Submit`
- `Cancel`

Backend:
Finance + Document

---

# 71. Expense Detail Screen

Displays:
- Amount
- Status
- Vendor
- Created by
- Approved by
- Receipt
- Related work

Buttons:
- `Edit` when allowed
- `Approve`
- `Reject`
- `Mark Paid`
- `Void`

All status transitions audited.

---

# 72. Work Order List Screen

Filters:
- Status
- Priority
- Contractor
- Due date

Views:
- List
- Kanban optional
- Calendar optional

Buttons:
- `+ Create Work`
- Filter
- Search

---

# 73. Create Work Order Screen

Fields:
- Title
- Description
- Category
- Priority
- Planned start
- Due date
- Contractor
- Estimated cost
- Attachments

Buttons:
- `Save Draft`
- `Assign`
- `Cancel`

---

# 74. Work Order Detail Screen

Shows:
- Status timeline
- Description
- Contractor
- Deadlines
- Cost
- Attachments
- Comments

Buttons:
- `Assign`
- `Start Work`
- `Mark Blocked`
- `Complete`
- `Add Expense`
- `Add Attachment`
- `Add Comment`

---

# 75. Contractor List Screen

Rows:
- Name
- Trade
- Phone
- Active works
- Total paid

Buttons:
- `+ Add Contractor`
- Search
- Filter

---

# 76. Contractor Detail Screen

Displays:
- Contacts
- Trade
- Current works
- Past works
- Payments
- Documents

Buttons:
- `Edit`
- `Create Work`
- `Call`
- `Share Contact`

---

# 77. Announcement List Screen

Tabs:
- Active
- Scheduled
- Expired

Cards:
- Title
- Audience
- Time
- Priority
- Read %

Buttons:
- `+ New Announcement`
- Filter

---

# 78. Create Announcement Screen

Fields:
- Title
- Message
- Audience
- Specific units/users
- Priority
- Publish now/schedule
- Expiry
- Require acknowledgement
- Attachment

Buttons:
- `Save Draft`
- `Preview`
- `Publish`

Backend:
Communication

---

# 79. Announcement Detail Screen

Displays:
- Content
- Attachments
- Audience
- Published by
- Read/acknowledgement state

Buttons:
- `Acknowledge`
- `Edit` if allowed
- `Expire Now`
- `Share`

---

# 80. Meetings List Screen

Rows:
- Meeting title
- Date
- Type
- Status
- Decisions count

Buttons:
- `+ Create Meeting`
- Calendar
- Filter

---

# 81. Create Meeting Screen

Fields:
- Title
- Type
- Date/time
- Venue
- Participants
- Agenda items
- Attachments

Buttons:
- `Save`
- `Publish Invitation`
- `Cancel`

---

# 82. Meeting Detail Screen

Tabs:
- Overview
- Agenda
- Participants
- Minutes
- Decisions
- Files

Buttons:
- `Start Meeting`
- `Add Decision`
- `Add Minutes`
- `Create Work from Decision`
- `Close Meeting`

---

# 83. Asset List Screen

Fields shown:
- Asset code
- Name
- Category
- Status
- Next service

Buttons:
- `+ Add Asset`
- Filter
- Search

---

# 84. Asset Detail Screen

Displays:
- Asset metadata
- Warranty
- Supplier
- Work history
- Next service

Buttons:
- `Edit`
- `Create Maintenance Work`
- `Add Document`

---

# 85. Notifications Screen

Tabs:
- All
- Finance
- Building
- Work
- Announcement

Actions:
- Mark read
- Mark all read
- Open destination

---

# 86. Reports Screen

Report cards:

- Rent collection
- Outstanding rent
- Owner statement
- Tenant payment ledger
- Maintenance collection
- Expense report
- Income vs expense
- Work cost
- Unit occupancy
- Lease expiry
- Advance rent ledger
- Audit export

Filters:
- Date range
- Building
- Unit
- Owner
- Tenant

Buttons:
- `View`
- `Export PDF`
- `Export Excel`
- `Share`

Backend:
Reporting

---

# 87. Contacts Screen

Categories:
- Committee
- Owners
- Tenants
- Contractors
- Emergency

Buttons:
- `Call`
- `Message`
- `View Profile`

Respect data-visibility policy.

---

# 88. Calendar Screen

Events:
- Rent due
- Maintenance due
- Meetings
- Lease expiry
- Work deadlines
- Asset service

Views:
- Month
- Week
- Agenda

Tap item -> domain detail.

---

# 89. Settings Screen

Sections:

### Building
- Building details
- Currency
- Timezone
- Rent defaults
- Maintenance rules
- Financial year

### Access
- Members
- Roles
- Permissions

### Notification
- Reminder timing
- Channels

### Finance
- Payment methods
- Expense categories
- Receipt numbering
- Approval rules

### Sync
- Last sync
- Device info
- Retry failed items

---

# 90. Audit Log Screen

Restricted role.

Filters:
- User
- Action
- Entity
- Date
- Module

Shows:
- Actor
- Action
- Entity
- Old summary
- New summary
- Timestamp
- Trace ID

Financial audit records cannot be deleted from UI.

---

# 91. Backend Endpoint Ownership Mapping

| UI Feature | Backend |
|---|---|
| Login | Identity |
| Building selector | Identity + Building |
| Unit | Building |
| Ownership | Building |
| Tenant | Rental |
| Lease | Rental |
| Rent invoices | Rental |
| Payments | Finance |
| Maintenance invoices | Finance |
| Expense | Finance |
| Work order | Work |
| Contractor | Work |
| Announcement | Communication |
| Meeting | Communication |
| Notification | Notification |
| Files | Document |
| Dashboards | Reporting |
| Audit | Audit |
| Offline sync | Sync |

---

# 92. Key API Examples

## Record Payment

```http
POST /api/v1/payments
Idempotency-Key: 4ea7...
Authorization: Bearer ...
X-Building-Id: ...
```

```json
{
  "payerId": "uuid",
  "unitId": "uuid",
  "paymentType": "RENT",
  "amount": 30000,
  "currency": "BDT",
  "paymentMethod": "CASH",
  "paidAt": "2026-09-21T10:30:00+06:00",
  "allocations": [
    {
      "invoiceId": "jan-invoice",
      "amount": 20000
    },
    {
      "invoiceId": "feb-invoice",
      "amount": 10000
    }
  ],
  "note": "Collected by manager"
}
```

---

# 93. Security Requirements

- JWT/OAuth2-compatible auth
- Short-lived access token
- Refresh token rotation
- Role and permission authorization
- Building context validation
- Sensitive document access through signed URL
- Encryption in transit
- Secrets through secret manager/env
- No raw credentials in Git
- Rate limiting
- Login brute-force controls
- Audit privileged operations
- Financial mutation idempotency
- Server-side authorization for every request

Never rely on Flutter UI permissions for security.

---

# 94. NID and Personal Data

NID is optional.

If used:
- Restrict role access
- Encrypt sensitive values where appropriate
- Do not include NID in logs
- Mask on UI by default
- Audit access to sensitive documents where practical

---

# 95. Observability

Every service must support:

- Structured JSON logging
- `traceId`
- `correlationId`
- Metrics
- Health endpoints
- Distributed tracing
- Kafka consumer lag monitoring
- Dead-letter queue visibility

Recommended:
- OpenTelemetry
- Prometheus
- Grafana
- Loki/ELK
- Zipkin/Tempo

---

# 96. Kafka Failure Handling

Consumers:

1. Retry transient errors.
2. Use bounded retry policy.
3. Send unrecoverable event to DLQ.
4. Record reason.
5. Alert operations for financial-event DLQ.

Example:

```text
finance.events.dlq
```

---

# 97. Testing Strategy

## Backend

- Domain unit tests
- Use case tests
- Repository adapter integration tests
- Controller tests
- Kafka producer tests
- Kafka consumer tests
- Testcontainers
- Contract tests
- Security tests
- Idempotency tests
- Migration tests

## Flutter

- Unit tests
- Repository tests
- Controller tests
- Widget tests
- Golden tests for key receipts/reports
- Integration flows
- Offline-sync tests

---

# 98. Contract Testing

Use OpenAPI as API contract.

Recommended:
- Generate/update OpenAPI from backend.
- Maintain typed Dart API clients.
- Add CI breaking-change validation.

Kafka event schemas should also be version-controlled.

---

# 99. Database Migration

Use:

```text
Flyway
```

Each microservice owns its migrations.

No manual production schema changes.

---

# 100. Suggested Monorepo Structure

```text
buildingos/
├─ apps/
│  ├─ buildingos_flutter/
│  └─ buildingos_backoffice_web/
├─ backend/
│  ├─ api-gateway/
│  ├─ identity-service/
│  ├─ building-service/
│  ├─ rental-service/
│  ├─ finance-service/
│  ├─ work-service/
│  ├─ communication-service/
│  ├─ notification-service/
│  ├─ document-service/
│  ├─ reporting-service/
│  ├─ audit-service/
│  ├─ sync-service/
│  └─ subscription-service/
├─ contracts/
│  ├─ openapi/
│  └─ kafka/
├─ infra/
│  ├─ docker/
│  ├─ kubernetes/
│  └─ terraform/
├─ docs/
│  ├─ brd/
│  ├─ architecture/
│  ├─ adr/
│  └─ agent/
└─ scripts/
```

---

# 101. Service Build Standard

Each service should include:

```text
Dockerfile
README.md
openapi.yaml/generated docs
Flyway migrations
application.yml
application-local.yml
application-test.yml
health checks
metrics
testcontainers setup
```

---

# 102. CI/CD Requirements

On pull request:

1. Format/lint
2. Unit tests
3. Integration tests
4. Build
5. OpenAPI check
6. Migration validation
7. Docker image build
8. Security scan

On merge:
- Version
- Build image
- Push registry
- Deploy environment
- Smoke test

---

# 103. Environment Strategy

```text
local
dev
staging
production
```

Local Docker Compose should start:

- Kafka
- Kafka UI
- PostgreSQL databases
- Redis if used
- Object-storage emulator if needed
- API Gateway
- Core services

---

# 104. Redis Usage

Optional.

Good use cases:
- Rate limit
- Short cache
- Distributed lock when strictly needed
- OTP state
- Read-heavy reference data

Do not use Redis as system of record.

---

# 105. API Gateway Routing Example

```text
/api/v1/auth/**            -> identity-service
/api/v1/buildings/**       -> building-service
/api/v1/units/**           -> building-service
/api/v1/tenants/**         -> rental-service
/api/v1/leases/**          -> rental-service
/api/v1/rent-invoices/**   -> rental-service
/api/v1/payments/**        -> finance-service
/api/v1/expenses/**        -> finance-service
/api/v1/work-orders/**     -> work-service
/api/v1/contractors/**     -> work-service
/api/v1/announcements/**   -> communication-service
/api/v1/meetings/**        -> communication-service
/api/v1/notifications/**   -> notification-service
/api/v1/documents/**       -> document-service
/api/v1/reports/**         -> reporting-service
/api/v1/audit/**           -> audit-service
/api/v1/sync/**            -> sync-service
```

---

# 106. Service-to-Service Communication

Prefer:
- Kafka for domain propagation
- REST only when caller needs immediate response

Examples:

Rental Service should not synchronously call Finance for every dashboard query.

Instead:
- Rental emits event.
- Reporting consumes event.
- Dashboard reads projection.

---

# 107. Anti-Corruption Layer

When one service consumes another domain's data, translate into local model.

Do not leak JPA/entity classes across service boundaries.

Use:
- API DTO
- Kafka event DTO
- Adapter
- Mapper

---

# 108. Receipt Numbering

Receipt number must be server-generated.

Example:

```text
BLD01-RNT-202609-000123
```

Components:
- Building code
- Payment category
- Year/month
- Sequence

Offline receipt:
```text
TEMP-<device>-<sequence>
```

After sync:
- Map temp receipt to official receipt.

---

# 109. Bangladesh Localization

Defaults:
- Currency: BDT
- Timezone: Asia/Dhaka
- Date format configurable
- English/Bangla localization-ready
- Mobile numbers support `+880`

Do not encode only one language into domain records.

UI strings must use localization keys.

---

# 110. SaaS Multi-Tenancy

BuildingOS is a multi-tenant SaaS platform. The primary operational tenant boundary is:

```text
building_id
```

All building-domain business data must be scoped by building.

At minimum:
- application-level tenant enforcement
- indexes include `building_id` where relevant
- authorization validates membership and active building context
- platform/back-office APIs use separate platform permissions rather than ordinary building membership
- cross-building queries are exposed only through explicit portfolio/reporting use cases

## 110.1 Global User Identity

A `User` is global to BuildingOS and must not be duplicated per building.

Example:

```text
User Rahim
├─ Building A -> OWNER + COMMITTEE
│  ├─ Flat 4A -> 100% ownership
│  └─ Flat 4B -> 50% ownership
├─ Building B -> OWNER
│  └─ Flat 8C -> 100% ownership
├─ Building C -> BUILDING_ADMIN
└─ Building D -> TENANT
```

Membership, ownership, and tenancy are separate relationships.

## 110.2 Building as SaaS Customer Account

By default each approved building is an independently managed SaaS customer/account with its own:

- lifecycle state
- Building Admins
- configuration
- data boundary
- subscription
- entitlements
- usage limits
- audit trail

Building lifecycle:

```text
APPLICATION_DRAFT
SUBMITTED
UNDER_REVIEW
MORE_INFORMATION_REQUIRED
APPROVED
ONBOARDING
ACTIVE
SUSPENDED
ARCHIVED
REJECTED
```

A submitted application must not automatically become an active production building.

## 110.3 Building-Level Subscription

Default model:

```text
Building A -> Subscription A
Building B -> Subscription B
Building C -> Subscription C
```

A user who owns flats in several buildings does not receive one combined subscription automatically.

Subscription controls entitlements such as enabled modules, unit limits, storage, reporting capability, or support tier. It must not be used as a substitute for authorization.

## 110.4 Future Organization Support

Property-management companies or enterprise customers may later group multiple buildings:

```text
organization
  -> buildings
```

This must not require changing the global User, BuildingMembership, Unit, Ownership, or BuildingSubscription model.

---

# 111. Dashboard Read Model Examples

Owner dashboard projection:

```text
owner_dashboard_view
- building_id
- owner_id
- unit_count
- occupied_count
- expected_rent_month
- collected_rent_month
- outstanding_rent
- maintenance_due
- updated_at
```

Cross-building owner portfolio projection:

```text
owner_portfolio_view
- owner_user_id
- building_id
- unit_id
- ownership_share
- occupancy_status
- expected_rent_month
- collected_rent_month
- outstanding_rent
- maintenance_due
- updated_at
```

This projection powers `My Properties` without weakening building-level authorization. The user may only see properties for which active ownership grants access.

Committee finance projection:

```text
committee_finance_view
- building_id
- month
- maintenance_billed
- maintenance_collected
- expense_total
- outstanding_total
```

---

# 112. Scheduled Jobs

Required scheduled functions:

- Monthly rent invoice generation
- Monthly maintenance invoice generation
- Rent overdue detection
- Maintenance overdue detection
- Lease expiry reminders
- Work overdue detection
- Asset maintenance reminder
- Notification scheduling

For horizontally scaled services, ensure jobs are single-execution or safely idempotent.

---

# 113. Important Financial Invariants

1. Payment amount > 0.
2. Allocations cannot exceed payment amount.
3. Invoice allocated total cannot exceed payable amount unless overpayment explicitly moves to advance.
4. Reversed payment cannot be allocated.
5. Paid invoice cannot become unpaid without reversal/adjustment event.
6. Financial records are append-oriented.
7. Delete operation for finalized payment is forbidden.
8. Receipt numbers are unique.
9. Currency must be explicit.
10. Every financial mutation has actor + timestamp.

---

# 114. Audit Requirements

Audit these actions:

- Role changes
- Building configuration changes
- Ownership transfer
- Lease activation
- Lease termination
- Payment creation
- Payment reversal
- Expense approval
- Maintenance fee generation
- Report export optionally
- Sensitive document access optionally
- Work completion
- Meeting decision edits

---

# 115. Notification Rules

Examples:

### Rent Reminder
- 3 days before due
- due date
- 3 days overdue
- configurable

### Maintenance Reminder
- configurable

### Lease Expiry
- 60 days
- 30 days
- 7 days

### Work Deadline
- 3 days before
- overdue

Users can configure non-critical notifications.

Emergency announcement bypasses ordinary preference except platform limitations.

---

# 116. Report Definitions

## Rent Collection Report
Columns:
- Unit
- Tenant
- Expected
- Paid
- Outstanding
- Last payment
- Status

## Owner Statement
- Unit
- Rent billed
- Rent received
- Maintenance due
- Adjustments

## Expense Report
- Date
- Category
- Vendor
- Description
- Amount
- Work order
- Status

## Maintenance Report
- Unit
- Billing month
- Billed
- Collected
- Due

---

# 117. Search Requirements

Global/feature search by:

- Unit number
- Owner name
- Tenant name
- Phone
- Receipt number
- Invoice number
- Work title
- Contractor

Large lists must use pagination.

---

# 118. Pagination Standard

Request:

```text
?page=0&size=20&sort=createdAt,desc
```

Response meta:

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

Cursor pagination may be used for event feeds or sync.

---

# 119. Soft Delete Policy

Use selectively.

Good for:
- Contacts
- Draft records
- Non-financial configuration

Do not soft-delete finalized financial records as a substitute for reversal.

Historical entities should normally transition status.

---

# 120. Agentic Development Principles

The coding agent must:

1. Work feature-by-feature.
2. Read domain rules before coding.
3. Never bypass service boundaries.
4. Never directly query another service DB.
5. Add tests in same change.
6. Add migration when schema changes.
7. Update OpenAPI when endpoint changes.
8. Update Kafka schema when event changes.
9. Preserve backward compatibility.
10. Add observability for important flow.
11. Avoid TODO-only implementations.
12. Run tests before task completion.

---

# 121. Agent Work Unit Template

Every agent task should define:

```text
Feature:
Service:
Business goal:
Actors:
Preconditions:
Domain rules:
API:
DB changes:
Kafka produced:
Kafka consumed:
Permissions:
Validation:
Offline behavior:
Tests:
Acceptance criteria:
Out of scope:
```

---

# 122. Agent Definition of Done

A backend feature is done only when:

- Domain model implemented
- Use case implemented
- Repository port added
- Repository adapter implemented
- DB migration added
- REST endpoint implemented if needed
- Validation implemented
- Permission check implemented
- Kafka event implemented if needed
- Outbox used for event publication
- Unit tests pass
- Integration tests pass
- API contract updated
- Error handling implemented
- Logging/trace supported
- Documentation updated

A Flutter feature is done only when:

- Domain entity
- Repository abstraction
- Data models
- Remote data source
- Drift cache/offline model when needed
- Use case
- GetX controller
- Screen
- Loading state
- Empty state
- Error state
- Permission state
- Offline state
- Tests
- Navigation

---

# 123. Recommended Development Sequence

## Phase 0 – Platform Foundation

- Monorepo
- Shared conventions
- Docker Compose
- Kafka
- PostgreSQL
- Gateway
- Identity skeleton
- Observability
- CI/CD
- OpenAPI conventions

## Phase 1 – Identity + Building Core

- Auth
- Global user identity
- Platform roles
- Building memberships and building roles
- Building applications
- Back-office application review/approval
- Building lifecycle
- Initial Building Admin assignment
- Building onboarding progress
- Units
- Ownership, including cross-building ownership
- Invitations

## Phase 1B – SaaS Operations + Assisted Onboarding

- Back-office web application shell
- Platform building dashboard
- Assisted onboarding assignment/session
- Scoped support access
- Subscription plans
- Building subscriptions
- Entitlements
- Trial/suspension/reactivation lifecycle
- Audit for all platform-admin actions

## Phase 2 – Rental Core

- Tenant
- Lease
- Rent schedule
- Rent invoice

## Phase 3 – Finance Core

- Payment
- Allocation
- Receipt
- Maintenance fees
- Expenses
- Advance ledger

## Phase 4 – Offline Sync

- Drift
- Mutation queue
- Sync service
- Idempotency
- Conflict handling

## Phase 5 – Operations

- Work orders
- Contractors
- Assets

## Phase 6 – Communication

- Announcements
- Meetings
- Notifications

## Phase 7 – Reporting

- Kafka projections
- Dashboards
- PDF
- Excel

## Phase 8 – Hardening

- Security
- Load testing
- Audit
- Failure recovery
- DLQ
- Permission matrix testing

---

# 124. Suggested MVP

MVP must include:

1. Login and global user identity
2. My Buildings / active-building selection
3. Building application submission
4. Back-office building review, approval/rejection, and activation
5. Initial Building Admin assignment
6. Guided building onboarding
7. Assisted onboarding assignment with scoped/audited access
8. Basic subscription plan, status, and entitlement management
9. Building
10. Unit
11. Ownership, including multiple units across multiple buildings
12. Cross-building owner `My Properties` portfolio
13. Tenant
14. Lease
15. Rent invoice
16. Rent payment
17. Receipt
18. Maintenance invoice
19. Maintenance payment
20. Expense
21. Owner dashboard
22. Manager dashboard
23. Tenant dashboard
24. Committee dashboard
25. Announcement
26. Offline-safe payment entry
27. Basic reports
28. Audit for financial and platform-administration operations

---

# 125. MVP Exclusions

Can defer:

- Automated subscription charging / payment-provider integration
- Organization-level consolidated billing
- Promotional campaign management
- Advanced support ticketing/CRM
- Chat
- Full accounting double-entry ledger
- Direct payment gateway
- Visitor access
- OCR
- Advanced asset maintenance
- Complex workflow engine
- AI assistant

---

# 126. Example End-to-End Feature: Collect Rent

## Frontend

Manager opens:

```text
Manager Dashboard
 -> Collect Rent
```

Steps:

1. Select tenant/unit.
2. App loads open invoices.
3. Enter amount.
4. Select method.
5. App proposes allocation.
6. User confirms.
7. If online, API request.
8. If offline, mutation queued.
9. Receipt shown.

## API Gateway

- Validate JWT.
- Validate building context.
- Route Finance Service.
- Add correlation ID.

## Finance Service

`RecordPaymentController`
-> `RecordPaymentUseCase`
-> validate permission
-> validate invoices/allocation
-> create payment
-> create allocation
-> create advance if needed
-> create receipt metadata
-> save outbox event
-> commit

## Kafka

Publishes:

```text
payment.recorded
payment.allocated
receipt.generated
```

## Consumers

Rental:
- Updates invoice settlement state if payment projection is owned there, or consumes allocation status.

Reporting:
- Updates dashboard.

Notification:
- Sends payment receipt notification.

Audit:
- Appends audit record.

## Offline

Same use case request carries unique idempotency key after reconnect.

---

# 127. Example End-to-End Feature: Transfer Ownership

Frontend:
Unit Detail -> Transfer Ownership.

Building Service:

1. Validate current ownership.
2. Close existing ownership record.
3. Create new ownership.
4. Store transfer metadata.
5. Emit `ownership.transferred`.

Rental consumes:
- No active lease ownership mutation is automatic.
- New owner receives access according to effective date.

Reporting consumes:
- Rebuild owner/unit projection.

Identity:
- Membership/owner role may be adjusted by explicit workflow.

---

# 128. Error Codes

Examples:

```text
AUTH_REQUIRED
ACCESS_DENIED
BUILDING_CONTEXT_REQUIRED
RESOURCE_NOT_FOUND
VALIDATION_ERROR
UNIT_ALREADY_EXISTS
ACTIVE_LEASE_EXISTS
INVALID_OWNERSHIP_SHARE
INVOICE_ALREADY_PAID
PAYMENT_ALLOCATION_EXCEEDS_AMOUNT
PAYMENT_ALREADY_REVERSED
DUPLICATE_IDEMPOTENCY_KEY
SYNC_CONFLICT
VERSION_CONFLICT
FILE_UPLOAD_FAILED
```

Clients must branch on code, not parse message strings.

---

# 129. Concurrency Control

Use optimistic locking on high-conflict entities:

- Lease
- Invoice
- Payment status
- Ownership
- Work order

Example JPA:

```text
@Version
Long version;
```

Return HTTP 409 on version conflict.

---

# 130. API HTTP Status Guidance

```text
200 OK
201 Created
202 Accepted
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests
500 Internal Server Error
503 Service Unavailable
```

---

# 131. File Upload Workflow

Preferred:

1. Client requests upload intent.
2. Document Service returns signed upload URL.
3. Client uploads file directly to object storage.
4. Client confirms metadata.
5. Business service stores `documentId`.

Do not route large file bytes through unrelated domain services.

---

# 132. PDF Receipt Requirements

Receipt includes:

- Building name
- Receipt no
- Tenant/owner
- Unit
- Payment category
- Amount
- Payment method
- Paid date
- Allocation breakdown
- Collector
- Generated timestamp
- Verification code/QR optional

Server receipt data is canonical.

---

# 133. Excel Export Requirements

Excel must preserve:

- Numbers as numeric values
- Date columns as dates
- Currency formatting
- Header rows
- Filterable table
- Report metadata

Large export can be asynchronous later.

For MVP, synchronous export is acceptable under safe row limits.

---

# 134. Performance Targets

MVP target under normal load:

- Typical API P95 < 500 ms excluding file/report generation.
- Dashboard P95 < 1 s using read models.
- Mobile list first cached render immediate/near-immediate.
- Sync handles interrupted retries safely.
- No duplicate financial transaction after retry.

---

# 135. Backup & Recovery

Each DB:
- Automated backups
- Point-in-time recovery where available
- Restore drills
- Retention policy

Object storage:
- Versioning where practical

Kafka:
- Appropriate retention
- Replay policy

---

# 136. Data Retention

Recommended:
- Financial records retained long-term.
- Audit records retained long-term.
- Session/security records use shorter configurable retention.
- Deleted personal data must consider legal/business obligations.

Retention rules must be configurable before production scale.

---

# 137. Feature Permission Examples

```text
building.read
building.update
unit.read
unit.create
unit.update
ownership.assign
ownership.transfer

tenant.read
tenant.create
tenant.update
lease.create
lease.activate
lease.terminate

rent.read
rent.invoice.generate
payment.create
payment.reverse

maintenance.read
maintenance.invoice.generate

expense.read
expense.create
expense.approve

work.read
work.create
work.assign
work.complete

announcement.read
announcement.create

meeting.read
meeting.create

report.read
report.export

audit.read
```

---

# 138. Example Permission Mapping

## OWNER
```text
unit.read: owned units
tenant.read: owned units
lease.read: owned units
rent.read: owned units
maintenance.read: owned units
report.read: owner reports
announcement.read
```

## TENANT
```text
tenant.read: self
lease.read: own active lease
rent.read: own invoices
payment.read: own payments
announcement.read: targeted
```

## MANAGER
```text
tenant.*
lease.*
rent.*
payment.create
payment.read
```

Explicitly limit `payment.reverse`.

---

# 139. Agentic Backend Task Example

```text
TASK: Implement Record Rent Payment

Service:
finance-service

Feature:
payment

Use case:
RecordPaymentUseCase

Requirements:
- Accept RENT payment.
- Validate amount > 0.
- Validate payer/unit/building context.
- Validate target invoices.
- Support multiple invoice allocations.
- Support partial allocation.
- Excess becomes advance.
- Require idempotency key.
- Generate official receipt number.
- Save Payment, PaymentAllocation, AdvanceLedger if needed.
- Write payment.recorded and payment.allocated to outbox.
- Do not publish Kafka directly inside controller.
- No delete endpoint.
- Add optimistic locking where needed.

API:
POST /api/v1/payments

Tests:
- exact payment
- partial payment
- multi-invoice payment
- overpayment to advance
- duplicate idempotency key
- invalid allocation
- unauthorized user
- concurrent invoice modification

Acceptance:
All tests green and OpenAPI updated.
```

---

# 140. Agentic Flutter Task Example

```text
TASK: Implement Collect Rent Screen

Feature:
payments

Screen:
CollectRentPage

Requirements:
- Search/select tenant or unit.
- Load unpaid invoices.
- Enter amount.
- Payment method.
- Optional reference/note.
- Auto-allocation oldest invoice first.
- Allow manual allocation.
- Show remaining/advance preview.
- If online, submit.
- If offline, store pending mutation.
- Display provisional receipt for offline.
- Display server receipt after sync.
- Prevent double submission.
- Show loading/error/success states.
- Permission guard.

Tests:
- form validation
- partial payment UI
- offline queue
- duplicate tap prevention
- allocation validation
```

---

# 141. ADRs Required

Create Architecture Decision Records for:

1. Microservices boundaries
2. Database-per-service
3. Kafka + Outbox
4. API Gateway
5. Offline sync strategy
6. Payment allocation model
7. Reporting projections
8. Authentication model
9. File storage approach
10. Multi-tenancy/building context

---

# 142. Non-Functional Requirements

## Reliability
Financial operations must be retry-safe.

## Security
Strict tenant/building isolation.

## Availability
Non-financial read functions should degrade gracefully if optional services fail.

## Scalability
Services independently scalable.

## Maintainability
Feature-first Clean Architecture.

## Auditability
Critical changes traceable.

## Offline
Core collection workflow functional without continuous internet.

## Localization
Bangla/English-ready.

---

# 143. Business Acceptance Scenarios

## Scenario A – Owner Has 3 Flats

Owner dashboard shows:

- 3 units
- Occupancy per unit
- Rent expected
- Rent received
- Outstanding
- Maintenance due

Owner cannot see unrelated owners' private rental details.

---

## Scenario B – Partial Rent

Rent = 25,000  
Tenant pays = 15,000

System:
- records 15,000 payment
- allocates to invoice
- invoice -> PARTIALLY_PAID
- due -> 10,000
- receipt -> 15,000

---

## Scenario C – Advance Rent

Rent = 20,000  
Tenant pays = 50,000

System:
- allocates 20,000
- invoice -> PAID
- advance ledger -> 30,000
- receipt -> 50,000

---

## Scenario D – Offline Manager

Manager receives cash while offline.

System:
- creates pending local payment
- generates provisional receipt
- stores idempotency key
- syncs later
- backend processes once
- official receipt returned
- local record reconciled

---

## Scenario E – Ownership Transfer

Flat changes owner.

System:
- closes old ownership period
- creates new period
- history preserved
- dashboard projections update
- existing tenant lease remains unless explicitly changed

---

## Scenario F – Building Application and Approval

A new customer submits a building application.

System:
- creates a non-active `BuildingApplication`
- back-office operator reviews the application
- operator may request additional information, reject, or approve
- approval creates/links the Building record and assigns the initial Building Admin
- building enters `ONBOARDING`, not immediately `ACTIVE`
- every review decision is audited

---

## Scenario G – Assisted Onboarding

A Building Admin requests help configuring 60 units.

System:
- creates an assisted onboarding session
- platform operator assigns an `ONBOARDING_AGENT`
- agent receives temporary scoped access only to that building/setup scope
- agent bulk-creates floors/units and assists with invitations
- customer can review progress
- session expires or is explicitly completed
- all agent actions are recorded in the audit trail

---

## Scenario H – One Owner, Multiple Buildings

One user owns two flats in Building A and one flat in Building B.

System:
- maintains one global User account
- stores three independent Ownership records
- user sees both buildings in `My Buildings`
- `My Properties` aggregates the three owned units
- selecting Building A shows only Building A context/permissions
- Building B subscription and Building A subscription remain independent

---

## Scenario I – Subscription Suspension

A building subscription is suspended by an authorized platform operator.

System:
- records the subscription state transition and reason
- recalculates building entitlements/access policy
- does not delete building data
- does not alter ownership, leases, or payment history
- exposes a clear restricted-access state to authorized users
- reactivation restores permitted features according to the current plan
- all changes are audited

---

# 144. Critical Anti-Patterns to Avoid

Do not:

- Build one shared database for all microservices.
- Let controllers access JPA repositories directly.
- Share JPA entities across services.
- Put business logic in API Gateway.
- Let Flutter decide authorization.
- Delete finalized financial records.
- Calculate outstanding purely from payment sum without allocation.
- Publish Kafka before DB transaction is durable.
- Use Kafka consumer logic without idempotency.
- Auto-resolve financial sync conflicts.
- Use generic global `controller/service/repository` package structure.
- Couple reporting screens to many live cross-service joins.
- Duplicate the same human User record per building.
- Treat the `OWNER` role as the ownership record.
- Auto-activate a self-registered building without back-office approval when approval is required by policy.
- Give onboarding/support agents permanent or unrestricted building access.
- Put subscription state directly into rental/finance records.
- Use subscription entitlements as a replacement for building authorization.

---

# 145. Final Recommended Service Set for MVP

Required:

```text
api-gateway
identity-service
building-service
rental-service
finance-service
work-service
communication-service
notification-service
document-service
reporting-service
audit-service
sync-service
subscription-service
```

If deployment overhead is initially too high, `work-service` and `communication-service` can still remain separate modules but be deployed together temporarily. Domain boundaries must remain intact so they can split later without code redesign.

---

# 146. MVP Success Criteria

BuildingOS MVP is successful when the SaaS platform can:

1. Maintain one global user identity across many buildings.
2. Accept a new building application without immediately activating the building.
3. Let authorized back-office staff review, request information, approve/reject, activate, suspend, and inspect building lifecycle state.
4. Assign the initial Building Admin after approval.
5. Complete self-service or assisted building onboarding.
6. Assign temporary, scoped, auditable onboarding/support access.
7. Assign and manage a building-level subscription plan/status/entitlements.
8. Register building floors and units efficiently, including bulk setup.
9. Map owners to units, including co-ownership and ownership across multiple buildings.
10. Give owners a cross-building `My Properties` view while preserving per-building authorization.
11. Register tenants.
12. Create leases.
13. Generate monthly rent.
14. Collect full/partial/advance rent.
15. Issue receipts.
16. Generate and collect maintenance fees.
17. Record common expenses.
18. Track current outstanding amounts.
19. Create and track building work.
20. Publish announcements and maintain meeting decisions.
21. Give owner/manager/tenant/committee role-specific dashboards.
22. Operate basic rent collection offline.
23. Synchronize safely without duplicate payment.
24. Export core financial reports.
25. Preserve financial, ownership, subscription, and administrative history.
26. Produce complete audit trace for critical financial and platform-admin actions.

---

# 147. Agent Starting Instruction

When an AI coding agent begins BuildingOS:

```text
Treat this BRD as the functional source of truth.

Implement vertically by feature.

For backend:
- Spring Boot microservices.
- API Gateway.
- Kafka event-driven integration.
- Database-per-service.
- Clean Architecture.
- Feature-first package structure.
- Repository ports/adapters.
- Explicit use cases.
- Transactional Outbox.
- Idempotent consumers.
- Flyway migrations.
- OpenAPI contracts.
- Permission enforcement at backend.
- Separate platform-role authorization from building-role authorization.
- Building-level subscription/entitlement enforcement through explicit application services.
- Unit + integration tests.

For Flutter:
- Feature-first Clean Architecture.
- Global My Buildings / My Properties context before building-scoped navigation where applicable.
- GetX for controller/state/DI.
- Drift for offline cache and pending mutations.
- Repository + use case.
- Never couple screen directly to HTTP client.
- Implement loading, empty, error, offline, conflict, and permission states.

Do not implement cross-service database access.
Do not place business logic in controllers.
Do not delete financial history.
Do not create duplicate payment during retry.
Do not bypass documented business invariants.

If a requirement is ambiguous:
1. Preserve financial correctness.
2. Preserve auditability.
3. Preserve historical data.
4. Preserve service boundaries.
5. Prefer explicit state transitions.
```

---

# 148. Next Recommended Engineering Documents

After this BRD, create:

1. `SYSTEM_ARCHITECTURE.md`
2. `SERVICE_BOUNDARIES.md`
3. `PERMISSION_MATRIX.md`
4. `DATABASE_SCHEMA.md` per service
5. `API_CONTRACTS.md`
6. `KAFKA_EVENT_CATALOG.md`
7. `OFFLINE_SYNC_SPEC.md`
8. `FINANCE_DOMAIN_RULES.md`
9. `FLUTTER_SCREEN_FLOW.md`
10. `AGENT_TASK_BACKLOG.md`
11. `DOCKER_LOCAL_SETUP.md`
12. `DEPLOYMENT_ARCHITECTURE.md`
13. `TEST_STRATEGY.md`
14. `ADMIN_BACKOFFICE_SPEC.md`
15. `SUBSCRIPTION_DOMAIN.md`
16. `ONBOARDING_FLOW.md`
17. `ADRs/`

These should reference this BRD and must not redefine domain rules inconsistently.

---

# 149. SaaS Back-Office Administration, Building Approval, Subscription & Onboarding

Status: **CORE PRODUCT SCOPE**.

BuildingOS is a SaaS platform. The back-office console is the operator control plane used by authorized BuildingOS staff to manage customer buildings, onboarding, subscriptions, support access, and platform-level lifecycle operations. It is separate from the ordinary building-resident/owner operational experience.

## 149.1 Back-Office Application

Recommended delivery:

```text
buildingos_backoffice_web
```

The back-office application may use Flutter Web to stay aligned with the primary client stack, but it must remain a distinct application surface with its own navigation and platform permission checks.

The back-office UI contains no authoritative business logic. All operations go through API Gateway/backend use cases.

Primary platform roles:

```text
SUPER_ADMIN
PLATFORM_ADMIN
ONBOARDING_AGENT
SUPPORT_AGENT
SUBSCRIPTION_ADMIN
```

## 149.2 Back-Office Navigation

```text
Dashboard

Buildings
├─ Applications
├─ Under Review
├─ Onboarding
├─ Active Buildings
├─ Suspended Buildings
└─ Archived Buildings

Users
├─ Platform Users
├─ Building Admins
├─ Onboarding Agents
└─ Support Agents

Subscriptions
├─ Plans
├─ Building Subscriptions
├─ Trials
├─ Past Due / Grace Period
└─ Suspended / Cancelled

Support
├─ Assisted Onboarding
├─ Active Support Sessions
└─ Support History

System
├─ Platform Settings
├─ Feature / Entitlement Configuration
├─ Audit Logs
└─ System Health
```

## 149.3 Building Application Sources

A building may enter BuildingOS through:

```text
SELF_SERVICE
ASSISTED
BACK_OFFICE
SALES
IMPORT
```

All sources create or attach to a `BuildingApplication` before activation unless an explicitly authorized migration/import workflow bypasses ordinary customer review.

Suggested entity:

```text
BuildingApplication
- id
- application_number
- applicant_user_id
- building_name
- building_type
- address
- district
- area
- postal_code optional
- total_floors optional
- estimated_units
- applicant_relationship
- contact_name
- contact_phone
- contact_email optional
- management_type optional
- verification_document_refs
- source
- assisted_by optional
- status
- submitted_at optional
- reviewed_at optional
- reviewed_by optional
- rejection_reason optional
- internal_notes optional
- created_at
- updated_at
```

The applicant does not automatically become the final Building Admin merely because they submitted the application.

## 149.4 Building Application State Machine

```text
DRAFT
  -> SUBMITTED
  -> UNDER_REVIEW
      -> MORE_INFORMATION_REQUIRED -> SUBMITTED
      -> REJECTED
      -> APPROVED
  -> ONBOARDING
  -> ACTIVE
```

Operational lifecycle after activation:

```text
ACTIVE
  -> SUSPENDED
  -> ACTIVE
  -> ARCHIVED
```

Rules:

- A submitted application is not an active tenant/building.
- Approval decisions require an authorized platform role.
- Rejection/request-information actions require a reason/message.
- Activation requires an assigned Building Admin and minimum onboarding prerequisites.
- Suspension does not delete customer data or historical records.
- Every state transition is audited.

## 149.5 Back-Office Building Application Review Screen

Display:

- Application number
- Building name/type
- Full address
- Applicant identity/contact
- Applicant relationship to building
- Estimated floor/unit count
- Verification documents
- Application source
- Submission date
- Review history
- Internal notes
- Possible duplicate-building matches

Actions:

- `Start Review`
- `Approve`
- `Reject`
- `Request More Information`
- `Assign Onboarding Agent`
- `Add Internal Note`
- `Open Applicant Profile`

Sensitive actions require confirmation and audit reason.

## 149.6 Duplicate Building Detection

Before approval, BuildingOS should flag possible duplicate registrations using available normalized signals such as:

- building name
- address
- road/area/district
- holding number when available
- approximate coordinates when provided by the user/device
- known contact numbers

Possible matches are review signals only. The system must not automatically merge buildings.

If an already-active building appears to be the intended building, the applicant may instead be directed to request/accept membership access.

## 149.7 Approval to Building Creation

On approval:

1. Persist the approval decision.
2. Create or activate the canonical `Building` record.
3. Create initial tenant-safe configuration.
4. Assign the approved initial `BUILDING_ADMIN`.
5. Create `BuildingOnboarding` state.
6. Create or attach an initial building subscription/trial according to platform policy.
7. Emit `building.application.approved` and related lifecycle events.
8. Move the building to `ONBOARDING`.

Approval does not require all units, owners, tenants, or finance settings to already exist.

## 149.8 Building Onboarding Modes

Supported modes:

```text
SELF_SERVICE
ASSISTED
BACK_OFFICE_SETUP
```

All modes use the same domain validation and produce the same canonical Building/Unit/Ownership records.

## 149.9 Guided Building Onboarding Flow

Recommended sequence:

```text
1. Building Information
2. Building Structure / Floors
3. Units
4. Owners / Ownership
5. Committee & Staff
6. Maintenance Configuration
7. Rent Management Configuration
8. Payment Methods
9. Invitations
10. Review
11. Activate / Finish Setup
```

The flow is resumable. Do not require completion in one session.

### Step 1 — Building Information

Fields:

- Building name
- Building type (`RESIDENTIAL`, `COMMERCIAL`, `MIXED`)
- Address
- Area/district/postal code
- Primary building contact
- Optional logo/photo
- Optional construction metadata

### Step 2 — Structure / Floors

Allow:

- add floors individually
- generate floor ranges
- represent basement/ground/roof/common floors
- configure a default unit naming pattern

### Step 3 — Units

Allow bulk generation/import for:

```text
FLAT
PARKING
STORAGE
COMMERCIAL
COMMON
OTHER
```

Bulk operations:

- generate units by floor/count/pattern
- duplicate floor layout
- CSV/Excel import
- edit generated rows before save

### Step 4 — Owners / Ownership

Ownership is not the same as building membership.

```text
User -> Ownership -> Unit -> Building
```

Rules:

- Reuse a global User when phone/identity matches an existing BuildingOS user.
- One user may own many units in the same building.
- One user may own units across many buildings.
- A unit may have multiple co-owners.
- Ownership percentages and effective dates are preserved historically.
- A pending owner may be represented by an invitation until the person claims/links their account.

### Step 5 — Committee & Staff

Building Admin may invite/assign:

- Committee
- Property/Rent Manager
- Accountant
- Viewer/Auditor
- other configured building roles

Human titles such as President, Secretary, or Treasurer are metadata and must not replace permission-based authorization.

### Step 6 — Maintenance Configuration

Configure building maintenance policy using existing domain options such as:

- fixed building rate
- unit-specific rate
- area-based rate
- owner category
- custom rate

### Step 7 — Rent Management Configuration

Support:

```text
CENTRALIZED
OWNER_MANAGED
MIXED
DISABLED
```

Not every owner/unit must use centralized BuildingOS rent collection.

### Step 8 — Payment Methods

Enable allowed payment methods and references. Direct bKash/Nagad/payment-gateway settlement can remain a later integration.

### Step 9 — Invitations

Allow bulk invitations for owners, committee members, managers, and tenants.

### Step 10 — Review

Show missing required data, optional incomplete setup, validation conflicts, and onboarding progress.

### Step 11 — Activation / Completion

Minimum activation prerequisites should include:

- approved building
- at least one active Building Admin
- required building identity/address data
- at least one valid unit for ordinary residential operation, unless migration policy permits otherwise
- accepted platform terms/policies where applicable

Optional setup may continue after activation.

## 149.10 Onboarding Progress Model

```text
BuildingOnboarding
- building_id
- mode
- current_step
- completed_steps
- completion_percentage
- status
- started_at
- last_updated_at
- completed_at optional
- assisted_session_id optional
```

Statuses:

```text
NOT_STARTED
IN_PROGRESS
WAITING_FOR_CUSTOMER
READY_FOR_ACTIVATION
COMPLETED
```

## 149.11 Assisted Onboarding

BuildingOS staff may assist customers without becoming permanent members of the building.

```text
AssistedOnboardingSession
- id
- building_id
- assigned_agent_user_id
- requested_by_user_id optional
- status
- access_scope
- reason
- started_at
- expires_at
- completed_at optional
- notes optional
```

Statuses:

```text
REQUESTED
ASSIGNED
IN_PROGRESS
WAITING_FOR_CUSTOMER
COMPLETED
CANCELLED
EXPIRED
```

Rules:

- Agent access is building-specific and scope-limited.
- Access must expire automatically.
- No implicit rent/payment/financial visibility unless the assistance scope explicitly requires and policy permits it.
- All mutations identify both the acting platform user and the customer/building context.
- The customer can see that assisted onboarding is active.

## 149.12 Support Assistance / Controlled Impersonation

BuildingOS must not implement unrestricted silent impersonation.

If support needs to act in a customer's context, create an auditable `SupportSession` or `SupportAccessGrant` containing:

```text
- support_session_id
- platform_user_id
- target_user_id optional
- building_id
- reason
- permission_scope
- approved_by optional
- started_at
- expires_at
- ended_at optional
```

High-risk actions such as payment reversal, ownership transfer, Building Admin removal, or unrestricted financial export should be blocked during ordinary support sessions or require an elevated, separately audited approval.

## 149.13 Multi-Building / Multi-Property User Model

A single user may simultaneously be:

```text
Building A -> OWNER + COMMITTEE
Building B -> OWNER
Building C -> BUILDING_ADMIN
Building D -> TENANT
```

And may own:

```text
Building A -> Flat 4A -> 100%
Building A -> Flat 4B -> 50%
Building B -> Flat 8C -> 100%
Building B -> Parking P12 -> 100%
```

This remains one `User` account.

Required relationships:

```text
User 1 --- * BuildingMembership * --- 1 Building
User 1 --- * Ownership          * --- 1 Unit
Unit * --- 1 Building
```

The system must never create a separate user identity merely because the same person joins another building.

## 149.14 My Properties — Cross-Building Owner Portfolio

Owners need a global portfolio separate from a single-building dashboard.

Summary may include:

- total units owned
- number of buildings
- occupied/vacant units
- expected monthly rent
- collected rent
- outstanding rent
- maintenance due

Each property row includes:

- building
- unit
- ownership percentage
- occupancy
- tenant summary when permitted
- rent summary when permitted
- maintenance due

Opening a property switches to or deep-links into that building's authorized context.

## 149.15 Subscription Domain

Subscription is building-level by default.

```text
SubscriptionPlan
- id
- code
- name
- status
- billing_cycle_options
- unit_limit optional
- user_limit optional
- storage_limit optional
- enabled_features / entitlement policy
- created_at
- updated_at
```

```text
BuildingSubscription
- id
- building_id
- plan_id
- status
- trial_start optional
- trial_end optional
- billing_cycle
- started_at
- current_period_start optional
- current_period_end optional
- grace_until optional
- cancelled_at optional
- suspended_at optional
- suspension_reason optional
- created_at
- updated_at
```

Subscription states:

```text
TRIAL
ACTIVE
PAST_DUE
GRACE_PERIOD
SUSPENDED
CANCELLED
EXPIRED
```

Back-office actions:

- start/extend/end trial
- assign/change plan
- suspend/reactivate
- cancel
- inspect subscription history
- inspect effective entitlements

All changes are audited.

## 149.16 Entitlements

Feature availability should be resolved from subscription/plan entitlements, for example:

```text
rent_management.enabled
maintenance.enabled
work_orders.enabled
reports.pdf_export
reports.excel_export
offline_sync.enabled
max_units
max_users
storage_limit_mb
support_tier
```

Rules:

- Backend is authoritative for entitlement enforcement.
- Flutter/back-office UIs may hide/disable unavailable functionality for UX but cannot enforce policy alone.
- Subscription suspension must not delete customer data.
- Read-only or restricted behavior during grace/suspension is policy-driven and explicit.

## 149.17 Back-Office Building Detail Screen

Tabs:

```text
Overview
Application
Verification
Admins
Members
Units
Onboarding
Subscription
Support
Activity
Audit
```

Header should expose:

- building name
- lifecycle status
- location summary
- unit count
- active membership count
- Building Admins
- onboarding completion
- subscription plan/status

Actions by permission:

- `Open Building Summary`
- `Assign Building Admin`
- `Assign Onboarding Agent`
- `Request Information`
- `Change Subscription`
- `Suspend / Reactivate Building`
- `Start Support Session`
- `View Audit`

## 149.18 Back-Office SaaS Dashboard

Recommended operational metrics:

- total buildings by lifecycle state
- applications awaiting review
- buildings in onboarding
- active/suspended buildings
- subscriptions by plan/status
- trials ending soon
- onboarding cases requiring action
- active support sessions

Financial SaaS revenue metrics may be added once automated billing/provider integrations exist.

## 149.19 API Requirements

Customer/onboarding APIs:

```text
POST  /api/v1/building-applications
GET   /api/v1/building-applications/{id}
POST  /api/v1/building-applications/{id}/submit
GET   /api/v1/buildings/{buildingId}/onboarding
PATCH /api/v1/buildings/{buildingId}/onboarding
POST  /api/v1/buildings/{buildingId}/units/bulk
POST  /api/v1/buildings/{buildingId}/invitations/bulk
```

Platform APIs:

```text
GET  /api/v1/platform/building-applications
GET  /api/v1/platform/building-applications/{id}
POST /api/v1/platform/building-applications/{id}/approve
POST /api/v1/platform/building-applications/{id}/reject
POST /api/v1/platform/building-applications/{id}/request-information

POST /api/v1/platform/buildings/{buildingId}/assign-admin
POST /api/v1/platform/buildings/{buildingId}/assign-onboarding-agent
POST /api/v1/platform/buildings/{buildingId}/suspend
POST /api/v1/platform/buildings/{buildingId}/reactivate

GET  /api/v1/platform/subscription-plans
POST /api/v1/platform/subscription-plans
GET  /api/v1/platform/buildings/{buildingId}/subscription
POST /api/v1/platform/buildings/{buildingId}/subscription/change-plan
POST /api/v1/platform/buildings/{buildingId}/subscription/suspend
POST /api/v1/platform/buildings/{buildingId}/subscription/reactivate

POST /api/v1/platform/support-sessions
POST /api/v1/platform/support-sessions/{id}/end
```

## 149.20 Event Requirements

```text
building.application.created
building.application.submitted
building.application.review_started
building.application.info_requested
building.application.approved
building.application.rejected
building.onboarding.started
building.onboarding.step_completed
building.onboarding.completed
building.activated
building.suspended
building.reactivated
building.admin.assigned
building.admin.removed
onboarding.agent.assigned
onboarding.session.completed
support.session.started
support.session.ended
owner.invited
owner.invitation.accepted
subscription.started
subscription.plan_changed
subscription.past_due
subscription.suspended
subscription.reactivated
subscription.cancelled
building.entitlements.changed
```

All events follow the common event envelope, outbox, versioning, and idempotent-consumer rules defined elsewhere in this BRD.

## 149.21 Audit Requirements

Audit at minimum:

- application state transitions
- approval/rejection actor and reason
- building activation/suspension/reactivation
- Building Admin assignment/removal
- onboarding-agent assignment and all assisted mutations
- support-session creation/termination and actions
- subscription plan/status changes
- entitlement changes
- bulk unit/owner imports

Back-office internal notes should be access-controlled and must not leak into ordinary building-member views.

## 149.22 Security Boundaries

- Platform roles are not automatically building roles.
- Building Admins cannot grant themselves platform roles.
- Onboarding/support agents receive no access to buildings unless assigned or explicitly granted.
- Platform endpoints require platform permission checks.
- Ordinary building APIs continue to enforce building membership and `activeBuildingId`.
- Cross-building owner portfolio queries verify ownership/membership per returned record.
- Subscription status never grants access to data a user is otherwise unauthorized to view.
- Support access is temporary, reason-bound, scope-limited, and auditable.

## 149.23 MVP Subscription Boundary

MVP includes:

- plan definitions
- building-to-plan assignment
- trial status
- active/suspended/cancelled lifecycle
- entitlements
- back-office manual plan/status management
- audit history

MVP does **not** require:

- automatic card charging
- bKash/Nagad subscription collection
- invoices for BuildingOS SaaS fees
- tax/VAT automation
- dunning automation
- organization-level consolidated billing

These can be integrated later without redesigning building tenancy or subscription state.

---
