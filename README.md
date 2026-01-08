# canonical-logs-example-springboot

Example Spring Boot application demonstrating canonical structured JSON logging.

## What is Canonical Logging?

This example demonstrates a production-ready approach to structured logging where:
- **Each HTTP request emits exactly one canonical structured JSON log line**
- That one log line contains:
  - Common request attributes (method, route template, status, duration, request id)
  - Common deployment attributes (service, env, region, version)
  - Controller-added attributes (domain identifiers from path or headers)
  - Service-added attributes (work details, timings, output summary)
- **Thread-safe for concurrent requests** because every request gets its own independent "record under construction" (no static mutable state, no shared maps)

## Key Concept: Request-Scoped Context

In Spring MVC, the cleanest "idiomatic" way to share per-request state across controller and services is a `@RequestScope` bean. Each concurrent request gets a different instance, so concurrent requests never touch the same map.

## Architecture

### Core Components

1. **CanonicalLogContext** - Request-scoped bean that holds the log data structure being built across layers
2. **CanonicalInitFilter** - Filter that initializes baseline request attributes early in the request lifecycle
3. **CanonicalEmitInterceptor** - Interceptor that finalizes and emits exactly one JSON log line per request
4. **CanonicalLogger** - Dedicated logger component (keeps intent clear)
5. **AppRuntimeProperties** - Configuration properties for deployment attributes (env, region, version)

### Example Implementation

- **TurboEncabulatorController** - Shows how controllers add domain and request context
- **TurboEncabulatorService** - Shows how services add processing details

#### Available Endpoints

1. **POST /v1/turboencabulators/{id}/runs** - Compute a turboencabulator run value
   - Demonstrates basic structured logging with request-scoped context
   
2. **POST /v1/turboencabulators/{id}/churn** - Execute a background churn task
   - Demonstrates how background tasks executed via an executor pool can add data to the canonical HTTP request log
   - The task generates a random sleep time (1-1000ms) and logs the `churn_time` field from the background thread
   - Shows thread-safe handling of request-scoped beans by propagating RequestAttributes to background threads
   - The `churn_time` and `churn_thread` fields appear in the same canonical log entry as the HTTP request
   - Key technique: Passing the `CanonicalLogContext` to background threads by propagating `RequestContextHolder` attributes

## Sample Canonical Log Output

```json
{
    "ts_start": "2026-01-07T23:31:47.084272336Z",
    "service": "turboencabulator-service",
    "env": "prod",
    "region": "us-east-1",
    "version": "2026.01.07.1",
    "kind": "http",
    "request_id": "req_410e6f0a-b55a-4666-98c4-ab52a713d34f",
    "http.method": "POST",
    "http.target": "/v1/turboencabulators/turbo-123/runs",
    "http.route": "/v1/turboencabulators/{id}/runs",
    "turboencabulator.id": "turbo-123",
    "user_id": "user-456",
    "compute.strategy": "random",
    "compute.duration_ms": 0,
    "turboencabulator.value": 4.072070056910634,
    "ts": "2026-01-07T23:31:47.148807915Z",
    "duration_ms": 64,
    "http.status_code": 200,
    "outcome": "success"
}
```

## Why This is Thread-Safe for Concurrent Requests

- `CanonicalLogContext` is `@RequestScope`, so each request gets a distinct instance and distinct map
- No static mutable state. No shared builders.
- Optional hardening inside one request: `synchronizedMap` plus snapshot copy
- Uses `ThreadLocalRandom` instead of shared `Random` instance

### Background Task Thread Safety with Request-Scoped Beans

The `/churn` endpoint demonstrates thread-safe handling of background tasks that need to access request-scoped beans:

**The Challenge**: Request-scoped beans (like `CanonicalLogContext`) are proxies that resolve to thread-local storage. Background threads in an executor pool don't have access to the HTTP request's thread-local context.

**The Solution**: Propagate `RequestAttributes` from the request thread to background threads using `RequestContextHolder`:

```java
// In the request thread, capture the request attributes
RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

// In the background thread, set the request attributes
executor.submit(() -> {
    RequestContextHolder.setRequestAttributes(requestAttributes);
    try {
        // Now request-scoped beans can be accessed via their proxies
        context.put("churn_time", churnTimeMs);
    } finally {
        RequestContextHolder.resetRequestAttributes();
    }
});
```

**Key Benefits**:
- Background threads can safely access request-scoped beans
- The same `CanonicalLogContext` instance is shared between request and background threads
- Thread-safe due to `synchronizedMap` in `CanonicalLogContext`
- Multiple concurrent requests can each dispatch tasks without interference
- Clean separation: each request's background tasks only access that request's context

## Running the Application

```bash
mvn spring-boot:run
```

## Testing the Canonical Logging

### Basic Turboencabulator Run

```bash
# Make a request with user ID
curl -X POST http://localhost:8080/v1/turboencabulators/turbo-123/runs \
  -H "X-User-Id: user-456" \
  -H "Content-Type: application/json"

# Make a request without user ID
curl -X POST http://localhost:8080/v1/turboencabulators/turbo-789/runs \
  -H "Content-Type: application/json"
```

### Churn Endpoint (Background Task with Canonical Logging)

```bash
# Execute a background churn task with user ID
curl -X POST http://localhost:8080/v1/turboencabulators/turbo-999/churn \
  -H "X-User-Id: churn-user" \
  -H "Content-Type: application/json"

# Execute a background churn task without user ID
curl -X POST http://localhost:8080/v1/turboencabulators/turbo-888/churn \
  -H "Content-Type: application/json"
```

Check the application logs to see a **single** canonical JSON log entry for the HTTP request that includes:
- Standard HTTP request fields (method, path, status, duration, etc.)
- The `churn_time` field with the random sleep duration (logged from the background thread)
- The `churn_thread` field showing which executor thread handled the task
- All in one unified log entry, not separate log records

## Running Tests

```bash
mvn test
```

## Configuration

Deployment attributes are configured in `src/main/resources/application.yml`:

```yaml
app:
  env: prod
  region: us-east-1
  version: 2026.01.07.1
```

## Benefits

1. **Observability** - One log line per request with complete context
2. **Searchability** - Structured JSON makes it easy to query and analyze
3. **Traceability** - Request IDs link logs across distributed services
4. **Performance** - Minimal overhead, emits exactly once
5. **Thread Safety** - No race conditions or data corruption across concurrent requests

