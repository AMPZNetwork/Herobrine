package com.ampznetwork.herobrine.analyzer;

import com.ampznetwork.herobrine.haste.HasteService;
import com.ampznetwork.herobrine.model.logs.LogEntry;
import lombok.Value;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class AnalysisResults {
    String         id;
    List<LogEntry> entries;

    public Map<String, List<LogEntry>> getEntriesByLevel() {
        return entries.stream().collect(Collectors.groupingBy(LogEntry::getLogLevel));
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
