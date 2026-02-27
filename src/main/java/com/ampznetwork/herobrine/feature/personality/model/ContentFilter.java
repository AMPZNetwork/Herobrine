package com.ampznetwork.herobrine.feature.personality.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.util.function.BiPredicate;
import java.util.regex.Pattern;

@Data
@Embeddable
public class ContentFilter {
    StringMatching matching;
    String         pattern;

    public boolean matches(CharSequence text) {
        return matching.test(text.toString(), pattern);
    }

    public enum StringMatching implements BiPredicate<String, String> {
        Equals {
            @Override
            public boolean test(String text, String pattern) {
                return text.equals(pattern);
            }
        }, Contains {
            @Override
            public boolean test(String text, String pattern) {
                return text.contains(pattern);
            }
        }, RegExpFullMatch {
            @Override
            public boolean test(String text, String pattern) {
                return text.matches(pattern);
            }
        }, RegExpPartialMatch {
            @Override
            public boolean test(String text, String pattern) {
                return Pattern.compile(pattern).matcher(text).find();
            }
        };

        @Override
        public abstract boolean test(String text, String pattern);
    }
}
