package com.ampznetwork.herobrine.feature.errorlog.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogPreferences {
    @Id long guildId;
    long channelId;

    public EmbedBuilder toEmbed() {
        var channel = bean(JDA.class).getTextChannelById(channelId);

        return new EmbedBuilder().setTitle("Error Log Configuration")
                .addField("Channel", channel.getAsMention(), false);
    }
}
