package com.ampznetwork.herobrine.feature.accountlink.model.event;

import com.ampznetwork.herobrine.feature.accountlink.model.LinkType;
import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.context.ApplicationEvent;

@Value
public class AccountLinkEvent extends ApplicationEvent {
    Guild         guild;
    LinkedAccount account;
    LinkType      type;

    public AccountLinkEvent(Object source, Guild guild, LinkedAccount account, LinkType type) {
        super(source);

        this.guild   = guild;
        this.account = account;
        this.type    = type;
    }
}
