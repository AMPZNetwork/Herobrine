package com.ampznetwork.herobrine.component.template;

public final class TypeUtil {
    public static boolean toBoolean(Object object) {
        return switch (object) {
            case Boolean b -> b;
            case Number n -> n.doubleValue() != 0;
            default -> object != null;
        };
    }

    private TypeUtil() {
        throw new UnsupportedOperationException();
    }
}
