# maricopa-lib-nytimes

A Spring Boot app that automatically redeems a Maricopa Library NYTimes access code every day at midnight using your Google account.

## Quickstart (Clone → Configure → Run as a Service)

```bash
# 1. Clone the repo
git clone https://github.com/kamalbahadur/maricopa-lib-nytimes.git
cd maricopa-lib-nytimes

# 2. Copy the secrets template and fill in your values
cp secrets.env.example secrets.env
```

Edit `secrets.env`:

```env
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_EMAIL=you@example.com
```

See **[Get Google Client ID And Secret](#get-google-client-id-and-secret)** below for how to obtain these.

```bash
# 3. Build and install as a background service (Linux/systemd)
make install-service
```

That's it. The app will:

- Start automatically now and on every reboot.
- Attempt NYTimes renewal every day at midnight using the Google account in `secrets.env`.
- Restart automatically if it crashes.

### Other Useful Commands

```bash
make service-status    # check if the service is running
make service-logs      # follow live service logs
make uninstall-service # stop and remove the service
make test              # run the test suite
make build             # build the jar only (no install)
```

---

## Features

- OAuth2 login with Google (`spring-security-oauth2-client`)
- Public health endpoint via `GET /health`
- Manual trigger via `GET /renew/trigger` (and backward-compatible alias `GET /renew`)
- Scheduled renewal every day at midnight (`@Scheduled(cron = "0 0 0 * * ?")`)
- Centralized config for gift code and campaign id
- Unit and integration tests for success, missing-token, and downstream-error paths
- Systemd service installation via `make install-service`

## Redeem Link

`https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=1fd71a2edc5d2d0f`

## Get Google Client ID And Secret

You need OAuth credentials for Google to authorize this app to act on your behalf.

1. Open: `https://console.cloud.google.com/`
2. Select an existing project or create one.
3. Go to **APIs & Services → OAuth consent screen**:
   - Choose **External**, fill in app name, and add your Google email as a **test user**.
4. Go to **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
5. Choose **Web application**.
   - Do **not** choose **Desktop app**. It will cause OAuth redirect/login loops with this service.
6. Under **Authorized redirect URIs** add:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
7. Save and copy:
   - **Client ID** → `GOOGLE_CLIENT_ID` in your `secrets.env`
   - **Client secret** → `GOOGLE_CLIENT_SECRET` in your `secrets.env`

> **Note:** keep `secrets.env` private. It is git-ignored and never committed.

## Configuration

All settings are loaded from `secrets.env` (required, git-ignored) plus `src/main/resources/application.yml` (defaults, committed).

| Variable | Required | Default | Description |
|---|---|---|---|
| `GOOGLE_CLIENT_ID` | ✅ | — | From Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | ✅ | — | From Google Cloud Console |
| `GOOGLE_EMAIL` | ✅ | — | Your Google account email |
| `NYTIMES_REDEEM_URL` | ❌ | `https://api.nytimes.com/svc/subscription/redeem` | NYTimes endpoint |
| `NYTIMES_CAMPAIGN_ID` | ❌ | `87LH8` | Maricopa library campaign |
| `NYTIMES_GIFT_CODE` | ❌ | `1fd71a2edc5d2d0f` | Maricopa library gift code |

## Endpoints

| Endpoint | Auth | Description |
|---|---|---|
| `GET /health` | Public | Returns `ok` |
| `GET /renew/trigger` | Required | Trigger renewal immediately |
| `GET /renew` | Required | Alias for `/renew/trigger` |

## How It Works

1. User (or service startup) authenticates with Google via Spring Security OAuth2 login.
2. Spring stores and refreshes the authorized client token automatically.
3. `SubscriptionService` builds a bearer-authenticated POST to the NYTimes redeem endpoint.
4. `DailyJob` runs at midnight and calls the same renew logic automatically.
5. `NYTimesController` exposes `/renew/trigger` for on-demand testing.

## Authentication Notes

- `/health` is public.
- `/renew` and `/renew/trigger` require an authenticated session.
- If no authorized OAuth2 client is cached yet, renew returns:
  ```
  Failed to retrieve access token. Login once with Google OAuth2 to cache an authorized client.
  ```
  Visit `http://localhost:8080` in your browser, log in with Google, then call renew again.

## Makefile Reference

| Target | Description |
|---|---|
| `make test` | Run the test suite |
| `make build` | Build the jar (skips tests) |
| `make bundle` | Build + package into `bundle/maricopa-lib-nytimes/` |
| `make run-bundle` | Run the bundled app locally |
| `make install-service` | Build + install + enable + start as a systemd service |
| `make uninstall-service` | Stop, disable, and remove the service |
| `make service-status` | Check service status |
| `make service-logs` | Follow live service logs |
| `make clean` | Clean Maven build output |
| `make clean-bundle` | Remove the bundle directory |

## Bundle Output

```
bundle/maricopa-lib-nytimes/
  app.jar          ← Spring Boot fat jar
  bundle.env       ← baked-in credentials (not for git)
  run-bundled.sh   ← launcher (requires only email arg)
```

## Test

```bash
make test
```

Current test coverage:

- `SubscriptionService` unit tests: token-missing and successful redeem.
- `NYTimesController` integration tests:
  - `/health` public access
  - `/renew` success path
  - `/renew/trigger` success path
  - missing authorized client
  - downstream NYTimes 500 error

## Troubleshooting

- **`Failed to retrieve access token`**
  - Check `GOOGLE_EMAIL` in `secrets.env` matches the Google account you logged in with.
  - Visit `http://localhost:8080` in your browser to trigger the initial OAuth login.

- **401/redirect on renew endpoints**
  - `/renew` and `/renew/trigger` require an OAuth session. Use `/health` for quick unauthenticated checks.

- **Downstream NYTimes failure (`Subscription failed with status: ...`)**
  - Verify the token is valid and the gift code/campaign id are correct in `secrets.env`.
  - Retry after a short interval if the NYTimes API is transiently unavailable.

- **Service won't start**
  - Run `make service-logs` to check for startup errors.
  - Confirm `java` is installed at `/usr/bin/java`:
    ```bash
    which java
    ```
  - Confirm `/opt/maricopa-lib-nytimes/bundle.env` exists and has correct values.
