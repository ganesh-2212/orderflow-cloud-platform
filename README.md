# OrderFlow — Cloud-Native Distributed Order Fulfillment & Operations Platform

OrderFlow is a cloud-native distributed order fulfillment and operations platform built with **Java, Spring Boot, PostgreSQL, Python, Docker, Kubernetes and AWS**.

The platform models an end-to-end order workflow:

**Order → Inventory → Fulfillment → Delivery**

It also includes operational failure handling:

**Failure → Incident → Automated Remediation → Monitoring**

The project was designed to demonstrate practical microservices, cloud deployment, security, observability, automation and CI/CD concepts in a single production-style system.

---

## Live Demo

**Public Dashboard:**
http://a17dc9406dc4d48a5a9508d0590c9787-1599414992.ap-south-1.elb.amazonaws.com

> The public URL is backed by an AWS Elastic Load Balancer exposing only the frontend. Backend services remain internal Kubernetes services.
>
> **Note:** This demo is not a permanent URL. The hostname may become unavailable if the Kubernetes service or cluster is deleted.

---

## Project Overview

OrderFlow is designed around independent backend services responsible for different parts of an order lifecycle.

When an order is created:

1. The Order Service creates the order.
2. Inventory Service reserves the required inventory.
3. If inventory reservation succeeds, the order is confirmed.
4. Fulfillment Service creates a warehouse fulfillment record.
5. The fulfillment progresses through its lifecycle.
6. Failures generate operational incidents.
7. A Python automation processor evaluates eligible incidents.
8. Eligible incidents are remediated through a bounded retry mechanism.
9. Metrics and service health are exposed through Spring Boot Actuator.

The system uses synchronous REST communication between services and implements simplified Saga-style compensation for important failure paths.

---

## Architecture

```mermaid
flowchart TB
    User(["End User"])
    ELB["AWS Load Balancer"]

    subgraph EKS["Amazon EKS · namespace: orderflow"]
        direction TB
        FE["React + Nginx<br/>Operations Dashboard"]

        subgraph SVC["Spring Boot Microservices"]
            direction LR
            ORD["Order Service<br/>:8081"]
            INV["Inventory Service<br/>:8082"]
            FUL["Fulfillment Service<br/>:8083"]
            INC["Incident Service<br/>:8084"]
        end

        PY["Python Processor<br/>Kubernetes CronJob"]
        DB[("PostgreSQL<br/>orderflow_order<br/>orderflow_inventory<br/>orderflow_fulfillment<br/>orderflow_incident")]
    end

    User -->|HTTP| ELB
    ELB --> FE
    FE -->|"REST via Nginx proxy"| ORD
    FE -.->|"health, incidents, metrics"| INC
    ORD -->|"reserve / release"| INV
    ORD -->|"create fulfillment"| FUL
    ORD -.->|"report failure"| INC
    FUL -.->|"report failure"| INC
    PY -->|"REST polling and remediation"| INC
    ORD --> DB
    INV --> DB
    FUL --> DB
    INC --> DB

    classDef user fill:#fff3e0,stroke:#ef6c00,color:#000
    classDef edge fill:#e3f2fd,stroke:#1565c0,color:#000
    classDef svc fill:#e8f5e9,stroke:#2e7d32,color:#000
    classDef auto fill:#f3e5f5,stroke:#6a1b9a,color:#000
    classDef data fill:#fffde7,stroke:#f9a825,color:#000
    class User user
    class ELB,FE edge
    class ORD,INV,FUL,INC svc
    class PY auto
    class DB data
```

### Order Processing Flow

```mermaid
flowchart TB
    START(["Client submits order"])

    subgraph ORDER["Order Service"]
        direction TB
        O1["Authenticate request<br/>JWT and role check"]
        O2["Validate payload<br/>and create order"]
        O3{"All items<br/>reserved?"}
        O4{"Fulfillment<br/>created?"}
        O5["Mark order CONFIRMED"]
        O6["Mark order FAILED"]
    end

    subgraph INVENTORY["Inventory Service"]
        direction TB
        I1["Reserve stock<br/>item by item"]
        I2["Release earlier reservations<br/>compensation"]
    end

    subgraph FULFILLMENT["Fulfillment Service"]
        direction TB
        F1["Create fulfillment record"]
        F2["WAREHOUSE_PROCESSING"]
        F3["READY_FOR_SHIPPING"]
        F4["SHIPPED<br/>tracking number generated"]
        F5["DELIVERED"]
    end

    subgraph INCIDENT["Incident Service"]
        direction TB
        N1["Create incident<br/>status OPEN"]
    end

    START --> O1 --> O2
    O2 -->|"reserve request"| I1
    I1 --> O3
    O3 -->|"Yes"| F1
    O3 -->|"No"| I2
    F1 --> O4
    O4 -->|"Yes"| O5
    O4 -->|"No"| I2
    O5 --> F2 --> F3 --> F4 --> F5
    I2 --> O6
    O6 -->|"report failure"| N1
    F5 --> DONE(["Order completed"])
    N1 --> FAIL(["Order closed as failed"])

    classDef order fill:#e3f2fd,stroke:#1565c0,color:#000
    classDef inv fill:#e8f5e9,stroke:#2e7d32,color:#000
    classDef ful fill:#f3e5f5,stroke:#6a1b9a,color:#000
    classDef bad fill:#ffebee,stroke:#c62828,color:#000
    classDef decision fill:#fff8e1,stroke:#f9a825,color:#000
    classDef terminal fill:#eceff1,stroke:#546e7a,color:#000
    class O1,O2,O5 order
    class I1 inv
    class F1,F2,F3,F4,F5 ful
    class O6,I2,N1 bad
    class O3,O4 decision
    class START,DONE,FAIL terminal
```

### Failure & Remediation Flow

```mermaid
flowchart TB
    subgraph SOURCE["Failure Sources"]
        direction TB
        S1["Order, Inventory or Fulfillment Service<br/>detects a failure"]
    end

    subgraph INCSVC["Incident Service - source of truth"]
        direction TB
        C1["Create incident<br/>status OPEN"]
        C2["Return open incidents"]
        C3{"Retry limit<br/>reached?"}
        C4["Set status MITIGATING<br/>increment retry count"]
        C5["Reject remediation request"]
        C6["Update metrics<br/>orderflow.incidents.*"]
    end

    subgraph PROC["Python Processor - Kubernetes CronJob"]
        direction TB
        P1["Scheduled run starts"]
        P2["Poll open incidents<br/>via REST"]
        P3["Normalize incident data"]
        P4["Classify eligibility and apply<br/>deterministic risk rules"]
        P5{"Eligible for<br/>remediation?"}
        P6["Skip incident"]
        P7["Request bounded remediation<br/>via REST"]
        P8["Record result and continue<br/>with next incident"]
    end

    S1 --> C1
    C1 -.->|"stored until polled"| C2
    P1 --> P2
    P2 -->|"GET open incidents"| C2
    C2 -->|"incident list"| P3
    P3 --> P4 --> P5
    P5 -->|"No"| P6
    P5 -->|"Yes"| P7
    P7 -->|"remediation request"| C3
    C3 -->|"No"| C4
    C3 -->|"Yes"| C5
    C4 --> C6
    C4 -->|"updated state"| P8
    C5 -->|"rejected"| P8
    P6 --> P8
    P8 -.->|"next incident"| P3

    classDef src fill:#ffebee,stroke:#c62828,color:#000
    classDef svc fill:#e8f5e9,stroke:#2e7d32,color:#000
    classDef auto fill:#f3e5f5,stroke:#6a1b9a,color:#000
    classDef decision fill:#fff8e1,stroke:#f9a825,color:#000
    classDef neutral fill:#eceff1,stroke:#546e7a,color:#000
    class S1 src
    class C1,C2,C4,C6 svc
    class C5,P6 neutral
    class P1,P2,P3,P4,P7,P8 auto
    class C3,P5 decision
```

The Python processor does **not** directly modify database state. It communicates with the Incident Service through its REST API, while the Incident Service remains the source of truth for incident state and retry limits.

---

## Key Features

### 1. Microservices Architecture

The backend is divided into four independently deployable Spring Boot services:

| Service | Responsibility | Port |
|---|---|---|
| Order Service | Orders, authentication, workflow orchestration | 8081 |
| Inventory Service | Stock management and reservations | 8082 |
| Fulfillment Service | Warehouse and shipping lifecycle | 8083 |
| Incident Service | Failure tracking and remediation | 8084 |

### 2. Order & Inventory Management

**Order Service** supports:

- Order creation
- Order retrieval
- Order status updates
- Multiple order items
- Inventory reservation
- Inventory compensation
- Failure handling
- Order workflow orchestration

**Inventory Service** supports:

- Product creation
- Inventory lookup
- Inventory reservation
- Inventory release
- Quantity validation
- Transactional updates

### 3. Fulfillment Lifecycle

Fulfillment records progress through controlled states:

```mermaid
stateDiagram-v2
    direction LR
    [*] --> WAREHOUSE_PROCESSING: order confirmed
    WAREHOUSE_PROCESSING --> READY_FOR_SHIPPING
    READY_FOR_SHIPPING --> SHIPPED: tracking number generated
    SHIPPED --> DELIVERED
    DELIVERED --> [*]
```

Invalid state transitions are rejected. A tracking number is generated when an order enters the shipping stage.

### 4. Saga-Style Compensation

Order processing uses simplified synchronous compensation. For example:

```mermaid
sequenceDiagram
    autonumber
    participant O as Order Service
    participant I as Inventory Service
    participant N as Incident Service

    O->>I: Reserve Product A
    I-->>O: Reserved
    O->>I: Reserve Product B
    I-->>O: Insufficient stock
    Note over O,I: Compensation (best-effort, synchronous)
    O->>I: Release Product A
    I-->>O: Released
    O->>N: Report failure
    Note over O: Order marked FAILED
```

This prevents successfully reserved inventory from remaining locked when a later reservation fails.

> OrderFlow uses best-effort synchronous compensation rather than a full distributed transaction or event-driven Saga implementation.

### 5. Incident Management

Failures can generate incidents containing:

- Incident key
- Service
- Resource
- Category
- Severity
- Status
- Error code
- Error message
- Retry count
- Resolution notes
- Timestamps

Supported incident categories:

- Service unavailable
- Timeout
- Validation failure
- Database error
- Inventory failure
- Fulfillment failure
- Security finding
- Performance degradation
- Unknown

### 6. Automated Operational Remediation

A separate Python processor periodically polls the Incident Service for open incidents. The processor:

1. Retrieves open incidents.
2. Normalizes incident data.
3. Classifies remediation eligibility.
4. Applies deterministic risk rules.
5. Attempts bounded remediation.
6. Records processing results.
7. Continues processing if one incident fails.

Remediation attempts are limited to prevent uncontrolled retries. The Kubernetes deployment runs the processor as a scheduled **CronJob**.

---

## Security

OrderFlow implements application-level security using:

- Spring Security
- JWT authentication
- BCrypt password hashing
- Role-Based Access Control (RBAC)
- Audit logging
- Configurable JWT expiration
- Environment-based secrets

### Roles

| Role | Access |
|---|---|
| `ADMIN` | Full access and administration |
| `OPERATOR` | Business operations and incident remediation |
| `VIEWER` | Read-only access |

### Authentication Endpoints

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Protected requests use:

```http
Authorization: Bearer <JWT>
```

Audit logs capture security-related actions such as successful and failed logins. Passwords, password hashes and JWT values are **not** logged.

---

## Observability

Spring Boot Actuator is enabled across the backend services.

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Health |
| `/actuator/info` | Information |
| `/actuator/metrics` | Metrics |

### Custom Micrometer Metrics

| Service | Metrics |
|---|---|
| Order Service | `orderflow.orders.created`, `orderflow.orders.failed` |
| Inventory Service | `orderflow.inventory.reservations`, `orderflow.inventory.failures`, `orderflow.inventory.releases` |
| Fulfillment Service | `orderflow.fulfillments.created`, `orderflow.fulfillments.shipped`, `orderflow.fulfillments.delivered`, `orderflow.fulfillments.failed` |
| Incident Service | `orderflow.incidents.created`, `orderflow.incidents.resolved`, `orderflow.incidents.remediation.attempts` |

The React operations dashboard periodically polls service health, incidents and metrics.

---

## AWS Deployment

OrderFlow is deployed on Amazon Web Services using **Amazon EKS**.

```mermaid
flowchart LR
    U(["User"]) --> ELB["AWS Load Balancer<br/>public"]

    subgraph AWS["AWS · ap-south-1"]
        direction LR
        ELB
        ECR[("Amazon ECR<br/>container images")]
        IAM["IAM + GitHub OIDC"]

        subgraph VPC["Amazon VPC"]
            subgraph EKS["Amazon EKS · namespace: orderflow<br/>EC2 worker nodes"]
                direction TB
                FE["Frontend<br/>React + Nginx<br/>LoadBalancer"]
                BE["Backend Services<br/>Order · Inventory · Fulfillment · Incident<br/>ClusterIP"]
                PG[("PostgreSQL<br/>ClusterIP")]
                CJ["Python CronJob"]
            end
            EBS[("Amazon EBS<br/>persistent volume")]
        end
    end

    ELB --> FE
    FE --> BE
    BE --> PG
    CJ --> BE
    PG --- EBS
    ECR -. "image pull" .-> EKS
    IAM -. "auth" .-> ECR

    classDef pub fill:#e3f2fd,stroke:#1565c0,color:#000
    classDef priv fill:#e8f5e9,stroke:#2e7d32,color:#000
    classDef store fill:#fffde7,stroke:#f9a825,color:#000
    class ELB,FE pub
    class BE,CJ priv
    class PG,EBS,ECR store
```

### AWS Components

- Amazon EKS
- Amazon EC2 worker nodes
- Amazon ECR
- Amazon EBS
- AWS Load Balancer
- Amazon VPC
- IAM
- CloudFormation (through `eksctl`)

The Kubernetes cluster contains:

- React/Nginx frontend
- Four Spring Boot services
- PostgreSQL
- Python remediation CronJob

Only the frontend is publicly exposed through a `LoadBalancer` service. Backend services and PostgreSQL remain internal `ClusterIP` services.

---

## Kubernetes

Kubernetes resources include:

- Namespace
- ConfigMaps
- Secrets
- Deployments
- Services
- PersistentVolumeClaim
- PersistentVolume
- CronJob
- Readiness probes
- Liveness probes
- Resource requests/limits

The Python processor runs periodically using a Kubernetes CronJob. PostgreSQL uses persistent storage through Amazon EBS and the EBS CSI driver.

---

## Docker

All application components are containerized.

- **Java services:** Multi-stage Docker builds compile the Spring Boot application with Maven and produce a smaller runtime image.
- **Frontend:** The React application is built using Node.js and served through Nginx. Nginx also acts as a reverse proxy for backend API requests.
- **Python:** The processor uses a lightweight Python runtime image.

---

## CI/CD

GitHub Actions workflows are separated by application component:

```text
.github/
└── workflows/
    ├── deploy-backend.yml
    ├── deploy-frontend.yml
    └── deploy-python.yml
```

### Backend Pipeline

```mermaid
flowchart LR
    A(["Git push"]) --> B["GitHub Actions"]
    B --> C["Maven build<br/>and tests"]
    C --> D["Docker build<br/>tagged with commit SHA"]
    D --> E[("Amazon ECR")]
    E --> F["Kubernetes<br/>manifest update<br/>Kustomize"]
    F --> G["Amazon EKS<br/>deploy"]
    G --> H(["Rollout<br/>verification"])
    B -. "OIDC, no long-lived keys" .-> AWS["AWS IAM"]

    classDef ci fill:#e3f2fd,stroke:#1565c0,color:#000
    classDef aws fill:#fff3e0,stroke:#ef6c00,color:#000
    classDef ok fill:#e8f5e9,stroke:#2e7d32,color:#000
    class B,C,D ci
    class E,F,G,AWS aws
    class H ok
```

### Image Versioning

Container images are tagged using the Git commit SHA, for example:

```text
orderflow-order-service:a347aaa5e6a1841deb4dcbe2f116b4ca9a5b756b
```

This provides immutable deployment references instead of relying only on the `latest` tag.

GitHub Actions authenticates with AWS using **OIDC** rather than storing long-lived AWS access keys.

---

## Testing

The project includes unit and controller-level tests across the Spring Boot services, covering:

- Order creation
- Validation
- Inventory reservation
- Inventory compensation
- Fulfillment transitions
- Incident creation
- Incident remediation
- Authentication
- JWT generation/validation
- RBAC
- Audit logging
- Python risk rules
- Python incident processing
- REST client behavior

The backend observability implementation was also verified with the service test suites.

---

## Technology Stack

| Category | Technologies |
|---|---|
| Backend | Java 17, Spring Boot, Spring Web, Spring Data JPA, Spring Security, JWT, Hibernate, Maven, Micrometer, Spring Boot Actuator |
| Database | PostgreSQL |
| Frontend | React, TypeScript, Vite, Nginx |
| Automation | Python 3.11+, Requests, Pytest |
| Containers & Orchestration | Docker, Docker Compose, Kubernetes, Kubernetes CronJob |
| AWS | Amazon EKS, Amazon ECR, Amazon EC2, Amazon EBS, AWS Load Balancer, IAM, VPC |
| CI/CD | GitHub Actions, GitHub OIDC, Kustomize |

---

## Project Structure

```text
OrderFlow/
│
├── backend/
│   ├── order-service/
│   ├── inventory-service/
│   ├── fulfillment-service/
│   └── incident-service/
│
├── frontend/
│   └── src/
│
├── python-processor/
│   ├── app/
│   │   ├── client/
│   │   ├── models/
│   │   ├── processor/
│   │   ├── rules/
│   │   └── main.py
│   └── tests/
│
├── infrastructure/
│   ├── aws/
│   ├── docker/
│   └── kubernetes/
│
├── docs/
│   └── architecture.md
│
├── tests/
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## Running Locally

### Prerequisites

- Java 17+
- Maven
- PostgreSQL
- Python 3.11+
- Node.js
- Docker
- Docker Compose

### Start Using Docker Compose

Clone the repository:

```bash
git clone https://github.com/ganesh-2212/orderflow-cloud-platform.git
cd orderflow-cloud-platform
```

Build the services:

```bash
docker compose build
```

Start the platform:

```bash
docker compose up
```

The frontend is available at:

```text
http://localhost:5173
```

Backend services:

| Service | URL |
|---|---|
| Order | http://localhost:8081 |
| Inventory | http://localhost:8082 |
| Fulfillment | http://localhost:8083 |
| Incident | http://localhost:8084 |

---

## Example Workflow

### 1. Register

```http
POST /api/v1/auth/register
```

### 2. Login

```http
POST /api/v1/auth/login
```

Receive a JWT access token.

### 3. Create Inventory

```http
POST /api/v1/inventory
```

```json
{
  "productId": "PROD-001",
  "productName": "Laptop",
  "quantity": 10
}
```

### 4. Create Order

Create an order containing the required product and quantity.

### 5. Inventory Reservation

The Order Service communicates with the Inventory Service. A successful reservation changes:

- Available Quantity ↓
- Reserved Quantity ↑

### 6. Fulfillment

The Order Service creates a fulfillment record, which progresses through:

```text
WAREHOUSE_PROCESSING → READY_FOR_SHIPPING → SHIPPED → DELIVERED
```

### 7. Failure Testing

If an order requests more inventory than available:

```text
Order     → FAILED
Inventory → Compensated
Incident  → OPEN
```

The Python processor can then identify the incident for bounded remediation.

---

## Design Limitations

OrderFlow intentionally uses a relatively lightweight architecture. Current limitations include:

- Service communication is synchronous REST.
- No Kafka/RabbitMQ event broker is used.
- Saga compensation is best-effort rather than fully distributed.
- There is no distributed transaction coordinator.
- JWT token revocation is not implemented.
- Service-to-service JWT authentication is not enforced across every internal service.
- Python remediation is deterministic rather than ML-based.
- Concurrent Python processors could encounter duplicate remediation attempts.
- No centralized log aggregation platform is currently deployed.
- No full service mesh is used.
- CI/CD workflows are implemented with AWS OIDC; deployment mechanics were additionally validated using immutable Git SHA image references.

These limitations are documented intentionally rather than hidden.

---

## Project Objectives

The project demonstrates practical understanding of:

- Microservice architecture
- REST API design
- Spring Boot development
- Database-backed services
- Distributed workflows
- Failure compensation
- Authentication and authorization
- JWT and RBAC
- Audit logging
- Operational incident management
- Python automation
- Containerization
- Kubernetes
- AWS EKS and Amazon ECR
- Persistent cloud storage
- Application observability
- CI/CD
- Immutable container deployments

---

## Author

**Ganesh S**
Computer Science & Engineering

GitHub: [ganesh-2212](https://github.com/ganesh-2212)

---

## License

This project is licensed under the MIT License.