package com.ampznetwork.herobrine.model.cfg;

import lombok.Data;
import org.comroid.api.data.seri.DataNode;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config implements DataNode {
    DiscordInfo      discord  = new DiscordInfo();
    DatabaseInfo     database = new DatabaseInfo();
    List<OAuth2Info> oAuth2   = new ArrayList<>();
}
