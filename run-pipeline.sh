#!/bin/bash

# Distributed Ski Resort System - Pipeline Runner
# This script runs the complete CI/CD pipeline using tb-cicd

set -e  # Exit on error

PIPELINE_FILE=".pipelines/ski-resort-pipeline.yaml"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "Distributed Ski Resort System Pipeline"
echo "========================================"
echo ""
echo "Project Directory: $PROJECT_DIR"
echo "Pipeline File: $PIPELINE_FILE"
echo ""

# Step 1: Verify pipeline file exists
if [ ! -f "$PIPELINE_FILE" ]; then
    echo "❌ Error: Pipeline file not found: $PIPELINE_FILE"
    exit 1
fi

# Step 2: Validate pipeline configuration
echo "Step 1/4: Validating pipeline configuration..."
cicd verify "$PIPELINE_FILE"
if [ $? -ne 0 ]; then
    echo "❌ Pipeline validation failed!"
    exit 1
fi
echo "✅ Pipeline validation passed!"
echo ""

# Step 3: Generate dry-run execution plan
echo "Step 2/4: Generating execution plan..."
cicd dryrun "$PIPELINE_FILE"
echo ""
echo "✅ Execution plan generated!"
echo ""

# Step 4: Check if services are running
echo "Step 3/4: Checking services..."
if ! curl -s http://localhost:8082/job-executor/api/jobs/health > /dev/null 2>&1; then
    echo "❌ Error: Job Executor Service is not running!"
    echo "Please start the Job Executor Service first:"
    echo "  cd /Users/di/tb-cicd"
    echo "  ./start-job-executor.sh"
    exit 1
fi
echo "✅ Job Executor Service is running"

if ! curl -s http://localhost:8080/api/pipelines/health > /dev/null 2>&1; then
    echo "❌ Error: Pipeline Service is not running!"
    echo "Please start the Pipeline Service first:"
    echo "  cd /Users/di/tb-cicd"
    echo "  ./start-pipeline-service.sh"
    exit 1
fi
echo "✅ Pipeline Service is running"
echo ""

# Step 5: Execute pipeline
echo "Step 4/4: Executing pipeline..."
echo ""
echo "Converting YAML to JSON and sending to Pipeline Service..."

# Use Python to convert YAML to JSON and submit
python3 << 'PYTHON_SCRIPT'
import json
import yaml
import requests
import sys
import os

pipeline_file = ".pipelines/ski-resort-pipeline.yaml"
project_dir = os.getcwd()

try:
    # Read and parse YAML
    with open(pipeline_file, 'r') as f:
        pipeline_config = yaml.safe_load(f)
    
    # Extract pipeline info
    pipeline_name = pipeline_config.get('pipeline', {}).get('name', 'unknown')
    pipeline_desc = pipeline_config.get('pipeline', {}).get('description', '')
    
    # Build jobs list
    jobs = []
    for job_name, job_config in pipeline_config.items():
        if job_name in ['pipeline', 'stages']:
            continue
        
        # Extract job details
        if isinstance(job_config, list):
            job_data = {}
            for item in job_config:
                if isinstance(item, dict):
                    job_data.update(item)
            
            job = {
                "jobName": job_name,
                "image": job_data.get('image', 'ubuntu:22.04'),
                "script": job_data.get('script', []),
                "stage": job_data.get('stage', 'default'),
                "needs": job_data.get('needs', []),
                "workingDirectory": project_dir
            }
            jobs.append(job)
    
    # Build request
    request_data = {
        "pipelineName": pipeline_name,
        "pipelineDescription": pipeline_desc,
        "jobs": jobs
    }
    
    # Submit to Pipeline Service
    print(f"📤 Submitting pipeline: {pipeline_name}")
    print(f"📊 Total jobs: {len(jobs)}")
    print("")
    
    response = requests.post(
        'http://localhost:8080/api/pipelines/execute',
        json=request_data,
        headers={'Content-Type': 'application/json'}
    )
    
    result = response.json()
    
    if response.status_code == 200 and result.get('status') in ['PENDING', 'RUNNING']:
        execution_id = result.get('executionId')
        print(f"✅ Pipeline execution started!")
        print(f"📋 Execution ID: {execution_id}")
        print(f"📊 Status: {result.get('status')}")
        print(f"🔢 Total Jobs: {result.get('totalJobs')}")
        print("")
        print(f"Monitor status: curl http://localhost:8080/api/pipelines/{execution_id}/status")
        sys.exit(0)
    else:
        print(f"❌ Pipeline execution failed!")
        print(f"Status: {result.get('status')}")
        print(f"Message: {result.get('message')}")
        sys.exit(1)
        
except FileNotFoundError:
    print(f"❌ Error: Pipeline file not found: {pipeline_file}")
    sys.exit(1)
except Exception as e:
    print(f"❌ Error: {str(e)}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
PYTHON_SCRIPT

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "✅ Pipeline submitted successfully!"
    echo "========================================"
else
    echo ""
    echo "========================================"
    echo "❌ Pipeline execution failed!"
    echo "========================================"
    exit 1
fi

