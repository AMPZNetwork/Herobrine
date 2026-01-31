package com.ampznetwork.herobrine.config.model;

import lombok.Data;

@Data
public class OAuth2Info {
    String name;
    String clientId;
    String secret;
    String scope;
    String redirectUrl;
    String authorizationUrl;
    String tokenUrl;
    String userInfoUrl;
    String userNameAttributeName;
}
