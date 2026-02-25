package com.ampznetwork.herobrine.feature.template.model.decl.stmt;

import com.ampznetwork.herobrine.feature.template.context.Reference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.decl.Statement;
import lombok.Value;

@Value
public class CallStatement implements Statement {
    Reference reference;

    @Override
    public String toSerializedString() {
        return reference.toSerializedString() + ';';
    }

    @Override
    public void execute(TemplateContext context) {
        reference.evaluate(context);
    }
}
