package com.ampznetwork.herobrine.component.user.tags;

import com.ampznetwork.herobrine.component.user.tags.model.UserTag;
import com.ampznetwork.herobrine.repo.UserTagProviderRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.stream.Collectors;

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
        return providers.findAllByGuildId(member.getGuild().getIdLong())
                .stream()
                .flatMap(provider -> provider.find(member))
                .flatMap(UserTag::expand)
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(Enum::name)
                .collect(Collectors.joining("\n- ", "## Tags of %s\n- ".formatted(member), ""));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }
}
