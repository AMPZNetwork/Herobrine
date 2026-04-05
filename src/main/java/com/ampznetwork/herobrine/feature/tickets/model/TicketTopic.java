package com.ampznetwork.herobrine.feature.tickets.model;

import com.ampznetwork.herobrine.repo.TicketTopicRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import org.comroid.annotations.Instance;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;

import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@IdClass(TicketTopic.Key.class)
public class TicketTopic implements Named, Described {
    @Id long   guildId;
    @Id String name;
    String description;
    long   handlerRoleId;

    public SelectOption toSelectOption() {
        return SelectOption.of(name, name).withDescription(description == null ? null : description.split("\r?\n")[0]);
    }

    public Container toInfoContainer() {
        return Container.of(TextDisplay.of("### " + name),
                TextDisplay.of(description),
                TextDisplay.of("-# Please make sure to provide all relevant information"));
    }

    public enum AutoFill implements Completion.Provider {
        @Instance INSTANCE;

        @Override
        public Stream<Completion.Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue) {
            return context.children(Guild.class)
                    .flatMap(guild -> bean(TicketTopicRepository.class).findAllByGuildId(guild.getIdLong()).stream())
                    .map(Completion.Option::new);
        }
    }

    public record Key(long guildId, String name) {}
}
