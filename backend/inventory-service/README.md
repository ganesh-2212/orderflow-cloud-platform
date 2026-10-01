# Inventory Service

This is the Inventory Service component for the OrderFlow platform. It handles product inventory, available stock, reserved stock, and operations to reserve or release stock for orders.

## Architecture
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL
- **Pattern**: Controller -> Service -> Repository
- **Testing**: JUnit 5, Mockito, Spring WebMvcTest

## Database Entity
Table `inventory`:
- `id`: Primary key
- `product_id`: Unique string identifier
- `product_name`: Name of the product
- `available_quantity`: Stock available for new orders
- `reserved_quantity`: Stock reserved for in-progress orders
- `created_at`: Timestamp
- `updated_at`: Timestamp

## Environment Variables
- `DB_HOST`: Database hostname (default: `localhost`)
- `DB_PORT`: Database port (default: `5432`)
- `DB_NAME`: Database name (default: `orderflow_inventory`)
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password

## API Endpoints

### 1. Create Inventory
- **URL**: `POST /api/v1/inventory`
- **Request Body**:
  ```json
  {
    "productId": "PROD-1001",
    "productName": "Laptop",
    "quantity": 10
  }
  ```
- **Response**: `201 Created`

### 2. Get Inventory by Product ID
- **URL**: `GET /api/v1/inventory/{productId}`
- **Response**: `200 OK` or `404 Not Found`

### 3. List All Inventory
- **URL**: `GET /api/v1/inventory`
- **Response**: `200 OK`

### 4. Reserve Stock
- **URL**: `POST /api/v1/inventory/reserve`
- **Behavior**: Decreases `availableQuantity`, increases `reservedQuantity`. 
- **Request Body**:
  ```json
  {
    "productId": "PROD-1001",
    "quantity": 2
  }
  ```
- **Error**: `409 Conflict` if insufficient stock.

### 5. Release Stock
- **URL**: `POST /api/v1/inventory/release`
- **Behavior**: Increases `availableQuantity`, decreases `reservedQuantity`.
- **Request Body**:
  ```json
  {
    "productId": "PROD-1001",
    "quantity": 1
  }
  ```
- **Error**: `409 Conflict` if attempting to release more than is reserved.

## Running Locally

**Start dependencies:**
From the project root:
```bash
docker compose up -d postgres
```

**Run application:**
```bash
cd backend/inventory-service
mvn spring-boot:run
```

**Run tests:**
```bash
mvn test
```
