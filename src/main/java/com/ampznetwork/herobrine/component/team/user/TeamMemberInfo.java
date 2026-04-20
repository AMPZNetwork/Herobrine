package com.ampznetwork.herobrine.component.team.user;

import com.ampznetwork.herobrine.component.team.role.TeamRoleInfo;
import com.ampznetwork.herobrine.repo.TeamRoleInfoRepository;
import com.ampznetwork.herobrine.util.EmbedTemplate;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.api.func.util.Streams;
import org.comroid.api.text.Markdown;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TeamMemberInfo.Key.class)
public class TeamMemberInfo {
    @Id                                 long             guildId;
    @Id                                 long             userId;
    @OneToMany(fetch = FetchType.EAGER) Set<AbsenceInfo> absences;

    public Optional<AbsenceInfo> findAbsence() {
        return absences.stream().filter(AbsenceInfo::isAcute).findAny();
    }

    public EmbedBuilder toEmbed() {
        var member = guild().getMemberById(userId);
        if (member == null) return EmbedTemplate.warning("<@%d> is not a member of this guild".formatted(userId));

        var roles = member.getUnsortedRoles().stream().mapToLong(ISnowflake::getIdLong).mapToObj(id -> new TeamRoleInfo.Key(guildId, id)).toList();

        var memberRoles = bean(TeamRoleInfoRepository.class).findAllById(roles);
        if (Streams.of(memberRoles).findAny().isEmpty()) return EmbedTemplate.warning("You are not a team member");

        var embed = new EmbedBuilder().setTitle("Team Member Card").setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());

        // TeamCategory
        Streams.of(memberRoles)
                .flatMap(role -> Stream.ofNullable(role.getTeamCategory()))
                .max(Comparator.naturalOrder())
                .ifPresent(cat -> embed.addField("Member Category", cat.getName(), false));

        // SupportLevel
        Streams.of(memberRoles)
                .flatMap(role -> Stream.ofNullable(role.getSupportLevel()))
                .max(Comparator.naturalOrder())
                .ifPresent(lvl -> embed.addField("Support Level", lvl.getName(), false));

        // absences
        findAbsence().ifPresent(abs -> embed.setDescription("Currently absent " + abs));
        embed.addField("Absences",
                absences.stream()
                        .map(Object::toString)
                        .collect(Streams.orElseGet(() -> Markdown.Code.apply("<no entries>")))
                        .collect(Collectors.joining("\n- ", "- ", "")),
                false);

        return embed;
    }

    private Guild guild() {
        return bean(JDA.class).getGuildById(guildId);
    }

    public record Key(long guildId, long userId) {
        public Key(Member member) {
            this(member.getGuild().getIdLong(), member.getIdLong());
        }
    }
}
