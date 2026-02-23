package com.ampznetwork.herobrine.feature.timezone;

import com.ampznetwork.herobrine.model.UserPreferences;
import com.ampznetwork.herobrine.repo.UserPreferenceRepo;
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
import org.comroid.annotations.Description;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
@Component
@Command("time")
@Description("Set your timezone preferences")
public class TimezoneConverter extends ListenerAdapter {
    public static final DateTimeFormatter FORMATTER    = DateTimeFormatter.ofPattern("HH:mm");
    public static final Emoji             EMOJI        = Emoji.fromUnicode("⏰"); // ⏰
    public static final Pattern           TIME_PATTERN = Pattern.compile(
            "(?<hour>\\d{1,2})([.:]?(?<minute>\\d{1,2}))?(?<mid>[ap]m)?");

    @Autowired UserPreferenceRepo users;

    @Bean
    public File timezoneConfigFile(@Autowired File botDir) {
        return new File(botDir, "timezones.json");
    }

    @Command(value = "zone", privacy = CommandPrivacyLevel.EPHEMERAL)
    @Description("Set your timezone")
    public String zone(
            User user, @Command.Arg(value = "timezone", autoFillProvider = TimeZoneAutoFillProvider.class) @Description(
                    "The timezone to set") String timezone
    ) {
        var zone = ZoneId.of(timezone);
        users.setTimezone(user.getIdLong(), zone);
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
            // find reacter timezone
            var opt = users.findById(user.getIdLong()).map(UserPreferences::getTimezone);
            if (opt.isEmpty()) {
                action = action.flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription(
                                "Sorry, but you did not set your timezone!")
                        .setFooter("Set it with `/time zone <id>` - This message self destructs in 30 seconds")
                        .build()).delay(30, TimeUnit.SECONDS).flatMap(Message::delete));
                return;
            }
            var targetZone = opt.get();

            // find author timezone
            opt = users.findById(author.getIdLong()).map(UserPreferences::getTimezone);
            if (opt.isEmpty()) {
                action = action.flatMap($ -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription(
                                "Sorry, but %s did not set their timezone!".formatted(author))
                        .setFooter("Set it with `/time zone <id>` - This message self destructs in 30 seconds")
                        .build()).delay(30, TimeUnit.SECONDS).flatMap(Message::delete));
                return;
            }
            var authorZone = opt.get();

            // start build embed & find time
            var matcher = TIME_PATTERN.matcher(message.getContentDisplay());
            var embed = new EmbedBuilder().setTitle("Timezone conversion")
                    .setFooter(user.getEffectiveName() + " - This message self destructs in 1 minute",
                            user.getAvatarUrl());
            var any = false;

            // find all matching times
            while (matcher.find()) {
                var time          = matchTime(matcher);
                var zonedTime     = ZonedDateTime.of(LocalDate.now(), time, authorZone);
                var convertedTime = zonedTime.withZoneSameInstant(targetZone);

                embed.addField("%s mentioned the time `%s` (`%s`)".formatted(author.getEffectiveName(),
                                matcher.group(0),
                                FORMATTER.format(zonedTime)),
                        "That would be `%s` in your time zone".formatted(FORMATTER.format(convertedTime)),
                        false);
                any = true;
            }

            // fallback if no time was found
            if (!any) embed.addField("No timestamp was found",
                    "Sorry, I could not find any timestamp to convert",
                    false);

            // respond
            action = action.flatMap($ -> channel.sendMessageEmbeds(embed.build())
                    .delay(1, TimeUnit.MINUTES)
                    .flatMap(Message::delete));
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Could not scan message for time mention", t);
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
