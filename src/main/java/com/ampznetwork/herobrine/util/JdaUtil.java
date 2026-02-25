package com.ampznetwork.herobrine.util;

import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import org.comroid.api.Polyfill;
import org.comroid.api.java.StackTraceUtils;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdaUtil {
    public static <RA extends RestAction<?>> Function<Throwable, RA> exceptionLogger(
            Logger log, InteractionHook hook,
            String message
    ) {
        return exceptionLogger(log, new MessageDeliveryTarget.Hook(hook), message);
    }

    public static <RA extends RestAction<?>> Function<Throwable, RA> exceptionLogger(
            Logger log,
            MessageDeliveryTarget delivery,
            String message
    ) {
        return t -> {
            log.log(Level.SEVERE, message, t);
            return Polyfill.uncheckedCast(delivery.send("%s ```\n%s\n```".formatted(message,
                    StackTraceUtils.toString(t))));
        };
    }
}
