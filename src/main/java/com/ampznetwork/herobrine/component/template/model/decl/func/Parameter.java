package com.ampznetwork.herobrine.component.template.model.decl.func;

import com.ampznetwork.herobrine.component.template.model.CodeComponent;
import lombok.Value;

@Value
public class Parameter implements CodeComponent {
    String name;

    @Override
    public String toSerializedString() {
        return name;
    }
}
