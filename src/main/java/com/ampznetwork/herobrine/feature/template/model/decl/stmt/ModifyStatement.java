package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import com.ampznetwork.herobrine.feature.template.model.op.Operator;
import lombok.Value;
import org.comroid.api.Polyfill;

import java.util.Collection;

@Value
public class ModifyStatement implements Statement {
    Reference  reference;
    Operator   operator;
    Expression expression;

    @Override
    public String toSerializedString() {
        return "%#s %#s= %#s;".formatted(reference, operator, expression);
    }

    @Override
    public void execute(TemplateContext context) {
        var x = reference.evaluate(context);
        var y = expression.evaluate(context);

        if (operator == Operator.Plus && x instanceof Collection<?> col) {
            col.add(Polyfill.uncheckedCast(y));
        } else context.getVariables().put(reference, operator.apply(x, y));
    }
}
