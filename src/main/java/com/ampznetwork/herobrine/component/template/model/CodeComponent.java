package com.ampznetwork.herobrine.component.template.model;

import org.comroid.api.data.seri.StringSerializable;
import org.comroid.api.func.WrappedFormattable;

public interface CodeComponent extends StringSerializable, WrappedFormattable {
    @Override
    default String getPrimaryName() {
        return toString();
    }

    @Override
    default String getAlternateName() {
        return toSerializedString();
    }
}
