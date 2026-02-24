package com.ampznetwork.herobrine.component.template.model.expr;

import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.model.CodeComponent;

public interface Expression extends CodeComponent {
    Object evaluate(TemplateContext context);
}
