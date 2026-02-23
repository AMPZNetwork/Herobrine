package com.ampznetwork.herobrine.component.config.model;

import lombok.Data;
import org.comroid.annotations.Default;
import org.comroid.annotations.Ignore;
import org.comroid.api.config.ConfigurationManager;

@Data
public class DatabaseInfo {
    @Default("jdbc:mariadb://localhost:3306/dev")                    String uri;
    @Default("dev")                                                  String username;
    @Default("dev") @Ignore(ConfigurationManager.Presentation.class) String password;
}
