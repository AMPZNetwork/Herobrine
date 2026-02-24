package com.ampznetwork.herobrine.component.template.model.expr.lit;

import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.model.expr.Expression;
import lombok.Value;

@Value
public class LiteralString implements Expression {
    String value;

    @Override
    public String toSerializedString() {
        return "\"%s\"".formatted(value);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return value;
    }
}
