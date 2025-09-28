package com.ampznetwork.herobrine.model.logs;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

@Value
@Builder
public class ExceptionEntry {
    public static final Pattern PATTERN = Pattern.compile("^(Caused by: )?(?<exception>[\\w.]+)(: (?<message>.+))?");
    String className;
    String message;
    @Nullable               ExceptionEntry               causedBy;
    @Singular("stackTrace") List<StackTraceElementEntry> stackTrace;
}
