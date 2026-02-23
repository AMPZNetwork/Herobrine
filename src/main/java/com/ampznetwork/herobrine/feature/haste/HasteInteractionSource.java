package com.ampznetwork.herobrine.feature.haste;

import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;

import java.util.stream.Stream;

public interface HasteInteractionSource {
    Stream<ActionRowChildComponent> createHasteInteraction(String id);
}
