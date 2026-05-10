SHELL := /bin/bash

APP_NAME    := maricopa-lib-nytimes
APP_JAR     := target/maricopa-lib-nytimes-0.0.1-SNAPSHOT.jar
BUNDLE_DIR  := bundle/$(APP_NAME)
SECRETS_ENV := secrets.env
SERVICE_NAME := $(APP_NAME)
INSTALL_DIR := /opt/$(APP_NAME)
NYTIMES_REDEEM_URL ?= https://www.nytimes.com/subscription/redeem/all-access
NYTIMES_CAMPAIGN_ID ?= 87LH8
NYTIMES_GIFT_CODE ?= 1fd71a2edc5d2d0f
NYTIMES_OAUTH2_REGISTRATION_ID ?= google
NYTIMES_BROWSER_HEADLESS ?= true
NYTIMES_BROWSER_TIMEOUT_SECONDS ?= 60
NYTIMES_BROWSER_BOOTSTRAP_TIMEOUT_MINUTES ?= 15
PLAYWRIGHT_CLI := mvn -B -DskipTests exec:java -Dexec.mainClass=com.microsoft.playwright.CLI

.PHONY: test build bundle run-bundle install-service uninstall-service \
		service-logs service-status install-browser bootstrap-browser-session \
		renew-once clean clean-bundle

# ---------------------------------------------------------------------------
# Load secrets.env if it exists (used by bundle, install-service targets)
# ---------------------------------------------------------------------------
-include $(SECRETS_ENV)
export GOOGLE_CLIENT_ID
export GOOGLE_CLIENT_SECRET
export GOOGLE_EMAIL
export NYTIMES_REDEEM_URL
export NYTIMES_CAMPAIGN_ID
export NYTIMES_GIFT_CODE
export NYTIMES_OAUTH2_REGISTRATION_ID
export NYTIMES_BROWSER_HEADLESS
export NYTIMES_BROWSER_TIMEOUT_SECONDS
export NYTIMES_BROWSER_BOOTSTRAP_TIMEOUT_MINUTES

# ---------------------------------------------------------------------------
# Development
# ---------------------------------------------------------------------------
test:
	mvn -B test

install-browser:
	$(PLAYWRIGHT_CLI) -Dexec.args="install chromium"

build:
	mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Bundle: bakes credentials into bundle/maricopa-lib-nytimes/
# Reads GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET from secrets.env
# ---------------------------------------------------------------------------
bundle: build
	@test -n "$(GOOGLE_CLIENT_ID)" || \
	  (echo "ERROR: set GOOGLE_CLIENT_ID in $(SECRETS_ENV)" && exit 1)
	@test -n "$(GOOGLE_CLIENT_SECRET)" || \
	  (echo "ERROR: set GOOGLE_CLIENT_SECRET in $(SECRETS_ENV)" && exit 1)
	mkdir -p "$(BUNDLE_DIR)"
	cp "$(APP_JAR)" "$(BUNDLE_DIR)/app.jar"
	printf '%s\n' \
	  "GOOGLE_CLIENT_ID=$(GOOGLE_CLIENT_ID)" \
	  "GOOGLE_CLIENT_SECRET=$(GOOGLE_CLIENT_SECRET)" \
	  "NYTIMES_REDEEM_URL=$(NYTIMES_REDEEM_URL)" \
	  "NYTIMES_CAMPAIGN_ID=$(NYTIMES_CAMPAIGN_ID)" \
	  "NYTIMES_GIFT_CODE=$(NYTIMES_GIFT_CODE)" \
	  "NYTIMES_OAUTH2_REGISTRATION_ID=$(NYTIMES_OAUTH2_REGISTRATION_ID)" \
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
	@echo ""
	@echo "Bundle created at $(BUNDLE_DIR)"
	@echo "Run it with:  $(BUNDLE_DIR)/run-bundled.sh <google_email>"
	@echo "Or install as a service: make install-service"

run-bundle:
	@test -n "$(GOOGLE_EMAIL)" || \
	  (echo "ERROR: set GOOGLE_EMAIL in $(SECRETS_ENV) or pass EMAIL=..." && exit 1)
	"$(BUNDLE_DIR)/run-bundled.sh" "$(if $(EMAIL),$(EMAIL),$(GOOGLE_EMAIL))"

bootstrap-browser-session:
	@test -n "$(GOOGLE_EMAIL)" || \
	  (echo "ERROR: set GOOGLE_EMAIL in $(SECRETS_ENV)" && exit 1)
	NYTIMES_BROWSER_HEADLESS=false \
	NYTIMES_BROWSER_USER_DATA_DIR="$(INSTALL_DIR)/browser-data" \
	NYTIMES_BROWSER_COMMAND=bootstrap \
	mvn -B spring-boot:run

renew-once:
	NYTIMES_BROWSER_HEADLESS=$(NYTIMES_BROWSER_HEADLESS) \
	NYTIMES_BROWSER_USER_DATA_DIR="$(INSTALL_DIR)/browser-data" \
	NYTIMES_BROWSER_COMMAND=renew-once \
	mvn -B spring-boot:run

# ---------------------------------------------------------------------------
# Service: install/run as a systemd service (Linux only)
# Usage: make install-service
# Reads GOOGLE_EMAIL from secrets.env (or pass EMAIL=you@example.com)
# ---------------------------------------------------------------------------
install-service: bundle
	@test -n "$(GOOGLE_EMAIL)" || \
	  (echo "ERROR: set GOOGLE_EMAIL in $(SECRETS_ENV)" && exit 1)
	@echo "Installing $(SERVICE_NAME) to $(INSTALL_DIR) ..."
	sudo mkdir -p "$(INSTALL_DIR)"
	sudo mkdir -p "$(INSTALL_DIR)/browser-data"
	sudo chown -R "$(USER)":"$(USER)" "$(INSTALL_DIR)/browser-data"
	sudo cp "$(BUNDLE_DIR)/app.jar" "$(INSTALL_DIR)/app.jar"
	sudo cp "$(BUNDLE_DIR)/bundle.env" "$(INSTALL_DIR)/bundle.env"
	sudo chmod 600 "$(INSTALL_DIR)/bundle.env"
	printf '%s\n' \
	  '[Unit]' \
	  'Description=NYTimes Auto Subscriber (Maricopa Library)' \
	  'After=network.target' \
	  '' \
	  '[Service]' \
	  'Type=simple' \
	  'User=$(USER)' \
	  'EnvironmentFile=$(INSTALL_DIR)/bundle.env' \
	  'Environment="NYTIMES_OAUTH2_PRINCIPAL_NAME=$(if $(EMAIL),$(EMAIL),$(GOOGLE_EMAIL))"' \
	  'Environment="NYTIMES_BROWSER_USER_DATA_DIR=$(INSTALL_DIR)/browser-data"' \
	  'Environment="NYTIMES_BROWSER_HEADLESS=true"' \
	  'Environment="NYTIMES_BROWSER_TIMEOUT_SECONDS=$(NYTIMES_BROWSER_TIMEOUT_SECONDS)"' \
	  'ExecStart=/usr/bin/java -jar $(INSTALL_DIR)/app.jar' \
	  'Restart=on-failure' \
	  'RestartSec=30' \
	  '' \
	  '[Install]' \
	  'WantedBy=multi-user.target' \
	  | sudo tee /etc/systemd/system/$(SERVICE_NAME).service > /dev/null
	sudo systemctl daemon-reload
	sudo systemctl enable $(SERVICE_NAME)
	sudo systemctl restart $(SERVICE_NAME)
	@echo ""
	@echo "Service installed and started."
	@echo "  Status:  make service-status"
	@echo "  Logs:    make service-logs"
	@echo "  Bootstrap browser session: make bootstrap-browser-session"
	@echo "  Run one headless renewal now: make renew-once"
	@echo "  Remove:  make uninstall-service"

uninstall-service:
	sudo systemctl stop $(SERVICE_NAME) || true
	sudo systemctl disable $(SERVICE_NAME) || true
	sudo rm -f /etc/systemd/system/$(SERVICE_NAME).service
	sudo systemctl daemon-reload
	sudo rm -rf "$(INSTALL_DIR)"
	@echo "Service $(SERVICE_NAME) removed."

service-status:
	sudo systemctl status $(SERVICE_NAME)

service-logs:
	sudo journalctl -u $(SERVICE_NAME) -f

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
clean:
	mvn -B clean

clean-bundle:
	rm -rf bundle

