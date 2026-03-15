package com.ampznetwork.herobrine.feature.accountlink.model;

import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkRoleConfiguration;
import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import org.comroid.api.attr.Named;

public enum LinkType implements Named {
    Minecraft {
        @Override
        public Object getLinkedId(LinkedAccount account) {
            return account.getMinecraftId();
        }

        @Override
        public long getRoleId(LinkRoleConfiguration config) {
            return config.getMinecraftRoleId();
        }
    }, Hytale {
        @Override
        public Object getLinkedId(LinkedAccount account) {
            return account.getHytaleId();
        }

        @Override
        public long getRoleId(LinkRoleConfiguration config) {
            return config.getHytaleRoleId();
        }
    };

    public abstract Object getLinkedId(LinkedAccount account);

    public abstract long getRoleId(LinkRoleConfiguration config);
}
