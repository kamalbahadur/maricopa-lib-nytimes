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
.PHONY: test build bundle run-bundle install-service uninstall-service \
        service-logs service-status clean clean-bundle
-include $(SECRETS_ENV)
export NYTIMES_REDEEM_URL
export NYTIMES_CAMPAIGN_ID
export NYTIMES_GIFT_CODE
test:
	mvn -B test
build:
	mvn -B clean package -DskipTests
bundle: build
	mkdir -p "$(BUNDLE_DIR)"
	cp "$(APP_JAR)" "$(BUNDLE_DIR)/app.jar"
	printf '%s\n' \
	  "NYTIMES_REDEEM_URL=$(NYTIMES_REDEEM_URL)" \
	  "NYTIMES_CAMPAIGN_ID=$(NYTIMES_CAMPAIGN_ID)" \
	  "NYTIMES_GIFT_CODE=$(NYTIMES_GIFT_CODE)" \
	  > "$(BUNDLE_DIR)/bundle.env"
	printf '%s\n' \
	  '#!/usr/bin/env bash' \
	  'set -euo pipefail' \
	  '' \
	  'SCRIPT_DIR="$$(cd "$$(dirname "$${BASH_SOURCE[0]}")" && pwd)"' \
	  'source "$$SCRIPT_DIR/bundle.env"' \
	  '' \
	  'export NYTIMES_REDEEM_URL' \
	  'export NYTIMES_CAMPAIGN_ID' \
	  'export NYTIMES_GIFT_CODE' \
	  '' \
	  'exec java -jar "$$SCRIPT_DIR/app.jar"' \
	  > "$(BUNDLE_DIR)/run-bundled.sh"
	chmod +x "$(BUNDLE_DIR)/run-bundled.sh"
	@echo ""
	@echo "Bundle created at $(BUNDLE_DIR)"
	@echo "Run it with:  $(BUNDLE_DIR)/run-bundled.sh"
	@echo "Or install as a service: make install-service"
run-bundle:
	"$(BUNDLE_DIR)/run-bundled.sh"
install-service: bundle
	@echo "Installing $(SERVICE_NAME) to $(INSTALL_DIR) ..."
	sudo mkdir -p "$(INSTALL_DIR)"
	sudo cp "$(BUNDLE_DIR)/app.jar" "$(INSTALL_DIR)/app.jar"
	sudo cp "$(BUNDLE_DIR)/bundle.env" "$(INSTALL_DIR)/bundle.env"
	sudo chmod 600 "$(INSTALL_DIR)/bundle.env"
	printf '%s\n' \
	  '[Unit]' \
	  'Description=NYTimes Manual Renewal Reminder (Maricopa Library)' \
	  'After=network.target' \
	  '' \
	  '[Service]' \
	  'Type=simple' \
	  'User=$(USER)' \
	  'EnvironmentFile=$(INSTALL_DIR)/bundle.env' \
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
clean:
	mvn -B clean
clean-bundle:
	rm -rf bundle
