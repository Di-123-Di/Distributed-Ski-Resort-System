# TB-CICD Pipeline Guide for Distributed Ski Resort System

## Overview

This guide explains how to use the TB-CICD system to build, test, and package the Distributed Ski Resort System.

## Quick Start

### One-Command Execution

```bash
curl -X POST http://localhost:8080/api/pipelines/execute \
  -H "Content-Type: application/json" \
  -d @.pipelines/pipeline-request.json
```

### Check Execution Status

```bash
# Replace {execution-id} with your actual execution ID
curl http://localhost:8080/api/pipelines/{execution-id}/status
```

## Pipeline Stages

The pipeline executes 4 stages in sequence:

### 1. Validate (Code Quality)
- Checks Java and Maven installation
- Validates project structure
- Runs Maven validate
- Performs checkstyle analysis

### 2. Compile
- Cleans previous builds
- Compiles all 5 modules:
  - swagger-client
  - server-spring  
  - server-servlet
  - client-part1
  - consumer

### 3. Test
- Runs all unit tests
- Generates test reports (surefire-reports)
- Validates test coverage

### 4. Package
- Packages all modules to JAR/WAR files
- Generates deployable artifacts:
  - `server-spring-1.0-SNAPSHOT.jar`
  - `server-servlet-1.0-SNAPSHOT.war`
  - `client-part1-1.0-SNAPSHOT-jar-with-dependencies.jar`
  - `consumer-1.0-SNAPSHOT-jar-with-dependencies.jar`
  - `swagger-client-1.0-SNAPSHOT.jar`

## File Structure

```
.pipelines/
├── ski-resort-pipeline.yaml      # Human-readable YAML pipeline definition
└── pipeline-request.json         # JSON request for API execution

run-pipeline.sh                   # Automated execution script (requires PyYAML)
TB-CICD-GUIDE.md                  # This file
```

## Validation and Dry-Run

Before executing the pipeline, you can validate and preview it:

```bash
# Validate pipeline syntax
cicd verify .pipelines/ski-resort-pipeline.yaml

# Preview execution plan
cicd dryrun .pipelines/ski-resort-pipeline.yaml
```

## Prerequisites

Make sure the following services are running:

### Job Executor Service
```bash
curl http://localhost:8082/job-executor/api/jobs/health
```

### Pipeline Service  
```bash
curl http://localhost:8080/api/pipelines/health
```

## Example Successful Execution

```json
{
  "executionId": "53f68dc7-2a16-4e60-a13f-69bc8f35aa26",
  "status": "SUCCESS",
  "message": "Pipeline completed successfully: 4/4 jobs completed",
  "pipelineName": "distributed-ski-resort-pipeline",
  "totalJobs": 4,
  "completedJobs": 4,
  "failedJobs": 0
}
```

## Build Artifacts Location

After successful execution, find artifacts in:

```
server-spring/target/server-spring-1.0-SNAPSHOT.jar
server-servlet/target/server-servlet-1.0-SNAPSHOT.war
client-part1/target/client-part1-1.0-SNAPSHOT-jar-with-dependencies.jar
consumer/target/consumer-1.0-SNAPSHOT-jar-with-dependencies.jar
swagger-client/target/swagger-client-1.0-SNAPSHOT.jar
```

## Troubleshooting

### Pipeline Validation Failed
- Ensure YAML syntax is correct
- Check that all required fields are present
- Verify job dependencies are valid

### Services Not Running
- Start Job Executor: `cd /Users/di/tb-cicd && ./start-job-executor.sh`
- Start Pipeline Service: `cd /Users/di/tb-cicd && ./start-pipeline-service.sh`

### Build Failures
- Check job logs in Job Executor Service
- Verify Maven dependencies are available
- Ensure Java 8 is properly installed in container

## Advantages of TB-CICD

### ✅ Local Execution
- No internet dependency
- Faster feedback cycles
- Free unlimited usage

### ✅ Docker Isolation
- Clean build environment every time
- Consistent across different machines
- No local environment pollution

### ✅ Multi-Module Support
- Handles complex Maven projects
- Respects dependency ordering
- Parallel execution where possible

### ✅ Full Pipeline Visibility
- Real-time status monitoring
- Detailed execution logs
- Clear error messages

## Next Steps

1. **Customize Pipeline**: Modify `.pipelines/ski-resort-pipeline.yaml` for your needs
2. **Add More Stages**: Include deployment, Docker build, etc.
3. **Integrate with Git**: Add pre-commit hooks
4. **Monitor Performance**: Track build times and optimize

## Support

For issues or questions about TB-CICD:
- Check Job Executor logs: `docker logs <container-id>`
- Review Pipeline Service logs
- Validate YAML syntax with `cicd verify`

---

**Pipeline Created**: October 13, 2025
**Last Successful Run**: Execution ID: 53f68dc7-2a16-4e60-a13f-69bc8f35aa26

