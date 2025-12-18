package com.ampznetwork.herobrine.analyzer;

import com.ampznetwork.herobrine.haste.HasteInteractionSource;
import com.ampznetwork.herobrine.haste.HasteService;
import com.ampznetwork.herobrine.model.logs.ExceptionEntry;
import com.ampznetwork.herobrine.model.logs.LogComponent;
import com.ampznetwork.herobrine.model.logs.LogEntry;
import com.ampznetwork.herobrine.model.logs.PlaintextLogEntry;
import com.ampznetwork.herobrine.model.logs.StackTraceElementEntry;
import com.ampznetwork.herobrine.model.logs.ToplevelLogComponent;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.Builder;
import org.comroid.api.func.util.Streams;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jetbrains.annotations.NotNull;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
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
    @Description("Analyze an uploaded log file")
    public AnalysisResults analyze(@Command.Arg @Description("The haste ID of the log file") String id) {
        return results(id);
    }

    @SneakyThrows
    @ResponseBody
    @GetMapping("/results")
    public AnalysisResults results(@PathVariable("id") String id) {
        var entries = Objects.requireNonNull(hasteService.get(id).getBody(), "paste body")
                .lines()
                .flatMap(line -> Arrays.stream(LogLineAdapter.values())
                        .filter(lla -> lla.test(line))
                        .map(lla -> lla.apply(line)))
                .collect(new Collector<Builder<? extends LogComponent>, Stack<Builder<? extends ToplevelLogComponent>>, List<ToplevelLogComponent>>() {
                    @Override
                    public Supplier<Stack<Builder<? extends ToplevelLogComponent>>> supplier() {
                        return Stack::new;
                    }

                    @Override
                    public BiConsumer<Stack<Builder<? extends ToplevelLogComponent>>, Builder<? extends LogComponent>> accumulator() {
                        return (ls, c) -> {
                            switch (c) {
                                case LogEntry.Builder msg:
                                    ls.add(msg);
                                    break;
                                case ExceptionEntry.Builder ex:
                                    if (ls.isEmpty()) break;
                                    ls.reversed()
                                            .stream()
                                            .flatMap(Streams.cast(LogEntry.Builder.class))
                                            .findFirst()
                                            .ifPresent(head -> head.setException(ex));
                                    break;
                                case StackTraceElementEntry.Builder ste:
                                    if (ls.isEmpty()) break;
                                    ls.reversed()
                                            .stream()
                                            .flatMap(Streams.cast(LogEntry.Builder.class))
                                            .findFirst()
                                            .map(LogEntry.Builder::getException)
                                            .ifPresent(ex -> ex.addStackTrace(ste));
                                    break;
                                case PlaintextLogEntry.Builder plain:
                                    ls.add(plain);
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + c);
                            }
                        };
                    }

                    @Override
                    public BinaryOperator<Stack<Builder<? extends ToplevelLogComponent>>> combiner() {
                        return (l, r) -> {
                            l.addAll(r);
                            return l;
                        };
                    }

                    @Override
                    public Function<Stack<Builder<? extends ToplevelLogComponent>>, List<ToplevelLogComponent>> finisher() {
                        return ls -> ls.stream().map(Builder::build)
                                //.sorted(Comparator.comparing(e -> LocalDateTime.from(e.getDatetime())))
                                .map(Polyfill::<ToplevelLogComponent>uncheckedCast).toList();
                    }

                    @Override
                    public Set<Characteristics> characteristics() {
                        return Set.of();
                    }
                });

        var unparsed = entries.stream()
                .flatMap(Streams.cast(PlaintextLogEntry.Builder.class))
                .map(PlaintextLogEntry.Builder::getText)
                .toList();
        if (!unparsed.isEmpty()) log.fine("Unparseable log entries:\n\t" + String.join("\n\t", unparsed));

        return new AnalysisResults(id, entries);
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
                MinecraftLogAnalyzer.EMOJI.getFormatted() + " Analyze Logs")).filter(button -> id.contains("log"));
    }
}
