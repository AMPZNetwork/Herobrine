package com.ampznetwork.herobrine.feature.games.model;

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
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;
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

    public enum All implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return usage.fromContext(Guild.class).flatMap(guild -> bean(GameRepository.class).findAllByGuildId(guild.getIdLong()).stream()).map(Game::getName);
        }
    }
}
