package com.smartcommute.reminder;

public final class SmartCommuteReminderApplication {
    private SmartCommuteReminderApplication() {
    }

    public static void main(String[] args) {
        System.out.println(buildStartupMessage());
    }

    static String buildStartupMessage() {
        return "Smart Commute Reminder is set up and ready.";
    }
}
