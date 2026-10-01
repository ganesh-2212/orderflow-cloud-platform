# Incident Service

The Incident Service is an independent microservice responsible for tracking and recording operational incidents within the OrderFlow platform. It maintains the operational lifecycle of a failure from discovery to resolution.

## Architecture
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL
- **Pattern**: Controller -> Service -> Repository
- **Testing**: JUnit 5, Mockito, Spring WebMvcTest

*The Incident Service currently records and manages incidents independently. Integration with other OrderFlow services (e.g. automatic trigger on fulfillment failure) will be implemented in a later phase.*

## Entities & Enums
- **IncidentCategory**: Pre-defined failure categories (e.g., `FULFILLMENT_FAILURE`, `SERVICE_UNAVAILABLE`).
- **IncidentSeverity**: Impacts levels (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- **IncidentStatus**: Lifecycle stages (`OPEN`, `INVESTIGATING`, `MITIGATING`, `RESOLVED`, `CLOSED`).

## Remediation Behavior
You can manually record a remediation attempt (e.g. `RETRY`). The service:
1. Tracks attempt counts.
2. Transitions the status to `MITIGATING` if currently `OPEN` or `INVESTIGATING`.
3. Implements a maximum retry limit of `3`. Exceeding this returns a `409 Conflict`.

## API Endpoints

### 1. Create Incident
- **URL**: `POST /api/v1/incidents`
- **Payload**:
  ```json
  {
    "serviceName": "order-service",
    "resourceId": "ORDER-1001",
    "category": "FULFILLMENT_FAILURE",
    "severity": "HIGH",
    "title": "Fulfillment creation failed",
    "description": "Fulfillment service returned HTTP 500"
  }
  ```
- **Response**: `201 Created`

### 2. Update Status
- **URL**: `PATCH /api/v1/incidents/{id}/status`
- **Payload**: `{"status": "INVESTIGATING"}`
- **Response**: `200 OK` or `409 Conflict` (for invalid transition)

### 3. Record Remediation
- **URL**: `POST /api/v1/incidents/{id}/remediation`
- **Payload**: `{"action": "RETRY_OPERATION"}`
- **Response**: `200 OK` or `409 Conflict`

### 4. Fetch / List
- **URL**: `GET /api/v1/incidents/{id}`
- **URL**: `GET /api/v1/incidents/key/{incidentKey}`
- **URL**: `GET /api/v1/incidents`
- **URL**: `GET /api/v1/incidents/status/{status}`

## Configuration
Requires PostgreSQL configuration:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

## Running Locally
**Start dependencies:**
```bash
docker compose up -d postgres
```

**Run application:**
```bash
mvn spring-boot:run
```
