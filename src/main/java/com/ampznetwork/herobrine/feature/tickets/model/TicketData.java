package com.ampznetwork.herobrine.feature.tickets.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TicketData.Key.class)
public class TicketData {
    @Id        long        guildId;
    @Id        long        ticketId;
    @ManyToOne TicketTopic topic;
    String title, description;
    long authorId, threadId;
    TicketState state;

    @Override
    public String toString() {
        return "Ticket #%d - `%s` - by <@%d>".formatted(ticketId, state, authorId);
    }

    public record Key(long guildId, long ticketId) {}
}
