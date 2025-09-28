package com.ampznetwork.herobrine.analyzer;

import com.ampznetwork.herobrine.haste.HasteInteractionSource;
import com.ampznetwork.herobrine.haste.HasteService;
import com.ampznetwork.herobrine.model.logs.ExceptionEntry;
import com.ampznetwork.herobrine.model.logs.LogEntry;
import com.ampznetwork.herobrine.model.logs.StackTraceElementEntry;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

@Log
@Component
@Controller
@RequestMapping("/log/{id}")
public class MinecraftLogAnalyzer extends ListenerAdapter implements HasteInteractionSource {
    public static final Emoji  EMOJI     = Emoji.fromUnicode("\uD83D\uDD0D"); // 🔍
    public static final String EVENT_KEY = "analyze-";

    @Autowired HasteService hasteService;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getCustomId().startsWith(EVENT_KEY)) return;
        var id = event.getCustomId().substring(EVENT_KEY.length());
        event.deferReply().flatMap(hook -> {
            var results = MinecraftLogAnalyzer.this.analyze(id);
            return hook.sendMessageEmbeds(results.toEmbed().build());
        }).queue();
    }

    @Command
    public AnalysisResults analyze(@Command.Arg String id) {
        return results(id);
    }

    @SneakyThrows
    @ResponseBody
    @GetMapping("/results")
    public AnalysisResults results(@PathVariable("id") String id) {
        var lines = Objects.requireNonNull(hasteService.get(id).getBody(), "paste body")
                .lines()
                .toList()
                .listIterator();
        var entries = new ArrayList<LogEntry>();

        while (lines.hasNext()) {
            var line = lines.next();
            var m0   = LogEntry.PATTERN.matcher(line);

            if (!m0.matches()) continue;
            var log = LogEntry.builder();

            log.datetime(LogEntry.DATETIME.parse(m0.group("datetime")));
            log.threadName(m0.group("thread"));
            log.logLevel(m0.group("level"));
            log.loggerName(m0.group("logger"));
            log.componentName(m0.group("component"));
            log.message(m0.group("message"));

            var exception = readException(lines);
            if (exception != null) log.exception(exception.build());

            entries.add(log.build());
        }
        entries.sort(Comparator.comparing(e -> LocalDateTime.from(e.getDatetime())));

        return new AnalysisResults(id, Collections.unmodifiableList(entries));
    }

    private @Nullable ExceptionEntry.Builder readException(ListIterator<String> lines) {
        if (lines.hasNext()) {
            var line = lines.next();
            var m1   = ExceptionEntry.PATTERN.matcher(line);
            if (!m1.matches()) return null;
            var exception = ExceptionEntry.builder();

            exception.className(m1.group("exception"));
            exception.message(m1.group("message"));

            while (lines.hasNext()) {
                line = peek(lines);
                if (LogEntry.PATTERN.matcher(line).matches() || line.matches("\\s*\\.{3} \\d+ more")) break;
                if (line.trim().startsWith("Caused by")) {
                    var cause = readException(lines);
                    if (cause != null) {
                        lines.next();
                        exception.causedBy(cause.build());
                        continue;
                    }
                }

                Matcher m2;
                while (lines.hasNext() && (m2 = StackTraceElementEntry.PATTERN.matcher(line = peek(lines))).matches()) {
                    lines.next();

                    var element = StackTraceElementEntry.builder();

                    element.className(m2.group("class"));
                    element.method(m2.group("method"));
                    element.sourceFile(m2.group("file"));
                    var lineNo = m2.group("line");
                    element.line(lineNo != null && lineNo.matches("\\d+") ? Integer.parseInt(lineNo) : null);

                    exception.stackTrace(element.build());
                }
            }

            return exception;
        } else return null;
    }

    private String peek(ListIterator<String> lines) {
        var line = lines.next();
        lines.previous();
        return line;
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    @Override
    public Stream<ActionRowChildComponent> createHasteInteraction(String id) {
        return Stream.<ActionRowChildComponent>of(Button.secondary(MinecraftLogAnalyzer.EVENT_KEY + id,
                        MinecraftLogAnalyzer.EMOJI.getFormatted() + " Analyze Logs"))
                .filter(button -> id.contains("log"));
    }
}
