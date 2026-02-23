package com.ampznetwork.herobrine.feature.analyzer.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

@Value
public class PlaintextLogEntry implements ToplevelLogComponent, CharSequence {
    @Delegate String text;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Builder implements org.comroid.api.func.ext.Builder<PlaintextLogEntry> {
        String text;

        @Override
        public PlaintextLogEntry build() {
            return new PlaintextLogEntry(text);
        }
    }
}
