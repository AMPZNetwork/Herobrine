package com.ampznetwork.herobrine.feature.tickets.model;

import com.ampznetwork.herobrine.util.ApplicationContextProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.func.WrappedFormattable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TicketData.Key.class)
public class TicketData implements WrappedFormattable {
    public static Stream<Role> mentionables(@Nullable TicketConfiguration config, @Nullable TicketTopic topic) {
        return LongStream.concat(Stream.ofNullable(config).filter(Objects::nonNull).mapToLong(TicketConfiguration::getTeamRoleId),
                        Stream.ofNullable(topic).filter(Objects::nonNull).mapToLong(TicketTopic::getHandlerRoleId))
                .distinct()
                .filter(id -> id > 0)
                .mapToObj(ApplicationContextProvider.bean(JDA.class)::getRoleById)
                .filter(Objects::nonNull);
    }

    @Id                  long        guildId;
    @Id                  long        ticketId;
    @ManyToOne @Nullable TicketTopic topic;
    String title;
    @Column(length = 1800) String description;
    long authorId, threadId;
    TicketState state;

    @Override
    public String getPrimaryName() {
        return "Ticket #%d - `%s`".formatted(ticketId, state);
    }

    @Override
    public String getAlternateName() {
        return "Ticket #%d - `%s` - by <@%d>".formatted(ticketId, state, authorId);
    }

    public MessageCreateBuilder toInfoMessage(TicketConfiguration config) {
        return state.toInfoMessage(config, this);
    }

    public record Key(long guildId, long ticketId) {}
}
