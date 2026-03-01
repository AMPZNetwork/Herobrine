package com.ampznetwork.herobrine.feature.personality;

import com.ampznetwork.herobrine.feature.personality.model.PersonalityTrait;
import com.ampznetwork.herobrine.feature.template.MessageTemplateEngine;
import com.ampznetwork.herobrine.repo.PersonalityTraitRepo;
import com.ampznetwork.herobrine.util.JdaUtil;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.api.tree.Container;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.jetbrains.annotations.Nullable;
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
    @Autowired MessageTemplateEngine   templateEngine;
    @Autowired PersonalityTraitRepo traitRepo;
    @Autowired Event.Bus<GenericEvent> jdaEventBus;
    @Autowired TraitEditorService   creatorService;

    private @NonFinal List<? extends Event.Listener<? extends GenericEvent>> listeners = new ArrayList<>();

    @Command(permission = "8")
    @Description("Reload listeners for all personality traits")
    public void reload() {
        listeners.forEach(Container.Base::close);
        listeners = Streams.of(traitRepo.findAll())
                .filter(trait -> trait.getRandomDetail().check())
                .map(trait -> trait.getDiscordTrigger()
                        .apply(jdaEventBus)
                        .mapData(JdaUtil.eventGuildFilter(trait.getGuildId()))
                        .filterData(event -> {
                            var message = JdaUtil.getMessage(event);
                            return message != null && trait.getContentFilter().matches(message.getContentDisplay());
                        })
                        .subscribeData(event -> handle(trait, event)))
                .toList();
    }

    @Command(permission = "16")
    @Description("Create a new personality trait using a flow")
    public void create(
            IReplyCallback callback, Guild guild, Member member,
            @Command.Arg @Description("The name of the new personality trait") String name
    ) {
        if (guild == null) throw new CommandError("This only works inside guilds");

        creatorService.findTraitEditor(guild, member).ifPresent(creatorService.editors::remove);

        openEditor(callback, guild, member, name, null).queue();
    }

    @Command(permission = "16")
    @Description("Create a new personality trait using a flow")
    public void edit(
            IReplyCallback callback, Guild guild, Member member,
            @Command.Arg(autoFillProvider = PersonalityTrait.AutoFillTraitNamesByGuild.class) @Description(
                    "The name of the personality trait") String name
    ) {
        if (guild == null) throw new CommandError("This only works inside guilds");

        var result = traitRepo.findById(new PersonalityTrait.Key(guild.getIdLong(), name));
        if (result.isEmpty()) throw new CommandError("Personality trait with name %s not found".formatted(name));
        var trait = result.get();

        openEditor(callback, guild, member, name, trait).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        reload();

        log.info("Initialized");
    }

    private RestAction<Message> openEditor(
            IReplyCallback callback, Guild guild, Member member, String name, @Nullable PersonalityTrait trait) {
        return callback.deferReply(true).flatMap(hook -> {
            var message = hook.getCallbackResponse().getMessage();
            var editor = new TraitEditorService.TraitEditor(guild,
                    member,
                    message,
                    trait == null
                    ? PersonalityTrait.builder().name(name).guildId(guild.getIdLong())
                    : trait.toBuilder());

            creatorService.editors.add(editor);

            return editor.refreshInfoMessage();
        });
    }

    private void handle(PersonalityTrait trait, GenericMessageEvent event) {
        var template = trait.getTemplateScript();
        var context  = templateEngine.parse(template, event);
        var message  = context.evaluate().build();

        event.getChannel().sendMessage(message).queue();
    }
}
