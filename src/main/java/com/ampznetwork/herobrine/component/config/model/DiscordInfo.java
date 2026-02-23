package com.ampznetwork.herobrine.component.config.model;

import lombok.Data;
import org.comroid.annotations.Ignore;
import org.comroid.api.config.ConfigurationManager;

@Data
public class DiscordInfo {
    @Ignore(ConfigurationManager.Presentation.class) String token;
}
