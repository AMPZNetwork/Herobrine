package com.ampznetwork.herobrine.feature.reactionroles;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.reactionroles.model.ReactionRoleBinding;
import com.ampznetwork.herobrine.feature.reactionroles.model.ReactionRoleSet;
import com.ampznetwork.herobrine.repo.ReactionSetRepo;
import com.ampznetwork.herobrine.util.Constant;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Log
@Service
@Command("roles")
@Description("Configure reaction roles")
public class ReactionRoleService extends ListenerAdapter implements AuditLogSender {
    public static final String COMPONENT_ID_ROLESET_EDIT = "roleset-edit-";
    public static final String COMPONENT_ID_ROLE_ADD     = "roles-add-";
    public static final String COMPONENT_ID_ROLE_REMOVE  = "roles-remove-";
    public static final String COMPONENT_ID_ROLE_EDIT    = "roles-edit-";
    public static final String OPTION_ID_ROLE            = "option-role";
    public static final String OPTION_ID_CHANNEL         = "option-channel";
    public static final String OPTION_ID_EMOJI           = "option-emoji";
    public static final String OPTION_ID_NAME            = "option-name";
    public static final String OPTION_ID_DESCRIPTION     = "option-description";
    public static final String OPTION_ID_METHOD          = "option-method";

    @Autowired ReactionSetRepo setRepo;
    @Autowired JDA             jda;

    @Command(permission = "268435456")
    @Description("Resend reaction role messages")
    @SuppressWarnings("UnusedReturnValue")
    public String resend(
            Guild guild, Member member,
            @Command.Arg(required = false, autoFillProvider = ReactionRoleSet.AutoFillSetNames.class) @Description(
                    "The reaction set to resend") @Nullable String set
    ) {
        var sets = setRepo.findAllByGuildId(guild.getIdLong());
        if (sets.isEmpty()) return "There are no configured reaction roles";

        for (var roleSet : sets) {
            if (set != null && !roleSet.getName().equals(set)) continue;

            var channelId = roleSet.getChannelId();
            var channel   = jda.getTextChannelById(channelId);

            if (channel == null) {
                log.warning("Could not find channel with ID %d for role set %s".formatted(channelId, roleSet));
                continue;
            }

            // send new message
            var msg = roleSet.createMessage();
            channel.sendMessage(msg.build()).flatMap(it -> {
                setRepo.setMessageId(guild.getIdLong(), roleSet.getName(), it.getIdLong());

                return RestAction.allOf(roleSet.getRoles()
                        .stream()
                        .map(ReactionRoleBinding::getEmoji)
                        .map(Emoji::fromFormatted)
                        .map(it::addReaction)
                        .toList());
            }).queue();
        }

        audit().newEntry()
                .guild(guild)
                .source(this)
                .message("%s has resent reaction-role message for set %s".formatted(member, set))
                .queue();
        return "Reaction messages were resent";
    }

    @Command(permission = "268435456")
    @Description("Create a set of role reactions")
    public String createset(
            Guild guild, Member member, @Command.Arg String name, @Command.Arg String description,
            @Command.Arg TextChannel channel, @Command.Arg ReactionRoleSet.Method method
    ) {
        if (name.contains("-")) throw new CommandError("Name cannot contain dashes (`-`)");

        var set = new ReactionRoleSet(guild.getIdLong(),
                name,
                description,
                channel.getIdLong(),
                method,
                null,
                new ArrayList<>());

        audit().newEntry()
                .guild(guild)
                .source(this)
                .message("%s is applying changes to set %s".formatted(member, set))
                .queue();
        setRepo.save(set);

        return "Reaction role set `%s` was created".formatted(name);
    }

    @Command(permission = "268435456")
    @Description("Remove a set of role reactions")
    public String removeset(
            Guild guild, Member member,
            @Command.Arg(autoFillProvider = ReactionRoleSet.AutoFillSetNames.class) String name
    ) {
        var guildId = guild.getIdLong();

        if (setRepo.findById(new ReactionRoleSet.Key(guildId, name)).isEmpty())
            return "There is no reaction set with that name";

        audit().newEntry().guild(guild).source(this).message("%s is removing set %s".formatted(member, name)).queue();
        setRepo.removeBy(guildId, name);
        return "Role reaction set removed";
    }

    @Command(permission = "268435456")
    @Description("Edit an existing set of role reactions")
    public MessageCreateData editset(
            Guild guild,
            @Command.Arg(autoFillProvider = ReactionRoleSet.AutoFillSetNames.class) String set
    ) {
        final var guildId = guild.getIdLong();

        return setRepo.findById(new ReactionRoleSet.Key(guildId, set)).map(roleSet -> {
            var message = roleSet.createMessage();

            message.addComponents(ActionRow.of(Button.secondary(COMPONENT_ID_ROLESET_EDIT + set, "Edit details..."),
                    Button.secondary(COMPONENT_ID_ROLE_ADD + set, "Add Role..."),
                    Button.danger(COMPONENT_ID_ROLE_REMOVE + set, "Remove Role...")));

            return message.build();
        }).orElseThrow(() -> new CommandError("Could not find reaction role set with name `%s`".formatted(set)));
    }

    @Command(permission = "268435456")
    @Description("Create a new reaction role")
    public Object createrole(
            Guild guild, Member member,
            @Command.Arg(autoFillProvider = ReactionRoleSet.AutoFillSetNames.class) String set, @Command.Arg Role role,
            @Command.Arg String emoji, @Command.Arg String name, @Command.Arg String description
    ) {
        var roleSet = setRepo.findById(new ReactionRoleSet.Key(guild.getIdLong(), set))
                .orElseThrow(() -> new CommandError("No such roleset: " + set));

        if (roleSet.findBinding(name).isPresent()) {
            return "%s Role binding with name %s already exists".formatted(Constant.EMOJI_WARNING, name);
        }

        var obj = new ReactionRoleBinding(emoji, name, description, role.getIdLong());
        roleSet.getRoles().add(obj);

        audit().newEntry()
                .guild(guild)
                .source(this)
                .message("%s is adding role %s to set %s".formatted(member, obj, roleSet))
                .queue();
        setRepo.save(roleSet);

        return new MessageCreateBuilder().setContent("Role created").setEmbeds(roleSet.toEmbed().build()).build();
    }

    @Command(permission = "268435456")
    @Description("Edit an existing reaction role")
    public MessageCreateData editrole(
            Guild guild, @Command.Arg(autoFillProvider = ReactionRoleSet.AutoFillSetNames.class) String set,
            @Command.Arg(autoFillProvider = ReactionRoleSet.AutoFillRoleNames.class) String role
    ) {
        final var guildId = guild.getIdLong();
        return setRepo.findById(new ReactionRoleSet.Key(guildId, set)).map(roleSet -> {
            var roleBind = roleSet.findBinding(role).orElseThrow(() -> new CommandError("No such role: " + role));
            var embed    = new EmbedBuilder().setColor(new Color(role.hashCode())).addField(roleBind.toField());

            return new MessageCreateBuilder().addEmbeds(embed.build())
                    .addComponents(ActionRow.of(Button.primary(COMPONENT_ID_ROLE_EDIT + set + ':' + role, "Edit...")))
                    .build();
        }).orElseThrow(() -> new CommandError("Could not find reaction role set with name `%s`".formatted(set)));
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!event.getInteraction().getName().equals("Refresh")) return;

        var message = event.getTarget();
        if (!(message.getAuthor() instanceof SelfUser)) return;

        var set = setRepo.findByMessageId(message.getIdLong()).orElseThrow();
        message.editMessageEmbeds(set.toEmbed().build()).queue();

        audit().newEntry()
                .guild(event.getGuild())
                .source(this)
                .message("%s is refreshing reaction-role message for set %s".formatted(event.getMember(), set))
                .queue();
        event.reply("Successfully refreshed the message").setEphemeral(true).queue();
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null || Stream.of(COMPONENT_ID_ROLESET_EDIT,
                COMPONENT_ID_ROLE_ADD,
                COMPONENT_ID_ROLE_REMOVE,
                COMPONENT_ID_ROLE_EDIT).noneMatch(event.getComponentId()::startsWith)) return;

        var    componentId = event.getCustomId();
        var    li          = componentId.lastIndexOf('-');
        var    setName     = componentId.substring(li + 1);
        var    ci          = setName.indexOf(':');
        String roleName    = null;
        if (ci != -1) {
            roleName = setName.substring(ci + 1);
            setName  = setName.substring(0, ci);
        }
        var action  = componentId.substring(0, li + 1);
        var roleSet = setRepo.findById(new ReactionRoleSet.Key(event.getGuild().getIdLong(), setName)).orElseThrow();

        switch (action) {
            case COMPONENT_ID_ROLE_ADD -> event.replyModal(createRoleMutatorModal(componentId,
                    "Select role to add",
                    null).build()).queue();
            case COMPONENT_ID_ROLE_EDIT -> {
                var role = roleSet.findBinding(roleName).orElseThrow();
                event.replyModal(createRoleMutatorModal(componentId, "Editing role", role).build()).queue();
            }
            case COMPONENT_ID_ROLESET_EDIT -> event.replyModal(createRoleSetMutatorModal(componentId,
                    "Editing roleset",
                    roleSet).build()).queue();
            case COMPONENT_ID_ROLE_REMOVE -> {
                var selectMenu = StringSelectMenu.create(OPTION_ID_ROLE);
                for (var role : roleSet.getRoles())
                    selectMenu.addOption(role.getName(),
                            role.getName(),
                            role.getDescription(),
                            Emoji.fromFormatted(role.getEmoji()));
                event.replyModal(Modal.create(componentId, "Select role to remove")
                        .addComponents(Label.of("Role", selectMenu.build()))
                        .build()).queue();
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getGuild() == null || Stream.of(COMPONENT_ID_ROLESET_EDIT,
                COMPONENT_ID_ROLE_ADD,
                COMPONENT_ID_ROLE_REMOVE,
                COMPONENT_ID_ROLE_EDIT).noneMatch(event.getModalId()::startsWith)) return;

        var    componentId = event.getCustomId();
        var    li          = componentId.lastIndexOf('-');
        var    setName     = componentId.substring(li + 1);
        var    ci          = setName.indexOf(':');
        String roleName    = null;
        if (ci != -1) {
            roleName = setName.substring(ci + 1);
            setName  = setName.substring(0, ci);
        }
        var action      = componentId.substring(0, li + 1);
        var guildId     = event.getGuild().getIdLong();
        var roleSet     = setRepo.findById(new ReactionRoleSet.Key(guildId, setName)).orElseThrow();
        var interaction = event.getInteraction();

        switch (action) {
            case COMPONENT_ID_ROLESET_EDIT -> {
                var mapping = interaction.getValue(OPTION_ID_CHANNEL);
                if (mapping != null) roleSet.setChannelId(mapping.getAsMentions().getChannels().getFirst().getIdLong());

                mapping = interaction.getValue(OPTION_ID_NAME);
                if (mapping != null && !mapping.getAsString().isBlank()) {
                    var buf = mapping.getAsString();
                    if (setRepo.findById(new ReactionRoleSet.Key(guildId, buf)).isPresent()) {
                        event.reply("%s Role set with name %s already exists".formatted(Constant.EMOJI_WARNING, buf))
                                .setEphemeral(true)
                                .queue();
                        return;
                    }
                    roleSet.setName(buf);
                }

                mapping = interaction.getValue(OPTION_ID_DESCRIPTION);
                if (mapping != null && !mapping.getAsString().isBlank()) roleSet.setDescription(mapping.getAsString());

                mapping = interaction.getValue(OPTION_ID_METHOD);
                if (mapping != null) roleSet.setMethod(ReactionRoleSet.Method.valueOf(mapping.getAsStringList()
                        .getFirst()));

                audit().newEntry()
                        .guild(event.getGuild())
                        .source(this)
                        .message("%s is editing reaction-role set %s".formatted(event.getMember(), roleSet))
                        .queue();
                setRepo.save(roleSet);
            }
            case COMPONENT_ID_ROLE_ADD -> {
                var role = Objects.requireNonNull(interaction.getValue(OPTION_ID_ROLE), "role value")
                        .getAsMentions()
                        .getRoles()
                        .getFirst();
                var emoji = Objects.requireNonNull(interaction.getValue(OPTION_ID_EMOJI), "emoji value").getAsString();
                var name  = Objects.requireNonNull(interaction.getValue(OPTION_ID_NAME), "name value").getAsString();
                var description = Objects.requireNonNull(interaction.getValue(OPTION_ID_DESCRIPTION),
                        "description value").getAsString();

                if (roleSet.findBinding(name).isPresent()) {
                    event.reply("%s Role binding with name %s already exists".formatted(Constant.EMOJI_WARNING, name))
                            .setEphemeral(true)
                            .queue();
                }

                createrole(event.getGuild(), event.getMember(), setName, role, emoji, name, description);
            }
            case COMPONENT_ID_ROLE_REMOVE -> {
                var _roleName = Objects.requireNonNull(interaction.getValue(OPTION_ID_ROLE), "role value")
                        .getAsString();
                roleSet.getRoles().removeIf(role -> role.getName().equals(_roleName));

                audit().newEntry()
                        .guild(event.getGuild())
                        .source(this)
                        .message("%s is removing reaction-role %s from set %s".formatted(event.getMember(),
                                _roleName,
                                roleSet))
                        .queue();
                setRepo.save(roleSet);
            }
            case COMPONENT_ID_ROLE_EDIT -> {
                var role = roleSet.findBinding(roleName).orElseThrow();

                var mapping = interaction.getValue(OPTION_ID_ROLE);
                if (mapping != null) role.setRoleId(mapping.getAsMentions().getRoles().getFirst().getIdLong());

                mapping = interaction.getValue(OPTION_ID_EMOJI);
                if (mapping != null && !mapping.getAsString().isBlank()) role.setEmoji(mapping.getAsString());

                mapping = interaction.getValue(OPTION_ID_NAME);
                if (mapping != null && !mapping.getAsString().isBlank()) {
                    var buf = mapping.getAsString();
                    if (roleSet.findBinding(roleName).isPresent()) {
                        event.reply("%s Role binding with name %s already exists".formatted(Constant.EMOJI_WARNING,
                                buf)).setEphemeral(true).queue();
                        return;
                    }
                    role.setName(buf);
                }

                mapping = interaction.getValue(OPTION_ID_DESCRIPTION);
                if (mapping != null && !mapping.getAsString().isBlank()) role.setDescription(mapping.getAsString());

                setRepo.save(roleSet);
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }

        audit().newEntry()
                .guild(event.getGuild())
                .source(this)
                .message("%s is editing reaction-role %s in set %s".formatted(event.getMember(), roleName, roleSet))
                .queue();
        event.reply("This interaction has been successful!").setEphemeral(true).queue();
    }

    @Override
    public void onMessageReactionAdd(@NonNull MessageReactionAddEvent event) {
        reactionInteract(event, event.getGuild()::addRoleToMember);
    }

    @Override
    public void onMessageReactionRemove(@NonNull MessageReactionRemoveEvent event) {
        reactionInteract(event, event.getGuild()::removeRoleFromMember);
    }

    private void reactionInteract(GenericMessageReactionEvent event, BiFunction<User, Role, RestAction<?>> action) {
        var emoji = event.getEmoji();
        var user  = event.getUser();

        if (user == null || user instanceof SelfUser) return;

        var result = setRepo.findByMessageId(event.getMessageIdLong());
        if (result.isEmpty()) return;
        var set = result.get();

        var binding = set.getRoles().stream().filter(role -> role.getEmoji().equals(emoji.getFormatted())).findAny();

        // remove unknown reactions
        if (binding.isEmpty() || !set.getMethod().mayPerformAction(event)) {
            event.getReaction().removeReaction(event.getUser()).queue();
            return;
        }

        binding.map(ReactionRoleBinding::getRoleId)
                .map(jda::getRoleById)
                .ifPresent(role -> action.apply(user, role).queue());
    }

    @SuppressWarnings("SameParameterValue")
    private Modal.Builder createRoleSetMutatorModal(String modalId, String title, @Nullable ReactionRoleSet roleSet) {
        var optionChannel     = EntitySelectMenu.create(OPTION_ID_CHANNEL, EntitySelectMenu.SelectTarget.CHANNEL);
        var optionName        = TextInput.create(OPTION_ID_NAME, TextInputStyle.SHORT);
        var optionDescription = TextInput.create(OPTION_ID_DESCRIPTION, TextInputStyle.PARAGRAPH);
        var optionMethod = StringSelectMenu.create(OPTION_ID_METHOD)
                .addOptions(Arrays.stream(ReactionRoleSet.Method.values())
                        .map(ReactionRoleSet.Method::getSelectOption)
                        .toList())
                .setDefaultOptions(ReactionRoleSet.Method.PICK_MANY.getSelectOption());

        if (roleSet != null) {
            optionChannel.setDefaultValues(EntitySelectMenu.DefaultValue.channel(roleSet.getChannelId()));
            optionName.setRequired(false).setPlaceholder(roleSet.getName());
            optionDescription.setRequired(false).setPlaceholder(roleSet.getDescription());
            if (roleSet.getMethod() != null) optionMethod.setDefaultOptions(roleSet.getMethod().getSelectOption());
        }

        return Modal.create(modalId, title)
                .addComponents(Label.of("Channel", optionChannel.build()),
                        Label.of("Name", optionName.build()),
                        Label.of("Description", optionDescription.build()),
                        Label.of("Method", optionMethod.build()));
    }

    private Modal.Builder createRoleMutatorModal(String modalId, String title, @Nullable ReactionRoleBinding role) {
        var optionRole        = EntitySelectMenu.create(OPTION_ID_ROLE, EntitySelectMenu.SelectTarget.ROLE);
        var optionEmoji       = TextInput.create(OPTION_ID_EMOJI, TextInputStyle.SHORT);
        var optionName        = TextInput.create(OPTION_ID_NAME, TextInputStyle.SHORT);
        var optionDescription = TextInput.create(OPTION_ID_DESCRIPTION, TextInputStyle.PARAGRAPH);

        if (role != null) {
            optionRole.setDefaultValues(EntitySelectMenu.DefaultValue.role(role.getRoleId()));
            optionEmoji.setRequired(false).setPlaceholder(role.getEmoji());
            optionName.setRequired(false).setPlaceholder(role.getName());
            optionDescription.setRequired(false).setPlaceholder(role.getDescription());
        }

        return Modal.create(modalId, title)
                .addComponents(Label.of("Role", optionRole.build()),
                        Label.of("Emoji", optionEmoji.build()),
                        Label.of("Name", optionName.build()),
                        Label.of("Description", optionDescription.build()));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        var jda = event.getApplicationContext().getBean(JDA.class);
        jda.addEventListener(this);
        jda.upsertCommand(Commands.message("Refresh")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))).queue();

        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
