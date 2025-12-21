package com.ampznetwork.herobrine.chatmod;

import com.ampznetwork.chatmod.api.model.config.ChatModules;
import lombok.Value;
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
                .map(ChatModules.NamedBaseConfig::getName);
    }
}
