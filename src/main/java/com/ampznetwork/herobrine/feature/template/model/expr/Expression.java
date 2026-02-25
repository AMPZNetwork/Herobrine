package com.ampznetwork.herobrine.feature.template.model.expr;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.CodeComponent;

public interface Expression extends CodeComponent {
    Object evaluate(TemplateContext context);
}
