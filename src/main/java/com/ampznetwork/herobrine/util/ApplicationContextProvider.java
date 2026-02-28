package com.ampznetwork.herobrine.util;

import lombok.extern.java.Log;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.Context;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

@Log
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationContextProvider implements ApplicationContextAware, Context {
    private static ApplicationContextProvider instance;
    private static ApplicationContext         applicationContext;

    public static ApplicationContextProvider get() {
        return instance;
    }

    public static <T, R extends T> R bean(Class<T> type) {
        return bean(type, null);
    }

    public static <T, R extends T> R bean(Class<? super T> type, @Nullable String name) {
        try {
            var context = instance;
            return Polyfill.uncheckedCast(name == null
                                          ? applicationContext.getBean(type)
                                          : applicationContext.getBean(name, type));
        } catch (NoUniqueBeanDefinitionException nubde) {
            throw new RuntimeException("More than one bean definition found for Bean '%s' of type %s".formatted(name,
                    StackTraceUtils.lessSimpleName(type)), nubde);
        } catch (NoSuchBeanDefinitionException nsbde) {
            throw new RuntimeException("No bean definition found for Bean of type " + StackTraceUtils.lessSimpleName(
                    type), nsbde);
        }
    }

    public static <T, R extends T> Optional<R> wrap(Class<T> type) {
        return wrap(type, null);
    }

    public static <T, R extends T> Optional<R> wrap(Class<T> type, @Nullable String name) {
        try {
            return Polyfill.uncheckedCast(name == null
                                          ? applicationContext.getBean(type)
                                          : applicationContext.getBean(name, type));
        } catch (Throwable t) {
            log.log(Level.WARNING,
                    "Bean '%s' of type %s could not be obtained; %s".formatted(name,
                            StackTraceUtils.lessSimpleName(type),
                            t.getMessage()));
            return Optional.empty();
        }
    }

    {
        instance = this;
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) {
        ApplicationContextProvider.applicationContext = applicationContext;
    }

    public <T> Stream<? extends T> streamContextMembers(boolean includeChildren, Class<T> type) {
        return applicationContext.getBeansOfType(type, false, includeChildren).values().stream();
    }
}
