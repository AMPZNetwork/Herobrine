package com.ampznetwork.herobrine.component.team.role;

import com.ampznetwork.herobrine.component.team.model.SupportLevel;
import com.ampznetwork.herobrine.component.team.model.TeamCategory;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import net.dv8tion.jda.api.entities.Role;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@IdClass(TeamRoleInfo.Key.class)
public class TeamRoleInfo {
    @Id                          long               guildId;
    @Id                          long               roleId;
    @Nullable                    TeamCategory       teamCategory;
    @Nullable                    SupportLevel       supportLevel;
    @ElementCollection @Singular Set<@NonNull Long> inherits = new HashSet<>();

    public record Key(long guildId, long roleId) {
        public Key(Role role) {
            this(role.getGuild().getIdLong(), role.getIdLong());
        }
    }
}
