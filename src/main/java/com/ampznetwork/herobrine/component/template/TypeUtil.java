package com.ampznetwork.herobrine.component.template;

public final class TypeUtil {
    public static boolean toBoolean(Object object) {
        return object != null && switch (object) {
            case Boolean b -> b;
            case Number n -> n.doubleValue() != 0;
            case CharSequence chars -> !chars.isEmpty();
            default -> true;
        };
    }

    private TypeUtil() {
        throw new UnsupportedOperationException();
    }
}
