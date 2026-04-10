package com.ampznetwork.herobrine.component.user.permission.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.comroid.api.func.WrappedFormattable;
import org.comroid.api.text.WrappedCharSequence;
import org.jspecify.annotations.NonNull;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum HerobrinePermission implements WrappedFormattable, WrappedCharSequence {
    Superadmin("herobrine.superadmin"), Gameadmin("herobrine.games");

    String primaryName;

    @Override
    public String getAlternateName() {
        return name();
    }

    @Override
    public @NonNull String toString() {
        return primaryName;
    }
}
