# Testing Guide

Use this file to test the current project state.

## 0. Postman Manual Testing

You can manually test the external integrations in Postman before the full app flow is wired.

### 0.1 Google Maps Distance Matrix API

Method:

```text
GET
```

URL:

```text
https://maps.googleapis.com/maps/api/distancematrix/json
```

Query params:

```text
origins=23.57331801470762,58.33842328754992
destinations=23.43318302828463,58.47086190334765
departure_time=now
traffic_model=best_guess
key=YOUR_GOOGLE_MAPS_API_KEY
```

Expected:

- top-level `status` is `OK`
- `rows[0].elements[0].status` is `OK`
- `distance.text` exists
- `duration.text` exists
- `duration_in_traffic.text` exists

Fields to inspect in Postman response:

```text
status
origin_addresses[0]
destination_addresses[0]
rows[0].elements[0].status
rows[0].elements[0].distance.text
rows[0].elements[0].distance.value
rows[0].elements[0].duration.text
rows[0].elements[0].duration.value
rows[0].elements[0].duration_in_traffic.text
rows[0].elements[0].duration_in_traffic.value
```

### 0.2 Slack Webhook

Method:

```text
POST
```

Webhook URL:

```text
YOUR_SLACK_WEBHOOK_URL
```

Headers:

```text
Content-Type: application/json
```

Body:

```json
{
  "text": "Postman test from Smart Commute Reminder"
}
```

Expected:

- Slack returns success
- message appears in the target Slack channel

## 1. Automated Tests

Run:

```powershell
.\mvnw.cmd test
```

Expected:

- Maven build succeeds
- `AppConfigTest` passes
- `GoogleMapsServiceTest` passes
- `SmartCommuteReminderApplicationTest` passes

## 2. Check Config Values

Open:

- `src/main/resources/application.properties`

Expected values:

```properties
home.location=23.57331801470762, 58.33842328754992
work.location=23.43318302828463, 58.47086190334765
polling.interval.minutes=30
notification.cooldown.minutes=5
```

## 3. Check Local Secrets

Open your local `.env` file.

Expected keys:

```env
GOOGLE_MAPS_API_KEY=your_real_key
SLACK_WEBHOOK_URL=your_real_webhook
```

Important:

- `.env` must stay local
- `.env` must not appear in `git status`

Check:

```powershell
git status
```

Expected:

- `.env` is not listed

## 4. Verify Google Response Fixture

Open:

- `src/test/resources/google-distance-matrix-response.json`

Compare it with your real Google Maps response from Postman.

Make sure these fields exist in the real response:

- `status`
- `origin_addresses[0]`
- `destination_addresses[0]`
- `rows[0].elements[0].status`
- `rows[0].elements[0].distance.text`
- `rows[0].elements[0].distance.value`
- `rows[0].elements[0].duration.text`
- `rows[0].elements[0].duration_in_traffic.text`
- `rows[0].elements[0].duration_in_traffic.value`

## 5. Manual Code Checks

Open these files:

- `src/main/java/com/smartcommute/reminder/AppConfig.java`
- `src/main/java/com/smartcommute/reminder/GoogleMapsService.java`
- `src/main/java/com/smartcommute/reminder/SlackNotifier.java`

Verify:

- `AppConfig` reads `home.location`
- `AppConfig` reads `work.location`
- `AppConfig` reads `polling.interval.minutes`
- `AppConfig` reads `notification.cooldown.minutes`
- `AppConfig` reads secrets from environment variables first
- `AppConfig` falls back to `.env`
- `GoogleMapsService` calls Google Distance Matrix API
- `GoogleMapsService` uses `departure_time=now`
- `GoogleMapsService` uses `traffic_model=best_guess`
- `SlackNotifier` prepares a Slack webhook JSON message

## 6. Current Limits

You can test these parts now:

- config loading
- JSON parsing
- Slack notifier code presence
- build and tests

You cannot fully test these parts yet:

- scheduled polling
- best commute tracking
- cooldown enforcement
- automatic Slack notification flow
- full startup-to-notification app behavior

## 7. Copy-Paste Commands

Run tests:

```powershell
.\mvnw.cmd test
```

Check repo status:

```powershell
git status
```

Package app:

```powershell
.\mvnw.cmd package
```

## 8. Postman Copy-Paste Values

Google Maps request:

```text
GET https://maps.googleapis.com/maps/api/distancematrix/json?origins=23.57331801470762,58.33842328754992&destinations=23.43318302828463,58.47086190334765&departure_time=now&traffic_model=best_guess&key=YOUR_GOOGLE_MAPS_API_KEY
```

Slack request body:

```json
{
  "text": "Postman test from Smart Commute Reminder"
}
```
