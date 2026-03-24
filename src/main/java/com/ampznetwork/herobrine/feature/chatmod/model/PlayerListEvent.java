package com.ampznetwork.herobrine.feature.chatmod.model;

import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

@Value
public class PlayerListEvent extends ApplicationEvent {
    LoadedBridge      bridge;
    ChatMessagePacket packet;
    Type              type;

    public PlayerListEvent(LoadedBridge bridge, ChatMessagePacket packet, Type type) {
        super(bridge);

        this.bridge = bridge;
        this.packet = packet;
        this.type   = type;
    }

    public enum Type {
        JOIN, LEAVE
    }
}
