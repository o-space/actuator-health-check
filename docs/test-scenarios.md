# Test Scenarios Documentation

This document describes the comprehensive test scenarios implemented for the health check application, covering
Controller, Service, and Repository layers.

## Overview

The test suite demonstrates:

- **Controller Layer**: MockMvc for REST endpoint testing
- **Service Layer**: Unit tests with mocked dependencies (Repository and RestClient)
- **Repository Layer**: Integration tests with H2 in-memory database
- **Integration Tests**: Testcontainers for real external API calls

## Architecture

### Domain Model

- **HealthCheckRecord**: Entity that stores health check results with service name, status, details, timestamp, and
  response time

### Layers

1. **Repository Layer** (`HealthCheckRecordRepository`)
    - JPA repository with custom query methods
    - Handles all database operations

2. **Service Layer** (`HealthCheckService`)
    - Business logic for health checks
    - Calls external APIs via RestClient
    - Saves results to database via Repository

3. **Controller Layer** (`HealthCheckController`)
    - REST endpoints for health check operations
    - Exposes API for performing checks and retrieving history

## Test Scenarios

### 1. Repository Tests (`HealthCheckRecordRepositoryTest`)

**Technology**: H2 in-memory database with `@DataJpaTest`

**Test Cases**:

- ✅ Save and find health check records
- ✅ Find records by service name
- ✅ Find records by status
- ✅ Find recent records within a time window
- ✅ Find latest record by service name
- ✅ Count records by service name and status
- ✅ Handle empty results gracefully

**Key Features**:

- Uses H2 database configured via `@TestPropertySource`
- Tests JPA repository methods including custom queries
- Verifies database operations work correctly

### 2. Service Tests (`HealthCheckServiceTest`)

**Technology**: Mockito with `@ExtendWith(MockitoExtension.class)`

**Test Cases**:

- ✅ Perform successful health check with UP status
- ✅ Handle failed health check (exception scenarios)
- ✅ Handle non-OK HTTP responses (DEGRADED status)
- ✅ Get health check history
- ✅ Get latest health check for a service
- ✅ Get recent health checks within time window
- ✅ Get failure count for a service

**Key Features**:

- Mocks `HealthCheckRecordRepository` to isolate service logic
- Mocks `RestClient` to simulate external API calls
- Verifies business logic without database or network dependencies
- Tests error handling and edge cases

### 3. Controller Tests (`HealthCheckControllerTest`)

**Technology**: MockMvc with `@WebMvcTest`

**Test Cases**:

- ✅ Perform health check via POST endpoint
- ✅ Return bad request when parameters are missing
- ✅ Get health check history via GET endpoint
- ✅ Get latest health check for a service
- ✅ Return 404 when no latest health check exists
- ✅ Get recent health checks with custom hours parameter
- ✅ Get recent health checks with default hours (24)
- ✅ Get health check statistics

**Key Features**:

- Uses MockMvc to test REST endpoints without starting full application
- Mocks service layer to focus on controller behavior
- Tests HTTP status codes, request/response bodies, and JSON structure
- Verifies endpoint parameter handling

### 4. Integration Tests (`HealthCheckIntegrationTest`)

**Technology**: Testcontainers with WireMock container

**Test Cases**:

- ✅ Perform health check with real external API (WireMock container)
- ✅ Handle failed external API calls
- ✅ Save and retrieve health check history from database
- ✅ Get latest health check from database
- ✅ Count failures in database
- ✅ Get recent health checks from database

**Key Features**:

- Uses Testcontainers to spin up WireMock in Docker
- Tests real HTTP calls to external services
- Uses H2 database for persistence testing
- Verifies end-to-end flow: API call → Service → Repository → Database
- Tests integration between all layers

## Running the Tests

### All Tests

```bash
./gradlew test
```

### Specific Test Class

```bash
./gradlew test --tests HealthCheckRecordRepositoryTest
./gradlew test --tests HealthCheckServiceTest
./gradlew test --tests HealthCheckControllerTest
./gradlew test --tests HealthCheckIntegrationTest
```

### With Testcontainers

The integration tests require Docker to be running, as they use Testcontainers to start WireMock containers.

## Dependencies

### Test Dependencies Added

- `com.h2database:h2` - In-memory database for repository tests
- `org.testcontainers:junit-jupiter` - Testcontainers JUnit 5 support
- `org.testcontainers:testcontainers` - Core Testcontainers library
- `org.testcontainers:wiremock` - WireMock Testcontainers module

## Test Coverage

The test suite covers:

- ✅ Database operations (CRUD, queries, counts)
- ✅ External API calls (success, failure, error handling)
- ✅ REST endpoint behavior (GET, POST, parameters, status codes)
- ✅ Business logic (health check processing, status determination)
- ✅ Error handling (exceptions, invalid inputs, missing data)
- ✅ Integration scenarios (end-to-end flows)

## Best Practices Demonstrated

1. **Separation of Concerns**: Each layer tested independently
2. **Isolation**: Unit tests use mocks, integration tests use real components
3. **Test Data**: Each test sets up its own data and cleans up
4. **Assertions**: Clear, descriptive assertions using AssertJ
5. **Naming**: Test methods clearly describe what they test
6. **Given-When-Then**: Tests follow AAA (Arrange-Act-Assert) pattern
