package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.tickets.model.TicketConfiguration;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketConfigurationRepository extends CrudRepository<TicketConfiguration, @NonNull Long> {

}
