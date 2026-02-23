package com.ampznetwork.herobrine.feature.timezone;

import org.comroid.annotations.Instance;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.time.ZoneId;
import java.util.stream.Stream;

public enum TimeZoneAutoFillProvider implements IAutoFillProvider {
    @Instance INSTANCE;

    @Override
    public Stream<String> autoFill(CommandUsage usage, String argName, String currentValue) {
        return ZoneId.getAvailableZoneIds().stream().filter(str -> str.contains(currentValue));
    }
}
