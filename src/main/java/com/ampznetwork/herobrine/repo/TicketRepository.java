package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.tickets.model.TicketData;
import com.ampznetwork.herobrine.feature.tickets.model.TicketTopic;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends CrudRepository<TicketData, TicketData.Key> {
    @Query("select MAX(td.ticketId) from TicketData td where td.guildId = :guildId")
    @Nullable Long lastTicketId(long guildId);

    Optional<TicketData> findByGuildIdAndThreadId(long guildId, long threadId);

    long countAllByGuildIdAndTopic(long guildId, TicketTopic topic);

    @Modifying
    @Transactional
    @Query("update TicketData td set td.topic = null where td.guildId = :guildId and td.topic = :topic")
    void dropTopicByGuildIdAndTopic(long guildId, TicketTopic topic);
}
