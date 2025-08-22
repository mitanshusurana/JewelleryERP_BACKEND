Gemstone ERP - Backend (v2.0 Detailed)
This repository contains the source code for the backend services of the Gemstone ERP platform. It is built using a microservices architecture with Spring Boot, designed for scalability, maintainability, and high performance. This document serves as the primary technical guide for backend developers.
Table of Contents
1. Core Principles & Architecture
2. Microservice Deep Dive
3. Technology Stack & Rationale
4. API Design & Communication Protocol
5. Database Schema & Design
6. Security Implementation
7. Logging & Error Handling
8. DevOps & Deployment
9. Getting Started (Developer Guide)
1. Core Principles & Architecture
The backend follows a Domain-Driven Design (DDD) approach, implemented as a Microservices Architecture. Each service is a self-contained application representing a specific business capability (Bounded Context).
Loose Coupling: Services should know as little as possible about each other. Communication is achieved via a well-defined API Gateway and an asynchronous message bus. Direct service-to-service synchronous calls are strictly forbidden to prevent cascading failures.
High Cohesion: Each service has a single, well-defined responsibility. All logic related to that responsibility resides within that service.
Event-Driven: State changes within the system are broadcast as events. For instance, when a product's price is updated, the pricing-service emits a PriceUpdatedEvent. Other services can then react to this event without the pricing-service needing to know about them. This creates a highly extensible and resilient system.
Single Entry Point: All external traffic (from the frontend or third-party integrations) MUST go through the gateway-service. This centralizes concerns like authentication, rate-limiting, and logging.
2. Microservice Deep Dive
Service 1: inventory-service
Responsibility: The authoritative source for all product data.
Key Endpoints (via Gateway):
POST /api/inventory/products: Create a new product.
GET /api/inventory/products/{id}: Retrieve a single product by its ID.
PUT /api/inventory/products/{id}: Update an existing product's details.
GET /api/inventory/products/search: Perform complex searches (delegated to Elasticsearch).
GET /api/inventory/schemas/{product_type}: Fetch the dynamic form schema for a product type.
POST /api/inventory/media/presigned-url: Generate a pre-signed S3 URL for direct media upload from the client.
Published Events (to RabbitMQ):
ProductCreatedEvent: When a new product is successfully saved.
ProductUpdatedEvent: When product details (excluding price) are changed.
ProductDeletedEvent: When a product is removed.
Service 2: pricing-service
Responsibility: Manages all pricing, cost, and markup logic.
Key Endpoints (via Gateway):
PUT /api/pricing/products/{id}/cost: Set or update the base cost price of a product.
GET /api/pricing/products/{id}/price: Get the calculated selling price for a product, potentially for a specific customer tier.
Subscribed Events (from RabbitMQ):
ProductCreatedEvent: To create an initial pricing record for the new product.
Published Events (to RabbitMQ):
PriceUpdatedEvent: When a product's selling price changes due to cost or rule updates.
Service 3: syndication-service
Responsibility: Pushes product data to external sales channels.
Key Endpoints (via Gateway):
POST /api/syndication/products/{id}/publish: Trigger a manual push to a specific channel.
GET /api/syndication/products/{id}/status: Check the publication status across all channels.
Subscribed Events (from RabbitMQ):
ProductUpdatedEvent, PriceUpdatedEvent: Listens for any relevant product change to trigger an update on external platforms if the product is marked for syndication.
Service 4: gateway-service
Responsibility: Securely route all incoming traffic.
Configuration: Uses route definitions to map paths like /api/inventory/** to the inventory-service. It will also be responsible for attaching authenticated user information to the request headers before forwarding them to downstream services.
3. Technology Stack & Rationale
Framework: Spring Boot 3+ (Provides rapid development, dependency management, and a robust ecosystem).
Language: Kotlin 1.8+ (Chosen for its conciseness, null-safety, and full Java interoperability, leading to more readable and less error-prone code).
Data Access: Spring Data JPA / Hibernate (Abstracts away boilerplate JDBC code, providing a clean repository pattern).
Search: Elasticsearch (Provides near real-time, powerful full-text search and aggregation capabilities that are impossible with a traditional RDBMS).
Messaging: RabbitMQ (A mature, reliable message broker that is perfect for our event-driven architecture).
API Gateway: Spring Cloud Gateway (The standard, reactive, and highly configurable gateway solution within the Spring ecosystem).
Security: Spring Security with JWT (Stateless, secure, and widely adopted standard for securing APIs).
4. API Design & Communication Protocol
API Style: RESTful API using JSON.
Versioning: APIs will be versioned in the URL, e.g., /api/v1/inventory/....
Specification: Each microservice will expose an OpenAPI 3.0 (Swagger) specification at /v3/api-docs. This is non-negotiable and must be kept up-to-date with the code.
Payload Naming Convention: Use camelCase for all JSON properties.
5. Database Schema & Design
Each service owns its data. There is no direct database access between services.
inventory-db (PostgreSQL):
products table:
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| id | UUID | Primary Key | Unique product identifier |
| qr_code_id | VARCHAR(255) | Unique, Not Null | The ID scanned from the physical tag |
| product_type | VARCHAR(50) | Not Null | e.g., 'LOOSE_GEMSTONE', 'JEWELRY' |
| name | TEXT | | AI-generated name |
| description | TEXT | | AI-generated description |
| created_at | TIMESTAMPTZ | Not Null | |
| updated_at | TIMESTAMPTZ | Not Null | |
product_attributes table: (Key-Value store for dynamic properties)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | Primary Key | |
| product_id | UUID | Foreign Key to products.id | |
| attribute_name| VARCHAR(100)| Not Null | e.g., 'carat', 'color', 'metal_type' |
| attribute_value| VARCHAR(255)| Not Null | e.g., '2.5', 'G', 'GOLD' |
6. Security Implementation
Authentication: The gateway-service will handle authentication. It will expose a /login endpoint that, upon successful credential validation, returns a JWT.
Authorization: Every subsequent request to the gateway must include the JWT in the Authorization: Bearer <token> header. The gateway will validate the token's signature and expiration.
Roles & Permissions: User roles (e.g., ADMIN, DATA_ENTRY_OPERATOR) will be encoded within the JWT payload. Downstream services can inspect these roles to authorize specific actions.
Secret Management: No secrets (API keys, passwords) will be stored in the codebase. They will be injected as environment variables or managed by a service like HashiCorp Vault or AWS Secrets Manager.
7. Logging & Error Handling
Logging: Use a structured logging format (JSON) with a library like Logback. This allows for easy parsing and searching in a centralized logging platform (e.g., ELK Stack, Datadog).
Correlation ID: The gateway-service will generate a unique X-Correlation-ID for every incoming request. This ID will be passed down to all downstream services and included in every log message, allowing us to trace a single request's journey through the entire system.
Error Handling: A standardized error response format will be used for all API failures:
{
  "timestamp": "2025-08-22T10:30:00.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product with ID 'xyz' not found.",
  "path": "/api/v1/inventory/products/xyz"
}


8. DevOps & Deployment
Containerization: Every microservice will have a Dockerfile for creating a production-ready image.
Orchestration: Kubernetes (K8s) is the target deployment environment. Each service will have a corresponding Helm chart for managing its K8s manifests.
CI/CD: GitHub Actions will be used. A typical pipeline will consist of:
On Pull Request: Build -> Run Unit & Integration Tests.
On Merge to main: Build Docker Image -> Push to Registry (e.g., Docker Hub, ECR) -> Deploy to Staging Environment.
On Git Tag: Promote build from Staging to Production.
9. Getting Started (Developer Guide)
Prerequisites:
JDK 17+
Maven / Gradle
Docker & Docker Compose
An IDE (IntelliJ IDEA recommended for Kotlin/Spring).
Environment Setup:
Clone the repository.
Create an application.properties file in the resource directory of each service (copy from .example file if provided).
Start the core infrastructure: docker-compose up -d
Running a Service:
Open the service's project in your IDE.
The IDE should automatically detect it as a Spring Boot application.
Run the main application class. The service will connect to the Dockerized infrastructure.
