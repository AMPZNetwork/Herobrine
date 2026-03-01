package com.ampznetwork.herobrine.feature.personality;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogSender;
import com.ampznetwork.herobrine.feature.personality.model.ContentFilter;
import com.ampznetwork.herobrine.feature.personality.model.PersonalityTrait;
import com.ampznetwork.herobrine.feature.personality.model.RandomDetail;
import com.ampznetwork.herobrine.repo.PersonalityTraitRepo;
import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import com.ampznetwork.herobrine.util.Constant;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.comroid.api.Polyfill;
import org.comroid.commands.impl.CommandManager;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@Service
public class TraitEditorService extends ListenerAdapter implements ErrorLogSender, AuditLogSender {
    public static final String INTERACTION_EDIT_TRIGGER  = "personality_trait_edit_trigger";
    public static final String INTERACTION_EDIT_FILTER   = "personality_trait_edit_filter";
    public static final String INTERACTION_EDIT_RANDOM   = "personality_trait_edit_random";
    public static final String INTERACTION_EDIT_TEMPLATE = "personality_trait_edit_template";
    public static final String INTERACTION_SUMBIT        = "personality_trait_submit";

    public static final String OPTION_TRIGGER        = "option_trigger";
    public static final String OPTION_FILTER_METHOD  = "option_filter_method";
    public static final String OPTION_FILTER_PATTERN = "option_filter_pattern";
    public static final String OPTION_RANDOM_CHANCE  = "option_random_chance";
    public static final String OPTION_RANDOM_LIMES   = "option_random_limes";
    public static final String OPTION_TEMPLATE_PASTE = "option_template_paste";
    public static final String OPTION_TEMPLATE_FILE  = "option_template_file";

    final Collection<TraitEditor> editors = new ArrayList<>();

    @Autowired PersonalityTraitRepo personalities;

    Optional<TraitEditor> findTraitEditor(Guild guild, UserSnowflake user) {
        return editors.stream().filter(creator -> creator.guild.equals(guild) && creator.user.equals(user)).findAny();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    @Override
    public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
        var guild  = event.getGuild();
        var editor = findTraitEditor(guild, event.getMember()).orElse(null);

        if (editor == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle(Constant.EMOJI_WARNING.getFormatted() + " This editor has expired")
                            .setColor(Constant.COLOR_ERROR)
                            .setFooter(Constant.STRING_SELF_DESTRUCT.formatted(5))
                            .build())
                    .setEphemeral(true)
                    .flatMap(hook -> event.getMessage().delete().map($ -> hook))
                    .delay(5, TimeUnit.SECONDS)
                    .flatMap(InteractionHook::deleteOriginal)
                    .queue();
            return;
        }

        switch (event.getComponentId()) {
            case INTERACTION_EDIT_TRIGGER -> event.replyModal(createEditTriggerModal(editor).build()).queue();
            case INTERACTION_EDIT_FILTER -> event.replyModal(createEditFilterModal(editor).build()).queue();
            case INTERACTION_EDIT_RANDOM -> event.replyModal(createEditRandomModal(editor).build()).queue();
            case INTERACTION_EDIT_TEMPLATE -> event.replyModal(createEditTemplateModal(editor).build()).queue();
            case INTERACTION_SUMBIT -> {
                var trait  = editor.builder.build();
                var key    = trait.key();
                var result = personalities.findById(key);

                if (result.isPresent()) wrapErrors(guild,
                        "Clearing previous trait value",
                        () -> personalities.deleteById(key));

                if (null != wrapErrors(guild, "Saving new trait", () -> personalities.save(trait))) {
                    if (null != wrapErrors(guild,
                            "Removing old editor",
                            () -> editors.remove(editor))) editor.infoMessage.delete().queue();
                }

                newAuditEntry().level(Level.INFO)
                        .message("%s modified personality trait %s".formatted(event.getMember(), trait))
                        .queue();

                event.replyEmbeds(new EmbedBuilder().setTitle(Constant.EMOJI_SUCCESS.getFormatted() + " Personality Trait was successfully edited")
                                .setColor(Constant.COLOR_SUCCESS)
                                .setFooter(Constant.STRING_SELF_DESTRUCT.formatted(3))
                                .build())
                        .setEphemeral(true)
                        .map(hook -> hook.getCallbackResponse().getMessage())
                        .flatMap(response -> event.getMessage().delete().map($ -> response))
                        .delay(3, TimeUnit.SECONDS)
                        .flatMap(Message::delete)
                        .queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NonNull ModalInteractionEvent event) {
        var editor = findTraitEditor(event.getGuild(), event.getMember()).orElseThrow();

        switch (event.getModalId()) {
            case INTERACTION_EDIT_TRIGGER -> {
                var triggerName = Objects.requireNonNull(event.getValue(OPTION_TRIGGER), "trigger")
                        .getAsStringList()
                        .getFirst();
                var trigger = DiscordTrigger.valueOf(triggerName);

                editor.builder.discordTrigger(Polyfill.uncheckedCast(trigger));
            }
            case INTERACTION_EDIT_FILTER -> {
                var filterMethodName = Objects.requireNonNull(event.getValue(OPTION_FILTER_METHOD), "method")
                        .getAsStringList()
                        .getFirst();
                var filterMethod = ContentFilter.StringMatching.valueOf(filterMethodName);

                var filterPattern = Objects.requireNonNull(event.getValue(OPTION_FILTER_PATTERN), "pattern")
                        .getAsString();

                editor.builder.contentFilter(new ContentFilter(filterMethod, filterPattern));
            }
            case INTERACTION_EDIT_RANDOM -> {
                var randomChance = Integer.parseInt(Objects.requireNonNull(event.getValue(OPTION_RANDOM_CHANCE),
                        "chance").getAsString());
                var randomLimes = Integer.parseInt(Objects.requireNonNull(event.getValue(OPTION_RANDOM_LIMES), "limes")
                        .getAsString());

                editor.builder.randomDetail(new RandomDetail(randomChance, randomLimes));
            }
            case INTERACTION_EDIT_TEMPLATE -> {
                var                      pasteValue   = event.getValue(OPTION_TEMPLATE_PASTE);
                var                      templateFile = event.getValue(OPTION_TEMPLATE_FILE);
                List<Message.Attachment> attachments;

                if (templateFile == null || (attachments = templateFile.getAsAttachmentList()).isEmpty()) {
                    editor.builder.templateScript(Objects.requireNonNull(pasteValue, "any template").getAsString());
                    break;
                }

                String template;
                try (
                        var is = attachments.getFirst().getProxy().download().join();
                        var isr = new InputStreamReader(is); var br = new BufferedReader(isr)
                ) {
                    template = br.lines().collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read attached template script", e);
                }

                editor.builder.templateScript(template);
            }
        }

        event.replyEmbeds(new EmbedBuilder().setTitle(Constant.EMOJI_SUCCESS.getFormatted() + " Success")
                        .setColor(Constant.COLOR_SUCCESS)
                        .setFooter(Constant.STRING_SELF_DESTRUCT.formatted(2))
                        .build())
                .setEphemeral(true)
                .map(hook -> hook.getCallbackResponse().getMessage())
                .flatMap(response -> editor.refreshInfoMessage().map($ -> response))
                .delay(2, TimeUnit.SECONDS)
                .flatMap(Message::delete)
                .queue();
    }

    private Modal.Builder createEditTriggerModal(@Nullable TraitEditor editor) {
        Optional<? extends DiscordTrigger<? extends GenericMessageEvent>> discordTrigger = Optional.ofNullable(editor)
                .map(x -> x.builder.build())
                .map(PersonalityTrait::getDiscordTrigger);
        return Modal.create(INTERACTION_EDIT_TRIGGER, "Editing Trigger")
                .addComponents(Label.of("Select Trigger",
                        StringSelectMenu.create(OPTION_TRIGGER)
                                .addOptions(DiscordTrigger.VALUES.values()
                                        .stream()
                                        .filter(trigger -> GenericMessageEvent.class.isAssignableFrom(trigger.getEventType()))
                                        .map(DiscordTrigger::getOption)
                                        .toList())
                                .setDefaultOptions(discordTrigger.map(DiscordTrigger::getOption).stream().toList())
                                .build()));
    }

    private Modal.Builder createEditFilterModal(@Nullable TraitEditor editor) {
        var contentFilter = Optional.ofNullable(editor)
                .map(x -> x.builder.build())
                .map(PersonalityTrait::getContentFilter);
        return Modal.create(INTERACTION_EDIT_FILTER, "Editing Filter")
                .addComponents(Label.of("Select matching Method",
                                StringSelectMenu.create(OPTION_FILTER_METHOD)
                                        .addOptions(Arrays.stream(ContentFilter.StringMatching.values())
                                                .map(ContentFilter.StringMatching::getOption)
                                                .toList())
                                        .setDefaultOptions(contentFilter.map(ContentFilter::getMatching)
                                                .map(ContentFilter.StringMatching::getOption)
                                                .orElseGet(ContentFilter.StringMatching.CONTAINS::getOption))
                                        .build()),
                        Label.of("Pattern",
                                TextInput.create(OPTION_FILTER_PATTERN, TextInputStyle.SHORT)
                                        .setValue(contentFilter.map(ContentFilter::getPattern).orElse(null))
                                        .build()));
    }

    private Modal.Builder createEditRandomModal(@Nullable TraitEditor editor) {
        var randomDetail = Optional.ofNullable(editor)
                .map(x -> x.builder.build())
                .map(PersonalityTrait::getRandomDetail);
        return Modal.create(INTERACTION_EDIT_RANDOM, "Editing randomness")
                .addComponents(Label.of("Select chance",
                                TextInput.create(OPTION_RANDOM_CHANCE, TextInputStyle.SHORT)
                                        .setMaxLength(9)
                                        .setValue(String.valueOf(randomDetail.map(RandomDetail::getChance).orElse(1)))
                                        .build()),
                        Label.of("Select limes",
                                TextInput.create(OPTION_RANDOM_LIMES, TextInputStyle.SHORT)
                                        .setMaxLength(9)
                                        .setValue(String.valueOf(randomDetail.map(RandomDetail::getLimes).orElse(100)))
                                        .build()));
    }

    private Modal.Builder createEditTemplateModal(@Nullable TraitEditor editor) {
        var template = Optional.ofNullable(editor)
                .map(x -> x.builder.build())
                .map(PersonalityTrait::getTemplateScript)
                .orElse("<no template>");
        return Modal.create(INTERACTION_EDIT_TEMPLATE, "Editing template").addComponents(TextDisplay.of(("""
                        Current template: ```dmt
                        %s
                        ```
                        -# Please paste a template script OR select a template file to upload""").formatted(template)),
                Label.of("Paste template",
                        TextInput.create(OPTION_TEMPLATE_PASTE, TextInputStyle.PARAGRAPH).setRequired(false).build()),
                Label.of("Upload file", AttachmentUpload.create(OPTION_TEMPLATE_FILE).setRequired(false).build()));
    }

    @Value
    @EqualsAndHashCode(of = { "guild", "user" })
    static class TraitEditor {
        Guild                    guild;
        UserSnowflake            user;
        Message                  infoMessage;
        PersonalityTrait.Builder builder;

        public MessageEditBuilder toInfoMessage() {
            var trait = builder.build();

            return new MessageEditBuilder().useComponentsV2()
                    .setComponents(TextDisplay.of("# Editing Personality Trait %s".formatted(trait.getName())),
                            Separator.create(true, Separator.Spacing.LARGE),
                            Section.of(Button.secondary(INTERACTION_EDIT_TRIGGER, Constant.STRING_EDIT),
                                    TextDisplay.of("### Trigger\n%s".formatted(trait.getDiscordTrigger()))),
                            Separator.create(true, Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_FILTER, Constant.STRING_EDIT),
                                    TextDisplay.of("### Filter\n%s".formatted(trait.getContentFilter()))),
                            Separator.create(true, Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_RANDOM, Constant.STRING_EDIT),
                                    TextDisplay.of("### Randomness\n%s".formatted(trait.getRandomDetail()))),
                            Separator.create(true, Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_TEMPLATE, Constant.STRING_EDIT),
                                    TextDisplay.of("### Template\n```dmt\n%s\n```".formatted(trait.getTemplateScript()))),
                            ActionRow.of(Button.primary(INTERACTION_SUMBIT, Constant.STRING_APPLY)));
        }

        public RestAction<Message> refreshInfoMessage() {
            return infoMessage.editMessage(toInfoMessage().build());
        }
    }
}
