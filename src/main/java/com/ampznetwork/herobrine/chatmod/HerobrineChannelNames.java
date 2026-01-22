package com.ampznetwork.herobrine.chatmod;

import lombok.Value;
import org.comroid.api.attr.Named;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Value
public class HerobrineChannelNames implements IAutoFillProvider {
    @Override
    public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
        return bean(RabbitChatConnector.ChannelBindings.class).keySet()
                .stream()
                .map(Named::getName);
    }
}
