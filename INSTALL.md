# Installation Guide

This guide includes only the required steps to install and verify `maricopa-lib-nytimes`.

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
3. Go to **APIs & Services -> OAuth consent screen** and complete setup.
4. Add your Google account as a test user (if app is in testing mode).
5. Go to **APIs & Services -> Credentials -> Create Credentials -> OAuth client ID**.
6. Choose **Web application**.
7. Add this Authorized redirect URI:
   - `http://localhost:8080/login/oauth2/code/google`
8. Copy the generated values into `secrets.env`:
   - Client ID -> `GOOGLE_CLIENT_ID`
   - Client Secret -> `GOOGLE_CLIENT_SECRET`

## 3) Install the App

Install and run as a Linux `systemd` service:

```bash
make install-service
```

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

Use a browser while signed in with the same Google account as `GOOGLE_EMAIL`:

- Open: `http://localhost:8080/renew/trigger`

Expected successful response contains:

- `Manual renewal triggered: Subscription successful`

### C) Verify via logs

```bash
make service-logs
```

Look for lines similar to:

- `NYTimes subscription redemption succeeded. status=200`

### D) Optional health check

- Open: `http://localhost:8080/health`
- Expected response: `ok`

