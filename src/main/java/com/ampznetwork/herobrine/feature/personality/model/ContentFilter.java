package com.ampznetwork.herobrine.feature.personality.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ContentFilter {
    StringMatching matching;
    String         pattern;

    public enum StringMatching {
        Equals, Contains, RegExp
    }
}
