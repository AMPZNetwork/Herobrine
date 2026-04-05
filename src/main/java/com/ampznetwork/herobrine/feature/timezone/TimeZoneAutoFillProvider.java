package com.ampznetwork.herobrine.feature.timezone;

import org.comroid.annotations.Instance;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;

import java.time.ZoneId;
import java.util.stream.Stream;

public enum TimeZoneAutoFillProvider implements Completion.Provider.OfStrings {
    @Instance INSTANCE;

    @Override
    public Stream<String> findCompletionValues(InteractionContext context, ParameterNode parameter, String currentValue) {
        return ZoneId.getAvailableZoneIds().stream().filter(str -> str.contains(currentValue));
    }
}
