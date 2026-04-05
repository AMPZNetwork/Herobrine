package com.ampznetwork.herobrine.feature.personality;

import com.ampznetwork.herobrine.feature.personality.model.PersonalityTrait;
import com.ampznetwork.herobrine.feature.template.MessageTemplateEngine;
import com.ampznetwork.herobrine.repo.PersonalityTraitRepo;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.comroid.util.JdaUtil;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Log
@Service
@Interaction("personality")
@Description("Configure Herobrine's personality")
public class PersonalityTraitService {
    @Autowired MessageTemplateEngine templateEngine;
    @Autowired PersonalityTraitRepo traitRepo;
    @Autowired TraitEditorService    creatorService;

    @EventListener
    public void on(GenericEvent event) {
        if (!(event instanceof GenericMessageEvent)) return; // todo remove this antipattern

        var guild   = JdaUtil.getGuild(event).orElse(null);
        var message = JdaUtil.getMessage((GenericMessageEvent) event);

        if (guild == null) return;

        for (var trait : traitRepo.findAllByGuildId(guild.getIdLong())) {
            if (!trait.getDiscordTrigger().test(event)) continue;
            if (!trait.getRandomDetail().check()) continue;

            if (message == null || !trait.getContentFilter().matches(message.getContentDisplay())) continue;

            handle(trait, (GenericMessageEvent) event);
        }
    }

    @Interaction(definitions = @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "16"))
    @Description("Create a new personality trait using a flow")
    public void create(IReplyCallback callback, Guild guild, Member member, @Parameter @Description("The name of the new personality trait") String name) {
        if (guild == null) throw Response.of("This only works inside guilds");

        creatorService.findTraitEditor(guild, member).ifPresent(creatorService.editors::remove);

        openEditor(callback, guild, member, name, null).queue();
    }

    @Interaction(definitions = @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "16"))
    @Description("Create a new personality trait using a flow")
    public void edit(
            IReplyCallback callback, Guild guild, Member member,
            @Parameter(completion = @Completion(provider = PersonalityTrait.AutoFillTraitNamesByGuild.class)) @Description("The name of the personality trait") String name
    ) {
        if (guild == null) throw Response.of("This only works inside guilds");

        var result = traitRepo.findById(new PersonalityTrait.Key(guild.getIdLong(), name));
        if (result.isEmpty()) throw Response.of("Personality trait with name %s not found".formatted(name));
        var trait = result.get();

        openEditor(callback, guild, member, name, trait).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }

    private RestAction<Message> openEditor(IReplyCallback callback, Guild guild, Member member, String name, @Nullable PersonalityTrait trait) {
        return callback.deferReply(true).flatMap(hook -> {
            var message = hook.getCallbackResponse().getMessage();
            var editor = new TraitEditorService.TraitEditor(guild,
                    member,
                    message,
                    trait == null ? PersonalityTrait.builder().name(name).guildId(guild.getIdLong()) : trait.toBuilder());

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
