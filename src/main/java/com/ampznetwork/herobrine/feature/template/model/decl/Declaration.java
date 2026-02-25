package com.ampznetwork.herobrine.feature.template.model.decl;

import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.model.CodeComponent;

public interface Declaration extends CodeComponent {
    void execute(TemplateContext context);
}
