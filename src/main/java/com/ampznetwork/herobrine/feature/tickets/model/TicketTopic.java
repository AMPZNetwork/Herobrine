package com.ampznetwork.herobrine.feature.tickets.model;

import com.ampznetwork.herobrine.repo.TicketTopicRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.entities.Guild;
import org.comroid.annotations.Instance;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@IdClass(TicketTopic.Key.class)
public class TicketTopic {
    @Id long   guildId;
    @Id String name;
    String description;
    long   handlerRoleId;

    public SelectOption toSelectOption() {
        return SelectOption.of(name, name).withDescription(description == null ? null : description.split("\r?\n")[0]);
    }

    public enum AutoFill implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return usage.fromContext(Guild.class)
                    .flatMap(guild -> bean(TicketTopicRepository.class).findAllByGuildId(guild.getIdLong()).stream())
                    .map(TicketTopic::getName);
        }
    }

    public record Key(long guildId, String name) {}
}
