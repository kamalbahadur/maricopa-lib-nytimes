SHELL := /bin/bash

APP_NAME := maricopa-lib-nytimes
APP_JAR := target/maricopa-lib-nytimes-0.0.1-SNAPSHOT.jar
BUNDLE_DIR := bundle/$(APP_NAME)

.PHONY: test build bundle run-bundle clean clean-bundle

test:
	mvn -B test

build:
	mvn -B clean package -DskipTests

bundle: build
	@test -n "$(GOOGLE_CLIENT_ID)" || (echo "ERROR: GOOGLE_CLIENT_ID is required for bundling" && exit 1)
	@test -n "$(GOOGLE_CLIENT_SECRET)" || (echo "ERROR: GOOGLE_CLIENT_SECRET is required for bundling" && exit 1)
	mkdir -p "$(BUNDLE_DIR)"
	cp "$(APP_JAR)" "$(BUNDLE_DIR)/app.jar"
	printf '%s\n' \
	  "GOOGLE_CLIENT_ID=$(GOOGLE_CLIENT_ID)" \
	  "GOOGLE_CLIENT_SECRET=$(GOOGLE_CLIENT_SECRET)" \
	  "NYTIMES_REDEEM_URL=https://api.nytimes.com/svc/subscription/redeem" \
	  "NYTIMES_CAMPAIGN_ID=87LH8" \
	  "NYTIMES_GIFT_CODE=1fd71a2edc5d2d0f" \
	  "NYTIMES_OAUTH2_REGISTRATION_ID=google" \
	  > "$(BUNDLE_DIR)/bundle.env"
	printf '%s\n' \
	  '#!/usr/bin/env bash' \
	  'set -euo pipefail' \
	  '' \
	  'if [[ $$# -ne 1 ]]; then' \
	  '  echo "Usage: ./run-bundled.sh <google_email>"' \
	  '  exit 1' \
	  'fi' \
	  '' \
	  'SCRIPT_DIR="$$(cd "$$(dirname "$${BASH_SOURCE[0]}")" && pwd)"' \
	  'source "$$SCRIPT_DIR/bundle.env"' \
	  '' \
	  'export GOOGLE_CLIENT_ID' \
	  'export GOOGLE_CLIENT_SECRET' \
	  'export NYTIMES_REDEEM_URL' \
	  'export NYTIMES_CAMPAIGN_ID' \
	  'export NYTIMES_GIFT_CODE' \
	  'export NYTIMES_OAUTH2_REGISTRATION_ID' \
	  'export NYTIMES_OAUTH2_PRINCIPAL_NAME="$$1"' \
	  '' \
	  'exec java -jar "$$SCRIPT_DIR/app.jar"' \
	  > "$(BUNDLE_DIR)/run-bundled.sh"
	chmod +x "$(BUNDLE_DIR)/run-bundled.sh"
	@echo "Bundle created at $(BUNDLE_DIR)"
	@echo "Run it with: $(BUNDLE_DIR)/run-bundled.sh <google_email>"

run-bundle:
	@test -n "$(EMAIL)" || (echo "ERROR: EMAIL is required, ex: make run-bundle EMAIL=you@example.com" && exit 1)
	"$(BUNDLE_DIR)/run-bundled.sh" "$(EMAIL)"

clean:
	mvn -B clean

clean-bundle:
	rm -rf bundle

