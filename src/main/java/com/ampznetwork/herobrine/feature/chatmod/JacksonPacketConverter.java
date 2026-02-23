package com.ampznetwork.herobrine.feature.chatmod;

import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.comroid.api.ByteConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JacksonPacketConverter implements ByteConverter<ChatMessagePacket> {
    @Autowired ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public byte[] toBytes(ChatMessagePacket it) {
        return objectMapper.writeValueAsBytes(it);
    }

    @Override
    @SneakyThrows
    public ChatMessagePacket fromBytes(byte[] bytes) {
        return objectMapper.readValue(bytes, ChatMessagePacket.class);
    }
}
