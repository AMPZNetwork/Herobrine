package com.ampznetwork.herobrine.feature.template.types;

import lombok.Value;
import org.comroid.api.Polyfill;

import java.util.Map;

@Value
public class TemplateObjectInstance {
    Type                      type;
    Map<CharSequence, Object> properties;

    public void validateType(Type type) {
        if (this.type.equals(type)) return;

        throw new RuntimeException("Instance of %s has wrong type; expected %s".formatted(this.type, type));
    }

    public <T> T get(CharSequence propertyName) {
        var property = type.getProperty(propertyName.toString()).orElseThrow();
        var value    = properties.getOrDefault(propertyName, property.defaultValue());

        return Polyfill.uncheckedCast(value);
    }
}
