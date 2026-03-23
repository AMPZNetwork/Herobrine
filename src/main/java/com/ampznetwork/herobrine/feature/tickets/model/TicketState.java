package com.ampznetwork.herobrine.feature.tickets.model;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.attr.Named;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public enum TicketState implements Named {
    Opened(false, false, true),
    Undetailed(false, true, true),
    Investigating(false, false, true),
    Completing(true, true, true),
    Incomplete(true, true, false),
    Unplanned(true, false, false),
    Completed(true, false, true);

    boolean closed;
    boolean awaitingResponse;
    boolean privileged;

    public @Nullable MessageCreateBuilder toInfoMessage() {
        return null; // todo
    }
}
