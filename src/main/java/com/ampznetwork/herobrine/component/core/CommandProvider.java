package com.ampznetwork.herobrine.component.core;

import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Interaction;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.stream.Stream;

@Log
@Component
public class CommandProvider {
    /// exists for lifecycle control
    @Lazy @Autowired JdaAdapter jdaAdapter;

    @Bean
    public InteractionCore core() {
        return new InteractionCore();
    }

    @Bean
    @Lazy(false)
    public JdaAdapter jdaAdapter(@Autowired InteractionCore core, @Autowired JDA jda) {
        var adapter = new JdaAdapter(core, jda);
        core.addChild(adapter);
        return adapter;
    }

    @Order
    @EventListener
    public void on(ApplicationStartedEvent event) {
        var context = event.getApplicationContext();
        var core    = context.getBean(InteractionCore.class);

        registerInteractions(context, core);
        core.initialize();

        log.info("Initialized");
    }

    private void registerInteractions(ConfigurableApplicationContext context, InteractionCore core) {
        var reflections = new Reflections("com.ampznetwork.herobrine", Scanners.TypesAnnotated, Scanners.MethodsAnnotated);

        Stream.concat(reflections.getTypesAnnotatedWith(Interaction.class).stream(),
                        reflections.getFieldsAnnotatedWith(Interaction.class).stream().map(Field::getDeclaringClass))
                .distinct()
                .map(context::getBean)
                .peek(bean -> log.finer("Found @Interaction annotated members in " + bean))
                .forEach(core::register);
    }
}
