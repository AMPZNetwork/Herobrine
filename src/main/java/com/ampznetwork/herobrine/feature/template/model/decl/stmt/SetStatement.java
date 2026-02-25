package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Value;

@Value
public class SetStatement implements Statement {
    Reference  reference;
    Expression expression;

    @Override
    public String toSerializedString() {
        return "%#s = %#s;".formatted(reference, expression);
    }

    @Override
    public void execute(TemplateContext context) {
        var value = expression.evaluate(context);
        context.getVariables().put(reference, value);
    }
}
