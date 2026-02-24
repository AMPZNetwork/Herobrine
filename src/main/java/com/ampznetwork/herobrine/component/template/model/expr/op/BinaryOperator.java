package com.ampznetwork.herobrine.component.template.model.expr.op;

import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.model.expr.Expression;
import com.ampznetwork.herobrine.component.template.model.op.Operator;
import lombok.Value;

@Value
public class BinaryOperator implements Expression {
    Operator   operator;
    Expression left, right;

    @Override
    public String toSerializedString() {
        return "%#s %#s %#s".formatted(left, operator, right);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return operator.apply(left.evaluate(context), right.evaluate(context));
    }
}
