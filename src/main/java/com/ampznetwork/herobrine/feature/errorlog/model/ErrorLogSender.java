package com.ampznetwork.herobrine.feature.errorlog.model;

import com.ampznetwork.herobrine.feature.errorlog.ErrorLogService;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

public interface ErrorLogSender {
    default String getErrorSourceName() {
        return getClass().getSimpleName();
    }

    default ErrorLogService error() {
        return bean(ErrorLogService.class);
    }

    default ErrorLogService.EntryAPI newErrorEntry() {
        return error().newEntry().source(this);
    }

    default <R> Function<Throwable, R> exceptionLogger(
            Guild guild, CharSequence message,
            @Nullable Supplier<R> fallback
    ) {
        return t -> {
            newErrorEntry().message(message).level(Level.SEVERE).guild(guild).queue();
            return fallback == null ? null : fallback.get();
        };
    }
}
