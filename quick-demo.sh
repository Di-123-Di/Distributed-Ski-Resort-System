#!/bin/bash

# =====================================================
# Report Command Demo Script - Key Features
# =====================================================
# Focused demonstration of report command core functionality
# =====================================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CICD_CLI="/Users/di/tb-cicd/cicd"
PROJECT_DIR="/Users/di/Downloads/DistributedSkiResortSystem"
PIPELINE_NAME="DistributedSkiResortSystem"

cd "$PROJECT_DIR"

echo -e "${BLUE}🚀 Report Command Demo - Key Features${NC}"
echo "======================================"
echo ""

# Demo function
demo() {
    echo -e "${YELLOW}📋 $1${NC}"
    echo "Command: $2"
    echo ""
    eval "$2" 2>&1 || true
    echo ""
    echo "----------------------------------------"
    echo ""
}

# =====================================================
# 1. ERROR HANDLING (Most Important)
# =====================================================
echo -e "${RED}❌ Error Handling${NC}"
echo "=================="
echo ""

demo "Missing Required Parameter" \
    "$CICD_CLI report"

demo "Non-existent Pipeline" \
    "$CICD_CLI report --pipeline \"NonExistentPipeline\""

demo "Run doesn't exist (0)" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 0"

demo "Run doesn't exist (negative)" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run -1"

demo "Run doesn't exist (large)" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 999"

demo "Stage provided without run" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --stage \"build\""

demo "Job provided without stage" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --job \"compile\""

demo "Stage not found (existing run)" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 1 --stage \"unknown-stage\""

demo "Job not found (existing stage)" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 1 --stage \"build\" --job \"unknown-job\""

# =====================================================
# 2. MULTI-LEVEL REPORTS (Core Functionality)
# =====================================================
echo -e "${GREEN}✅ Multi-level Reports${NC}"
echo "======================="
echo ""

demo "Pipeline Report (All Runs)" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\""

demo "Specific Run Report" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 1"

demo "Stage Report" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 1 --stage \"build\""

demo "Job Report" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --run 1 --stage \"build\" --job \"maven-compile\""

# =====================================================
# 3. OUTPUT OPTIONS
# =====================================================
echo -e "${BLUE}🎨 Output Options${NC}"
echo "=================="
echo ""

demo "No Color Output" \
    "$CICD_CLI report --pipeline \"$PIPELINE_NAME\" --no-color"

# =====================================================
# 4. HELP
# =====================================================
echo -e "${YELLOW}ℹ️  Help${NC}"
echo "====="
echo ""

demo "Report Help" \
    "$CICD_CLI report --help"

echo -e "${GREEN}🎯 Demo Complete!${NC}"
echo "Key features demonstrated:"
echo "• Error handling and validation"
echo "• Multi-level reporting (pipeline → run → stage → job)"
echo "• No-color output option"
echo "• Help system"
