#!/bin/bash

# =====================================================
# Quick Report Command Demo Script
# =====================================================
# Quick demonstration of report command error scenarios
# Perfect for live demo presentations
# =====================================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CICD_CLI="/Users/di/tb-cicd/cicd"
PROJECT_DIR="/Users/di/Downloads/DistributedSkiResortSystem"

cd "$PROJECT_DIR"

echo -e "${BLUE}🚀 CI/CD Report Command Demo${NC}"
echo "=================================="
echo ""

# Quick demo function
demo() {
    echo -e "${YELLOW}📋 $1${NC}"
    echo "Command: $2"
    echo ""
    eval "$2"
    echo ""
    echo "----------------------------------------"
    echo ""
}

# 1. Missing pipeline parameter
demo "❌ Missing Required Parameter" \
    "$CICD_CLI report"

# 2. Non-existent pipeline
demo "❌ Non-existent Pipeline" \
    "$CICD_CLI report --pipeline \"NonExistentPipeline\""

# 3. Invalid run number
demo "❌ Invalid Run Number (0)" \
    "$CICD_CLI report --pipeline \"DistributedSkiResortSystem\" --run 0"

# 4. Job without stage
demo "❌ Job without Stage" \
    "$CICD_CLI report --pipeline \"DistributedSkiResortSystem\" --job \"compile\""

# 5. Valid command (should work if service running)
demo "✅ Valid Pipeline Report" \
    "$CICD_CLI report --pipeline \"DistributedSkiResortSystem\""

# 6. Help command
demo "ℹ️  Help Command" \
    "$CICD_CLI report --help"

echo -e "${GREEN}🎯 Demo Complete!${NC}"
echo "Perfect for showing error handling capabilities."
