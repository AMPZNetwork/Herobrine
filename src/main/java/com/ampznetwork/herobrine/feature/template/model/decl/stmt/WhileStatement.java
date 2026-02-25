package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.TypeUtil;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Declaration;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Value;

@Value
public class WhileStatement implements Statement {
    Expression  condition;
    Declaration inner;

    @Override
    public String toSerializedString() {
        return "while (%#s) %#s".formatted(condition, inner);
    }

    @Override
    public void execute(TemplateContext context) {
        context.innerContext(ctx -> {
            while (TypeUtil.toBoolean(condition.evaluate(ctx))) inner.execute(ctx);
        });
    }
}
