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
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.comroid.util.JdaUtil;
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
@Interaction("auditlog")
@Description("Configure internal Audit Log")
public class AuditLogService {
    @Autowired AuditLogPreferenceRepo prefRepo;
    @Autowired JDA                    jda;

    @Interaction(definitions = @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "8"))
    @Description("Show current audit log configuration")
    public MessageEmbed info(Guild guild) {
        return prefRepo.findById(guild.getIdLong()).map(prefs -> prefs.toEmbed().build()).orElseThrow(() -> Response.of("No audit log configuration found"));
    }

    @Interaction(definitions = @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "8"))
    @Description("Change audit log configuration")
    public EmbedBuilder config(
            Guild guild, @Parameter @Description("The channel to send the audit log to") TextChannel channel, @Parameter(required = false,
                                                                                                                         completion = @Completion(provider = JdaUtil.AutoFillLogLevels.class)) @Description(
                    "The minimum level to log") @Nullable String level
    ) {
        final var logLevel = level != null ? Level.parse(level) : Level.ALL;

        var guildId   = guild.getIdLong();
        var channelId = channel.getIdLong();
        var preferences = prefRepo.findById(guildId)
                .map(prefs -> prefs.setChannelId(channelId).setMinimumLevel(logLevel))
                .orElseGet(() -> new AuditLogPreferences(guildId, channelId, logLevel));

        prefRepo.save(preferences);
        return preferences.toEmbed();
    }

    @Builder(builderMethodName = "newEntry", buildMethodName = "queue", builderClassName = "EntryAPI")
    public void queueEntry(Guild guild, @Nullable Level level, Object source, CharSequence message, @Nullable Throwable t) {
        try {
            if (level == null) level = Level.INFO;

            var guildId     = guild.getIdLong();
            var prefsResult = prefRepo.findById(guildId);

            if (prefsResult.isEmpty()) return;

            var prefs        = prefsResult.get();
            var channel      = jda.getTextChannelById(prefs.getChannelId());
            var minimumLevel = prefs.getMinimumLevel();

            if (minimumLevel != null && minimumLevel.intValue() < level.intValue()) return;

            if (channel == null) {
                log.warning("Unable to send Audit Log to channel with id %d; channel not found".formatted(prefs.getChannelId()));
                return;
            }

            var sourceName = source instanceof AuditLogSender sender ? sender.getAuditSourceName() : String.valueOf(source);
            var embed      = JdaUtil.logEntryEmbed(level, sourceName, message, t);

            channel.sendMessageEmbeds(embed).queue();
            log.log(Level.FINE, "[%s @ %s] %s: %s".formatted(level.getName(), guild, sourceName, message), t);
        } catch (Throwable t0) {
            log.log(Level.WARNING, "Could not append audit log entry", t0);
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }
}
