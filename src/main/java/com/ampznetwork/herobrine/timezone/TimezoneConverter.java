package com.ampznetwork.herobrine.timezone;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandPrivacyLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Log
@Component
@Command("time")
public class TimezoneConverter extends ListenerAdapter {
    public static final DateTimeFormatter FORMATTER    = DateTimeFormatter.ofPattern("HH:mm");
    public static final Emoji             EMOJI        = Emoji.fromUnicode("⏰"); // ⏰
    public static final Pattern           TIME_PATTERN = Pattern.compile(
            "(?<hour>\\d{1,2})([.:]?(?<minute>\\d{1,2}))?(?<mid>[ap]m)?");

    @Bean
    public File timezoneConfigFile(@Autowired File botDir) {
        return new File(botDir, "timezones.json");
    }

    @Bean
    public TimezoneConfiguration timezoneConfiguration(
            @Autowired File timezoneConfigFile,
            @Autowired ObjectMapper objectMapper
    ) throws IOException {
        if (!timezoneConfigFile.exists()) return new TimezoneConfiguration();
        return objectMapper.readValue(timezoneConfigFile, TimezoneConfiguration.class);
    }

    @SneakyThrows
    public void saveConfiguration(TimezoneConfiguration config) {
        var configFile = bean(File.class, "timezoneConfigFile");
        bean(ObjectMapper.class).writeValue(configFile, config);
    }

    @Command(value = "zone", privacy = CommandPrivacyLevel.EPHEMERAL)
    public String zone(
            User user,
            @Command.Arg(value = "timezone", autoFillProvider = TimeZoneAutoFillProvider.class) String timezone
    ) {
        var config = bean(TimezoneConfiguration.class);
        var zone   = ZoneId.of(timezone);

        config.setZoneId(user, zone);
        saveConfiguration(config);

        return "%s Your timezone was set to `%s`".formatted(EMOJI, zone);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem()) return;

        var message = event.getMessage();
        var matcher = TIME_PATTERN.matcher(message.getContentDisplay());

        if (!matcher.find()) return;

        message.addReaction(EMOJI).queue();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.getReaction().getEmoji().equals(EMOJI)) return;

        var message  = event.retrieveMessage().complete();
        var reaction = message.getReaction(EMOJI);
        var user     = event.getUser();
        var author   = message.getAuthor();

        if (reaction == null || user == null || user instanceof SelfUser) return;

        var config  = bean(TimezoneConfiguration.class);
        var opt     = config.getZoneId(author);
        var channel = event.getChannel();
        if (opt.isEmpty()) {
            reaction.removeReaction(user)
                    .flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription(
                                    "Sorry, but %s did not set their timezone!".formatted(author))
                            .setFooter("Set it with `/time zone <id>` - This message self destructs in 30 seconds")
                            .build()))
                    .delay(30, TimeUnit.SECONDS)
                    .flatMap(Message::delete)
                    .queue();
            return;
        }
        var authorZone = opt.get();
        opt = config.getZoneId(user);
        if (opt.isEmpty()) {
            reaction.removeReaction(user)
                    .flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription(
                                    "Sorry, but you did not set your timezone!")
                            .setFooter("Set it with `/time zone <id>` - This message self destructs in 30 seconds")
                            .build()))
                    .delay(30, TimeUnit.SECONDS)
                    .flatMap(Message::delete)
                    .queue();
            return;
        }
        var targetZone = opt.get();

        var matcher = TIME_PATTERN.matcher(message.getContentDisplay());
        var embed = new EmbedBuilder().setTitle("Timezone conversion")
                .setFooter(user.getEffectiveName() + " - This message self destructs in 5 minutes",
                        user.getAvatarUrl());

        while (matcher.find()) {
            var mid  = matcher.group("mid");
            var hour = Integer.parseInt(matcher.group("hour"));
            if ("pm".equalsIgnoreCase(mid)) hour += 12;

            var minuteStr     = matcher.group("minute");
            var minute        = minuteStr == null || minuteStr.isBlank() ? 0 : Integer.parseInt(minuteStr);
            var time          = LocalTime.of(hour, minute);
            var zonedTime     = ZonedDateTime.of(LocalDate.now(), time, authorZone);
            var convertedTime = zonedTime.withZoneSameInstant(targetZone);

            embed.addField("%s mentioned the time `%s` (`%s`)".formatted(author.getEffectiveName(),
                            matcher.group(0),
                            FORMATTER.format(zonedTime)),
                    "That would be `%s` in your time zone".formatted(FORMATTER.format(convertedTime)),
                    false);
        }

        reaction.removeReaction(user)
                .flatMap($ -> channel.sendMessageEmbeds(embed.build()))
                .delay(5, TimeUnit.MINUTES)
                .flatMap(Message::delete)
                .queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
