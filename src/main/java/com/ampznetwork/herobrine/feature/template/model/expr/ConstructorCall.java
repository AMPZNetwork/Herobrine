package com.ampznetwork.herobrine.feature.template.model.expr;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.types.TemplateObjectInstance;
import com.ampznetwork.herobrine.feature.template.types.Type;
import lombok.Value;
import org.comroid.api.map.StringKeyMap;

import java.util.Map;
import java.util.stream.Collectors;

@Value
public class ConstructorCall implements Expression {
    Type                                    type;
    Map<? extends CharSequence, Expression> properties;

    @Override
    public String toSerializedString() {
        return "new %#s { %s }".formatted(type,
                properties.entrySet()
                        .stream()
                        .map(e -> "%s = %#s".formatted(e.getKey(), e.getValue()))
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public TemplateObjectInstance evaluate(TemplateContext context) {
        var props = new StringKeyMap<>();

        for (var entry : properties.entrySet()) {
            var value = entry.getValue().evaluate(context);
            props.put(entry.getKey(), value);
        }

        return new TemplateObjectInstance(type, props);
    }
}
