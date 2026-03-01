package com.ampznetwork.herobrine.feature.personality.model;

import com.ampznetwork.herobrine.repo.PersonalityTraitRepo;
import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.comroid.annotations.Instance;
import org.comroid.api.attr.Named;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@IdClass(PersonalityTrait.Key.class)
public class PersonalityTrait implements Named, BooleanSupplier {
    @Id                                                  long                                          guildId;
    @Id                                                  String                                        name;
    @Convert(converter = DiscordTrigger.Converter.class) DiscordTrigger<? extends GenericMessageEvent> discordTrigger;
    ContentFilter contentFilter;
    RandomDetail randomDetail;
    @Column(length = 8192) String templateScript;

    @Override
    public boolean getAsBoolean() {
        return false;
    }

    public Key key() {
        return new Key(guildId, name);
    }

    public enum AutoFillTraitNamesByGuild implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return usage.fromContext(Guild.class)
                    .flatMap(guild -> bean(PersonalityTraitRepo.class).findAllByGuildId(guild.getIdLong()).stream())
                    .map(PersonalityTrait::getName);
        }
    }

    public record Key(long guildId, String name) {}
}
