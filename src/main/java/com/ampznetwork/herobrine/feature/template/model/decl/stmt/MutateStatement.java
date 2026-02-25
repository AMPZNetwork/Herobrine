package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import com.ampznetwork.herobrine.feature.template.model.op.Operator;
import lombok.Value;

@Value
public class MutateStatement implements Statement {
    Reference  reference;
    Operator   operator;
    Expression expression;

    @Override
    public String toSerializedString() {
        return "%#s %#s= %#s;".formatted(reference, operator, expression);
    }

    @Override
    public void execute(TemplateContext context) {
        var value = context.getVariables().get(reference);
        var other = expression.evaluate(context);
        value = operator.apply(value, other);
        context.getVariables().put(reference, value);
    }
}
