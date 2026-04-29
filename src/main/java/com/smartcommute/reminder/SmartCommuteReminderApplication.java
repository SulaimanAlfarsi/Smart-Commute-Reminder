package com.smartcommute.reminder;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;

public final class SmartCommuteReminderApplication {
    private SmartCommuteReminderApplication() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        if (args.length > 0 && "summary".equalsIgnoreCase(args[0])) {
            printWeeklySummary(config);
            return;
        }

        CommuteMonitor monitor = new CommuteMonitor(config, new GoogleMapsService(), new SlackNotifier());
        ScheduledExecutorService executorService = monitor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
    }

    private static void printWeeklySummary(AppConfig config) {
        CommuteHistoryStore historyStore = new CommuteHistoryStore(config.getHistoryFile());
        WeeklySummaryGenerator summaryGenerator = new WeeklySummaryGenerator();
        WeeklyCommuteSummary summary = summaryGenerator.generate(
                historyStore.readAll(),
                LocalDateTime.now(),
                config.getSummaryBucketMinutes(),
                config.getSummaryTopSlots()
        );

        System.out.println(summaryGenerator.format(summary));
    }
}
