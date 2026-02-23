package com.ampznetwork.herobrine.feature.analyzer.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Value
public class ExceptionEntry implements LogComponent {
    public static final Pattern PATTERN = Pattern.compile("^(Caused by: )?(?<exception>[\\w.]+)(: (?<message>.+))?");

    String className;
    String message;
    @Nullable ExceptionEntry causedBy;
    List<StackTraceElementEntry> stackTrace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Builder implements org.comroid.api.func.ext.Builder<ExceptionEntry> {
        String className;
        String message;
        @Nullable ExceptionEntry.Builder causedBy;
        List<StackTraceElementEntry.Builder> stackTrace = new ArrayList<>();

        public Builder addStackTrace(StackTraceElementEntry.Builder element) {
            stackTrace.add(element);
            return this;
        }

        @Override
        public ExceptionEntry build() {
            return new ExceptionEntry(className,
                    message,
                    causedBy == null ? null : causedBy.build(),
                    stackTrace.stream().map(StackTraceElementEntry.Builder::build).toList());
        }
    }
}
