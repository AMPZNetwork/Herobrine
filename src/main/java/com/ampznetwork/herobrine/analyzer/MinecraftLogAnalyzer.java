package com.ampznetwork.herobrine.analyzer;

import com.ampznetwork.herobrine.haste.HasteService;
import com.ampznetwork.herobrine.model.logs.ExceptionEntry;
import com.ampznetwork.herobrine.model.logs.LogEntry;
import com.ampznetwork.herobrine.model.logs.StackTraceElementEntry;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.regex.Matcher;

@Log
@Component
public class MinecraftLogAnalyzer extends ListenerAdapter {
    public static final Emoji  EMOJI     = Emoji.fromUnicode("\uD83D\uDD0D"); // 🔍
    public static final String EVENT_KEY = "analyze-";

    @Autowired HasteService hasteService;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getCustomId().startsWith(EVENT_KEY)) return;
        var id = event.getCustomId().substring(EVENT_KEY.length());
        event.deferReply().flatMap(hook -> {
            var result = MinecraftLogAnalyzer.this.analyze(id);
            return hook.sendMessage(String.valueOf(result));
        }).queue();
    }

    @Command
    @SneakyThrows
    public Object analyze(@Command.Arg String id) {
        var lines   = hasteService.get(id).lines().toList().listIterator();
        var entries = new ArrayList<LogEntry>();

        while (lines.hasNext()) {
            var line = lines.next();
            var m0   = LogEntry.PATTERN.matcher(line);

            if (!m0.matches()) continue;
            var log = LogEntry.builder();

            log.date(m0.group("date"));
            log.time(m0.group("time"));
            log.threadName(m0.group("thread"));
            log.logLevel(m0.group("level"));
            log.loggerName(m0.group("logger"));
            log.componentName(m0.group("component"));
            log.message(m0.group("message"));

            var exception = readException(lines);
            if (exception != null) log.exception(exception.build());

            entries.add(log.build());
        }

        return entries;
    }

    private @Nullable ExceptionEntry.Builder readException(ListIterator<String> lines) {
        if (lines.hasNext()) {
            var m1 = ExceptionEntry.PATTERN.matcher(lines.next());
            if (!m1.matches()) {
                lines.previous();
                return null;
            }
            var exception = ExceptionEntry.builder();

            exception.className(m1.group("exception"));
            exception.message(m1.group("message"));

            while (lines.hasNext()) {
                var cause = readException(lines);
                if (cause != null) {
                    exception.causedBy(cause.build());
                    continue;
                }

                Matcher m2;
                while (lines.hasNext() && (m2 = StackTraceElementEntry.PATTERN.matcher(lines.next())).matches()) {
                    if (!m2.matches()) {
                        lines.previous();
                        break;
                    }
                    var element = StackTraceElementEntry.builder();

                    element.className(m2.group("class"));
                    element.method(m2.group("method"));
                    element.sourceFile(m2.group("file"));
                    var line = m2.group("line");
                    element.line(line != null && line.matches("\\d+") ? Integer.parseInt(line) : null);

                    exception.stackTrace(element.build());
                }
                //lines.previous();
            }

            return exception;
        } else return null;
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
