package com.ampznetwork.herobrine.feature.autorole.model;

import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import org.comroid.api.func.util.Event;
import org.comroid.api.tree.UncheckedCloseable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AutoRoleMapping.Key.class)
public class AutoRoleMapping implements UncheckedCloseable {
    @Id                                                  long                                              guildId;
    @Id                                                  long                                              roleId;
    @Convert(converter = DiscordTrigger.Converter.class) DiscordTrigger<? extends GenericGuildMemberEvent> discordTrigger;
    @Transient @lombok.Builder.Default
    Set<Event.Listener<?>> listeners = new HashSet<>();

    @Override
    @PreDestroy
    public void close() {
        for (var listener : listeners) listener.close();
    }

    public MessageEmbed.Field toField() {
        var role = bean(JDA.class).getRoleById(roleId);

        Objects.requireNonNull(role, "role by id: " + roleId);

        return new MessageEmbed.Field(role.getName(),
                "Assigned on `%s`".formatted(discordTrigger.getEventType().getSimpleName()),
                false);
    }

    @Override
    public String toString() {
        var roleName = Optional.ofNullable(bean(JDA.class).getRoleById(roleId))
                .map(IMentionable::getAsMention)
                .orElse("<role not found: %d>".formatted(roleId));
        return "on %s -> %s".formatted(discordTrigger.getEventType().getSimpleName(), roleName);
    }

    public record Key(long guildId, long roleId) {}
}
