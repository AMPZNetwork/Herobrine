package com.ampznetwork.herobrine.util;

import lombok.AllArgsConstructor;
import lombok.Value;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public interface MessageDeliveryTarget {
    default RestAction<Message> send(String message) {
        return send(new MessageCreateBuilder().setContent(message).build());
    }

    RestAction<Message> send(MessageCreateData message);

    @Value
    class Channel implements MessageDeliveryTarget {
        MessageChannel channel;

        @Override
        public RestAction<Message> send(MessageCreateData message) {
            return channel.sendMessage(message);
        }
    }

    @Value
    class Reply implements MessageDeliveryTarget {
        Message target;

        @Override
        public RestAction<Message> send(MessageCreateData message) {
            return target.reply(message);
        }
    }

    @Value
    @AllArgsConstructor
    class ReplyCallback implements MessageDeliveryTarget {
        IReplyCallback callback;
        boolean        ephemeral;

        public ReplyCallback(IReplyCallback callback) {
            this(callback, true);
        }

        @Override
        public RestAction<Message> send(MessageCreateData message) {
            return callback.reply(message).setEphemeral(ephemeral).map(hook -> hook.getCallbackResponse().getMessage());
        }
    }

    @Value
    class Hook implements MessageDeliveryTarget {
        InteractionHook hook;

        @Override
        public WebhookMessageEditAction<Message> send(MessageCreateData message) {
            return hook.editOriginal(convertToEditData(message));
        }

        private MessageEditData convertToEditData(MessageCreateData message) {
            var edit = new MessageEditBuilder().setReplace(true);

            if (!message.getContent().isBlank()) edit = edit.setContent(message.getContent());
            if (!message.getEmbeds().isEmpty()) edit = edit.setEmbeds(message.getEmbeds());
            if (!message.getComponents().isEmpty()) edit = edit.setComponents(message.getComponents());
            if (!message.getAllowedMentions().isEmpty()) edit = edit.setAllowedMentions(message.getAllowedMentions());
            if (!message.getAttachments().isEmpty()) edit = edit.setAttachments(message.getAttachments());

            return edit.build();
        }
    }
}
