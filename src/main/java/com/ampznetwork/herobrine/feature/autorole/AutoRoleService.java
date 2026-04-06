package com.ampznetwork.herobrine.feature.autorole;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.autorole.model.AutoRoleMapping;
import com.ampznetwork.herobrine.repo.AutoRoleRepository;
import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Log
@Service
@Interaction("autorole")
@Description("Configure automatically assigned roles")
public class AutoRoleService implements AuditLogSender {
    @Autowired AutoRoleRepository repo;

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "268435456"))
    @Description("List all currently configured automated roles")
    public EmbedBuilder list(Guild guild) {
        var embed = new EmbedBuilder().setTitle("All configured automated roles");

        repo.findAllByGuildId(guild.getIdLong()).stream().map(AutoRoleMapping::toField).forEach(embed::addField);

        return embed;
    }

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "268435456"))
    @Description("Create a new mapping for an automated role")
    public String create(
            Guild guild, Member member, @Parameter @Description("The role to use for the automation") Role role,
            @Parameter(completion = @Completion(provider = DiscordTrigger.AutoFillNames.class)) @Description("The trigger to use for the automation") String trigger
    ) {
        if (repo.existsById(new AutoRoleMapping.Key(guild.getIdLong(), role.getIdLong())))
            throw Response.of("Automation for role %s already exists".formatted(role));

        DiscordTrigger<? extends GenericGuildMemberEvent> triggerResult = DiscordTrigger.valueOf(trigger);
        if (triggerResult == null) throw Response.of("Trigger with name `%s` was not found".formatted(trigger));
        if (!GenericGuildMemberEvent.class.isAssignableFrom(triggerResult.getEventType()))
            throw Response.of("This trigger is incompatible as a role automation");

        var mapping = AutoRoleMapping.builder().guildId(guild.getIdLong()).roleId(role.getIdLong()).discordTrigger(triggerResult);

        var autoRoleMapping = mapping.build();

        audit().newEntry().guild(guild).source(this).message("%s is creating an automation for role %s, based on %s".formatted(member, role, trigger)).queue();
        repo.save(autoRoleMapping);

        return "Role automation `%s` was created".formatted(autoRoleMapping);
    }

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "268435456"))
    @Description("Remove a mapping for an automated role")
    public String remove(Guild guild, Member member, @Parameter @Description("The role to remove from automations") Role role) {
        var key = new AutoRoleMapping.Key(guild.getIdLong(), role.getIdLong());

        if (!repo.existsById(key)) throw Response.of("Mapping for role %s was not found".formatted(role));

        audit().newEntry().guild(guild).source(this).message("%s is removing an automation for role %s".formatted(member, role)).queue();
        repo.deleteById(key);
        return "Automation for role %s was deleted".formatted(role);
    }

    @EventListener
    public void on(GenericEvent event) {
        if (!(event instanceof GenericGuildMemberEvent)) return; // todo remove this antipattern

        for (var mapping : repo.findAll()) {
            if (!mapping.getDiscordTrigger().test(event)) continue;

            mapping.accept(this, (GenericGuildMemberEvent) event);
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }
}
