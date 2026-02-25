package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Declaration;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Value;

import java.util.Iterator;
import java.util.List;

@Value
public class ForEachStatement implements Statement {
    Reference   variable;
    Expression  iterable;
    Declaration inner;

    @Override
    public String toSerializedString() {
        return "for (%#s in %#s) %#s".formatted(variable, iterable, inner);
    }

    @Override
    public void execute(TemplateContext context) {
        var         iter = iterable.evaluate(context);
        Iterator<?> iterator;

        if (iter.getClass().isArray()) iterator = List.of((Object[]) iter).iterator();
        else if (iter instanceof Iterable<?> it) iterator = it.iterator();
        else throw new IllegalStateException("Object is not iterable: " + iter);

        context.innerContext(ctx -> {
            while (iterator.hasNext()) {
                var each = iterator.next();
                ctx.getVariables().put(variable, each);
                inner.execute(ctx);
            }
        });
    }
}
