package com.ampznetwork.herobrine.component.template.model.decl;

import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import lombok.Value;
import org.comroid.api.data.seri.StringSerializable;

import java.util.Arrays;
import java.util.stream.Collectors;

@Value
public class CodeBlock implements Declaration {
    Declaration[] declarations;

    @Override
    public String toSerializedString() {
        return Arrays.stream(declarations)
                .map(StringSerializable::toSerializedString)
                .collect(Collectors.joining("\n\t", "{\n\t", "\n}"));
    }

    @Override
    public void execute(TemplateContext context) {
        for (var declaration : declarations)
            declaration.execute(context);
    }
}
