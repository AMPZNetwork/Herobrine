package com.ampznetwork.herobrine.feature.embed;

import lombok.Value;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.Nullable;

@Value
public class EmbedEditorSession {
    public static EmbedBuilder placeholderEmbed() {
        return new EmbedBuilder().setDescription("_Placeholder Text_");
    }

    User                user;
    MessageChannelUnion channel;
    EmbedBuilder        embed;
    @Nullable Message message;

    public EmbedEditorSession(User user, MessageChannelUnion channel, MessageEmbed embed) {
        this(user, channel, embed, null);
    }

    public EmbedEditorSession(User user, MessageChannelUnion channel, MessageEmbed embed, @Nullable Message message) {
        this(user, channel, new EmbedBuilder(embed), message);
    }

    public EmbedEditorSession(User user, MessageChannelUnion channel, EmbedBuilder embed, @Nullable Message message) {
        this.user    = user;
        this.channel = channel;
        this.embed   = embed;
        this.message = message;
    }

    public RestAction<Message> applyEdits() {
        var messageEmbed = embed.build();

        return message == null
               ? channel.sendMessage(new MessageCreateBuilder().setEmbeds(messageEmbed).build())
               : message.editMessageEmbeds(messageEmbed);
    }

    public EmbedBuilder toEmbed() {
        return embed.isEmpty() ? placeholderEmbed() : embed;
    }
}
