package com.ampznetwork.herobrine.timezone;

import com.ampznetwork.herobrine.haste.HasteService;
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
import net.dv8tion.jda.api.requests.RestAction;
import org.comroid.api.func.util.DelegateStream;
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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
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

        boolean any = false;
        while (matcher.find()) {
            if (matcher.group(0).matches("\\d+")) continue;
            any = true;
        }

        if (any) message.addReaction(EMOJI).queue();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.getReaction().getEmoji().equals(EMOJI)) return;

        var message  = event.retrieveMessage().complete();
        var reaction = message.getReaction(EMOJI);
        var user     = event.getUser();
        var author   = message.getAuthor();
        var channel = event.getChannel();

        if (reaction == null || user == null || user instanceof SelfUser) return;

        RestAction<?> action = reaction.removeReaction(user);
        try {
            var config = bean(TimezoneConfiguration.class);
            var opt    = config.getZoneId(author);
            if (opt.isEmpty()) {
                action = action.flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription(
                                "Sorry, but %s did not set their timezone!".formatted(author))
                        .setFooter("Set it with `/time zone <id>` - This message self destructs in 30 seconds")
                        .build()).delay(30, TimeUnit.SECONDS).flatMap(Message::delete));
                return;
            }
            var authorZone = opt.get();
            opt = config.getZoneId(user);
            if (opt.isEmpty()) {
                action = action.flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription(
                                "Sorry, but you" + " did not " + "set your " + "timezone!")
                        .setFooter("Set it with `/time zone <id>` - This message self destructs in 30 seconds")
                        .build()).delay(30, TimeUnit.SECONDS).flatMap(Message::delete));
                return;
            }
            var targetZone = opt.get();

            var matcher = TIME_PATTERN.matcher(message.getContentDisplay());
            var embed = new EmbedBuilder().setTitle("Timezone conversion")
                    .setFooter(user.getEffectiveName() + " - This message self destructs in 5 minutes",
                            user.getAvatarUrl());

            while (matcher.find()) {
                var time          = matchTime(matcher);
                var zonedTime     = ZonedDateTime.of(LocalDate.now(), time, authorZone);
                var convertedTime = zonedTime.withZoneSameInstant(targetZone);

                embed.addField("%s mentioned the time `%s` (`%s`)".formatted(author.getEffectiveName(),
                                matcher.group(0),
                                FORMATTER.format(zonedTime)),
                        "That would be `%s` in your time zone".formatted(FORMATTER.format(convertedTime)),
                        false);
            }

            action = action.flatMap($ -> channel.sendMessageEmbeds(embed.build())
                    .delay(5, TimeUnit.MINUTES)
                    .flatMap(Message::delete));
        } catch (Throwable t) {
            var haste = bean(HasteService.class);

            String str;
            try (
                    var sw = new StringWriter(); var adpOut = new DelegateStream.Output(sw);
                    var ps = new PrintStream(adpOut)
            ) {
                t.printStackTrace(ps);
                str = sw.toString();

                String hasteId;
                try (var sr = new StringReader(str); var adpIn = new DelegateStream.Input(sr)) {
                    hasteId = haste.post(adpIn, "exception.txt");
                    action  = action.flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.RED)
                            .setTitle("An internal error ocurred")
                            .setUrl(HasteService.URL_PREFIX + hasteId)
                            .build()));
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not upload exception info to Haste service", e);
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not handle exception", e);
                log.log(Level.SEVERE, "-> Underlying exception:", t);
            }
        } finally {
            action.queue();
        }
    }

    private LocalTime matchTime(Matcher matcher) {
        var mid  = matcher.group("mid");
        var hour = Integer.parseInt(matcher.group("hour"));
        if (mid != null) switch (mid) {
            case "am":
                if (hour == 12) hour = 0;
                break;
            case "pm":
                if (hour != 12) hour += 12;
                break;
        }

        var minuteStr = matcher.group("minute");
        var minute    = minuteStr == null || minuteStr.isBlank() ? 0 : Integer.parseInt(minuteStr);

        if (hour >= 24) hour %= 24;
        if (minute >= 60) minute %= 60;

        return LocalTime.of(hour, minute);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
