package com.ampznetwork.herobrine.feature.personality.model;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.components.selections.SelectOption;
import org.comroid.api.text.Capitalization;

import java.util.function.BiPredicate;
import java.util.regex.Pattern;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ContentFilter {
    StringMatching matching;
    String         pattern;

    public boolean matches(CharSequence text) {
        return matching.test(text.toString(), pattern);
    }

    @Override
    public String toString() {
        return matching.format.formatted(pattern);
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum StringMatching implements BiPredicate<String, String> {
        EQUALS("content is equal to `%s`") {
            @Override
            public boolean test(String text, String pattern) {
                return text.equals(pattern);
            }
        }, EQUALS_IGNORE_CASE("content is equal to `%s` (case-insensitive)") {
            @Override
            public boolean test(String text, String pattern) {
                return text.equalsIgnoreCase(pattern);
            }
        }, CONTAINS("content contains `%s`") {
            @Override
            public boolean test(String text, String pattern) {
                return text.contains(pattern);
            }
        }, CONTAINS_IGNORE_CASE("content contains `%s` (case-insensitive)") {
            @Override
            public boolean test(String text, String pattern) {
                return text.toLowerCase().contains(pattern.toLowerCase());
            }
        }, REG_EXP_FULL_MATCH("content matches regex pattern `%s`") {
            @Override
            public boolean test(String text, String pattern) {
                return text.matches(pattern);
            }
        }, REG_EXP_PARTIAL_MATCH("some of the content matches regex pattern `%s`") {
            @Override
            public boolean test(String text, String pattern) {
                return Pattern.compile(pattern).matcher(text).find();
            }
        };

        String       format;
        SelectOption option;

        StringMatching(String format) {
            this.format = format;
            this.option = SelectOption.of(Capitalization.CAPS_SNAKE_CASE.convert(Capitalization.Title_Case, name()),
                    name());
        }

        @Override
        public abstract boolean test(String text, String pattern);
    }
}
