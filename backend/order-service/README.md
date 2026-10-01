# Order Service

This is the Order Service component for the OrderFlow platform. It handles the creation and management of customer orders.

## Architecture
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL
- **Pattern**: Controller -> Service -> Repository
- **Testing**: JUnit 5, Mockito, Spring WebMvcTest
- **Security**: Spring Security + JWT authentication with RBAC and BCrypt password hashing.

## Security & Authentication
The Order Service acts as the central point for authentication using Spring Security and JWT.
All business endpoints (Orders, Fulfillment, Inventory) require a valid `Authorization: Bearer <token>` header.

### Roles & Permissions (RBAC)
- **ADMIN**: Can perform all operations and access `/api/v1/audit-logs`.
- **OPERATOR**: Can perform all business operations (create/update orders).
- **VIEWER**: Can only perform `GET` requests (read-only).

### Endpoints
- `POST /api/v1/auth/register`: Public registration (requires `username`, `password`, `role`).
- `POST /api/v1/auth/login`: Authenticate and receive a JWT.
- `GET /api/v1/audit-logs`: (ADMIN only) Fetch audit events.

## Environment Variables
The application expects the following database configuration variables:
- `DB_HOST`: Database hostname (default: `localhost`)
- `DB_PORT`: Database port (default: `5432`)
- `DB_NAME`: Database name (default: `orderflow_order`)
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password

## API Endpoints

### 1. Create Order
- **URL**: `POST /api/v1/orders`
- **Request Body**:
  ```json
  {
    "customerId": "CUST-1001",
    "totalAmount": 2499.00,
    "currency": "INR",
    "items": [
      {
        "productId": "PROD-1001",
        "quantity": 2
      }
    ]
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "customerId": "CUST-1001",
    "status": "CONFIRMED",
    "totalAmount": 2499.00,
    "currency": "INR",
    "items": [
      {
        "productId": "PROD-1001",
        "quantity": 2
      }
    ],
    "createdAt": "...",
    "updatedAt": "..."
  }
  ```
  *(Status will be `FAILED` if inventory reservation fails).*

### 2. Get Order by ID
- **URL**: `GET /api/v1/orders/{id}`
- **Response**: `200 OK` or `404 Not Found`

### 3. Get All Orders
- **URL**: `GET /api/v1/orders`
- **Response**: `200 OK`

### 4. Update Order Status
- **URL**: `PATCH /api/v1/orders/{id}/status`
- **Request Body**:
  ```json
  {
    "status": "CONFIRMED"
  }
  ```
- **Allowed Statuses**: `CREATED`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `FAILED`
- **Response**: `200 OK` or `400 Bad Request`

## Downstream Integration Workflow

When an order is created, the service orchestrates an HTTP workflow (Saga-like compensation pattern):
1. Order saved locally as `CREATED`.
2. `InventoryClient` attempts to reserve stock for every item via `POST /api/v1/inventory/reserve`.
3. If **any** reservation fails -> Order updated to `FAILED`. A compensation process triggers, releasing any previously reserved items for that order. An operational Incident is reported.
4. If all reservations succeed -> Order updated to `CONFIRMED`.
5. `FulfillmentClient` attempts to create a fulfillment record via `POST /api/v1/fulfillments`.
6. If fulfillment creation fails -> Order updated to `FAILED`. A compensation process releases **all** previously reserved inventory items. An operational Incident is reported.

*(Note: This is synchronous HTTP and relies on best-effort compensation. It is not a true atomic distributed transaction. Incident reporting is best-effort. Failure to report an incident does not replace or alter the original business failure.)*

## Running Locally

**Start dependencies:**
From the project root:
```bash
docker compose up -d postgres
```

**Run application:**
```bash
cd backend/order-service
mvn spring-boot:run
```

**Run tests:**
```bash
mvn test
```
