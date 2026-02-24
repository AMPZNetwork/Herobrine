package com.ampznetwork.herobrine.util;

import lombok.Value;
import net.dv8tion.jda.api.entities.emoji.Emoji;

@Value
public class Constant {
    public static final Emoji EMOJI_DELETE        = Emoji.fromUnicode("🗑️");
    public static final Emoji EMOJI_WARNING       = Emoji.fromUnicode("⚠️");
    public static final Emoji EMOJI_EVAL_TEMPLATE = Emoji.fromUnicode("💬");

    private Constant() {
        throw new UnsupportedOperationException();
    }
}
