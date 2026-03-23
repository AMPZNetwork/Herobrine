package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.tickets.model.TicketData;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.OptionalLong;

@Repository
public interface TicketRepository extends CrudRepository<TicketData, TicketData.Key> {
    @Query("select MAX(td.ticketId) + 1 from TicketData td where td.guildId = :guildId")
    OptionalLong nextTicketId(long guildId);

    Optional<TicketData> findByGuildIdAndThreadId(long guildId, long threadId);
}
