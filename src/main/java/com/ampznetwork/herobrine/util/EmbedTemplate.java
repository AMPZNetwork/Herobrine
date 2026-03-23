package com.ampznetwork.herobrine.util;

import net.dv8tion.jda.api.EmbedBuilder;

public class EmbedTemplate {
    public static EmbedBuilder success(CharSequence message) {
        return new EmbedBuilder()
                .setTitle("Success")
                .setDescription(message)
                .setColor(Constant.COLOR_SUCCESS);
    }

    private EmbedTemplate() {
        throw new UnsupportedOperationException();
    }
}
