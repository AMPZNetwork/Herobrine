package com.ampznetwork.herobrine.util;

import lombok.Value;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public interface ReplyCallbackWrapper {
    default RestAction<?> reply(String message) {
        return reply(new MessageCreateBuilder().setContent(message).build());
    }

    RestAction<?> reply(MessageCreateData message);

    @Value
    class Direct implements ReplyCallbackWrapper {
        IReplyCallback callback;
        boolean        ephemeral;

        public Direct(IReplyCallback callback) {
            this(callback, true);
        }

        public Direct(IReplyCallback callback, boolean ephemeral) {
            this.callback  = callback;
            this.ephemeral = ephemeral;
        }

        @Override
        public ReplyCallbackAction reply(MessageCreateData message) {
            return callback.reply(message).setEphemeral(ephemeral);
        }
    }

    @Value
    class Hook implements ReplyCallbackWrapper {
        InteractionHook hook;

        @Override
        public WebhookMessageEditAction<Message> reply(MessageCreateData message) {
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
