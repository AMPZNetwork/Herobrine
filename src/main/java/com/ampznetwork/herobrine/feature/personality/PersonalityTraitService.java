package com.ampznetwork.herobrine.feature.personality;

import com.ampznetwork.herobrine.feature.personality.model.PersonalityTrait;
import com.ampznetwork.herobrine.feature.template.MessageTemplateEngine;
import com.ampznetwork.herobrine.repo.PersonalityTraitRepo;
import com.ampznetwork.herobrine.util.JdaUtil;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.api.tree.Container;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Log
@Service
@Command("personality")
@Description("Configure Herobrine's personality")
public class PersonalityTraitService extends ListenerAdapter {
    @NonFinal List<? extends Event.Listener<? extends GenericEvent>> listeners = new ArrayList<>();

    @Autowired MessageTemplateEngine   templateEngine;
    @Autowired PersonalityTraitRepo    personalities;
    @Autowired Event.Bus<GenericEvent> jdaEventBus;

    @Command(permission = "8")
    public void reload() {
        listeners.forEach(Container.Base::close);
        listeners = Streams.of(personalities.findAll())
                .filter(trait -> trait.getRandomDetail().check())
                .map(trait -> trait.getDiscordTrigger()
                        .apply(jdaEventBus)
                        .mapData(JdaUtil.eventGuildFilter(trait.getGuildId()))
                        .subscribeData(event -> handle(trait, event)))
                .toList();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        reload();

        log.info("Initialized");
    }

    private void handle(PersonalityTrait trait, GenericMessageEvent event) {
        var template = trait.getTemplateScript();
        var context  = templateEngine.parse(template, event);
        var message  = context.evaluate().build();

        event.getChannel().sendMessage(message).queue();
    }
}
