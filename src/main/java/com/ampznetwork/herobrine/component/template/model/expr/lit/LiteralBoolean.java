package com.ampznetwork.herobrine.component.template.model.expr.lit;

import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.model.expr.Expression;
import lombok.Value;

@Value
public class LiteralBoolean implements Expression {
    boolean value;

    @Override
    public String toSerializedString() {
        return value ? "true" : "false";
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return value;
    }
}
