#!/bin/bash

# Test script for DistributedSkiResortSystem CI/CD pipeline
# This script will test the database and RabbitMQ connections

set -e

echo "=========================================="
echo "DistributedSkiResortSystem CI/CD Test"
echo "=========================================="

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "Error: Please run this script from the DistributedSkiResortSystem root directory"
    exit 1
fi

# Check if CI/CD CLI is available
CICD_CLI_PATH="/Users/di/tb-cicd/cicd-cli/target/cicd-cli-1.0.0-SNAPSHOT.jar"
if [ ! -f "$CICD_CLI_PATH" ]; then
    echo "Error: CI/CD CLI not found at $CICD_CLI_PATH"
    echo "Please build the CI/CD project first: cd /Users/di/tb-cicd && mvn clean package -DskipTests"
    exit 1
fi

# Check if database is running
echo "Checking database connection..."
if ! PGPASSWORD=cicd_password psql -h localhost -p 5433 -U cicd_user -d cicd_db -c "SELECT 1;" > /dev/null 2>&1; then
    echo "Error: Database connection failed. Please ensure PostgreSQL is running and accessible."
    echo "Run: ./test-database.sh (from /Users/di/tb-cicd)"
    exit 1
fi
echo "✓ Database connection successful"

# Check if RabbitMQ is running
echo "Checking RabbitMQ connection..."
if ! docker ps | grep rabbitmq > /dev/null 2>&1; then
    echo "Error: RabbitMQ is not running. Please start it first."
    echo "Run: cd /Users/di/tb-cicd && docker-compose up -d rabbitmq"
    exit 1
fi
echo "✓ RabbitMQ is running"

# Run the simple test pipeline
echo ""
echo "Running simple CI/CD test pipeline..."
echo "=========================================="

java -jar "$CICD_CLI_PATH" run \
  --file "/Users/di/Downloads/DistributedSkiResortSystem/cicd-simple-test.yaml"

echo ""
echo "=========================================="
echo "CI/CD test completed!"
echo "=========================================="

# Check database for pipeline execution records
echo ""
echo "Checking database for pipeline execution records..."
PGPASSWORD=cicd_password psql -h localhost -p 5433 -U cicd_user -d cicd_db -c "
SELECT 
    pe.id,
    pe.pipeline_name,
    pe.status,
    pe.start_time,
    pe.end_time,
    COUNT(se.id) as stage_count,
    COUNT(je.id) as job_count
FROM pipeline_executions pe
LEFT JOIN stage_executions se ON pe.id = se.pipeline_execution_id
LEFT JOIN job_executions je ON se.id = je.stage_execution_id
WHERE pe.pipeline_name = 'DistributedSkiResortSystem-SimpleTest'
GROUP BY pe.id, pe.pipeline_name, pe.status, pe.start_time, pe.end_time
ORDER BY pe.start_time DESC
LIMIT 5;
"

echo ""
echo "Test completed! Check the output above for pipeline execution details."
