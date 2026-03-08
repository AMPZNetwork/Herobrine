package com.ampznetwork.herobrine.feature.chatmod.model;

import com.ampznetwork.herobrine.repo.ChannelBridgeConfigRepo;
import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Value
public class GuildChannelNameAutoFillProvider implements IAutoFillProvider {
    @Override
    public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
        return usage.fromContext(Guild.class)
                .flatMap(guild -> bean(ChannelBridgeConfigRepo.class).findAllByGuildId(guild.getIdLong()).stream())
                .map(ChannelBridgeConfig::getName);
    }
}
