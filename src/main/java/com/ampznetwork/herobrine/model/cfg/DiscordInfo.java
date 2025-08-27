package com.ampznetwork.herobrine.model.cfg;

import com.ampznetwork.chatmod.api.model.config.ChatModules;
import com.ampznetwork.chatmod.api.model.config.channel.Channel;
import lombok.Data;

import java.util.List;

@Data
public class DiscordInfo {
    ChatModules   modules;
    List<Channel> channels;
}
