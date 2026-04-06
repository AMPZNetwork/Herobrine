package com.ampznetwork.herobrine.util;

import com.ampznetwork.chatmod.api.model.ServerInfoProvider;
import lombok.Value;
import org.springframework.stereotype.Component;

@Value
@Component
public class LibModProvider implements ServerInfoProvider {
    String serverName = "AMPZ Discord";
}
