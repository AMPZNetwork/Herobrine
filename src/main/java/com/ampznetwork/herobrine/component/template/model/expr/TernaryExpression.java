package com.ampznetwork.herobrine.component.template.model.expr;

import com.ampznetwork.herobrine.component.template.TypeUtil;
import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import lombok.Value;

@Value
public class TernaryExpression implements Expression {
    Expression condition, onTrue, onFalse;

    @Override
    public String toSerializedString() {
        return "%#s ? %#s : %#s".formatted(condition, onTrue, onFalse);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return TypeUtil.toBoolean(condition.evaluate(context)) ? onTrue.evaluate(context) : onFalse.evaluate(context);
    }
}
