package com.ampznetwork.herobrine.feature.template.model.expr.lit;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Value;

@Value
public class LiteralNumber implements Expression {
    double value;

    @Override
    public String toSerializedString() {
        return "%1.1f".formatted(value);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return value;
    }
}
