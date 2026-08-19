# SmartLog Backend — Codex Engineering Guidelines

## 1. Project Context

SmartLog is a logistics management system developed for a university project.

Backend stack:

* Java
* Spring Boot
* Spring Security
* Firebase Authentication
* Firebase Admin SDK
* Cloud Firestore
* Bean Validation
* OpenAPI / Swagger
* JUnit 5
* Mockito
* Spring Boot Test

Frontend clients:

* Flutter
* Angular

The backend is the only application layer responsible for protected access to Firestore.

## 2. Architectural Decisions

The backend follows:

```text
Controller
    ↓
Application / Service
    ↓
Domain
    ↓
Repository
    ↓
Firestore
```

Do not allow controllers to access Firestore directly.

Do not put business rules inside controllers.

Do not couple domain models directly to Firebase SDK classes.

Infrastructure-specific code must remain inside the infrastructure layer.

## 3. Authentication

Authentication is handled by Firebase Authentication.

Requests to protected endpoints use:

```http
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

The backend must validate the Firebase ID Token.

The authenticated context must expose:

* uid
* role
* empresaId

Never trust `empresaId` received from the client.

The backend must derive tenant identity from the authenticated context.

## 4. Authorization

The system uses RBAC with:

* ADMIN
* MOTORISTA
* CLIENTE

Authorization must enforce both:

1. role permissions;
2. resource ownership / tenant scope.

### ADMIN

Can operate on resources belonging to their own company.

### MOTORISTA

Can only access operational resources belonging to the authenticated driver and company.

### CLIENTE

Can only access their own deliveries/order history.

Never implement authorization only in the frontend.

## 5. Multi-Tenancy

Firestore structure:

```text
empresas/{empresaId}/
    clientes/{clienteId}
    motoristas/{motoristaId}
    rotas/{rotaId}
    entregas/{entregaId}
```

The authenticated `empresaId` must always determine the Firestore tenant path.

Never accept arbitrary `empresaId` from request bodies.

## 6. API

Base path:

```text
/api/v1
```

Resources:

* `/entregas`
* `/rotas`
* `/clientes`
* `/motoristas`

Do not create endpoints that are not required by the architecture or application use cases.

Do not create generic CRUD endpoints without a demonstrated requirement.

## 7. DTOs

Do not expose persistence models directly through REST.

Use dedicated request and response DTOs.

Request DTOs must contain only fields that clients are allowed to provide.

Server-controlled fields must not be accepted from clients.

Examples:

* empresaId
* timestamps
* authenticated uid
* ownership fields

must be controlled by the backend where applicable.

## 8. Validation

Use Jakarta Bean Validation.

Invalid requests must produce a consistent error response.

The API must distinguish:

* 400 Bad Request
* 401 Unauthorized
* 403 Forbidden
* 404 Not Found
* 409 Conflict
* 500 Internal Server Error

Never expose stack traces or internal infrastructure details.

## 9. Error Contract

Use a consistent error structure:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dados da requisição inválidos.",
  "timestamp": "2026-08-19T13:00:00Z",
  "path": "/api/v1/entregas"
}
```

## 10. Firestore

Firestore access must be isolated behind repository abstractions.

Do not leak Firebase SDK types into domain/application layers.

Repositories are responsible for:

* document paths
* queries
* persistence
* mapping

## 11. Testing

Every implemented feature must include tests.

Prioritize:

1. unit tests for business rules;
2. controller/API tests;
3. authorization tests;
4. repository tests where practical;
5. integration tests for critical flows.

Tests must verify security boundaries, not only happy paths.

Examples:

* ADMIN accessing own tenant;
* ADMIN attempting another tenant;
* MOTORISTA accessing another driver's delivery;
* CLIENTE accessing another client's delivery;
* missing token;
* invalid token;
* insufficient role;
* invalid request;
* nonexistent resource.

## 12. API Documentation

Swagger/OpenAPI must describe:

* endpoints
* parameters
* request bodies
* responses
* authentication
* authorization requirements
* error responses

Documentation must reflect the actual implementation.

Never document behavior that is not implemented.

## 13. Coding Standards

Prefer simple, explicit code.

Avoid premature abstractions.

Avoid speculative features.

Avoid unnecessary dependencies.

Do not introduce a framework/library when the existing Spring ecosystem already solves the problem.

Prefer constructor injection.

Prefer immutable DTOs where appropriate.

Keep methods focused.

Do not suppress compiler warnings without justification.

Do not disable tests to make builds pass.

Do not modify unrelated files.

## 14. Definition of Done

An issue is complete only when:

* implementation is complete;
* architecture is respected;
* tests pass;
* relevant tests were added;
* formatting/build passes;
* no unrelated changes were introduced;
* API documentation is updated when applicable;
* security boundaries are tested;
* the implementation matches the SmartLog architecture documents.

## 15. Working Method

Before modifying code:

1. inspect the existing repository;
2. understand current architecture;
3. identify affected modules;
4. state the implementation plan;
5. implement the smallest coherent change;
6. run relevant tests;
7. run the full test suite when appropriate;
8. inspect the final diff;
9. report what changed, tests executed, and remaining risks.

Do not rewrite working code unnecessarily.

Do not assume requirements that are not present in the issue, architecture documents, or existing code.
