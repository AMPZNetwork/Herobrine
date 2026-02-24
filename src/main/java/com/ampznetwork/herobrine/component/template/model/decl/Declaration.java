package com.ampznetwork.herobrine.component.template.model.decl;

import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.model.CodeComponent;

public interface Declaration extends CodeComponent {
    void execute(TemplateContext context);
}
