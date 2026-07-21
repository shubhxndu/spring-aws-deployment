# AWS Assignment Task API

A Spring Boot task manager deployed to the AWS Elastic Beanstalk Docker platform with Amazon RDS for PostgreSQL. The repository also contains the S3 static asset and the Lambda handler used by the assignment's S3-to-SQS-to-Lambda event flow.

Repository: [github.com/shubhxndu/spring-aws-deployment](https://github.com/shubhxndu/spring-aws-deployment)

## Architecture

The application uses a straightforward layered structure:

- REST controllers handle HTTP requests and responses.
- A service contains the task CRUD operations.
- Spring Data JPA persists `Task` entities.
- PostgreSQL stores local and production data.
- Spring Boot Actuator exposes application health.
- Amazon S3 publishes object-created notifications to Amazon SQS.
- AWS Lambda consumes the SQS messages and records S3 event details in CloudWatch Logs.
- GitHub Actions tests, builds, and deploys the application through short-lived AWS OIDC credentials.

The application does not call AWS APIs directly. S3, SQS, Lambda, RDS, and Elastic Beanstalk are configured as separate managed AWS resources.

## Deployed architecture

```text
Internet -> Elastic Beanstalk (single EC2 instance) -> private Amazon RDS PostgreSQL

S3 uploads/ -> SQS queue -> Lambda -> CloudWatch Logs
S3 static/  -> public read-only static files

GitHub Actions -> AWS OIDC role -> Elastic Beanstalk deployment
```

## Prerequisites

- Docker Desktop or Docker Engine with Docker Compose
- Optional for running Maven directly: Java 21 and Maven 3.9+
- `curl` for API verification

## Run with Docker Compose

Create a private local environment file from the committed template:

```bash
cp .env.example .env
```

On PowerShell, use:

```powershell
Copy-Item .env.example .env
```

Open `.env` and set `DB_PASSWORD` to a local-only PostgreSQL password. The `.env` file is ignored by Git and must never be committed.

Then build and start PostgreSQL and the application:

```bash
docker compose up --build
```

The application is available at:

```text
http://localhost:8080
```

Compose uses the PostgreSQL service name `postgres` as `DB_HOST`. It also creates a named `postgres_data` volume, so task data remains after container restarts.

Verify the running services:

```bash
curl http://localhost:8080/
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/tasks
```

## Stop the application

Stop and remove containers while keeping the database volume:

```bash
docker compose down
```

Also remove the database volume and all local task data:

```bash
docker compose down --volumes
```

## Run tests

With Java 21 and Maven installed:

```bash
mvn test
```

Or run the tests in a Java 21 Maven container:

```bash
docker run --rm -v maven-cache:/root/.m2 -v "$PWD:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-21 mvn test
```

Tests use an in-memory H2 database in PostgreSQL compatibility mode. They do not require a running PostgreSQL instance.

## API endpoints

| Method | Endpoint | Description | Success status |
| --- | --- | --- | --- |
| GET | `/` | Application name and running status | 200 |
| GET | `/actuator/health` | Application and database health | 200 |
| GET | `/api/tasks` | List all tasks | 200 |
| GET | `/api/tasks/{id}` | Retrieve one task | 200 |
| POST | `/api/tasks` | Create a task | 201 |
| PUT | `/api/tasks/{id}` | Update a task | 200 |
| DELETE | `/api/tasks/{id}` | Delete a task | 204 |

Missing tasks return `404`. Invalid requests return `400` with a JSON error body.

## Curl examples

Application information:

```bash
curl http://localhost:8080/
```

Actuator health:

```bash
curl http://localhost:8080/actuator/health
```

List all tasks:

```bash
curl http://localhost:8080/api/tasks
```

Retrieve task `1`:

```bash
curl http://localhost:8080/api/tasks/1
```

Create a task:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test AWS deployment",
    "description": "Verify application and database connectivity"
  }'
```

Update task `1`:

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test AWS deployment",
    "description": "Elastic Beanstalk and RDS verified",
    "completed": true
  }'
```

Delete task `1`:

```bash
curl -i -X DELETE http://localhost:8080/api/tasks/1
```

## Environment variables

| Variable | Local default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | PostgreSQL hostname or RDS endpoint |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `taskdb` | Database name |
| `DB_USERNAME` | `appadmin` | Database user |
| `DB_PASSWORD` | No default; required | Database password supplied at runtime |
| `PORT` | `8080` | HTTP listening port |
| `SEED_DATA_ENABLED` | `true` | Create two sample tasks if the database is empty |

The committed `.env.example` contains no credential value. Do not commit `.env`, AWS access keys, API keys, private keys, tokens, or production credentials. The repository also ignores common credential and private-key file formats.

## Amazon RDS connection

For AWS, set `DB_HOST` to the RDS PostgreSQL endpoint, `DB_PORT` to `5432`, and set the database name and credentials to values created for the RDS instance. The JDBC URL is assembled from these environment variables at startup.

Place RDS in private subnets. Elastic Beanstalk instances must have network routes to those subnets. Configure the RDS security group to accept TCP port `5432` from the Elastic Beanstalk instance security group, not from the public internet.

Use a dedicated application database user. Supply its password at deployment time through Elastic Beanstalk environment configuration or an approved secrets workflow; never put production credentials in Git, source bundles, screenshots, logs, or the Docker image.

## Elastic Beanstalk configuration

1. Create an Elastic Beanstalk environment using the current Docker platform.
2. The GitHub Actions workflow verifies the full `Dockerfile`, builds the application JAR, and packages it with `Dockerfile.elasticbeanstalk` for deployment. This avoids compiling Java on the small Elastic Beanstalk instance.
3. In the Elastic Beanstalk console, open **Configuration**, then configure environment properties.
4. Add `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `PORT` (normally `8080` for this container).
5. Set `SEED_DATA_ENABLED=false` when sample production data is not wanted.
6. Configure the environment health-check path as `/actuator/health`.
7. Enable log streaming to CloudWatch Logs and set an appropriate retention period.

Do not deploy `docker-compose.yml` as the production topology. It is for local development; production uses Elastic Beanstalk plus the separate RDS instance.

## Basic AWS deployment notes

- The Docker image runs as a non-root user and listens on the configurable `PORT`.
- The container health check calls `/actuator/health`, which includes database connectivity.
- `spring.jpa.hibernate.ddl-auto=update` is intentional for this academic assignment.
- Keep the Elastic Beanstalk environment and RDS in the same VPC, with RDS in private subnets.
- Use Elastic Beanstalk application logs and enhanced health plus CloudWatch Logs for startup, API, and failure diagnosis.
- `.github/workflows/deploy.yml` tests and builds every pull request and deploys pushes to `main`.
- Deployment uses GitHub Actions OIDC with the repository variable `AWS_DEPLOY_ROLE_ARN`; no long-lived AWS access key is stored in GitHub.
- The application does not need AWS SDK dependencies because it does not call AWS services directly.

## S3, SQS, and Lambda

The static example file is stored at `static/index.html`. In AWS, only objects under the bucket's `static/` prefix are granted public `s3:GetObject` access.

The Lambda console handler is stored at `lambda/sqs_logger/lambda_function.py`. It accepts SQS batches, parses the nested S3 event, URL-decodes the object key, and logs the message ID, event name, bucket, and key. It also returns failed SQS message identifiers for partial-batch retry handling.

Run its dependency-free unit test with:

```bash
python -m unittest discover -s lambda/sqs_logger -v
```

The Lambda execution role needs `AWSLambdaSQSQueueExecutionRole`, and the SQS event source mapping should enable `ReportBatchItemFailures`.

## Logs

Follow all local Compose logs:

```bash
docker compose logs -f
```

Follow only application logs:

```bash
docker compose logs -f app
```

Follow only PostgreSQL logs:

```bash
docker compose logs -f postgres
```

Show the last 100 application log lines:

```bash
docker compose logs --tail=100 app
```

Inspect container state and health:

```bash
docker compose ps
docker inspect --format='{{json .State.Health}}' aws-assignment-training-app-1
```

## Troubleshooting

### Port already in use

Stop the process or container using ports `8080` or `5432`, then run Compose again:

```bash
docker ps
docker compose down
```

### Application cannot connect to PostgreSQL

Confirm `DB_PASSWORD` is set in the ignored `.env` file, PostgreSQL is healthy, and the app uses `DB_HOST=postgres` under Compose:

```bash
docker compose ps
docker compose logs postgres
docker compose exec app printenv DB_HOST DB_PORT DB_NAME DB_USERNAME
```

Do not print `DB_PASSWORD` in logs or troubleshooting output.

### Health check is failing

Check the endpoint and application logs:

```bash
curl -i http://localhost:8080/actuator/health
docker compose logs --tail=200 app
```

The health response includes database status. A database connection failure makes the overall health status unhealthy.

### Rebuild after source changes

```bash
docker compose up --build --force-recreate
```

### Reset the local database

This permanently removes local task data:

```bash
docker compose down --volumes
docker compose up --build
```

### RDS connection fails on AWS

Check the RDS endpoint and port, VPC/subnet routing, security-group rules, database credentials, and whether the RDS instance is available. Review Elastic Beanstalk and CloudWatch logs for the JDBC connection error without exposing credentials.
