package com.ampznetwork.herobrine.component.permission.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.Instance;
import org.comroid.api.func.WrappedFormattable;
import org.comroid.api.text.WrappedCharSequence;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.stream.Stream;

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

    public enum AutoFill implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return Arrays.stream(HerobrinePermission.values()).map(HerobrinePermission::toString);
        }
    }
}
