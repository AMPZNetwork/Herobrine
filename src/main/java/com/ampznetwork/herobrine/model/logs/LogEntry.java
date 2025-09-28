package com.ampznetwork.herobrine.model.logs;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

@Value
@Builder
public class LogEntry {
    public static final Pattern PATTERN = Pattern.compile(
            "\\[(?<date>[\\w.:/]+) (?<time>[\\d.:/]+)] \\[(?<thread>[\\w._-]+)/(?<level>\\w+)] \\[(?<logger>[\\w._-]+)/(?<component>[\\w._-]+)?]: (?<message>.+)");
    String date;
    String time;
    String threadName;
    String logLevel;
    String loggerName;
    String componentName;
    String message;
    @Nullable ExceptionEntry exception;
}
