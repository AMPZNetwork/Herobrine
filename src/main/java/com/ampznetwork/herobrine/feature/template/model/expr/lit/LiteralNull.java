package com.ampznetwork.herobrine.feature.template.model.expr.lit;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Value;

@Value
public class LiteralNull implements Expression {
    @Override
    public String toSerializedString() {
        return "null";
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return null;
    }
}
