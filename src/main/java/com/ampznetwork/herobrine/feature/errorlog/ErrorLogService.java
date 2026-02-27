package com.ampznetwork.herobrine.feature.errorlog;

import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogPreferences;
import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogSender;
import com.ampznetwork.herobrine.repo.ErrorLogPreferenceRepo;
import com.ampznetwork.herobrine.util.JdaUtil;
import lombok.Builder;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandError;
import org.comroid.commands.model.CommandErrorHandler;
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
@Command("errorlog")
@org.comroid.annotations.Order(-10)
@Description("Configure internal Error Log")
public class ErrorLogService extends ListenerAdapter implements CommandErrorHandler {
    @Autowired ErrorLogPreferenceRepo prefRepo;
    @Autowired JDA                    jda;

    @Command(permission = "8")
    @Description("Show current error log configuration")
    public MessageEmbed info(Guild guild) {
        return prefRepo.findById(guild.getIdLong())
                .map(prefs -> prefs.toEmbed().build())
                .orElseThrow(() -> new CommandError("No error log configuration found"));
    }

    @Command(permission = "8")
    @Description("Change error log channel configuration")
    public EmbedBuilder channel(
            Guild guild,
            @Command.Arg @Description("The channel to send the error log to") TextChannel channel
    ) {
        var guildId   = guild.getIdLong();
        var channelId = channel.getIdLong();
        var preferences = prefRepo.findById(guildId)
                .map(prefs -> prefs.setChannelId(channelId))
                .orElseGet(() -> new ErrorLogPreferences(guildId, channelId));

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
                log.warning("Unable to send Error Log to channel with id %d; channel not found".formatted(prefs.getChannelId()));
                return;
            }

            var sourceName = source instanceof ErrorLogSender sender
                             ? sender.getErrorSourceName()
                             : String.valueOf(source);
            var embed = JdaUtil.logEntryEmbed(level, sourceName, message, t);

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

        var cmdr = event.getApplicationContext().getBean(CommandManager.class);
        cmdr.register(this);
        cmdr.addChild(this);

        log.info("Initialized");
    }

    @Override
    public Optional<String> handleThrowable(CommandUsage usage, Throwable throwable) {
        usage.fromContext(Guild.class)
                .findAny()
                .ifPresent(guild -> queueEntry(guild,
                        Level.SEVERE,
                        usage.getRegisteredTarget(),
                        "Error in command",
                        throwable));

        return Optional.empty();
    }
}
