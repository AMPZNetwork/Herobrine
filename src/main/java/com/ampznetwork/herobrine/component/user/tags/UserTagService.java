package com.ampznetwork.herobrine.component.user.tags;

import com.ampznetwork.herobrine.component.user.tags.model.UserTag;
import com.ampznetwork.herobrine.repo.UserTagProviderRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Streams;
import org.comroid.api.text.Markdown;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log
@Service
@Interaction(value = "user-tags", filter = { @ContextFilter(key = "guild") }, definitions = {
        @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "MANAGE_ROLES")
})
public class UserTagService {
    @Autowired UserTagProviderRepository providers;

    @Interaction
    @Description("Show all tags of a member")
    public String lookup(@Parameter Member member) {
        return findTags(member).map(Enum::name)
                .collect(Streams.orElseGet(() -> Markdown.Code.apply("<none>")))
                .collect(Collectors.joining("\n- ", "## Tags of %s\n- ".formatted(member), ""));
    }

    public Stream<UserTag> findTags(Member member) {
        return providers.findAllByGuildId(member.getGuild().getIdLong())
                .stream()
                .flatMap(provider -> provider.find(member))
                .flatMap(UserTag::expand)
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal));
    }
}
