package com.ampznetwork.herobrine.feature.tickets.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class TicketConfiguration {
    @Id long guildId;
    long infoChannelId;
    @OneToMany Collection<TicketTopic> topics;
}
