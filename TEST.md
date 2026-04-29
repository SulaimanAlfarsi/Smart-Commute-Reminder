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
origins=23.433256805800355,58.471094768840096
destinations=23.57199250778186,58.33931345805371
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
- `CommuteHistoryStoreTest` passes
- `CommuteMonitorTest` passes
- `CommuteSchedulePolicyTest` passes
- `GoogleMapsServiceTest` passes
- `SmartCommuteReminderApplicationTest` passes
- `WeeklySummaryGeneratorTest` passes

## 2. Check Config Values

Open:

- `src/main/resources/application.properties`

Expected values:

```properties
home.location=23.433256805800355, 58.471094768840096
work.location=23.57199250778186, 58.33931345805371
home.name=Al Amerat Home
work.name=Ghala Software House
polling.interval.minutes=5
notification.cooldown.minutes=5
commute.days=SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY
morning.window.start=06:00
morning.window.end=10:00
evening.window.enabled=true
evening.window.start=16:00
evening.window.end=21:00
history.file=data/commute-history.csv
summary.bucket.minutes=30
summary.top.slots=3
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
- `src/main/java/com/smartcommute/reminder/CommuteMonitor.java`
- `src/main/java/com/smartcommute/reminder/CommuteSchedulePolicy.java`
- `src/main/java/com/smartcommute/reminder/GoogleMapsService.java`
- `src/main/java/com/smartcommute/reminder/SlackNotifier.java`
- `src/main/java/com/smartcommute/reminder/SmartCommuteReminderApplication.java`

Verify:

- `AppConfig` reads `home.location`
- `AppConfig` reads `work.location`
- `AppConfig` reads `polling.interval.minutes`
- `AppConfig` reads `notification.cooldown.minutes`
- `AppConfig` reads commute days and time windows
- `AppConfig` reads history and summary settings
- `AppConfig` reads secrets from environment variables first
- `AppConfig` falls back to `.env`
- `CommuteSchedulePolicy` allows polling only during configured windows
- `CommuteSchedulePolicy` uses home to work during the morning window
- `CommuteSchedulePolicy` uses work to home during the evening window
- `CommuteMonitor` skips Google calls outside configured windows
- `CommuteMonitor` tracks best commute time in memory per direction
- `CommuteMonitor` sends Slack when a new best time is found or the commute becomes slower than the previous check
- `CommuteMonitor` applies notification cooldown
- `CommuteMonitor` logs every successful commute result to history
- `SmartCommuteReminderApplication` starts scheduled polling
- `SmartCommuteReminderApplication` supports summary mode
- `GoogleMapsService` calls Google Distance Matrix API
- `GoogleMapsService` uses `departure_time=now`
- `GoogleMapsService` uses `traffic_model=best_guess`
- `SlackNotifier` prepares a Slack webhook JSON message

## 6. Manual Full App Test

Use this only when you are ready to make a real Google Maps API request and possibly send one Slack message.

Before running:

- confirm `.env` contains real `GOOGLE_MAPS_API_KEY`
- confirm `.env` contains real `SLACK_WEBHOOK_URL`
- confirm current day is listed in `commute.days`
- confirm current time is inside `morning.window.start` and `morning.window.end`

Run:

```powershell
.\mvnw.cmd -q exec:java
```

Expected console output:

- route is printed
- direction is printed
- distance is printed
- normal duration is printed
- traffic duration is printed
- successful result is appended to `data/commute-history.csv`
- first result becomes the current best time
- Slack notification is sent for the first best time

Stop the app after the first cycle:

```text
Ctrl+C
```

Important:

- The app runs continuously after startup.
- It polls every `polling.interval.minutes`.
- With the current config, that means every 5 minutes during allowed commute windows.
- Slack can send at most every 5 minutes because `notification.cooldown.minutes=5`.
- Slack is sent only when a new best time is found or traffic becomes slower than the previous successful check.
- Stop it manually if you only want one test request.

## 7. Manual Skip Test

Use this to confirm the app avoids Google Maps requests outside commute windows.

Temporarily set `morning.window.start` and `morning.window.end` to a time range that does not include the current time.

Example:

```properties
morning.window.start=06:00
morning.window.end=06:01
evening.window.enabled=false
```

Run:

```powershell
.\mvnw.cmd -q exec:java
```

Expected:

- console prints `Skipping Google Maps request outside commute window`
- no Google Maps API request should be made
- no Slack message should be sent

Stop the app:

```text
Ctrl+C
```

After the skip test, put the real window back:

```properties
morning.window.start=06:00
morning.window.end=10:00
evening.window.enabled=true
evening.window.start=16:00
evening.window.end=21:00
```

## 8. Commute History Log Test

After running the app during an allowed window, open:

- `data/commute-history.csv`

Expected columns:

```text
timestamp,direction,duration_in_traffic_minutes,distance_meters,duration_in_traffic_text,distance_text
```

Expected behavior:

- one row is added for each successful Google Maps result
- morning rows use `HOME_TO_WORK`
- evening rows use `WORK_TO_HOME`
- this file is ignored by Git because it is runtime data

Check Git does not include the runtime data:

```powershell
git status --ignored --short
```

Expected:

- `!! data/`

## 9. Weekly Summary Test

Use this after you have collected some observations in `data/commute-history.csv`.

Run:

```powershell
.\mvnw.cmd -q exec:java -Dexec.args="summary"
```

Expected output:

```text
Weekly commute summary, last 7 days
```

The summary shows the best observed 30-minute buckets per direction.

Example shape:

```text
- home to work: 06:30-07:00, avg 24 min, best 21 min, samples 5
- work to home: 18:00-18:30, avg 28 min, best 24 min, samples 4
```

If there is no history yet, expected output:

```text
No commute observations were found for the last 7 days.
```

Use the summary to decide smarter windows after enough samples. For example, if the weekly summary repeatedly shows the best morning buckets around `07:00-08:00`, narrow the morning window later.

## 10. Direction Behavior

The app uses different route directions based on the current time:

- `06:00` to `10:00`: home to work
- `16:00` to `21:00`: work to home

Both windows run only on:

```text
SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY
```

## 11. Copy-Paste Commands

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

Run app manually:

```powershell
.\mvnw.cmd -q exec:java
```

Generate weekly summary:

```powershell
.\mvnw.cmd -q exec:java -Dexec.args="summary"
```

## 12. Postman Copy-Paste Values

Google Maps morning request, home to work:

```text
GET https://maps.googleapis.com/maps/api/distancematrix/json?origins=23.433256805800355,58.471094768840096&destinations=23.57199250778186,58.33931345805371&departure_time=now&traffic_model=best_guess&key=YOUR_GOOGLE_MAPS_API_KEY
```

Google Maps evening request, work to home:

```text
GET https://maps.googleapis.com/maps/api/distancematrix/json?origins=23.57199250778186,58.33931345805371&destinations=23.433256805800355,58.471094768840096&departure_time=now&traffic_model=best_guess&key=YOUR_GOOGLE_MAPS_API_KEY
```

Slack request body:

```json
{
  "text": "Postman test from Smart Commute Reminder"
}
```
