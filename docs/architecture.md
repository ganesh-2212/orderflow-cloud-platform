# Architecture

OrderFlow is a Cloud-Native Distributed Order Fulfillment & Operations Platform.

## Planned Services
- **order-service:** Creates and manages customer orders.
- **inventory-service:** Manages product inventory and stock reservations.
- **fulfillment-service:** Handles the warehouse and shipping workflows.
- **incident-service:** Manages operational incidents, failures, and retries.

## Planned Order Workflow
Customer Order -> Order Service -> Inventory Service -> Fulfillment / Warehouse -> Shipping -> Delivery.

### Architecture Overview

1. **Order Service**: Coordinates the overall order workflow.
2. **Inventory Service**: Manages reserved and available stock quantities.
3. **Fulfillment Service**: Manages the operational post-order state (Warehouse -> Shipped -> Delivered).
4. **Incident Service**: Records operational anomalies, categorizes severities, tracks remediation attempts, and enforces a strict investigation-to-resolution lifecycle.

*(Note: While these services exist independently and Order integrates with Inventory & Fulfillment, the downstream automatic integration into the Incident Service is NOT implemented yet. Incident Service manages incidents independently).*

### Phase 8 Distributed Workflow
Currently, the interaction between services relies on synchronous HTTP communication orchestrated by the Order Service:
```text
Order Service -> InventoryClient -> HTTP -> Inventory Service
Order Service -> FulfillmentClient -> HTTP -> Fulfillment Service

Order Service -> IncidentClient -> HTTP -> Incident Service
Fulfillment Service -> IncidentClient -> HTTP -> Incident Service
```
The Order Service implements a local saga/compensation pattern: 
1. If a multi-item order partially reserves inventory and then fails, the Order Service catches the failure, actively releases the previously reserved stock, marks the order as `FAILED`, and **reports an incident** to the Incident Service.
2. If inventory reservation completely succeeds, but creating the fulfillment record fails, the Order Service catches the failure, actively releases **all** reserved inventory, marks the order as `FAILED`, and **reports an incident**.

The Fulfillment Service also reports internal operational failures (e.g. duplicate creation, illegal status transitions) directly to the Incident Service.

*(Note: Incident reporting is best-effort. Failure to report an incident does not replace or alter the original business failure. This is synchronous HTTP and not a distributed transaction.)*

## Phase 9 Security (JWT + RBAC)
The Order Service acts as the application gateway for authentication. It utilizes **Spring Security**, **BCrypt**, and **JWT**.
- **Roles**: `ADMIN`, `OPERATOR`, `VIEWER`.
- **Flow**: User registers (passwords hashed) -> User logs in -> Server issues JWT -> Client sends JWT in `Authorization` header.
- **Auditing**: Core auth actions (login success/failure) are logged into the `audit_logs` table. Accessible only to `ADMIN`s via `/api/v1/audit-logs`.
- **Service to Service**: Internal WebClient communication between services is currently unauthenticated as this phase focuses purely on external-facing application security.

## Phase 10 Python Operational Automation
A Python-based event processor that is responsible for automation and event processing logic. Python is used for lightweight operational automation and deterministic incident processing, while Java/Spring Boot remains responsible for transactional microservices.

```text
Incident Service
       ↑
       │ HTTP (POST /remediation)
       │
Python Processor
       │
       ├── Normalize
       ├── Risk Rules
       └── Bounded Remediation (GET /status/OPEN)
```
The processor does not store any state. It polls the Incident Service for `OPEN` incidents, runs them through deterministic rules to assess `risk_level`, and triggers remediation (`MITIGATING`) if criteria (such as max attempt threshold and severity combinations) are met.

## Monitoring/Reliability
Using Spring Boot Actuator for health checks and metrics. Incident service will handle failure remediation.
