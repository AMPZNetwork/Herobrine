package com.ampznetwork.herobrine.model.logs;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

@Value
public class StackTraceElementEntry implements LogComponent {
    public static final Pattern PATTERN = Pattern.compile(
            "\\s+at (?<class>[\\w$.]+)\\.(?<method>[\\w$<>]+)\\((?<file>[\\w-_$. ]+)(:(?<line>\\d+))?\\) ~\\[(.+):(.+)]");
    String className;
    String method;
    String sourceFile;
    @Nullable Integer line;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Builder implements org.comroid.api.func.ext.Builder<StackTraceElementEntry> {
        String className;
        String method;
        String sourceFile;
        @Nullable Integer line;

        @Override
        public StackTraceElementEntry build() {
            return new StackTraceElementEntry(className, method, sourceFile, line);
        }
    }
}
