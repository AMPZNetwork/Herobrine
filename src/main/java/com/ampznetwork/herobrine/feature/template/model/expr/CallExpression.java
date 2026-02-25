package com.ampznetwork.herobrine.feature.template.model.expr;

import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import lombok.Value;

@Value
public class CallExpression implements Expression {
    Reference reference;

    @Override
    public String toSerializedString() {
        return reference.toSerializedString();
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return reference.evaluate(context);
    }
}
