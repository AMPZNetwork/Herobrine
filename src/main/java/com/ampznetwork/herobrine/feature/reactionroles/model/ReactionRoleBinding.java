package com.ampznetwork.herobrine.feature.reactionroles.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.interaction.annotation.Completion;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRoleBinding implements Named, Described {
    String emoji;
    String name;
    String description;
    long   roleId;

    public MessageEmbed.Field toField() {
        return new MessageEmbed.Field(emoji + " - " + name, description, false);
    }

    public Completion.Option toCompletionOption() {
        return new Completion.Option(name, emoji + ' ' + name + " - " + description);
    }
}
