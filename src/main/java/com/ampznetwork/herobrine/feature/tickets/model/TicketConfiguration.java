package com.ampznetwork.herobrine.feature.tickets.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class TicketConfiguration {
    @Id long guildId;
    long baseChannelId;
    long teamRoleId;
}
