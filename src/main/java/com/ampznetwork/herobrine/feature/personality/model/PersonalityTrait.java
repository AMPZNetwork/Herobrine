package com.ampznetwork.herobrine.feature.personality.model;

import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.function.BooleanSupplier;

@Data
@Entity
@IdClass(PersonalityTrait.Key.class)
public class PersonalityTrait implements BooleanSupplier {
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

    public record Key(long guildId, String name) {}
}
