package com.ampznetwork.herobrine.feature.template.model.decl.func;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Declaration;
import lombok.Value;
import org.comroid.api.data.seri.StringSerializable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

@Value
public class Function implements Declaration {
    String      name;
    Parameter[] parameters;
    Declaration inner;

    @Override
    public String toSerializedString() {
        return "function %s(%s) %#s".formatted(name,
                Arrays.stream(parameters).map(StringSerializable::toSerializedString).collect(Collectors.joining(",")),
                inner);
    }

    @Override
    public void execute(TemplateContext context) {
        var arguments = context.getArguments();
        var args      = new HashMap<String, Object>();

        for (var i = 0; i < parameters.length && i < arguments.length; i++) {
            var value = arguments[i].evaluate(context);
            args.put(parameters[i].getName(), value);
        }

        context.innerContext(ctx -> {
            ctx.getVariables().putAll(args);
            inner.execute(ctx);
        });
    }
}
