package com.ampznetwork.herobrine.feature.embed;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Log
@Service
@Command("embed")
public class EmbedBuilderService extends ListenerAdapter implements AuditLogSender {
    public static final String INTERACTION_EDIT         = "Edit Embed";
    public static final String INTERACTION_ADD_FIELD    = "edit-field-add";
    public static final String INTERACTION_EDIT_FIELD   = "edit-field-edit";
    public static final String INTERACTION_REMOVE_FIELD = "edit-field-remove";
    public static final String INTERACTION_SUBMIT       = "edit-submit";
    public static final String OPTION_NAME              = "option-name";
    public static final String OPTION_CONTENT           = "option-value";
    public static final String OPTION_INLINE            = "option-inline";
    public static final String OPTION_EMBED_COMPONENT   = "edit-embed-component";
    public static final String APPEND_APPLY             = "-apply";

    private final Map<@NotNull Long, @NotNull EmbedEditorSession> activeEdits = new ConcurrentHashMap<>();

    @Command(value = "create", permission = "8192")
    @Description("Create a new embed message")
    public MessageCreateData create(User user, MessageChannelUnion channel) {
        var messageCreateData = createEmbedEditMenu(null).build();
        var editor            = new EmbedEditorSession(user, channel, messageCreateData.getEmbeds().getFirst());

        activeEdits.put(user.getIdLong(), editor);

        return messageCreateData;
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!INTERACTION_EDIT.equals(event.getInteraction().getName())) return;

        var message = event.getTarget();
        if (!(message.getAuthor() instanceof SelfUser) || message.getEmbeds().isEmpty()) {
            event.reply("This embed cannot be edited").setEphemeral(true).queue();
            return;
        }

        var user   = event.getUser();
        var embed  = message.getEmbeds().getFirst();
        var editor = new EmbedEditorSession(user, event.getChannel(), embed, message);

        activeEdits.put(user.getIdLong(), editor);
        event.reply(createEmbedEditMenu(embed).build()).setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        var userId = event.getUser().getIdLong();

        if (!activeEdits.containsKey(userId)) return;

        var editor = activeEdits.get(userId);
        var id     = event.getComponentId();

        switch (id) {
            case INTERACTION_ADD_FIELD -> event.replyModal(fieldMutatorModal(id, null).build()).queue();
            case INTERACTION_EDIT_FIELD, INTERACTION_REMOVE_FIELD -> {
                // make list of field option
                var ls      = editor.getEmbed().getFields();
                var options = new ArrayList<SelectOption>();
                for (var i = 0; i < ls.size(); i++)
                    options.add(SelectOption.of(Objects.requireNonNull(ls.get(i).getName()), String.valueOf(i)));

                // reply with field selector
                event.reply(new MessageCreateBuilder().setContent("Select field to edit")
                        .addComponents(ActionRow.of(StringSelectMenu.create(id + APPEND_APPLY)
                                .addOptions(options)
                                .build()))
                        .build()).setEphemeral(true).queue();
            }
            case INTERACTION_SUBMIT -> {
                audit().newEntry()
                        .guild(event.getGuild())
                        .source(this)
                        .message(editor.getMessage() == null
                                 ? "%s is creating a new embed in channel %s".formatted(event.getMember(),
                                editor.getChannel())
                                 : "%s is submitting edits to embed in message %s".formatted(event.getMember(),
                                         editor.getMessage().getJumpUrl()))
                        .queue();
                editor.applyEdits().flatMap($ -> event.reply("Edits applied").setEphemeral(true)).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        var id     = event.getModalId();
        var userId = event.getUser().getIdLong();
        if (!activeEdits.containsKey(userId)) return;
        var editor = activeEdits.get(userId);

        if (id.startsWith(INTERACTION_ADD_FIELD) || id.startsWith(INTERACTION_EDIT_FIELD)) {
            var name    = Optional.ofNullable(event.getValue(OPTION_NAME)).map(ModalMapping::getAsString).orElse("");
            var content = Optional.ofNullable(event.getValue(OPTION_CONTENT)).map(ModalMapping::getAsString).orElse("");
            var inline = Optional.ofNullable(event.getValue(OPTION_INLINE))
                    .map(ModalMapping::getAsStringList)
                    .filter(Predicate.not(List::isEmpty))
                    .map(List::getFirst)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
            var mutated = new MessageEmbed.Field(name, content, inline);
            var fields  = editor.getEmbed().getFields();

            if (id.startsWith(INTERACTION_EDIT_FIELD)) {
                // edit by index

                var li    = id.lastIndexOf('-');
                var index = Integer.parseInt(id.substring(li + 1));

                fields.set(index, mutated);
            } else fields.add(mutated);
        } else if (id.startsWith(EmbedComponent.MODAL_ID)) {
            var li            = id.lastIndexOf('-');
            var componentType = id.substring(li + 1);
            var component     = EmbedComponent.valueOf(componentType);

            component.handleInteraction(editor.getEmbed(), event);
        }

        event.editMessageEmbeds(editor.getEmbed().build()).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        var id = event.getComponentId();

        var userId = event.getUser().getIdLong();
        if (!activeEdits.containsKey(userId)) return;
        var editor = activeEdits.get(userId);

        if (id.endsWith(APPEND_APPLY)) {
            var li     = id.lastIndexOf('-');
            var action = id.substring(0, li);
            var index  = Integer.parseInt(event.getValues().getFirst());

            switch (action) {
                case INTERACTION_EDIT_FIELD -> {
                    var field = editor.getEmbed().getFields().get(index);
                    event.replyModal(fieldMutatorModal(id + "-" + index, field).build()).queue();
                }
                case INTERACTION_REMOVE_FIELD -> {
                    editor.getEmbed().getFields().remove(index);
                    event.reply("Field removed").setEphemeral(true).queue();
                }
            }

            return;
        }

        if (!id.equals(OPTION_EMBED_COMPONENT)) return;

        var selected = event.getValues().getFirst();
        var option   = EmbedComponent.valueOf(selected);
        var modal    = option.createEditorModal(editor.toEmbed().build());

        event.replyModal(modal).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        var jda = event.getApplicationContext().getBean(JDA.class);
        jda.addEventListener(this);
        jda.upsertCommand(Commands.message(INTERACTION_EDIT)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))).queue();

        log.info("Initialized");
    }

    private MessageCreateBuilder createEmbedEditMenu(@Nullable MessageEmbed embed) {
        var message = new MessageCreateBuilder().setContent(embed == null
                                                            ? "Creating new embed"
                                                            : "Editing the following embed:")
                .setComponents(ActionRow.of(StringSelectMenu.create(OPTION_EMBED_COMPONENT)
                                .addOptions(Arrays.stream(EmbedComponent.values())
                                        .map(EmbedComponent::getSelectOption)
                                        .toList())
                                .setPlaceholder("Select an embed component to edit...")
                                .build()),
                        ActionRow.of(Button.secondary(INTERACTION_ADD_FIELD, "Add Field..."),
                                Button.secondary(INTERACTION_EDIT_FIELD, "Edit Field..."),
                                Button.secondary(INTERACTION_REMOVE_FIELD, "Remove Field..."),
                                Button.primary(INTERACTION_SUBMIT, "Apply changes")));

        message.setEmbeds(embed != null ? embed : EmbedEditorSession.placeholderEmbed().build());

        return message;
    }

    private Modal.Builder fieldMutatorModal(String id, @Nullable MessageEmbed.Field field) {
        var inputFieldTitle = TextInput.create(OPTION_NAME, TextInputStyle.SHORT)
                .setMaxLength(MessageEmbed.TITLE_MAX_LENGTH);
        var inputFieldContent = TextInput.create(OPTION_CONTENT, TextInputStyle.PARAGRAPH)
                .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH);
        var inputFieldInline = yesNoSelection(OPTION_INLINE, field != null && field.isInline());

        if (field != null) {
            inputFieldTitle   = inputFieldTitle.setValue(field.getName());
            inputFieldContent = inputFieldContent.setValue(field.getValue());
        }

        return Modal.create(id, "Creating embed field")
                .addComponents(Label.of("Field Title", inputFieldTitle.build()),
                        Label.of("Field Content", inputFieldContent.build()),
                        Label.of("Inline Field?", inputFieldInline.build()));
    }

    private StringSelectMenu.Builder yesNoSelection(String id, @Nullable Boolean state) {
        var optionYes = SelectOption.of("✅ Yes", "true");
        var optionNo  = SelectOption.of("❌ No", "false");
        var menu      = StringSelectMenu.create(id).addOptions(optionYes, optionNo);

        if (state != null) menu.setDefaultOptions(state ? optionYes : optionNo);

        return menu;
    }
}
