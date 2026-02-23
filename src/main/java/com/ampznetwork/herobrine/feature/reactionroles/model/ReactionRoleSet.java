package com.ampznetwork.herobrine.feature.reactionroles.model;

import com.ampznetwork.herobrine.repo.ReactionSetRepo;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Instance;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ReactionRoleSet.Key.class)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "message_id") })
public class ReactionRoleSet {
    @Id long   guildId;
    @Id String name;
    String description;
    long   channelId;
    Method method;
    @Nullable                                   Long                      messageId;
    @ElementCollection(fetch = FetchType.EAGER) List<ReactionRoleBinding> roles;

    public MessageCreateBuilder createMessage() {
        return new MessageCreateBuilder().addEmbeds(toEmbed().build());
    }

    public EmbedBuilder toEmbed() {
        var embed = new EmbedBuilder().setTitle(name)
                .setDescription(description)
                .setColor(new Color(hashCode()))
                .setFooter(method.getFooterText());
        roles.stream().map(ReactionRoleBinding::toField).forEachOrdered(embed::addField);
        return embed;
    }

    public Optional<ReactionRoleBinding> findBinding(String name) {
        return roles.stream().filter(role -> role.getName().equals(name)).findAny();
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, name);
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum Method {
        PICK_ONE("Select one", "Pick one of the options by reacting below") {
            @Override
            public boolean mayPerformAction(GenericMessageReactionEvent event) {
                var user  = event.retrieveUser().complete();
                var emoji = event.getEmoji();

                return event.retrieveMessage()
                        .complete()
                        .getReactions()
                        .stream()
                        .filter(reaction -> !reaction.getEmoji().equals(emoji))
                        .filter(reaction -> reaction.retrieveUsers().complete().stream().anyMatch(user::equals))
                        .findAny()
                        .isEmpty();
            }
        }, PICK_MANY("Pick all that apply", "Select your desired roles by reacting below") {
            @Override
            public boolean mayPerformAction(GenericMessageReactionEvent event) {
                return true;
            }
        }, VERIFY("Select one thing, only once", "Verify by reacting below") {
            @Override
            public boolean mayPerformAction(GenericMessageReactionEvent event) {
                var member = event.retrieveMember().complete();

                return bean(ReactionSetRepo.class).findByMessageId(event.getMessageIdLong())
                        .stream()
                        .flatMap(set -> set.roles.stream())
                        .mapToLong(ReactionRoleBinding::getRoleId)
                        .noneMatch(roleId -> member.getRoles()
                                .stream()
                                .mapToLong(ISnowflake::getIdLong)
                                .anyMatch(other -> roleId == other));
            }
        };

        SelectOption selectOption;
        String       footerText;

        Method(String label, String footerText) {
            this.selectOption = SelectOption.of(label, name());
            this.footerText   = footerText;
        }

        public abstract boolean mayPerformAction(GenericMessageReactionEvent event);
    }

    public enum AutoFillSetNames implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return usage.fromContext(Guild.class)
                    .flatMap(guild -> bean(ReactionSetRepo.class).findAllByGuildId(guild.getIdLong()).stream())
                    .map(ReactionRoleSet::getName);
        }
    }

    public enum AutoFillRoleNames implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return usage.fromContext(Guild.class)
                    .flatMap(guild -> bean(ReactionSetRepo.class).findAllByGuildId(guild.getIdLong()).stream())
                    .flatMap(set -> set.getRoles().stream())
                    .map(ReactionRoleBinding::getName);
        }
    }

    public record Key(long guildId, String name) {}
}
