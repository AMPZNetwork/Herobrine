package com.ampznetwork.herobrine.config.model;

import lombok.Data;
import org.comroid.annotations.Default;

@Data
public class DatabaseInfo {
    @Default("jdbc:mariadb://localhost:3306/dev") String uri;
    @Default("dev")                               String username;
    @Default("dev")                               String password;
}
