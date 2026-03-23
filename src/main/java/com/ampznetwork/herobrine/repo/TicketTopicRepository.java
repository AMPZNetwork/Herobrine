package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.tickets.model.TicketTopic;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TicketTopicRepository extends CrudRepository<TicketTopic, TicketTopic.Key> {
    Collection<TicketTopic> findAllByGuildId(long guildId);
}
