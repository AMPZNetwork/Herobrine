package com.ampznetwork.herobrine.feature.tickets.model;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.attr.Named;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public enum TicketState implements Named {
    Opened(false, false, true) {
        @Override
        public MessageCreateBuilder toInfoMessage(TicketConfiguration config, TicketData ticket) {
            return new MessageCreateBuilder().useComponentsV2()
                    .addComponents(TextDisplay.of("# %#s".formatted(ticket)),
                            Container.of(TextDisplay.of("## " + ticket.getTitle()), TextDisplay.of(ticket.getDescription())),
                            TextDisplay.of("-# Relevant Mentions: " + TicketData.mentionables(config, ticket.getTopic())
                                    .map(IMentionable::getAsMention)
                                    .collect(Collectors.joining(", "))));
        }
    },
    Undetailed(false, true, true),
    Investigating(false, false, true),
    Completing(true, true, true),
    Incomplete(true, true, false),
    Unplanned(true, false, false),
    Completed(true, false, true);

    boolean closed;
    boolean awaitingResponse;
    boolean privileged;

    public MessageCreateBuilder toInfoMessage(TicketConfiguration config, TicketData ticket) {
        return new MessageCreateBuilder().useComponentsV2().addComponents(TextDisplay.of("# %s".formatted(ticket)));
    }
}
