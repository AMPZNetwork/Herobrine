package com.ampznetwork.herobrine.component.team;

import com.ampznetwork.herobrine.component.team.role.TeamRoleInfo;
import com.ampznetwork.herobrine.component.team.user.TeamMemberInfo;
import com.ampznetwork.herobrine.repo.TeamMemberInfoRepository;
import com.ampznetwork.herobrine.repo.TeamRoleInfoRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.comroid.api.func.util.Streams;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Log
@Service
@Interaction(value = "team", filter = { @ContextFilter(key = "guild") })
public class TeamMemberService {
    @Autowired TeamMemberInfoRepository members;

    @Interaction
    public Optional<EmbedBuilder> myself(Member member) {
        return members.findById(new TeamMemberInfo.Key(member)).or(() -> init(member)).map(TeamMemberInfo::toEmbed);
    }

    @Interaction(definitions = { @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "MANAGE_ROLES") })
    public Optional<EmbedBuilder> lookup(Guild guild, @Parameter UserSnowflake user) {
        return Optional.ofNullable(guild.getMember(user)).flatMap(this::myself);
    }

    public Optional<TeamMemberInfo> init(Member member) {
        return members.findById(new TeamMemberInfo.Key(member)).or(() -> {
            var guildId = member.getGuild().getIdLong();
            var roles   = member.getUnsortedRoles().stream().mapToLong(ISnowflake::getIdLong).mapToObj(id -> new TeamRoleInfo.Key(guildId, id)).toList();
            if (Streams.of(bean(TeamRoleInfoRepository.class).findAllById(roles)).findAny().isEmpty()) return Optional.empty();

            var info = new TeamMemberInfo(guildId, member.getIdLong(), new HashSet<>());
            return Optional.of(members.save(info));
        });
    }
}
