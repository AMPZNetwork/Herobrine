package com.ampznetwork.herobrine.feature.chatmod.model;

import com.ampznetwork.herobrine.repo.ChannelBridgeConfigRepo;
import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;

import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Value
public class GuildChannelNameAutoFillProvider implements Completion.Provider {
    @Override
    public Stream<Completion.Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue) {
        return context.children(Guild.class)
                .flatMap(guild -> bean(ChannelBridgeConfigRepo.class).findAllByGuildId(guild.getIdLong()).stream())
                .map(config -> new Completion.Option(config.getName(), config.getDisplayName()));
    }
}
