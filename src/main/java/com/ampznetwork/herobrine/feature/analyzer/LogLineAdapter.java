package com.ampznetwork.herobrine.feature.analyzer;

import com.ampznetwork.herobrine.feature.analyzer.model.ExceptionEntry;
import com.ampznetwork.herobrine.feature.analyzer.model.LogComponent;
import com.ampznetwork.herobrine.feature.analyzer.model.LogEntry;
import com.ampznetwork.herobrine.feature.analyzer.model.PlaintextLogEntry;
import com.ampznetwork.herobrine.feature.analyzer.model.StackTraceElementEntry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.comroid.api.func.ext.Builder;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum LogLineAdapter implements Predicate<String>, Function<String, Builder<? extends LogComponent>> {
    LOG_MESSAGE(LogEntry.PATTERN) {
        @Override
        public LogEntry.Builder apply(String line) {
            var matcher = tryMatch(line);
            return new LogEntry.Builder(LogEntry.DATETIME.parse(matcher.group("datetime")),
                    matcher.group("thread"),
                    matcher.group("level"),
                    matcher.group("logger"),
                    matcher.group("component"),
                    matcher.group("message"),
                    null);
        }
    }, EXCEPTION_HEAD(ExceptionEntry.PATTERN) {
        @Override
        public ExceptionEntry.Builder apply(String line) {
            var matcher = tryMatch(line);
            return new ExceptionEntry.Builder(matcher.group("exception"),
                    matcher.group("message"),
                    null,
                    new ArrayList<>());
        }
    }, EXCEPTION_STACKTRACE(StackTraceElementEntry.PATTERN) {
        @Override
        public StackTraceElementEntry.Builder apply(String line) {
            var matcher = tryMatch(line);
            var lineTxt = matcher.group("line");
            return new StackTraceElementEntry.Builder(matcher.group("class"),
                    matcher.group("method"),
                    matcher.group("file"),
                    lineTxt == null || lineTxt.isBlank() ? null : Integer.parseInt(lineTxt));
        }
    }, PLAINTEXT(Pattern.compile(".+")) {
        @Override
        public PlaintextLogEntry.Builder apply(String line) {
            return new PlaintextLogEntry.Builder(line);
        }
    };

    Pattern pattern;

    @Override
    public boolean test(String line) {
        return pattern.matcher(line).matches();
    }

    Matcher tryMatch(String line) {
        var matcher = getPattern().matcher(line);
        if (!matcher.matches()) throw new IllegalStateException("apply() called when test() returned false");
        return matcher;
    }
}
