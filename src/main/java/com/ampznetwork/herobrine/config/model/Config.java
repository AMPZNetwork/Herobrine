package com.ampznetwork.herobrine.config.model;

import com.ampznetwork.chatmod.api.model.config.channel.Channel;
import lombok.Data;
import org.comroid.api.data.seri.DataNode;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config implements DataNode {
    DatabaseInfo     database  = new DatabaseInfo();
    DiscordInfo      discord   = new DiscordInfo();
    LuckPermsInfo    luckperms = new LuckPermsInfo();
    RabbitMqInfo     rabbitmq  = new RabbitMqInfo();
    List<Channel>    channels  = new ArrayList<>();
    List<OAuth2Info> oAuth2    = new ArrayList<>();
}
