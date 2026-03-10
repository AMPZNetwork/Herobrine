package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.chatmod.model.ChannelBridgeConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ChannelBridgeConfigRepo extends CrudRepository<ChannelBridgeConfig, ChannelBridgeConfig.Key> {
    Collection<ChannelBridgeConfig> findAllByGuildId(long guildId);

    Optional<ChannelBridgeConfig> findByGuildIdAndChannelName(long guildId, String channelName);

    @Query("select distinct cbc.rabbitUri from ChannelBridgeConfig cbc where cbc.guildId = :guildId")
    Optional<String> findRabbitByGuildId(long guildId);
}
