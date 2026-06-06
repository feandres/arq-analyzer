#!/bin/sh

PROJECT_PATH=${1:-.}
THRESHOLD=${2:-7}
FAIL_ON_VIOLATIONS=${3:-true}

echo "Analisando: $PROJECT_PATH"
echo "Coupling threshold: $THRESHOLD"
echo "Fail on violations: $FAIL_ON_VIOLATIONS"

java -jar /app/app.jar \
  --analyzer.path="$PROJECT_PATH" \
  --analyzer.threshold="$THRESHOLD" \
  --analyzer.fail="$FAIL_ON_VIOLATIONS"

EXIT_CODE=$?
exit $EXIT_CODE