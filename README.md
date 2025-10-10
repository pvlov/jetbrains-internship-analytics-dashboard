# Analytics Dashboard API

This project is a submission for the Jetbrains internship project "Backend for main analytics dashboard"

## How to Run the Application

To run the application, you can use the following command:

```bash
./gradlew bootRun
```

## How to Run the Tests

To run the tests, you can use the following command:

```bash
./gradlew test
```

## Design- and Implementation decisions

### Assumptions

*   **Statelessness:** The application is assumed to be truly stateless, with all the necessary data stored in memory during the application run.
*   **Infinite Ressources:** the applications somewhat expects an infinite amount of memory.
*   **Trusted Environment:** The application is assumed to be running in a trusted environment, with no authentication or authorization required.
*   **API Stability:** The API does not require a special format or any form of stable representation. The API has no cross-functional or third-party consumers.
*   **SQL Dialect:** The application assumes that the SQL queries are written in a dialect that is compatible with the H2 database.

### Overview

The application is built using a standard, layered architecture common in Spring. This structure is composed of:

- Controller Layer: Responsible for handling incoming HTTP requests, parsing and validating user input, and delegating the request to the appropriate service.
- Service Layer: Contains the core business logic. It orchestrates operations, processes data, and coordinates between the controller and persistence layers.
- Persistence Layer: Manages all data access and interaction with the database, handling tasks like querying, saving, and updating records.

The main difference to regular Spring MVC is the use of Spring Webflux which is built on top of project reactor to allow for reactive, non-blocking asynchronous programming that can handle a high number of concurrent users with minimal resources. 
It moves away from the classic "one-thread-per-request" model used by Spring MVC and towards an event-driven architecture. The more interesting aspects of the project are highlighted and discussed below.

### API Endpoints

*   `POST /queries`: Adds a new SQL query. It expects `{"text": "..."}` as the request body, where text is the query that should be stored.
*   `GET /queries`: Lists all saved queries.
*   `GET /poll/{id}`: Starts the execution or polls the result of the associated query, more on that in the next section.

### Configuration
All details and timouts mentioned later in this section are configurable in the `application.yaml` properties file.

### Input Validation & Read/Write Separation
The make sure the API is somewhat secure against bad agents, the API implements a two-step system of verifying the queries and securing the data:

- At the "edge" of the API, the application uses an AST-Parser for SQL to filter out malformed queries and immediately notify the client that their query will produce an error. It also checks that the AST represents a `SELECT`-statement to filter out more basic cases of injections. 
- The url or rather database user of the application only has read-only access to the database, so any queries that would try to execute DDL or DML statements would be stopped by the DBMS. To make sure that the application can still run the migrations on start-up, the app manages two connection factories, one with read-write privileges and one with only read privileges. The read-only connection is designated as the primary to ensure other (spring-)dependencies only autowire the read-only connection factory. The read-write connection factory is only used in the initial seeding stage when running the migrations. 

The API uses this to make sure to return sensible error messages and statuses when encountering validation errors.

### Long-Running Queries & Caching
To handle long-running queries without leaving the connection open for a long time or block the client, the API is designed as a job-system. Instead of simply executing the query synchronously, a job
begins running in the background executing the query. In the mean-time the client receives a `Pending` result from the API indicating that the query is being processed. The client can continuously poll
the API to check on the status of the query until it either receives a `Success` with the output or an `Error` indicating something went wrong. 

There are some caveats here tho:
- Queries get cached based on the actual content not their ID, meaning that if someone else saved and executed the same query text before you, you instantly get the result.
- For short-running queries the API allows for a small (configurable) grace period (i.e. 100-300ms) where the client will also receive the output without having to poll again.
- The API only caches queries, but does not persist the results permanently. The result could very easily just be saved in the database, but I opted for a cache to be a bit more realistic and to make the implementation more interesting by mixing reactive and blocking code.
- To make sure that a request does not run endlessly, the background jobs are cancelled with an error after a (configurable) max duration.

### Testing

The application has a lot of integration tests, testing the various flows and boundaries of the application. The focus here is on actual integration tests and not just Unit-Tests with endless mocks.

Here is the basic rundown of each test:
- `ApplicationLifecycleTest`: This set of integration tests make sure that the general spring application is able to spin up and report its health using actuator.
- `QueryValidationTest`: This set of integration tests validates that the validation done by the controller is done correctly by trying various queries and making sure they fail validation.
- `QueryExecutionTest`: This set of integration tests do full end-to-end flow testing to make sure that the API behaves as expected, especially in regard to long-running queries, caching and persistence.

### Utilities and Nice-to-haves

- The `scripts` folder contains a Python script (`fake-titanic.py`) that uses the `Faker` library to generate a large volume of realistic-looking fake data for the Titanic dataset. The application by default will load 100_000 additional rows to allow for more interesting queries.
- The application also uses the more modern version catalogues for easier dependency management.


---

## Limitations/Problems with the design and how to tackle them

- **Unauthorized data modification**
  * Even with the AST validation and making sure the connection is read-only, a user may accidentally gain privileges or a DBMS account of someone/something with write-access could get phished or otherwise exposed. To safeguard against this, you could employ a read replica (slave) which would get its data pushed by a master node which accepts writes through a secure channel.

- **(D)DOS:**
  * Even with a timout limiting the execution time of long-running background jobs, an attacker could still submit an endless amount of long-running queries that just scrape by the limit. You should add rate-limiting here.

- **No Query-Cancellation:**
  * It is not possible to cancel running queries. You could simply kill the running job for a provided ID.

- **Multi-Node Deployment/In-Memory persistence**:
  * The current implementation is not suitable for multi-node deployments as queries would run n-times or worse. It would also cost n-times the amount of memory, if the requests are distributed perfectly even. You can solve this by using Redis/Valkey/Any KV-Store instead of an in-memory cache to make sure it stays synchronized and doesn't overuse memory. It would provide a shared cache for all pods, improving performance and consistency. Redis could also be used as a message broker for a more advanced solution for long-running queries, where the server pushes a notification to the client when the query is complete, instead of relying on polling. You should also deploy and use and actual DBMS like Postgres instead of H2.

- **Missing Authentication and Authorization**:
  * The API is currently unauthenticated. In a production system, you would want to add authentication and authorization to control who can access the API. This can be as simple as putting keycloak or any other identity service in front of the application or as complicated as managing different DBMS account per log-in to make sure that you can restrict users to certain database schemas.
    
- **Telemetry**:
  * The implementation does not contain any form of telemetry to observe the application. You would usually opt for the usual LGTM-Stack or similar and use Open-Telemetry in the application to produce telemetry.

- **CI/CD**:
  * There is no continuous integration apart from tests and no deployment of course.

