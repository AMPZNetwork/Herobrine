package com.ampznetwork.herobrine.util;

import lombok.Value;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.awt.*;

@Value
public class Constant {
    public static final Emoji EMOJI_DELETE        = Emoji.fromUnicode("🗑️");
    public static final Emoji EMOJI_ERROR   = Emoji.fromUnicode("❌");
    public static final Emoji EMOJI_WARNING       = Emoji.fromUnicode("⚠️");
    public static final Emoji EMOJI_SUCCESS = Emoji.fromUnicode("✅");
    public static final Emoji EMOJI_EVAL_TEMPLATE = Emoji.fromUnicode("💬");

    public static final Color COLOR_ERROR   = new Color(0xda2d43);
    public static final Color COLOR_WARNING = new Color(0xffcc4d);
    public static final Color COLOR_SUCCESS = new Color(0x77b255);

    public static final String STRING_EDIT          = "Edit...";
    public static final String STRING_APPLY         = "Apply";
    public static final String STRING_SELF_DESTRUCT = "This message will self-destruct in %d seconds";

    private Constant() {
        throw new UnsupportedOperationException();
    }
}
