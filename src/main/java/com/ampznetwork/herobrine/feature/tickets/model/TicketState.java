package com.ampznetwork.herobrine.feature.tickets.model;

import com.ampznetwork.herobrine.component.user.tags.UserTagService;
import com.ampznetwork.herobrine.component.user.tags.model.UserTag;
import com.ampznetwork.herobrine.repo.UserTagProviderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.attr.Named;

import java.util.stream.Collectors;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public enum TicketState implements Named {
    Opened(false, false, true, false) {
        @Override
        public MessageCreateBuilder toInfoMessage(TicketConfiguration config, TicketData ticket) {
            var message = new MessageCreateBuilder().useComponentsV2()
                    .addComponents(TextDisplay.of("# %#s".formatted(ticket)),
                            ticket.topic == null ? TextDisplay.of("-# This ticket has no topic") : ticket.topic.toInfoContainer(),
                            TextDisplay.of("#### Ticket Information"),
                            Container.of(TextDisplay.of("### " + ticket.getTitle()), TextDisplay.of(ticket.getDescription())));

            // evaluate whether mentions should be placed
            mentions:
            {
                var jda = bean(JDA.class);

                // obtain guild
                var guild = jda.getGuildById(ticket.guildId);
                if (guild == null) break mentions;

                // obtain guild member
                var member = guild.getMemberById(ticket.authorId);
                if (member == null) break mentions;

                // check if needed configuration is present & member has VIP tag
                var tags      = bean(UserTagService.class);
                var providers = bean(UserTagProviderRepository.class);
                if (tags == null || providers == null) break mentions;
                if (!providers.findAllByGuildId(guild.getIdLong()).isEmpty() && tags.findTags(member).noneMatch(UserTag.VIP::equals)) break mentions;

                message.addComponents(TextDisplay.of("-# Relevant Mentions: " + TicketData.mentionables(config, ticket.getTopic())
                        .map(IMentionable::getAsMention)
                        .collect(Collectors.joining(", "))));
            }

            return message;
        }
    },
    Undetailed(false, true, true, false),
    Investigating(false, false, true, false),
    Completing(true, true, true, false),
    Incomplete(true, true, false, false),
    Unplanned(true, false, false, true),
    Completed(true, false, true, true),
    Archived(true, false, true, true) {
        @Override
        public RestAction<ThreadChannel> applyToChannel(ThreadChannel channel) {
            return channel.getManager().setArchived(true).map($ -> channel);
        }
    };

    boolean closed;
    boolean awaitingResponse;
    boolean privileged;
    boolean locking;

    public RestAction<ThreadChannel> applyToChannel(ThreadChannel channel) {
        return channel.getManager().setLocked(locking).map($ -> channel);
    }

    public MessageCreateBuilder toInfoMessage(TicketConfiguration config, TicketData ticket) {
        return new MessageCreateBuilder().useComponentsV2().addComponents(TextDisplay.of("# %s".formatted(ticket)));
    }
}
