package com.ampznetwork.herobrine.component.user.tags;

import com.ampznetwork.herobrine.component.user.tags.model.UserTag;
import com.ampznetwork.herobrine.component.user.tags.model.UserTagProvider;
import com.ampznetwork.herobrine.repo.UserTagProviderRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
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

@Log
@Service
@Interaction(value = "user-tag-provider", filter = { @ContextFilter(key = "guild") }, definitions = {
        @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "MANAGE_ROLES")
})
public class UserTagProviderService {
    @Autowired UserTagProviderRepository providers;

    @Interaction
    @Description("Create a new user tag provider")
    public void create(Guild guild, @Parameter UserTag tag, @Parameter UserTagProvider.Method method, @Parameter IMentionable meta) {
        var key = new UserTagProvider.Key(guild.getIdLong(), tag, method, meta.getIdLong());

        if (providers.existsById(key)) throw new RuntimeException("Provider already exists; " + key);

        var provider = new UserTagProvider(key);

        providers.save(provider);
    }

    @Interaction
    @Description("Delete an existing user tag provider")
    public void delete(
            Guild guild, @Parameter(completion = @Completion(provider = UserTagProvider.ContextualUserTagProvider.class)) UserTag tag,
            @Parameter(completion = @Completion(provider = UserTagProvider.ContextualUserTagMethodProvider.class)) UserTagProvider.Method method,
            @Parameter IMentionable meta
    ) {
        var key = new UserTagProvider.Key(guild.getIdLong(), tag, method, meta.getIdLong());

        if (providers.existsById(key)) throw new RuntimeException("Provider already exists; " + key);

        var provider = new UserTagProvider(key);

        providers.save(provider);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }
}
