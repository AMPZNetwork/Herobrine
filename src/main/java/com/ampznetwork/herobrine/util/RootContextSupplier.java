package com.ampznetwork.herobrine.util;

import lombok.NoArgsConstructor;
import lombok.Value;
import org.comroid.api.func.ext.Context;
import org.comroid.api.func.util.RootContextSource;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Value
@NoArgsConstructor
public class RootContextSupplier implements RootContextSource {
    @Override
    public Context getRootContext() {
        return bean(ApplicationContextProvider.class);
    }
}
