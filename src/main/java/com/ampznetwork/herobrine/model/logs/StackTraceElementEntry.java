package com.ampznetwork.herobrine.model.logs;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

@Value
@Builder
public class StackTraceElementEntry {
    public static final Pattern PATTERN = Pattern.compile(
            "\\s+at (?<class>[\\w$.]+)\\.(?<method>[\\w$<>]+)\\((?<file>[\\w-_$. ]+)(:(?<line>\\d+))?\\) ~\\[(.+):(.+)]");
    String className, method, sourceFile;
    @Nullable Integer line;
}
