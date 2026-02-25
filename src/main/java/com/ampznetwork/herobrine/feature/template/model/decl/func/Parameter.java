package com.ampznetwork.herobrine.feature.template.model.decl.func;

import com.ampznetwork.herobrine.feature.template.model.CodeComponent;
import lombok.Value;

@Value
public class Parameter implements CodeComponent {
    String name;

    @Override
    public String toSerializedString() {
        return name;
    }
}
