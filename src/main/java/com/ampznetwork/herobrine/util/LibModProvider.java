package com.ampznetwork.herobrine.util;

import com.ampznetwork.libmod.api.model.info.ServerInfoProvider;
import lombok.Value;
import org.springframework.stereotype.Component;

@Value
@Component
public class LibModProvider implements ServerInfoProvider {
    String serverName = "AMPZ Discord";
}
