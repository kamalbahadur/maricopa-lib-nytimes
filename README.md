# maricopa-lib-nytimes
A lightweight Spring Boot helper for the Maricopa Library NYTimes redeem flow.
It does **not** automate NYTimes account actions. Instead, it provides:
- a local redirect endpoint to open the NYTimes redeem link quickly
- a scheduled nightly reminder in service logs
- a simple installable Linux service
## Quickstart
```bash
git clone https://github.com/kamalbahadur/maricopa-lib-nytimes.git
cd maricopa-lib-nytimes
cp secrets.env.example secrets.env   # optional, only if you want to override defaults
make install-service
```
If you pull a newer version later, run:
```bash
make install-service
```
## What the Service Does
- Starts on boot
- Exposes:
  - `GET /health`
  - `GET /renew`
  - `GET /renew/trigger`
- Logs a nightly reminder at midnight with the NYTimes redeem URL
## Manual Renewal
Open this in your browser:
```text
http://localhost:8080/renew/trigger
```
That endpoint redirects you to the NYTimes redeem link:
```text
https://www.nytimes.com/subscription/redeem/all-access\?campaignId\=87LH8\&gift_code\=1fd71a2edc5d2d0f
```
Then complete the NYTimes redeem/login steps manually in the browser.
## Configuration
Defaults come from `src/main/resources/application.yml`.
You may override them in `secrets.env`.
Optional values:
```env
NYTIMES_REDEEM_URL=https://www.nytimes.com/subscription/redeem/all-access
NYTIMES_CAMPAIGN_ID=87LH8
NYTIMES_GIFT_CODE=1fd71a2edc5d2d0f
```
## Makefile Targets
```bash
make test
make build
make bundle
make run-bundle
make install-service
make uninstall-service
make service-status
make service-logs
```
## Scheduled Reminder Behavior
Every day at midnight, the service logs a message like:
```text
NYTimes renewal reminder: open https://www.nytimes.com/subscription/redeem/all-access\?... in a browser and complete the redeem flow manually.
```
## Testing
Run tests:
```bash
make test
```
Quick local verification:
```bash
make service-status
make service-logs
```
Then in a browser:
- `http://localhost:8080/health` → should return `ok`
- `http://localhost:8080/renew/trigger` → should redirect to the NYTimes redeem page
## Troubleshooting
- If the service does not start:
```bash
make service-logs
```
- If Java is missing:
```bash
which java
```
- If you changed `secrets.env`, reinstall so the service picks up the new values:
```bash
make install-service
```
