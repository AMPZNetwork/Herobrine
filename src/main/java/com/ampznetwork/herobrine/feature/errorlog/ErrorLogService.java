package com.ampznetwork.herobrine.feature.errorlog;

import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogPreferences;
import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogSender;
import com.ampznetwork.herobrine.repo.ErrorLogPreferenceRepo;
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
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.component.error.ErrorHandler;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.model.Response;
import org.comroid.interaction.registry.InstanceRegistry;
import org.comroid.util.JdaUtil;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.logging.Level;

@Log
@Service
@Interaction("errorlog")
@org.comroid.annotations.Order(-10)
@Description("Configure internal Error Log")
public class ErrorLogService implements ErrorHandler {
    @Autowired ErrorLogPreferenceRepo prefRepo;
    @Autowired JDA                    jda;

    @Interaction(definitions = { @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "ADMINISTRATOR") })
    @Description("Show current error log configuration")
    public MessageEmbed info(Guild guild) {
        return prefRepo.findById(guild.getIdLong()).map(prefs -> prefs.toEmbed().build()).orElseThrow(() -> Response.of("No error log configuration found"));
    }

    @Interaction(definitions = { @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "ADMINISTRATOR") })
    @Description("Change error log channel configuration")
    public EmbedBuilder channel(Guild guild, @Parameter @Description("The channel to send the error log to") TextChannel channel) {
        var guildId     = guild.getIdLong();
        var channelId   = channel.getIdLong();
        var preferences = prefRepo.findById(guildId).map(prefs -> prefs.setChannelId(channelId)).orElseGet(() -> new ErrorLogPreferences(guildId, channelId));

        prefRepo.save(preferences);
        return preferences.toEmbed();
    }

    @Builder(builderMethodName = "newEntry", buildMethodName = "queue", builderClassName = "EntryAPI")
    public void queueEntry(Guild guild, @Nullable Level level, Object source, CharSequence message, @Nullable Throwable throwable) {
        try {
            if (level == null) level = Level.INFO;

            var guildId     = guild.getIdLong();
            var prefsResult = prefRepo.findById(guildId);

            if (prefsResult.isEmpty()) return;

            var prefs   = prefsResult.get();
            var channel = jda.getTextChannelById(prefs.getChannelId());

            if (channel == null) {
                log.warning("Unable to send Error Log to channel with id %d; channel not found".formatted(prefs.getChannelId()));
                return;
            }

            var sourceName = source instanceof ErrorLogSender sender ? sender.getErrorSourceName() : String.valueOf(source);
            var embed      = JdaUtil.logEntryEmbed(level, sourceName, message, throwable);

            channel.sendMessageEmbeds(embed).queue();
            log.log(Level.FINE, "[%s @ %s] %s: %s".formatted(level.getName(), guild, sourceName, message), throwable);
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

    @Override
    public @org.jspecify.annotations.Nullable Object handle(InteractionContext context, Throwable error) {
        context.child(Guild.class)
                .ifPresent(guild -> queueEntry(guild,
                        Level.SEVERE,
                        context.getNode().getSource().as(InstanceRegistry.class).into(InstanceRegistry::getInstance),
                        "Error in command",
                        error));

        return Optional.empty();
    }
}
