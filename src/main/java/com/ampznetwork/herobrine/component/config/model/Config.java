package com.ampznetwork.herobrine.component.config.model;

import lombok.Data;
import org.comroid.api.data.seri.DataNode;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config implements DataNode {
    DatabaseInfo     database  = new DatabaseInfo();
    DiscordInfo      discord   = new DiscordInfo();
    LuckPermsInfo    luckperms = new LuckPermsInfo();
    List<OAuth2Info> oAuth2    = new ArrayList<>();
}
