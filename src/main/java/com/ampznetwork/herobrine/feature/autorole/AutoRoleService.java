package com.ampznetwork.herobrine.feature.autorole;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.autorole.model.AutoRoleMapping;
import com.ampznetwork.herobrine.repo.AutoRoleRepository;
import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import lombok.Value;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Event;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Log
@Service
@Command("autorole")
@Description("Configure automatically assigned roles")
public class AutoRoleService extends ListenerAdapter implements AuditLogSender {
    @Autowired       AutoRoleRepository      repo;
    @Lazy @Autowired Event.Bus<GenericEvent> jdaEventBus;

    @Command(permission = "268435456")
    @Description("List all currently configured automated roles")
    public EmbedBuilder list(Guild guild) {
        var embed = new EmbedBuilder().setTitle("All configured automated roles");

        repo.findAllByGuildId(guild.getIdLong()).stream().map(AutoRoleMapping::toField).forEach(embed::addField);

        return embed;
    }

    @Command(permission = "268435456")
    @Description("Create a new mapping for an automated role")
    public String create(
            Guild guild, Member member, @Command.Arg @Description("The role to use for the automation") Role role,
            @Command.Arg(autoFillProvider = DiscordTrigger.AutoFillNames.class) @Description(
                    "The trigger to use for the automation") String trigger
    ) {
        if (repo.existsById(new AutoRoleMapping.Key(guild.getIdLong(), role.getIdLong())))
            throw new CommandError("Automation for role %s already exists".formatted(role));

        DiscordTrigger<? extends GenericGuildMemberEvent> triggerResult = DiscordTrigger.valueOf(trigger);
        if (triggerResult == null) throw new CommandError("Trigger with name `%s` was not found".formatted(trigger));
        if (!GenericGuildMemberEvent.class.isAssignableFrom(triggerResult.getEventType()))
            throw new CommandError("This trigger is incompatible as a role automation");

        var mapping = AutoRoleMapping.builder()
                .guildId(guild.getIdLong())
                .roleId(role.getIdLong())
                .discordTrigger(triggerResult);

        var autoRoleMapping = mapping.build();

        audit().newEntry()
                .guild(guild)
                .source(this)
                .message("%s is creating an automation for role %s, based on %s".formatted(member, role, trigger))
                .queue();
        repo.save(autoRoleMapping);
        initialize(autoRoleMapping);

        return "Role automation `%s` was created".formatted(autoRoleMapping);
    }

    @Command(permission = "268435456")
    @Description("Remove a mapping for an automated role")
    public String remove(
            Guild guild, Member member,
            @Command.Arg @Description("The role to remove from automations") Role role
    ) {
        var key = new AutoRoleMapping.Key(guild.getIdLong(), role.getIdLong());

        if (!repo.existsById(key)) throw new CommandError("Mapping for role %s was not found".formatted(role));

        audit().newEntry()
                .guild(guild)
                .source(this)
                .message("%s is removing an automation for role %s".formatted(member, role))
                .queue();
        repo.deleteById(key);
        return "Automation for role %s was deleted".formatted(role);
    }

    public void initialize(final AutoRoleMapping mapping) {
        var listener = mapping.getDiscordTrigger().apply(jdaEventBus).subscribeData(new EventHandler(mapping));
        mapping.getListeners().add(listener);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        for (var entry : repo.findAll()) initialize(entry);

        log.info("Initialized");
    }

    @Value
    private class EventHandler implements Consumer<GenericGuildMemberEvent> {
        AutoRoleMapping mapping;

        @Override
        public void accept(GenericGuildMemberEvent event) {
            var guild  = event.getGuild();
            var member = event.getMember();
            var role   = guild.getRoleById(mapping.getRoleId());

            if (role == null) {
                log.warning("Invalid role mapping; role not found with id %d".formatted(mapping.getRoleId()));
                return;
            }

            audit().newEntry()
                    .guild(guild)
                    .source(this)
                    .message("%s is automatically receiving role %s, as defined by %s".formatted(member, role, mapping))
                    .queue();
            guild.addRoleToMember(member, role).queue();
        }
    }
}
