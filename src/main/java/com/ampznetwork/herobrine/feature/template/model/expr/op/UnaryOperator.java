package com.ampznetwork.herobrine.feature.template.model.expr.op;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import com.ampznetwork.herobrine.feature.template.model.op.Operator;
import lombok.Value;

@Value
public class UnaryOperator implements Expression {
    Operator   operator;
    Expression x;

    @Override
    public String toSerializedString() {
        return "%#s%#s".formatted(operator, x);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        return operator.apply(x.evaluate(context), null);
    }
}
