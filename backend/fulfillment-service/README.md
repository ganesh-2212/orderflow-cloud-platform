# Fulfillment Service

This is the Fulfillment Service component for the OrderFlow platform. It manages the operational lifecycle of an order after it has been confirmed, tracking warehouse processing, shipping, and delivery status.

## Architecture
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL
- **Pattern**: Controller -> Service -> Repository
- **Testing**: JUnit 5, Mockito, Spring WebMvcTest

## Entity Details
Table `fulfillments`:
- `id`: Primary key
- `order_id`: Unique identifier referencing the Order Service
- `status`: Enum (WAREHOUSE_PROCESSING, READY_FOR_SHIPPING, SHIPPED, DELIVERED, FAILED)
- `warehouse_id`: String representing the warehouse location
- `tracking_number`: String tracking reference (generated upon shipping)
- `created_at`: Timestamp
- `updated_at`: Timestamp

## Status Transition Rules
The fulfillment status MUST follow a strict linear progression. Backwards transitions are explicitly blocked and will yield a `409 Conflict`.
- `WAREHOUSE_PROCESSING` -> `READY_FOR_SHIPPING`
- `READY_FOR_SHIPPING` -> `SHIPPED` (Generates Tracking Number)
- `SHIPPED` -> `DELIVERED`
- Any normal status -> `FAILED`

## API Endpoints

### 1. Create Fulfillment
- **URL**: `POST /api/v1/fulfillments`
- **Request Body**:
  ```json
  {
    "orderId": 1,
    "warehouseId": "WH-001"
  }
  ```
- **Response**: `201 Created`

### 2. Get Fulfillment by ID
- **URL**: `GET /api/v1/fulfillments/{id}`
- **Response**: `200 OK`

### 3. Get Fulfillment by Order ID
- **URL**: `GET /api/v1/fulfillments/order/{orderId}`
- **Response**: `200 OK`

### 4. Update Status
- **URL**: `PATCH /api/v1/fulfillments/{id}/status`
- **Request Body**:
  ```json
  {
    "status": "READY_FOR_SHIPPING"
  }
  ```
- **Response**: `200 OK` or `409 Conflict`

## Environment Variables
- `DB_HOST`: Database hostname (default: `localhost`)
- `DB_PORT`: Database port (default: `5432`)
- `DB_NAME`: Database name (default: `orderflow_fulfillment`)
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password

## Running Locally
**Start dependencies:**
```bash
docker compose up -d postgres
```

**Run tests:**
```bash
mvn test
```

**Run application:**
```bash
mvn spring-boot:run
```
