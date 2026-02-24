package com.ampznetwork.herobrine.component.template.model.expr;

import com.ampznetwork.herobrine.component.template.context.Reference;
import com.ampznetwork.herobrine.component.template.context.TemplateContext;
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
