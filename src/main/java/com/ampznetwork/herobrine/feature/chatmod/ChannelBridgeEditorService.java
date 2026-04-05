package com.ampznetwork.herobrine.feature.chatmod;

import com.ampznetwork.herobrine.feature.chatmod.model.ChannelBridgeConfig;
import com.ampznetwork.herobrine.feature.chatmod.model.GuildChannelNameAutoFillProvider;
import com.ampznetwork.herobrine.model.GuildUserKey;
import com.ampznetwork.herobrine.repo.ChannelBridgeConfigRepo;
import com.ampznetwork.herobrine.util.Constant;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Description;
import org.comroid.api.tree.UncheckedCloseable;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.comroid.util.JdaUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Log
@Service
@Interaction("chatbridge")
@ConditionalOnBean(ChannelBridgeService.class)
public class ChannelBridgeEditorService {
    public static final String INTERACTION_EDIT_CHANNEL      = "cbe_edit_channel";
    public static final String INTERACTION_EDIT_RABBIT_URI   = "cbe_edit_rabbit_uri";
    public static final String INTERACTION_EDIT_CHANNEL_NAME = "cbe_edit_channel_name";
    public static final String INTERACTION_EDIT_DISPLAY_NAME = "cbe_edit_display_name";
    public static final String INTERACTION_EDIT_INVITE_URL   = "cbe_edit_invite_url";
    public static final String INTERACTION_SUBMIT            = "cbe_submit";

    public static final String                     OPTION_DISCORD_CHANNEL = "cbe_option_channel_id";
    public static final String                     OPTION_RABBIT_URI      = "cbe_option_rabbit_uri";
    public static final String                     OPTION_CHATMOD_CHANNEL = "cbe_option_channel_name";
    public static final String                     OPTION_DISPLAY_NAME    = "cbe_option_display_name";
    public static final String                     OPTION_INVITE_URL      = "cbe_option_invite_url";
    private final       Map<GuildUserKey, Session> sessions               = new ConcurrentHashMap<>();
    @Autowired          ChannelBridgeConfigRepo    channelBridges;

    /** todo: autofill channel name by seen channel names */
    @Interaction(definitions = { @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "16") })
    @Description("Create a new channel bridge mapping")
    public void create(Guild guild, MessageChannel channel, UserSnowflake user) {
        openEditor(guild, channel, user, ChannelBridgeConfig.builder().guildId(guild.getIdLong())).queue();
    }

    @Interaction(definitions = { @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "16") })
    @Description("Create a new channel bridge mapping")
    public void edit(
            Guild guild, MessageChannel messageChannel, UserSnowflake user,
            @Parameter(completion = @Completion(provider = GuildChannelNameAutoFillProvider.class)) @Description("The chat channel mapping to edit") String name
    ) {
        var bridge = channelBridges.findByGuildIdAndChannelName(guild.getIdLong(), name)
                .orElseThrow(() -> Response.of("Could not find channel bridge by guild id %d and channel name %s"));

        openEditor(guild, messageChannel, user, bridge.toBuilder()).queue();
    }

    @EventListener
    public void on(@NonNull ButtonInteractionEvent event) {
        if (!List.of(INTERACTION_EDIT_CHANNEL,
                INTERACTION_EDIT_RABBIT_URI,
                INTERACTION_EDIT_CHANNEL_NAME,
                INTERACTION_EDIT_DISPLAY_NAME,
                INTERACTION_EDIT_INVITE_URL,
                INTERACTION_SUBMIT).contains(event.getComponentId())) return;

        var session = findSession(event, event);
        if (session == null) return;

        switch (event.getComponentId()) {
            case INTERACTION_EDIT_CHANNEL -> event.replyModal(createEditChannelModal(session).build()).queue();
            case INTERACTION_EDIT_RABBIT_URI -> event.replyModal(createEditRabbitUriModal(session).build()).queue();
            case INTERACTION_EDIT_CHANNEL_NAME -> event.replyModal(createEditChannelNameModal(session).build()).queue();
            case INTERACTION_EDIT_DISPLAY_NAME -> event.replyModal(createEditDisplayNameModal(session).build()).queue();
            case INTERACTION_EDIT_INVITE_URL -> event.replyModal(createEditInviteUrlModal(session).build()).queue();
            case INTERACTION_SUBMIT -> {
                var config = session.builder.build();
                var key    = config.key();

                if (channelBridges.existsById(key)) channelBridges.deleteById(key);
                channelBridges.save(config);

                com.ampznetwork.herobrine.util.JdaUtil.replySuccess(event).queue();
                session.close();
            }
        }
    }

    @EventListener
    public void on(ModalInteractionEvent event) {
        if (!List.of(INTERACTION_EDIT_CHANNEL,
                INTERACTION_EDIT_RABBIT_URI,
                INTERACTION_EDIT_CHANNEL_NAME,
                INTERACTION_EDIT_DISPLAY_NAME,
                INTERACTION_EDIT_INVITE_URL,
                INTERACTION_SUBMIT).contains(event.getModalId())) return;

        var session = findSession(event, event);
        if (session == null) return;

        switch (event.getModalId()) {
            case INTERACTION_EDIT_CHANNEL -> {
                var value = getValue(event, OPTION_DISCORD_CHANNEL);
                if (value == null) return;

                var channel = value.getAsMentions().getChannels().getFirst();
                session.builder.channelId(channel.getIdLong());
            }
            case INTERACTION_EDIT_RABBIT_URI -> {
                var value = getValue(event, OPTION_RABBIT_URI);
                if (value == null) return;

                var rabbitUri = value.getAsString();
                session.builder.rabbitUri(rabbitUri);
            }
            case INTERACTION_EDIT_CHANNEL_NAME -> {
                var value = getValue(event, OPTION_CHATMOD_CHANNEL);
                if (value == null) return;

                var channelName = value.getAsString();
                session.builder.channelName(channelName);
            }
            case INTERACTION_EDIT_DISPLAY_NAME -> {
                var value = getValue(event, OPTION_DISPLAY_NAME);
                if (value == null) return;

                var displayName = value.getAsString();
                if (displayName.isBlank()) displayName = null;
                session.builder.displayName(displayName);
            }
            case INTERACTION_EDIT_INVITE_URL -> {
                var value = getValue(event, OPTION_INVITE_URL);
                if (value == null) return;

                var inviteUrl = value.getAsString();
                if (inviteUrl.isBlank()) inviteUrl = null;
                session.builder.inviteUrl(inviteUrl);
            }
        }

        com.ampznetwork.herobrine.util.JdaUtil.replySuccess(event, message -> session.refresh().map($ -> message)).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }

    private RestAction<?> openEditor(Guild guild, MessageChannel channel, UserSnowflake user, ChannelBridgeConfig.Builder builder) {
        var key     = new GuildUserKey(guild, user);
        var session = new Session(builder, user, channel, null);

        sessions.put(key, session);
        return session.refresh();
    }

    private Session findSession(GenericInteractionCreateEvent event, IReplyCallback callback) {
        var key     = new GuildUserKey(event.getGuild(), event.getUser());
        var session = sessions.getOrDefault(key, null);

        if (session == null) {
            callback.reply("%s No editor session with key %s was found".formatted(Constant.EMOJI_WARNING, key))
                    .delay(5, TimeUnit.SECONDS)
                    .flatMap(InteractionHook::deleteOriginal)
                    .queue();
            return null;
        }

        return session;
    }

    private Modal.Builder createEditChannelModal(Session session) {
        return Modal.create(INTERACTION_EDIT_CHANNEL, "Select Channel for the chat bridge")
                .addComponents(Label.of("Select Channel",
                        EntitySelectMenu.create(OPTION_DISCORD_CHANNEL, EntitySelectMenu.SelectTarget.CHANNEL)
                                .setDefaultValues(EntitySelectMenu.DefaultValue.channel(session.builder.build().getChannelId()))
                                .build()));
    }

    private ModalMapping getValue(@NonNull ModalInteractionEvent event, String optionId) {
        var value = event.getValue(optionId);

        if (value == null) {
            event.reply("%s No value was found".formatted(Constant.EMOJI_WARNING)).delay(5, TimeUnit.SECONDS).flatMap(InteractionHook::deleteOriginal).queue();
            return null;
        }

        return value;
    }

    private Modal.Builder createEditRabbitUriModal(Session session) {
        return Modal.create(INTERACTION_EDIT_RABBIT_URI, "Set RabbitMQ URI")
                .addComponents(Label.of("RabbitMQ URI",
                        TextInput.create(OPTION_RABBIT_URI, TextInputStyle.SHORT).setValue(session.builder.build().getRabbitUri()).build()));
    }

    private Modal.Builder createEditChannelNameModal(Session session) {
        return Modal.create(INTERACTION_EDIT_CHANNEL_NAME, "Set chat channel name")
                .addComponents(Label.of("Channel Name",
                        TextInput.create(OPTION_CHATMOD_CHANNEL, TextInputStyle.SHORT).setValue(session.builder.build().getChannelName()).build()));
    }

    private Modal.Builder createEditDisplayNameModal(Session session) {
        return Modal.create(INTERACTION_EDIT_DISPLAY_NAME, "Set display name for the bridge")
                .addComponents(Label.of("Display Name",
                        TextInput.create(OPTION_DISPLAY_NAME, TextInputStyle.SHORT)
                                .setValue(session.builder.build().getDisplayName())
                                .setRequired(false)
                                .build()));
    }

    private Modal.Builder createEditInviteUrlModal(Session session) {
        return Modal.create(INTERACTION_EDIT_RABBIT_URI, "Set custom Invite URL")
                .addComponents(Label.of("Invite URL",
                        TextInput.create(OPTION_INVITE_URL, TextInputStyle.SHORT).setValue(session.builder.build().getInviteUrl()).setRequired(false).build()));
    }

    @Value
    private static class Session implements UncheckedCloseable {
        ChannelBridgeConfig.Builder builder;
        UserSnowflake               user;
        MessageChannel              channel;
        @NonFinal @Nullable Message infoMessage;

        MessageCreateBuilder createInfoMessage() {
            var value = builder.build();
            return new MessageCreateBuilder().useComponentsV2()
                    .addComponents(TextDisplay.of("# Editing ChatMod channel bridge"),
                            Separator.createDivider(Separator.Spacing.LARGE),
                            Section.of(Button.secondary(INTERACTION_EDIT_CHANNEL, Constant.STRING_EDIT),
                                    TextDisplay.of("## Channel\n<#%d>".formatted(value.getChannelId()))),
                            Separator.createDivider(Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_RABBIT_URI, Constant.STRING_EDIT),
                                    TextDisplay.of("## Rabbit URI\n`%s`".formatted(value.getRabbitUri()))),
                            Separator.createDivider(Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_CHANNEL_NAME, Constant.STRING_EDIT),
                                    TextDisplay.of("## Chat Channel Name\n`%s`".formatted(value.getChannelName()))),
                            Separator.createDivider(Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_DISPLAY_NAME, Constant.STRING_EDIT),
                                    TextDisplay.of("## Channel display name\n-# optional\n`%s`".formatted(value.getDisplayName()))),
                            Separator.createDivider(Separator.Spacing.SMALL),
                            Section.of(Button.secondary(INTERACTION_EDIT_INVITE_URL, Constant.STRING_EDIT),
                                    TextDisplay.of("## Invite URL\n`%s`".formatted(value.getInviteUrl()))),
                            ActionRow.of(Button.primary(INTERACTION_SUBMIT, Constant.STRING_APPLY)));
        }

        RestAction<?> refresh() {
            var data = createInfoMessage().build();

            return infoMessage == null
                   ? channel.sendMessage(data).map(message -> infoMessage = message)
                   : infoMessage.editMessage(JdaUtil.convertToEditData(data));
        }

        @Override
        public void close() {
            if (infoMessage != null) infoMessage.delete().queue();
        }
    }
}
