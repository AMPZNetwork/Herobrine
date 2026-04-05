package com.ampznetwork.herobrine.feature.tickets;

import com.ampznetwork.herobrine.feature.tickets.model.TicketTopic;
import com.ampznetwork.herobrine.repo.TicketRepository;
import com.ampznetwork.herobrine.repo.TicketTopicRepository;
import com.ampznetwork.herobrine.util.Constant;
import com.ampznetwork.herobrine.util.EmbedTemplate;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Log
@Service
@Interaction("ticket-topic")
public class TicketTopicManager {
    public static final String INTERACTION_CREATE  = "ticket_topic_create";
    public static final String INTERACTION_EDIT    = "ticket_topic_edit";
    public static final String INTERACTION_DELETE  = "ticket_topic_delete";
    public static final String OPTION_NAME         = "option_name";
    public static final String OPTION_DESCRIPTION  = "option_description";
    public static final String OPTION_HANDLER_ROLE = "option_handler_role";

    @Autowired TicketRepository      tickets;
    @Autowired TicketTopicRepository topics;

    @Interaction(definitions = { @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "17179869184") })
    @Description("Create a new ticket topic")
    public void create(SlashCommandInteractionEvent event, Guild guild) {
        event.replyModal(createEditorModal(INTERACTION_CREATE, null, TicketTopic.builder()).build()).queue();
    }

    @Interaction(definitions = { @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "17179869184") })
    @Description("Edit an existing ticket topic")
    public void edit(SlashCommandInteractionEvent event, Guild guild, @Parameter(completion = @Completion(provider = TicketTopic.AutoFill.class)) String name) {
        var topic = topics.findById(new TicketTopic.Key(guild.getIdLong(), name))
                .orElseThrow(() -> Response.of("Ticket topic with name `%s` was not found".formatted(name)));

        event.replyModal(createEditorModal(INTERACTION_EDIT + '$' + name, topic, TicketTopic.builder()).build()).queue();
    }

    @Interaction(definitions = { @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "17179869184") })
    @Description("Delete an existing ticket topic")
    public MessageCreateData delete(
            SlashCommandInteractionEvent event, Guild guild,
            @Parameter(completion = @Completion(provider = TicketTopic.AutoFill.class)) String name
    ) {
        var topic = topics.findById(new TicketTopic.Key(guild.getIdLong(), name))
                .orElseThrow(() -> Response.of("Ticket topic with name `%s` was not found".formatted(name)));

        var affectedTicketCount = tickets.countAllByGuildIdAndTopic(guild.getIdLong(), topic);

        return new MessageCreateBuilder().addEmbeds(EmbedTemplate.warning("Do you really want to delete topic `%s`?\nThis will affect %d tickets".formatted(
                        topic,
                        affectedTicketCount)).build())
                .addComponents(ActionRow.of(Button.danger(INTERACTION_DELETE + '$' + name, "%s Yes, just do it!".formatted(Constant.EMOJI_DELETE))))
                .build();
    }

    @EventListener
    public void on(@NonNull ButtonInteractionEvent event) {
        var componentId = event.getComponentId();
        if (!componentId.startsWith(INTERACTION_DELETE)) return;

        var guild     = Objects.requireNonNull(event.getGuild(), "guild");
        var topicName = componentId.substring(INTERACTION_DELETE.length() + 1);
        var topicKey  = new TicketTopic.Key(guild.getIdLong(), topicName);
        var topic     = topics.findById(topicKey).orElseThrow();

        try {
            tickets.dropTopicByGuildIdAndTopic(guild.getIdLong(), topic);
            topics.deleteById(topicKey);

            event.replyEmbeds(EmbedTemplate.success("Topic `%s` was deleted".formatted(topic)).build()).setEphemeral(true).queue();
        } catch (Throwable t) {
            event.replyEmbeds(EmbedTemplate.error("Could not delete topic `%s`".formatted(topic), t).build()).setEphemeral(true).queue();
        }
    }

    @EventListener
    public void on(@NonNull ModalInteractionEvent event) {
        var modalId = event.getModalId();

        if (!modalId.equals(INTERACTION_CREATE) && !modalId.startsWith(INTERACTION_EDIT)) return;
        var oldName = modalId.startsWith(INTERACTION_EDIT) ? modalId.substring(INTERACTION_EDIT.length() + 1) : null;

        var guild = Objects.requireNonNull(event.getGuild(), "guild");
        var builder = oldName == null
                      ? TicketTopic.builder().guildId(guild.getIdLong())
                      : topics.findById(new TicketTopic.Key(guild.getIdLong(), oldName)).orElseThrow().toBuilder();

        var topicName = Objects.requireNonNull(event.getValue(OPTION_NAME), "name option").getAsString();
        if (topicName != null && !topicName.isBlank()) builder.name(topicName);

        var topicDescription = Objects.requireNonNull(event.getValue(OPTION_DESCRIPTION), "description option").getAsString();
        if (topicDescription != null && !topicDescription.isBlank()) builder.description(topicDescription);

        var handlerRoleId = Objects.requireNonNull(event.getValue(OPTION_HANDLER_ROLE), "handler role option")
                .getAsMentions()
                .getRoles()
                .stream()
                .findAny()
                .map(ISnowflake::getIdLong)
                .orElse(0L);
        builder.handlerRoleId(handlerRoleId);

        if (!topicName.equals(oldName)) topics.deleteById(new TicketTopic.Key(guild.getIdLong(), oldName));
        topics.save(builder.build());

        event.replyEmbeds(EmbedTemplate.success("Ticket topic was updated").build()).setEphemeral(true).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }

    private Modal.Builder createEditorModal(String modalId, @Nullable TicketTopic current, TicketTopic.Builder builder) {
        return Modal.create(modalId, switch (modalId) {
                    case INTERACTION_CREATE -> "Creating ticket topic";
                    case INTERACTION_EDIT -> "Editing ticket topic";
                    default -> "undefined";
                })
                .addComponents(Label.of("Name",
                                TextInput.create(OPTION_NAME, TextInputStyle.SHORT).setValue(current == null ? null : current.getName()).build()),
                        Label.of("Description",
                                TextInput.create(OPTION_DESCRIPTION, TextInputStyle.PARAGRAPH)
                                        .setValue(current == null ? null : current.getDescription())
                                        .build()),
                        Label.of("Handler Role",
                                EntitySelectMenu.create(OPTION_HANDLER_ROLE, EntitySelectMenu.SelectTarget.ROLE)
                                        .setDefaultValues(current == null || current.getHandlerRoleId() == 0
                                                          ? List.of()
                                                          : List.of(EntitySelectMenu.DefaultValue.role(current.getHandlerRoleId())))
                                        .setRequired(false)
                                        .build()));
    }
}
