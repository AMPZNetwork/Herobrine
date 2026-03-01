package com.ampznetwork.herobrine.feature.errorlog.model;

import com.ampznetwork.herobrine.feature.errorlog.ErrorLogService;
import net.dv8tion.jda.api.entities.Guild;
import org.comroid.api.func.exc.ThrowingRunnable;
import org.comroid.api.func.exc.ThrowingSupplier;
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

    default boolean wrapErrors(Guild guild, String taskDescription, ThrowingRunnable<?> task) {
        return wrapErrors(guild, taskDescription, () -> {
            task.run();
            return Boolean.TRUE;
        }) != null;
    }

    default <T> @Nullable T wrapErrors(Guild guild, String taskDescription, ThrowingSupplier<T, ?> task) {
        return wrapErrors(guild, taskDescription, task, null);
    }

    default <T> @Nullable T wrapErrors(
            Guild guild, String taskDescription, ThrowingSupplier<T, ?> task,
            @Nullable Supplier<T> fallback
    ) {
        try {
            return task.get();
        } catch (Throwable t) {
            return exceptionLogger(guild, taskDescription, fallback).apply(t);
        }
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
