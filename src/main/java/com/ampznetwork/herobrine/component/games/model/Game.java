package com.ampznetwork.herobrine.component.games.model;

import com.ampznetwork.herobrine.repo.GameRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.comroid.annotations.Instance;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.api.text.Markdown;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Game implements Named, Described {
    @Id                String name;
    @Default @Nullable String description = null;
    @Default @Nullable Long   steamAppId  = null;

    public MessageEmbed.Field toField() {
        return new MessageEmbed.Field(name, description == null ? Markdown.Italic.apply("No description") : description, false);
    }

    public enum All implements Completion.Provider {
        @Instance INSTANCE;

        @Override
        public Stream<Completion.Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue) {
            return context.children(Guild.class)
                    .flatMap(guild -> bean(GameRepository.class).findAllByGuildId(guild.getIdLong()).stream())
                    .map(game -> new Completion.Option(game.name, game.description));
        }
    }
}
