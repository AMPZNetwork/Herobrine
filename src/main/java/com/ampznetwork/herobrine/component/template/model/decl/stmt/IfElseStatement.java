package com.ampznetwork.herobrine.component.template.model.decl.stmt;

import com.ampznetwork.herobrine.component.template.TypeUtil;
import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.model.decl.Declaration;
import com.ampznetwork.herobrine.component.template.model.decl.Statement;
import com.ampznetwork.herobrine.component.template.model.expr.Expression;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class IfElseStatement implements Statement {
    Expression  condition;
    Declaration onTrue;
    @Nullable Declaration onFalse;

    @Override
    public String toSerializedString() {
        return "if (%#s) %#s else %#s".formatted(condition, onTrue, onFalse);
    }

    @Override
    public void execute(TemplateContext context) {
        context.innerContext(ctx -> {
            if (TypeUtil.toBoolean(condition.evaluate(ctx))) onTrue.execute(ctx);
            else if (onFalse != null) onFalse.execute(ctx);
        });
    }
}
