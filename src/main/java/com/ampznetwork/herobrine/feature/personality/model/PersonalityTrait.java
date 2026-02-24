package com.ampznetwork.herobrine.feature.personality.model;

import com.ampznetwork.herobrine.trigger.DiscordTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

@Data
@Entity
@IdClass(PersonalityTrait.Key.class)
public class PersonalityTrait {
    @Id                                                  long                                          guildId;
    @Id                                                  String                                        name;
    @Convert(converter = DiscordTrigger.Converter.class) DiscordTrigger<? extends GenericMessageEvent> discordTrigger;
    ContentFilter contentFilter;
    @Column(length = 8192) String templateScript;

    public record Key(long guildId, String name) {}
}
