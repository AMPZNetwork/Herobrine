package com.ampznetwork.herobrine.feature.tickets;

import com.ampznetwork.herobrine.component.log.audit.AuditLogService;
import com.ampznetwork.herobrine.feature.tickets.model.TicketConfiguration;
import com.ampznetwork.herobrine.feature.tickets.model.TicketData;
import com.ampznetwork.herobrine.feature.tickets.model.TicketState;
import com.ampznetwork.herobrine.repo.TicketRepository;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.jspecify.annotations.Nullable;

import java.util.logging.Level;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

public final class TicketOperator {
    public static void changeState(
            TicketData ticket, TicketState state, TicketConfiguration config, @Nullable ThreadChannel channel, @Nullable User user,
            AuditLogService.EntryAPI audit
    ) {
        audit.level(Level.FINE).message("%s is changing state of %s to %s".formatted(user, ticket, state)).queue();

        ticket.setState(state);
        tickets().save(ticket);

        if (channel == null) return;

        var infoMessage = ticket.toInfoMessage(config);
        if (infoMessage != null) channel.sendMessage(infoMessage.build()).queue();
        state.applyToChannel(channel, ticket).queue();
    }

    private TicketOperator() {
        throw new UnsupportedOperationException();
    }

    private static TicketRepository tickets() {
        return bean(TicketRepository.class);
    }
}
