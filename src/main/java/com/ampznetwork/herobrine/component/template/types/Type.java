package com.ampznetwork.herobrine.component.template.types;

import com.ampznetwork.herobrine.component.template.model.CodeComponent;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Value
@EqualsAndHashCode(of = { "name" })
public class Type implements CodeComponent {
    private static final Map<String, Type> $cache = new ConcurrentHashMap<>();
    public static final  Map<String, Type> CACHE  = Collections.unmodifiableMap($cache);

    public static final Type EMBED_AUTHOR = new Type("author",
            new Property<>("name", String.class),
            new Property<>("url", String.class),
            new Property<>("iconUrl", String.class));
    public static final Type EMBED_FOOTER = new Type("footer",
            new Property<>("text", String.class),
            new Property<>("iconUrl", String.class));
    public static final Type EMBED_FIELD  = new Type("field",
            new Property<>("title", String.class),
            new Property<>("content", String.class),
            new Property<>("inline", Boolean.class, false));

    public static final List<Type> EMBED_COMPONENTS = List.of(EMBED_AUTHOR, EMBED_FOOTER, EMBED_FIELD);

    public static Type forName(String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        throw new IllegalStateException("Unknown type: " + name);
    }

    String        name;
    Property<?>[] properties;

    private Type(String name, Property<?>... properties) {
        this.name       = name;
        this.properties = properties;

        $cache.put(name, this);
    }

    public <T> Optional<Property<T>> getProperty(String name) {
        return Arrays.stream(properties)
                .filter(property -> property.name.equals(name))
                .findAny()
                .map(Polyfill::uncheckedCast);
    }

    @Override
    public String toSerializedString() {
        return name;
    }

    public record Property<T>(String name, Class<T> type, @Nullable T defaultValue) implements CharSequence {
        public Property(String name, Class<T> type) {
            this(name, type, null);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CharSequence other && other.toString().equals(name);
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public @NonNull String toString() {
            return name;
        }

        @Override
        public int length() {
            return toString().length();
        }

        @Override
        public char charAt(int index) {
            return toString().charAt(index);
        }

        @Override
        public @NonNull CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }
    }
}
