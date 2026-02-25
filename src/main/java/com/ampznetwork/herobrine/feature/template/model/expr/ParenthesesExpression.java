package com.ampznetwork.herobrine.feature.template.model.expr;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import lombok.Value;

@Value
public class ParenthesesExpression implements Expression {
    Expression inner;

    @Override
    public String toSerializedString() {
        return "(%#s)".formatted(inner);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return inner.evaluate(context);
    }
}
