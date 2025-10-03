package com.ampznetwork.herobrine.model.logs;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Pattern;

@Value
public class LogEntry implements ToplevelLogComponent {
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("ddMMMyyy HH:mm:ss.SSS");
    public static final Pattern           PATTERN  = Pattern.compile(
            "\\[(?<datetime>[\\w.:/ ]+)] \\[(?<thread>[\\w._-]+)/(?<level>\\w+)] \\[(?<logger>[\\w._-]+)/(?<component>[\\w._-]+)?]: (?<message>.+)");
    TemporalAccessor datetime;
    String           threadName;
    String           logLevel;
    String           loggerName;
    String           componentName;
    String           message;
    @Nullable ExceptionEntry exception;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Builder implements org.comroid.api.func.ext.Builder<LogEntry> {
        TemporalAccessor datetime;
        String           threadName;
        String           logLevel;
        String           loggerName;
        String           componentName;
        String           message;
        @Nullable ExceptionEntry.Builder exception;

        @Override
        public LogEntry build() {
            return new LogEntry(datetime,
                    threadName,
                    logLevel,
                    loggerName,
                    componentName,
                    message,
                    exception == null ? null : exception.build());
        }
    }
}
