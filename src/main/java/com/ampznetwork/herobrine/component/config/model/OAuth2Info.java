package com.ampznetwork.herobrine.component.config.model;

import lombok.Data;
import org.comroid.annotations.Ignore;
import org.comroid.api.config.ConfigurationManager;

@Data
public class OAuth2Info {
    String name;
    String clientId;
    @Ignore(ConfigurationManager.Presentation.class) String secret;
    String scope;
    String redirectUrl;
    String authorizationUrl;
    String tokenUrl;
    String userInfoUrl;
    String userNameAttributeName;
}
