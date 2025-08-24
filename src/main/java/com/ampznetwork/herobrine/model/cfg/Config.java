package com.ampznetwork.herobrine.model.cfg;

import lombok.Data;
import org.comroid.api.data.seri.DataNode;

@Data
public class Config implements DataNode {
    DiscordInfo  discord  = new DiscordInfo();
    DatabaseInfo database = new DatabaseInfo();
}
