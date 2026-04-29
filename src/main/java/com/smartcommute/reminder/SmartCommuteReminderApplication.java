package com.smartcommute.reminder;

import java.util.concurrent.ScheduledExecutorService;

public final class SmartCommuteReminderApplication {
    private SmartCommuteReminderApplication() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        CommuteMonitor monitor = new CommuteMonitor(config, new GoogleMapsService(), new SlackNotifier());
        ScheduledExecutorService executorService = monitor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
    }
}
