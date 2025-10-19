# CI/CD Configuration for DistributedSkiResortSystem

This directory contains CI/CD configuration files for testing the DistributedSkiResortSystem with the custom CI/CD pipeline system.

## Files

- `cicd.yaml` - Complete CI/CD pipeline configuration with all stages
- `cicd-simple-test.yaml` - Simplified test configuration for database and RabbitMQ validation
- `test-cicd.sh` - Test script to run the CI/CD pipeline
- `CICD_README.md` - This documentation file

## Prerequisites

Before running the CI/CD tests, ensure the following services are running:

### 1. Database (PostgreSQL)
```bash
# From /Users/di/tb-cicd directory
./test-database.sh
```

### 2. RabbitMQ
```bash
# From /Users/di/tb-cicd directory
docker-compose up -d rabbitmq
```

### 3. CI/CD System
```bash
# From /Users/di/tb-cicd directory
mvn clean package -DskipTests
```

## Running the Tests

### Quick Test (Recommended)
Run the simplified test to verify database and RabbitMQ connections:

```bash
# From this directory
./test-cicd.sh
```

This will:
1. Check database and RabbitMQ connections
2. Build the Maven project
3. Start the Spring Boot server
4. Test API endpoints
5. Show pipeline execution records in the database

### Full Pipeline Test
Run the complete CI/CD pipeline:

```bash
# From /Users/di/tb-cicd directory
java -jar cicd-cli/target/cicd-cli-1.0.0-SNAPSHOT.jar run \
  --pipeline-name "DistributedSkiResortSystem" \
  --pipeline-file "/Users/di/Downloads/DistributedSkiResortSystem/cicd.yaml" \
  --working-dir "/Users/di/Downloads/DistributedSkiResortSystem"
```

## What the Tests Verify

### Database Connection
- Pipeline execution records are created
- Stage execution records are created
- Job execution records are created
- Status updates cascade properly (Job → Stage → Pipeline)

### RabbitMQ Connection
- Job messages are published to the queue
- Messages are consumed by the Job Executor Service
- Status updates are sent back to the Pipeline Service

### Application Integration
- Spring Boot server starts successfully
- API endpoints respond correctly
- RabbitMQ integration works properly

## Expected Results

After running the tests, you should see:

1. **Database Records**: New entries in `pipeline_executions`, `stage_executions`, and `job_executions` tables
2. **RabbitMQ Activity**: Messages flowing through the queues
3. **Server Logs**: Spring Boot server running and processing requests
4. **API Responses**: Successful HTTP responses from the server endpoints

## Troubleshooting

### Database Connection Issues
```bash
# Check if PostgreSQL is running
ps aux | grep postgres

# Test connection manually
psql -h localhost -U cicd_user -d cicd_db -c "SELECT 1;"
```

### RabbitMQ Connection Issues
```bash
# Check if RabbitMQ container is running
docker ps | grep rabbitmq

# Check RabbitMQ management interface
curl http://localhost:15672/api/overview -u admin:admin
```

### CI/CD System Issues
```bash
# Rebuild the CI/CD system
cd /Users/di/tb-cicd
mvn clean package -DskipTests

# Check if CLI is built
ls -la cicd-cli/target/cicd-cli-1.0.0-SNAPSHOT.jar
```

## Pipeline Stages

The complete pipeline includes:

1. **Build**: Maven clean, compile, package
2. **Test**: Unit tests, integration tests
3. **Quality Check**: SpotBugs, Checkstyle
4. **Build Artifacts**: JAR files for deployment
5. **Deploy**: Start RabbitMQ, Consumer, Server
6. **Integration Test**: API testing, load testing
7. **Cleanup**: Stop services

The simple test focuses on stages 1, 4, 5, and 7 for quick validation.
