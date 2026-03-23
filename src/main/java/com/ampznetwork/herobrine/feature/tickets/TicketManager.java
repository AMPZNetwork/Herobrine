package com.ampznetwork.herobrine.feature.tickets;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogSender;
import com.ampznetwork.herobrine.feature.tickets.model.TicketConfiguration;
import com.ampznetwork.herobrine.feature.tickets.model.TicketData;
import com.ampznetwork.herobrine.feature.tickets.model.TicketState;
import com.ampznetwork.herobrine.feature.tickets.model.TicketTopic;
import com.ampznetwork.herobrine.repo.TicketConfigurationRepository;
import com.ampznetwork.herobrine.repo.TicketRepository;
import com.ampznetwork.herobrine.repo.TicketTopicRepository;
import com.ampznetwork.herobrine.util.EmbedTemplate;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Streams;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.logging.Level;

@Log
@Service
@Command("ticket")
public class TicketManager extends ListenerAdapter implements AuditLogSender, ErrorLogSender {
    public static final String INTERACTION_OPEN   = "tickets_open";
    public static final String OPTION_TOPIC       = "tickets_option_topic";
    public static final String OPTION_TITLE       = "tickets_option_title";
    public static final String OPTION_DESCRIPTION = "tickets_option_description";

    @Autowired JDA                           jda;
    @Autowired TicketRepository              tickets;
    @Autowired TicketTopicRepository         topics;
    @Autowired TicketConfigurationRepository configs;

    @Command
    @Description("Set this state of this ticket")
    public EmbedBuilder state(Guild guild, ThreadChannel channel, User user, @Command.Arg @Description("The state to set the ticket to") TicketState state) {
        var guildId = guild.getIdLong();
        var config  = configs.findById(guildId).orElseThrow(() -> new CommandError("Tickets are not configured"));
        var ticket  = tickets.findByGuildIdAndThreadId(guildId, channel.getIdLong()).orElseThrow(() -> new CommandError("This is not a ticket channel"));

        if (state.privileged && !isPrivileged(guild, config, channel, user, ticket.getTopic()))
            throw new CommandError("Only team members can apply this state!");

        newAuditEntry().guild(guild).level(Level.FINE).message("%s is changing state of %s to %s".formatted(user, ticket, state)).queue();

        ticket.setState(state);
        tickets.save(ticket);

        var infoMessage = ticket.toInfoMessage(config);
        if (infoMessage != null) channel.sendMessage(infoMessage.build()).queue();

        return EmbedTemplate.success("State of ticket was updated to `%s`".formatted(state.name()));
    }

    @Command
    @Description("Open a new support ticket")
    public void open(SlashCommandInteractionEvent event, Guild guild) {
        var config = configs.findById(guild.getIdLong()).orElse(null);
        if (config == null) {
            event.reply("Tickets are not configured").setEphemeral(true).queue();
            return;
        }

        event.replyModal(createOpenTicketModal(guild).build()).queue();
    }

    @Override
    public void onModalInteraction(@NonNull ModalInteractionEvent event) {
        if (!event.getModalId().equals(INTERACTION_OPEN)) return;

        var guild = event.getGuild();
        if (guild == null) {
            event.reply("Only available inside Guilds").setEphemeral(true).queue();
            return;
        }

        // init metadata
        var topicName = Objects.requireNonNull(event.getValue(OPTION_TOPIC), "topic option").getAsStringList().getFirst();
        var topicKey    = new TicketTopic.Key(guild.getIdLong(), topicName);
        var topic       = topics.findById(topicKey).orElse(null);
        var title       = Objects.requireNonNull(event.getValue(OPTION_TITLE), "title option").getAsString();
        var description = Objects.requireNonNull(event.getValue(OPTION_DESCRIPTION), "description option").getAsString();

        // open ticket thread
        var config      = configs.findById(guild.getIdLong()).orElseThrow();
        var baseChannel = jda.getTextChannelById(config.getBaseChannelId());
        if (baseChannel == null) {
            newErrorEntry().guild(guild)
                    .level(Level.SEVERE)
                    .message("Ticket base channel with id `%d` could not be found".formatted(config.getBaseChannelId()))
                    .queue();
            event.reply("Ticket channel not found; please contact a team member").setEphemeral(true).queue();
            return;
        }

        var nextTicketId = nextTicketId(guild);
        event.deferReply(true)
                .flatMap(hook -> baseChannel.createThreadChannel("ticket-%d-%s".formatted(nextTicketId, event.getUser().getEffectiveName()), true)
                        .setInvitable(true)
                        .flatMap(thread -> {
                            // create ticket in db
                            var ticket = new TicketData(guild.getIdLong(),
                                    nextTicketId,
                                    topic,
                                    title,
                                    description,
                                    event.getUser().getIdLong(),
                                    thread.getIdLong(),
                                    TicketState.Opened);

                            tickets.save(ticket);

                            return thread.sendMessage(ticket.toInfoMessage(config).build());
                        })
                        .flatMap($ -> hook.editOriginal("Ticket opened!")))
                .queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private Modal.Builder createOpenTicketModal(Guild guild) {
        return Modal.create(INTERACTION_OPEN, "Opening a new Ticket")
                .addComponents(Label.of("Select Topic",
                                StringSelectMenu.create(OPTION_TOPIC)
                                        .addOptions(topics.findAllByGuildId(guild.getIdLong()).stream().map(TicketTopic::toSelectOption).toList())
                                        .build()),
                        Label.of("Title",
                                TextInput.create(OPTION_TITLE, TextInputStyle.SHORT).setPlaceholder("A short rundown of what this ticket is about").build()),
                        Label.of("Description",
                                TextInput.create(OPTION_DESCRIPTION, TextInputStyle.PARAGRAPH).setPlaceholder("A detailed description on the issue").build()));
    }

    private long nextTicketId(Guild guild) {
        var lastTicketId = tickets.lastTicketId(guild.getIdLong());
        return lastTicketId == null ? 1 : lastTicketId + 1;
    }

    private boolean isPrivileged(Guild guild, TicketConfiguration config, GuildChannel channel, UserSnowflake user, TicketTopic topic) {
        var member = user instanceof Member it ? it : guild.getMember(user);
        if (member == null) return false;

        final var userId = user.getIdLong();
        return member.hasPermission(channel, Permission.MANAGE_THREADS) || TicketData.mentionables(config, topic)
                .flatMap(Streams.expand(role -> guild.getRoles().stream().filter(other -> role.getPosition() <= other.getPosition())))
                .flatMap(role -> guild.getMembersWithRoles(role).stream())
                .mapToLong(ISnowflake::getIdLong)
                .anyMatch(id -> id == userId);
    }
}
