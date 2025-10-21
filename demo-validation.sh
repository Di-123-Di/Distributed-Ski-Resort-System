#!/bin/bash

# Demo script to show validation of correct and incorrect pipeline configurations

echo "🔍 CI/CD Pipeline Validation Demo"
echo "=================================="
echo ""

echo "✅ Testing VALID pipeline configuration:"
echo "----------------------------------------"
cd /Users/di/tb-cicd/cicd-cli
mvn exec:java -Dexec.mainClass="com.company.cicd.cli.CicdCliApplication" -Dexec.args="verify /Users/di/Downloads/DistributedSkiResortSystem/cicd-pipeline.yaml -v"
echo ""

echo "❌ Testing INVALID pipeline configuration:"
echo "------------------------------------------"
mvn exec:java -Dexec.mainClass="com.company.cicd.cli.CicdCliApplication" -Dexec.args="verify /Users/di/Downloads/DistributedSkiResortSystem/invalid-cicd-pipeline.yaml -v"
echo ""

echo "📋 Summary of errors found in invalid-cicd-pipeline.yaml:"
echo "--------------------------------------------------------"
echo "1. Missing required fields (image, script, volumeMounts)"
echo "2. Invalid stage references"
echo "3. Circular dependencies"
echo "4. References to non-existent jobs"
echo "5. Empty stage names"
echo "6. Duplicate stage names"
echo ""
echo "🎯 This demonstrates the comprehensive validation capabilities!"
