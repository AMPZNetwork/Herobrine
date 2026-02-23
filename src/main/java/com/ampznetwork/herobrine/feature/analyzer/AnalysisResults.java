package com.ampznetwork.herobrine.feature.analyzer;

import com.ampznetwork.herobrine.feature.analyzer.model.LogEntry;
import com.ampznetwork.herobrine.feature.analyzer.model.ToplevelLogComponent;
import com.ampznetwork.herobrine.feature.haste.HasteService;
import lombok.Value;
import net.dv8tion.jda.api.EmbedBuilder;
import org.comroid.api.func.util.Streams;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class AnalysisResults {
    String                     id;
    List<ToplevelLogComponent> entries;

    public Map<String, List<LogEntry>> getEntriesByLevel() {
        return entries.stream()
                .flatMap(Streams.cast(LogEntry.class))
                .collect(Collectors.groupingBy(LogEntry::getLogLevel));
    }

    public EmbedBuilder toEmbed() {
        var entriesByLevel = getEntriesByLevel();
        return new EmbedBuilder().setTitle("Minecraft Log Analyzer Results")
                .setUrl(HasteService.URL_PREFIX + id)
                .addField("%d Log entries".formatted(entries.size()),
                        entriesByLevel.entrySet()
                                .stream()
                                .map(e -> e.getValue().size() + " " + e.getKey())
                                .collect(Collectors.joining("\n- ", "- ", "")),
                        false);
    }
}
