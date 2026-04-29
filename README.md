# Smart Commute Reminder 🚗

Smart Commute Reminder is a Java-based application that monitors the travel time between your home and your software house using real-time traffic data from the Google Maps Distance Matrix API.

The app checks the commute time periodically, keeps track of the best/minimum travel time recorded during the current running session, and sends a Slack notification when a new best commute time is detected.

The goal is to help the user know the best time to leave for work.

---

## 📌 Project Description

As a developer, I want to build a Java-based smart commute reminder application that monitors the travel time between my home and my software house in real time using traffic data from the Google Maps Distance Matrix API.

The app periodically checks the commute duration, keeps track of the best minimum travel time recorded so far, and automatically sends a Slack notification when a new minimum is detected.

This helps the user know when it is the perfect time to leave for work.

---

## 🎯 Project Objectives

This project helps practice:

- Building a Java application
- Working with scheduled background tasks
- Calling external REST APIs
- Using Google Maps Distance Matrix API
- Using Slack Incoming Webhooks
- Parsing JSON responses
- Reading configuration from `application.properties`
- Using environment variables for secrets
- Handling API errors safely
- Deploying a Java app on Oracle Linux
- Running a Java app as a background service using `systemd`

---

## ✅ Acceptance Criteria

The application should meet the following requirements:

- The app reads the home location from `application.properties`
- The app reads the software house/work location from `application.properties`
- The app reads the polling interval from `application.properties`
- Every polling cycle, the app calls the Google Maps Distance Matrix API
- The app gets traffic-aware travel duration
- The app displays the current travel time in minutes
- The app displays the distance in kilometers
- The app keeps track of the minimum travel time recorded during the current session
- When a new minimum travel time is detected, the app sends a Slack notification
- The Slack notification includes:
    - Current travel time
    - Distance
    - Route
    - Motivational message to leave now
- The app should not send Slack notifications too frequently
- The app should use a notification cooldown, for example 5 minutes
- API keys and Slack webhook URL must not be hardcoded
- Secrets must not be pushed to GitHub
- The app handles errors gracefully
- The app should run on Oracle Linux
- The app should run as a background service

---

## 🧠 How the App Works

The app follows this simple flow:

```text
1. Start application
2. Load config from application.properties
3. Read secrets from environment variables
4. Start scheduled polling task
5. Call Google Maps API
6. Get distance and traffic-aware duration
7. Print commute information in console
8. Compare current travel time with best time so far
9. If current time is better:
      - Update best time
      - Send Slack notification if cooldown allows
10. Repeat after the configured polling interval


smart-commute-reminder/
├── pom.xml
├── README.md
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sulaiman/
│   │   │           └── commute/
│   │   │               ├── Main.java
│   │   │               ├── AppConfig.java
│   │   │               ├── CommuteMonitor.java
│   │   │               ├── GoogleMapsService.java
│   │   │               ├── SlackNotifier.java
│   │   │               └── CommuteResult.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/
│
└── target/