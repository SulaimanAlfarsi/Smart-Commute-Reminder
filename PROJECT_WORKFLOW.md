# Smart Commute Reminder Workflow

This project monitors commute time between home and work using Google Maps traffic data, then sends Slack alerts only when the commute changes in an important way.

## Main Flow

1. The app loads `src/main/resources/application.properties`.
2. The app loads secrets from environment variables or local `.env`.
3. The scheduler starts immediately.
4. Every `polling.interval.minutes`, the app checks whether the current day and time are inside a configured commute window.
5. If outside the commute window, the app skips Google Maps to avoid unnecessary API requests.
6. If inside the commute window, the app calls Google Maps Distance Matrix API with `departure_time=now` and `traffic_model=best_guess`.
7. The app logs every successful commute result to `data/commute-history.csv`.
8. The app compares the current traffic time against the best time and the previous check.
9. Slack is sent only when the commute becomes a new best time or becomes slower than the previous check.
10. Weekly summary mode can read the history file and show the best observed time buckets.

## Current Route Config

```properties
home.location=23.433256805800355, 58.471094768840096
work.location=23.57199250778186, 58.33931345805371
home.name=Al Amerat Home
work.name=Ghala Software House
```

The coordinates are used for Google Maps API requests.
The names are used only for readable console and Slack messages.

## Direction Rules

Morning window:

```text
06:00 to 10:00
Al Amerat Home -> Ghala Software House
```

Evening window:

```text
15:30 to 21:00
Ghala Software House -> Al Amerat Home
```

Active days:

```text
SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY
```

## Notification Rules

The app polls every 5 minutes during the active window, but it does not send Slack every time.

Slack is sent when:

- The current commute time is the first result for that direction.
- The current commute time is lower than the previous best time.
- The current commute time is higher than the previous successful check.

Slack is skipped when:

- The app is outside the commute window.
- The commute time is unchanged.
- The commute time improves compared with the previous check but is not a new best.
- The Slack cooldown has not passed.

Current cooldown:

```properties
notification.cooldown.minutes=5
```

## Example Behavior

```text
32 min -> Slack sent, first best
34 min -> Slack sent, commute became slower
33 min -> Slack skipped, better than last check but not a new best
30 min -> Slack sent, new best
30 min -> Slack skipped, no important change
35 min -> Slack sent, commute became slower
```

## Runtime Files

Commute history is written to:

```text
data/commute-history.csv
```

This file is runtime data and should stay ignored by Git.

Local secrets should be stored in environment variables or `.env`.
Do not commit real API keys or Slack webhook URLs.

Required secret keys:

```env
GOOGLE_MAPS_API_KEY=your_google_maps_key
SLACK_WEBHOOK_URL=your_slack_webhook_url
```

## Run Commands

Build and test:

```powershell
.\mvnw.cmd package
```

Run the app:

```powershell
java -jar target\smart-commute-reminder-1.0-SNAPSHOT.jar
```

Generate weekly summary:

```powershell
.\mvnw.cmd -q exec:java -Dexec.args="summary"
```

Stop the running app:

```text
Ctrl+C
```

## Cost Control

The app avoids Google Maps requests outside configured windows.

With the current config, Google Maps is called at most every 5 minutes during:

- Morning commute window
- Evening commute window
- Sunday through Thursday only

To reduce API usage, increase:

```properties
polling.interval.minutes=10
```

or narrow the commute windows after you collect enough history.
