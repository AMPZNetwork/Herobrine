package com.ampznetwork.herobrine.component.template.context;

import com.ampznetwork.herobrine.component.template.types.TemplateObjectInstance;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.BiConsumer;

import static com.ampznetwork.herobrine.component.template.types.Type.*;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum EmbedComponentReference implements BiConsumer<EmbedBuilder, Object> {
    url {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            embed.setUrl(it == null ? null : String.valueOf(it));
        }
    }, title {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            embed.setTitle(it == null ? null : String.valueOf(it));
        }
    }, description {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            embed.setDescription(it == null ? null : String.valueOf(it));
        }
    }, timestamp {
        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm");

        @Override
        public void accept(EmbedBuilder embed, Object it) {
            switch (it) {
                case TemporalAccessor temporal -> embed.setTimestamp(temporal);
                case ISnowflake snowflake -> embed.setTimestamp(snowflake.getTimeCreated());
                case CharSequence seq -> embed.setTimestamp(FORMAT.parse(seq));
                case Number unixTimestamp -> embed.setTimestamp(Instant.ofEpochMilli(unixTimestamp.longValue()));
                case null -> embed.setTimestamp(null);
                default -> throw invalidArgument(it, "timestamp");
            }
        }
    }, color {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            switch (it) {
                case Member member -> embed.setColor(member.getColors().getPrimary());
                case Color awtColor -> embed.setColor(awtColor);
                case Number rgb -> embed.setColor(rgb.intValue());
                case CharSequence hex -> embed.setColor(Integer.parseInt(hex.toString(), 16));
                case null -> embed.setColor(null);
                default -> throw invalidArgument(it, "color");
            }
        }
    }, thumbnail {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            embed.setThumbnail(it == null ? null : String.valueOf(it));
        }
    }, author {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            switch (it) {
                case Member member -> embed.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
                case User user -> embed.setAuthor(user.getGlobalName(), null, user.getAvatarUrl());
                case TemplateObjectInstance toi -> {
                    toi.validateType(EMBED_AUTHOR);

                    String name    = toi.get("name");
                    String url     = toi.get("url");
                    String iconUrl = toi.get("iconUrl");

                    embed.setAuthor(name, url, iconUrl);
                }
                case CharSequence chars -> embed.setAuthor(chars.toString());
                default -> throw invalidArgument(it, "author");
            }
        }
    }, footer {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            switch (it) {
                case TemplateObjectInstance toi -> {
                    toi.validateType(EMBED_FOOTER);

                    String text    = toi.get("text");
                    String iconUrl = toi.get("iconUrl");

                    if (iconUrl != null) embed.setFooter(text, iconUrl);
                    else embed.setFooter(text);
                }
                case CharSequence chars -> embed.setFooter(chars.toString());
                default -> throw invalidArgument(it, "footer");
            }
        }
    }, image {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            embed.setImage(it == null ? null : String.valueOf(it));
        }
    }, field {
        @Override
        public void accept(EmbedBuilder embed, Object it) {
            if (!(it instanceof TemplateObjectInstance toi)) throw invalidArgument(it, "field");

            String title   = toi.get("title");
            String content = toi.get("content");
            var    inline  = Boolean.TRUE.equals(toi.get("inline"));

            embed.addField(title, content, inline);
        }
    };

    protected IllegalArgumentException invalidArgument(Object actual, String expectedDetail) {
        return new IllegalArgumentException("Invalid argument `%s`, expected some kind of %s");
    }
}
