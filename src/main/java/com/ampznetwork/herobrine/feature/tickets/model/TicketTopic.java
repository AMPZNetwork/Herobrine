package com.ampznetwork.herobrine.feature.tickets.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TicketTopic.Key.class)
public class TicketTopic {
    @Id long   guildId;
    @Id String name;
    String description;
    long   handlerRoleId;

    record Key(long guildId, String name) {}
}
