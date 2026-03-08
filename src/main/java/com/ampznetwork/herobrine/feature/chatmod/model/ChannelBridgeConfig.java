package com.ampznetwork.herobrine.feature.chatmod.model;

import com.ampznetwork.chatmod.api.model.config.channel.Channel;
import com.ampznetwork.chatmod.api.model.config.discord.DiscordChannel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.comroid.api.attr.Named;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@IdClass(ChannelBridgeConfig.Key.class)
public class ChannelBridgeConfig implements Named {
    @Id long guildId;
    @Id long channelId;
    String rabbitUri;
    String channelName;
    @Nullable String displayName;
    @Nullable String inviteUrl;

    @Override
    public String getName() {
        return channelName;
    }

    @Override
    public String getAlternateName() {
        return displayName;
    }

    public Channel toChannel() {
        return new Channel(true,
                channelName,
                null,
                Objects.requireNonNullElse(displayName, channelName),
                null,
                new DiscordChannel(null, channelName, channelId, null, inviteUrl),
                true);
    }

    public Key key() {
        return new Key(guildId, channelId);
    }

    public record Key(long guildId, long channelId) {}
}
