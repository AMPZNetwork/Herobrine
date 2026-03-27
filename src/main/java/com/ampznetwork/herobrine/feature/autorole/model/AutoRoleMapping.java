package com.ampznetwork.herobrine.feature.autorole.model;

import com.ampznetwork.herobrine.feature.autorole.AutoRoleService;
import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Log
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AutoRoleMapping.Key.class)
public class AutoRoleMapping implements BiConsumer<AutoRoleService, GenericGuildMemberEvent> {
    @Id                                                  long                                              guildId;
    @Id                                                  long                                              roleId;
    @Convert(converter = DiscordTrigger.Converter.class) DiscordTrigger<? extends GenericGuildMemberEvent> discordTrigger;

    public MessageEmbed.Field toField() {
        var role = bean(JDA.class).getRoleById(roleId);

        Objects.requireNonNull(role, "role by id: " + roleId);

        return new MessageEmbed.Field(role.getName(), "Assigned on `%s`".formatted(discordTrigger.getEventType().getSimpleName()), false);
    }

    @Override
    public String toString() {
        var roleName = Optional.ofNullable(bean(JDA.class).getRoleById(roleId))
                .map(IMentionable::getAsMention)
                .orElse("<role not found: %d>".formatted(roleId));
        return "on %s -> %s".formatted(discordTrigger.getEventType().getSimpleName(), roleName);
    }

    @Override
    public void accept(AutoRoleService autoRoles, GenericGuildMemberEvent event) {
        var guild  = event.getGuild();
        var member = event.getMember();
        var role   = guild.getRoleById(roleId);

        if (role == null) {
            log.warning("Invalid role mapping; role not found with id %d".formatted(roleId));
            return;
        }

        autoRoles.newAuditEntry().guild(guild).message("%s is automatically receiving role %s, as defined by %s".formatted(member, role, this)).queue();
        guild.addRoleToMember(member, role).queue();
    }

    public record Key(long guildId, long roleId) {}
}
