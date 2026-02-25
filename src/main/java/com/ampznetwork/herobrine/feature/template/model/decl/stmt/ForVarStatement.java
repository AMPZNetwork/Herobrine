package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.TypeUtil;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Declaration;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Value;

@Value
public class ForVarStatement implements Statement {
    Declaration initialize;
    Expression  condition;
    Declaration accumulate;
    Declaration inner;

    @Override
    public String toSerializedString() {
        return "for (%#s; %#s; %#s) %#s".formatted(initialize, condition, accumulate, inner);
    }

    @Override
    public void execute(TemplateContext context) {
        context.innerContext(ctx -> {
            initialize.execute(ctx);

            while (TypeUtil.toBoolean(condition.evaluate(ctx))) {
                inner.execute(ctx);
                accumulate.execute(ctx);
            }
        });
    }
}
