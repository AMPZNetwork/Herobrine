package com.ampznetwork.herobrine.model.logs;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Pattern;

@Value
@Builder
public class LogEntry {
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("ddMMMyyy HH:mm:ss.SSS");
    public static final Pattern           PATTERN  = Pattern.compile(
            "\\[(?<datetime>[\\w.:/ ]+)] \\[(?<thread>[\\w._-]+)/(?<level>\\w+)] \\[(?<logger>[\\w._-]+)/(?<component>[\\w._-]+)?]: (?<message>.+)");
    TemporalAccessor datetime;
    String threadName;
    String logLevel;
    String loggerName;
    String componentName;
    String message;
    @Nullable ExceptionEntry exception;
}
