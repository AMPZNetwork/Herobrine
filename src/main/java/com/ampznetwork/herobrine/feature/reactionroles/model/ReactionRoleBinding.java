package com.ampznetwork.herobrine.feature.reactionroles.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRoleBinding {
    String emoji;
    String name;
    String description;
    long   roleId;

    public MessageEmbed.Field toField() {
        return new MessageEmbed.Field(emoji + " - " + name, description, false);
    }
}
