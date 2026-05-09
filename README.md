# NYTimes Auto Subscriber

This Spring Boot app redeems a NYTimes access code using a Google OAuth2 access token.

It supports:

- OAuth2 login with Google (`spring-security-oauth2-client`)
- Public health endpoint via `GET /health`
- Manual trigger via `GET /renew/trigger` (and backward-compatible alias `GET /renew`)
- Scheduled renewal every day at midnight (`@Scheduled(cron = "0 0 0 * * ?")`)
- Centralized config for gift code and campaign id
- Unit and integration tests for success, missing-token, and downstream-error paths

## Redeem Link

Library link:

`https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=1fd71a2edc5d2d0f`

## Configuration

Set these environment variables before running:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `NYTIMES_OAUTH2_PRINCIPAL_NAME` (the Google user email that has logged in)

Optional overrides:

- `NYTIMES_REDEEM_URL` (default: `https://api.nytimes.com/svc/subscription/redeem`)
- `NYTIMES_CAMPAIGN_ID` (default: `87LH8`)
- `NYTIMES_GIFT_CODE` (default: `1fd71a2edc5d2d0f`)
- `NYTIMES_OAUTH2_REGISTRATION_ID` (default: `google`)

All defaults live in `src/main/resources/application.yml`.

## Get Google Client ID And Secret

You need OAuth credentials for the Google app registration used by this service.

1. Open Google Cloud Console:
   - `https://console.cloud.google.com/`
2. Select an existing project or create one.
3. Go to **APIs & Services** -> **OAuth consent screen**:
   - Configure the consent screen if this is your first OAuth app in the project.
   - Add your Google user as a test user if the app is in testing mode.
4. Go to **APIs & Services** -> **Credentials** -> **Create Credentials** -> **OAuth client ID**.
5. Choose **Web application**.
6. Add authorized redirect URI:
   - `http://localhost:8080/login/oauth2/code/google`
7. Create the client and copy:
   - **Client ID** -> `GOOGLE_CLIENT_ID`
   - **Client secret** -> `GOOGLE_CLIENT_SECRET`

Notes:

- If you only have an existing client ID but not the secret, Google may not show the full secret again. In that case, create a new OAuth client and update this app with the new values.
- Keep the client secret private. Do not commit it to git.

Example for local shell session:

```bash
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
export NYTIMES_OAUTH2_PRINCIPAL_NAME=you@example.com
```

Example for bundle creation:

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
make bundle GOOGLE_CLIENT_ID=your-client-id GOOGLE_CLIENT_SECRET=your-client-secret
```

## Endpoints

- `GET /health` - Public health check, returns `ok`
- `GET /renew/trigger` - Authenticated endpoint to trigger renew immediately
- `GET /renew` - Authenticated backward-compatible alias for `/renew/trigger`

## How It Works

1. User authenticates with Google through Spring Security OAuth2 login.
2. Spring stores the authorized client and refresh token support is enabled.
3. `SubscriptionService` builds a bearer-authenticated POST request to the redeem endpoint.
4. `DailyJob` runs nightly and calls the same service method as the manual endpoints.
5. `NYTimesController` exposes manual trigger endpoints so you can test renew instantly.

## Authentication Notes

- `/health` is public.
- `/renew` and `/renew/trigger` require an authenticated session.
- If no authorized OAuth2 client is available yet, renew returns:
  - `Failed to retrieve access token. Login once with Google OAuth2 to cache an authorized client.`

## Run

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
mvn spring-boot:run
```

Then:

- Visit `http://localhost:8080/renew/trigger` after OAuth2 login to trigger manual renewal immediately.
- `http://localhost:8080/renew` can be used as an alias.
- Visit `http://localhost:8080/health` for a public health check.

## Build And Bundle (Makefile)

This project includes a `Makefile` with targets to test, build, and create a runnable bundle.

Available targets:

- `make test` - run test suite
- `make build` - build jar (`target/maricopa-lib-nytimes-0.0.1-SNAPSHOT.jar`)
- `make bundle GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=...` - create distributable bundle
- `make run-bundle EMAIL=you@example.com` - run bundled app

Create bundle:

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
make bundle GOOGLE_CLIENT_ID=your-google-client-id GOOGLE_CLIENT_SECRET=your-google-client-secret
```

Bundle output:

- `bundle/maricopa-lib-nytimes/app.jar`
- `bundle/maricopa-lib-nytimes/bundle.env`
- `bundle/maricopa-lib-nytimes/run-bundled.sh`

Run bundled app (only parameter needed is Google email):

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
bundle/maricopa-lib-nytimes/run-bundled.sh you@example.com
```

The bundle stores `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in `bundle.env` at bundle time, so runtime only needs the email argument.

## Test

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
mvn test
```

Current coverage includes:

- `SubscriptionService` unit tests for token-missing and successful redeem behavior.
- `NYTimesController` integration tests for:
  - `/health` public access
  - `/renew` success path
  - `/renew/trigger` success path
  - missing authorized client path
  - downstream NYTimes 500 error path

## Troubleshooting

- **`/renew` or `/renew/trigger` returns access token message**
  - Symptom: `Failed to retrieve access token. Login once with Google OAuth2 to cache an authorized client.`
  - Check that `NYTIMES_OAUTH2_PRINCIPAL_NAME` matches the Google account email used to log in.
  - Trigger OAuth2 login once in the same app session, then call renew again.

- **401/redirect when calling renew endpoints**
  - `/renew` and `/renew/trigger` require authentication.
  - Use `/health` for unauthenticated checks.

- **Downstream NYTimes failure (`Subscription failed with status: ...`)**
  - Verify `NYTIMES_REDEEM_URL`, `NYTIMES_CAMPAIGN_ID`, and `NYTIMES_GIFT_CODE` values.
  - Confirm your OAuth2 token is valid for the configured principal.
  - Retry after a short interval if the NYTimes API is transiently unavailable.

- **Check logs while testing locally**

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
mvn spring-boot:run
```

- **Re-run tests to validate behavior quickly**

```bash
cd /home/kamal-bahadur-nextiva-com/git/maricopa-lib-nytimes
mvn test
```

