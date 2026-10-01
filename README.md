# OrderFlow

OrderFlow is a Cloud-Native Distributed Order Fulfillment & Operations Platform.

## Phase 1 Status
Currently in Phase 1: Foundation and Architecture Setup.

## Technology Stack
- **Backend:** Java 17+, Spring Boot 3.x, Maven, PostgreSQL, Spring Data JPA, Actuator
- **Frontend:** React, TypeScript, Vite
- **Python Processor:** Python 3.11+
- **Infrastructure:** Docker, Docker Compose

## Directory Structure
- `backend/`: Contains the Spring Boot microservices (order, inventory, fulfillment, incident).
- `frontend/`: Contains the React/Vite web application.
- `python-processor/`: Contains the scaffolding for Python automation processing.
- `infrastructure/`: Future home for Kubernetes/AWS configs.
- `docs/`: Architecture and platform documentation.
- `tests/`: End-to-end integration tests.

## Running the Project

> **Note**: Docker runtime verification pending because Docker is not currently installed on the development machine.

### Docker Architecture
The application runs as a containerized stack utilizing Docker Compose. 
- **Internal Network**: All containers communicate internally on the `orderflow_net` bridge network. Services connect to each other via internal DNS (e.g., `http://inventory-service:8082`).
- **Data Persistence**: PostgreSQL data is persisted via a named Docker volume (`postgres-data`). Initial database setup happens via `postgres-init.sh`.

### How to Build Images
To rebuild all service images locally:
```bash
docker-compose build
```

### How to Start Compose
To start the entire platform (PostgreSQL, 4 Java services, and Frontend) in detached mode:
```bash
docker-compose up -d
```

### How to Stop Compose
To gracefully stop all services and remove the containers:
```bash
docker-compose down
```

### How to View Logs
To view the aggregated logs for all services:
```bash
docker-compose logs -f
```
To view logs for a specific service (e.g., order-service):
```bash
docker-compose logs -f order-service
```

### How to Check Service Health
Wait for `docker-compose ps` to show services as `(healthy)`.
You can also manually check actuator endpoints:
```bash
curl http://localhost:8081/actuator/health
```

### How to Run Python Processor Separately
The Python Processor acts as a one-shot operational component. It is NOT started automatically with `docker-compose up`. To trigger a run manually:
```bash
docker-compose run --rm python-processor
```

