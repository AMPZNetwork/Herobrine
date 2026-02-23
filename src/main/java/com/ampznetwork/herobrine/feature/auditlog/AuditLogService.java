package com.ampznetwork.herobrine.feature.auditlog;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogPreferences;
import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.repo.AuditLogPreferenceRepo;
import lombok.Builder;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Markdown;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.logging.Level;

@Log
@Service
@Command("auditlog")
@Description("Configure internal Audit Log")
public class AuditLogService extends ListenerAdapter {
    @Autowired AuditLogPreferenceRepo prefRepo;
    @Autowired JDA                    jda;

    @Command(permission = "8")
    @Description("Show current audit log configuration")
    public MessageEmbed info(Guild guild) {
        return prefRepo.findById(guild.getIdLong())
                .map(prefs -> prefs.toEmbed().build())
                .orElseThrow(() -> new CommandError("No audit log configuration found"));
    }

    @Command(permission = "8")
    @Description("Change audit log channel configuration")
    public EmbedBuilder channel(
            Guild guild,
            @Command.Arg @Description("The channel to send the audit log to") TextChannel channel
    ) {
        var guildId   = guild.getIdLong();
        var channelId = channel.getIdLong();
        var preferences = prefRepo.findById(guildId)
                .map(prefs -> prefs.setChannelId(channelId))
                .orElseGet(() -> new AuditLogPreferences(guildId, channelId));

        prefRepo.save(preferences);
        return preferences.toEmbed();
    }

    @Builder(builderMethodName = "newEntry", buildMethodName = "queue", builderClassName = "EntryAPI")
    public void queueEntry(
            Guild guild, @Nullable Level level, Object source, CharSequence message,
            @Nullable Throwable t
    ) {
        try {
            if (level == null) level = Level.INFO;

            var guildId     = guild.getIdLong();
            var prefsResult = prefRepo.findById(guildId);

            if (prefsResult.isEmpty()) return;

            var prefs   = prefsResult.get();
            var channel = jda.getTextChannelById(prefs.getChannelId());

            if (channel == null) {
                log.warning("Unable to send Audit Log to channel with id %d; channel not found".formatted(prefs.getChannelId()));
                return;
            }

            var sourceName = source instanceof AuditLogSender sender
                             ? sender.getAuditSourceName()
                             : String.valueOf(source);
            var embed = toEmbed(level, sourceName, message, t);

            channel.sendMessageEmbeds(embed).queue();
            log.log(Level.FINE, "[%s @ %s] %s: %s".formatted(level.getName(), guild, sourceName, message), t);
        } catch (Throwable t0) {
            log.log(Level.WARNING, "Could not append audit log entry", t0);
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private MessageEmbed toEmbed(Level level, String sourceName, CharSequence message, @Nullable Throwable t) {
        var embed = new EmbedBuilder().setTitle(sourceName).setDescription(message).setFooter(level.getName());

        if (t != null) {
            var str = StackTraceUtils.toString(t);
            embed.addField("Attached Exception", Markdown.CodeBlock.apply(str), false);
        }

        return embed.build();
    }
}
