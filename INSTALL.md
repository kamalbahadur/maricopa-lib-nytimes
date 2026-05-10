# Installation Guide
This guide contains only the required steps to install and verify `maricopa-lib-nytimes`.
## Prerequisites
Install Java 17 if needed:
```bash
sudo apt install openjdk-17-jdk
java -version
```
Install Git if needed:
```bash
sudo apt install git
git --version
```
## 1) Clone the Git Repo
```bash
git clone https://github.com/kamalbahadur/maricopa-lib-nytimes.git
cd maricopa-lib-nytimes
```
## 2) Optional: Create `secrets.env` Overrides
If you want to override the default redeem link values:
```bash
cp secrets.env.example secrets.env
```
Optional override values:
```env
NYTIMES_REDEEM_URL=https://www.nytimes.com/subscription/redeem/all-access
NYTIMES_CAMPAIGN_ID=87LH8
NYTIMES_GIFT_CODE=1fd71a2edc5d2d0f
```
If you do not need any overrides, you can skip this step.
## 3) Install the App
```bash
make install-service
```
Useful commands:
```bash
make service-status
make service-logs
```
If you pull a newer version later, run this again:
```bash
make install-service
```
## 4) Test After Install
### A) Confirm the service is running
```bash
make service-status
```
You should see `active (running)`.
### B) Trigger the manual renewal helper
Open in a browser:
```text
http://localhost:8080/renew/trigger
```
Expected behavior:
- your browser is redirected to the NYTimes redeem page
- you complete the NYTimes flow manually there
### C) Verify the health endpoint
Open:
```text
http://localhost:8080/health
```
Expected response:
```text
ok
```
### D) Verify the nightly reminder logs
```bash
make service-logs
```
Look for a reminder message containing the NYTimes redeem URL.
