package com.ampznetwork.herobrine.util;

import net.dv8tion.jda.api.EmbedBuilder;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Markdown;
import org.comroid.api.text.StringUtil;
import org.jspecify.annotations.Nullable;

public class EmbedTemplate {
    public static EmbedBuilder success(CharSequence message) {
        return new EmbedBuilder().setTitle("%s Success".formatted(Constant.EMOJI_SUCCESS)).setDescription(message).setColor(Constant.COLOR_SUCCESS);
    }

    public static EmbedBuilder warning(CharSequence message) {
        return new EmbedBuilder().setTitle("%s Warning".formatted(Constant.EMOJI_WARNING)).setDescription(message).setColor(Constant.COLOR_WARNING);
    }

    public static EmbedBuilder error(CharSequence message, @Nullable Throwable t) {
        return new EmbedBuilder().setTitle("%s Error".formatted(Constant.EMOJI_ERROR))
                .setDescription(message + (t == null ? "" : '\n' + Markdown.CodeBlock.apply(StringUtil.maxLength(StackTraceUtils.toString(t), 950))))
                .setColor(Constant.COLOR_ERROR);
    }

    private EmbedTemplate() {
        throw new UnsupportedOperationException();
    }
}
