package com.ampznetwork.herobrine.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class JdaUtil {
    private JdaUtil() {
        throw new UnsupportedOperationException();
    }

    public static RestAction<?> replySuccess(
            IReplyCallback callback
    ) {
        return replySuccess(callback, null);
    }

    public static RestAction<?> replySuccess(IReplyCallback callback, @Nullable Function<Message, ? extends RestAction<Message>> finalizer) {
        var action = callback.replyEmbeds(new EmbedBuilder().setTitle(Constant.EMOJI_SUCCESS.getFormatted() + " Success")
                .setColor(Constant.COLOR_SUCCESS)
                .setFooter(Constant.STRING_SELF_DESTRUCT.formatted(2))
                .build()).setEphemeral(true).map(hook -> hook.getCallbackResponse().getMessage());
        if (finalizer != null) action = action.flatMap(finalizer);
        return action.delay(2, TimeUnit.SECONDS).flatMap(Message::delete);
    }
}
