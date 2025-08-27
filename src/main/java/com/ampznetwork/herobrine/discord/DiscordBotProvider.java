package com.ampznetwork.herobrine.discord;

import com.ampznetwork.chatmod.api.model.config.ChatModules;
import com.ampznetwork.chatmod.api.model.config.channel.Channel;
import com.ampznetwork.chatmod.core.ModuleContainerCore;
import com.ampznetwork.chatmod.core.module.impl.LinkToDiscordModule;
import com.ampznetwork.herobrine.model.cfg.Config;
import com.ampznetwork.libmod.api.interop.game.PlayerIdentifierAdapter;
import com.ampznetwork.libmod.api.model.info.ServerInfoProvider;
import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.comroid.api.tree.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Value
@Component
public class DiscordBotProvider extends Container.Base implements ModuleContainerCore {
    @Autowired ServerInfoProvider      lib;
    @Autowired PlayerIdentifierAdapter playerAdapter;
    @Autowired Config                  config;

    @Override
    public ChatModules getChatModules() {
        return config.getDiscord().getModules();
    }

    @Override
    public List<Channel> getChannels() {
        return config.getDiscord().getChannels();
    }

    @Override
    public boolean isListenerCompatibilityMode() {
        return false;
    }

    @Override
    public LinkToDiscordModule getDefaultModule() {
        return child(LinkToDiscordModule.class).assertion();
    }

    @Override
    public Stream<Object> streamOwnChildren() {
        return Stream.of(lib, playerAdapter, config);
    }

    @Override
    @PostConstruct
    public void start() {
        super.start();
        initModules();
    }
}
