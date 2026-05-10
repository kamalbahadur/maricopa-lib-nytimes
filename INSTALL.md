# Installation Guide

This guide includes only the required steps to install and verify `maricopa-lib-nytimes`.

## Prerequisite: Java 17

If JDK 17 is not installed, install it first:

```bash
sudo apt install openjdk-17-jdk
```

Optional verification:

```bash
java -version
```

## Prerequisite: Git

If Git is not installed, install it first:

```bash
sudo apt install git
```

Optional verification:

```bash
git --version
```

## 1) Clone the Git Repo

```bash
git clone https://github.com/kamalbahadur/maricopa-lib-nytimes.git
cd maricopa-lib-nytimes
```

## 2) Create `secrets.env`, Update Required Values, and Get Google Credentials

Create your local secrets file from the template:

```bash
cp secrets.env.example secrets.env
```

Edit `secrets.env` and set these required values:

```env
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_EMAIL=you@example.com
```

How to obtain `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`:

1. Open Google Cloud Console: `https://console.cloud.google.com/`
2. Select or create a project.
   - Click the project dropdown in the top-left area of the page, then choose your project (or create one).
3. Go to **APIs & Services -> OAuth consent screen** and complete setup.
4. Add your Google account as a test user (if app is in testing mode).
5. Go to **APIs & Services -> Credentials -> Create Credentials -> OAuth client ID**.
6. Choose **Web application**.
   - Do **not** choose **Desktop app**. Desktop app credentials can cause repeated Google account selection/login loops.
7. Add this Authorized redirect URI:
   - `http://localhost:8080/login/oauth2/code/google`
8. Copy the generated values into `secrets.env`:
   - Client ID -> `GOOGLE_CLIENT_ID`
   - Client Secret -> `GOOGLE_CLIENT_SECRET`

## 3) Install the App

Install the Playwright-managed Chromium browser used by the automation flow:

```bash
make install-browser
```

Install and run as a Linux `systemd` service:

```bash
make install-service
```

If you pull a newer version of the repo later, run `make install-service` again so the service jar and generated `bundle.env` are refreshed.

Bootstrap the NYTimes browser session one time so scheduled renewals can reuse it later:

```bash
make bootstrap-browser-session
```

This command runs the automation in a non-web mode, so it does **not** conflict with the installed service already using port `8080`.

Expected bootstrap behavior:

- A Chromium window opens.
- Complete the NYTimes redeem/login flow in that browser window.
- Close the window when finished.
- The automation session is saved for future scheduled renewals.

Useful service commands:

```bash
make service-status
make service-logs
```

## 4) Test After Install (Verify Renewal Process)

### A) Confirm service is running

```bash
make service-status
```

You should see it as `active (running)`.

### B) Trigger a manual renewal test

Run a one-time headless renewal using the saved browser session:

```bash
make renew-once
```

This command also runs in non-web mode and can be executed while the service is running.

Expected behavior:

- The job reuses the saved browser session from the bootstrap step.
- It attempts the NYTimes redeem flow automatically.
- Then you verify the result in your NYTimes account.

### C) Verify via logs

```bash
make service-logs
```

Look for lines similar to:

- `NYTimes daily subscription job result: status=SUCCESS`
- `NYTimes daily subscription job result: status=ALREADY_ACTIVE`
- `NYTimes daily subscription job result: status=BOOTSTRAP_REQUIRED`

If you see `BOOTSTRAP_REQUIRED`, run `make bootstrap-browser-session` again to refresh the saved browser session.

### D) Optional health check

- Open: `http://localhost:8080/health`
- Expected response: `ok`

