package com.ampznetwork.herobrine.feature.games;

import com.ampznetwork.herobrine.component.permission.PermissionManager;
import com.ampznetwork.herobrine.component.permission.model.HerobrinePermission;
import com.ampznetwork.herobrine.feature.games.model.Game;
import com.ampznetwork.herobrine.feature.games.model.GameFlaglist;
import com.ampznetwork.herobrine.repo.GameFlagListRepository;
import com.ampznetwork.herobrine.repo.GameRepository;
import com.ampznetwork.herobrine.util.EmbedTemplate;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Log
@Service
@Interaction("game")
public class GameManager {
    public static final String INTERACTION_CONFIRM = "games_confirm_";
    public static final String INTERACTION_MODIFY  = "games_modify_";
    public static final String OPTION_NAME         = "option_name";
    public static final String OPTION_DESCRIPTION  = "option_description";
    public static final String OPTION_STEAM_APPID  = "option_steam_appid";

    @Autowired PermissionManager      permissions;
    @Autowired GameRepository         games;
    @Autowired GameFlagListRepository lists;

    @EventListener
    public void on(ModalInteractionEvent event) {
        var modalId = event.getModalId();
        if (!modalId.startsWith(INTERACTION_MODIFY)) return;
        var game    = modalId.substring(INTERACTION_MODIFY.length() + 1);
        var builder = games.findById(game).orElseThrow(() -> Response.of("Game with name `%s` not found".formatted(game))).toBuilder();

        String buf;
        var    mapping = event.getValue(OPTION_NAME);
        if (mapping != null && !(buf = mapping.getAsString()).isBlank()) builder.name(buf);

        mapping = event.getValue(OPTION_DESCRIPTION);
        if (mapping != null && !(buf = mapping.getAsString()).isBlank()) builder.description(buf);

        mapping = event.getValue(OPTION_STEAM_APPID);
        if (mapping != null && (buf = mapping.getAsString()).matches("\\d+")) builder.steamAppId(Long.parseLong(buf));

        var result = builder.build();
        if (!result.getName().equals(game) && games.existsById(game)) games.deleteById(game);
        games.save(result);
    }

    @EventListener
    public void on(ButtonInteractionEvent event) {
        var componentId = event.getComponentId();
        if (!componentId.startsWith(INTERACTION_CONFIRM)) return;
        var game = componentId.substring(INTERACTION_CONFIRM.length() + 1);

        games.deleteById(game);

        event.replyEmbeds(EmbedTemplate.success("Game `%s` was deleted".formatted(game)).build()).queue();
    }

    @Interaction
    @Description("Create a new game entry")
    public void create(SlashCommandInteractionEvent event, User user) {
        permissions.verify(user, HerobrinePermission.Gameadmin);

        event.replyModal(createEditorModal(null).build()).queue();
    }

    @Interaction
    @Description("Edit a game entry")
    public void edit(
            SlashCommandInteractionEvent event, User user,
            @Parameter(completion = @Completion(provider = Game.All.class)) @Description("Game to edit") String game
    ) {
        permissions.verify(user, HerobrinePermission.Gameadmin);

        var result = games.findById(game).orElseThrow(() -> Response.of("Game with name `%s` not found".formatted(game)));

        event.replyModal(createEditorModal(result).build()).queue();
    }

    @Interaction
    @Description("Create a new game entry")
    public MessageCreateBuilder remove(User user, @Parameter(completion = @Completion(provider = Game.All.class)) @Description("Game to remove") String game) {
        permissions.verify(user, HerobrinePermission.Gameadmin);

        var result = games.findById(game).orElseThrow(() -> Response.of("Game with name `%s` not found".formatted(game)));

        return new MessageCreateBuilder().addEmbeds(EmbedTemplate.warning("Are you sure you want to delete this game?").addField(result.toField()).build())
                .addComponents(ActionRow.of(Button.danger(INTERACTION_CONFIRM + game, "Yes, delete!")));
    }

    @Interaction(definitions = @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "8589934592"))
    @Description("Edit game Blacklist for this discord server")
    public void blacklist(
            User user, Guild guild,
            @Parameter(completion = @Completion(provider = Game.All.class)) @Description("Game to blacklist") String game
    ) {
        var result = games.findById(game).orElseThrow(() -> Response.of("Game with name `%s` not found".formatted(game)));
        var list = lists.findById(new GameFlaglist.Key(guild.getIdLong(), GameFlaglist.Type.Blacklist))
                .orElseGet(() -> new GameFlaglist(guild.getIdLong(), GameFlaglist.Type.Blacklist, new ArrayList<>()));
        var col = list.getGames();

        if (col.contains(result)) col.remove(result);
        else col.add(result);

        lists.save(list);
    }

    @Interaction(definitions = @ContextDefinition(value = JdaAdapter.KEY_PERMISSION, expr = "8589934592"))
    @Description("Edit game Whitelist for this discord server")
    public void whitelist(
            User user, Guild guild,
            @Parameter(completion = @Completion(provider = Game.All.class)) @Description("Game to whitelist") String game
    ) {
        var result = games.findById(game).orElseThrow(() -> Response.of("Game with name `%s` not found".formatted(game)));
        var list = lists.findById(new GameFlaglist.Key(guild.getIdLong(), GameFlaglist.Type.Whitelist))
                .orElseGet(() -> new GameFlaglist(guild.getIdLong(), GameFlaglist.Type.Whitelist, new ArrayList<>()));
        var col = list.getGames();

        if (col.contains(result)) col.remove(result);
        else col.add(result);

        lists.save(list);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        log.info("Initialized");
    }

    private Modal.Builder createEditorModal(@Nullable Game game) {
        return Modal.create(INTERACTION_MODIFY, "Modifying Game")
                .addComponents(Label.of("Title", TextInput.create(OPTION_NAME, TextInputStyle.SHORT).setValue(game == null ? null : game.getName()).build()),
                        Label.of("Description",
                                TextInput.create(OPTION_DESCRIPTION, TextInputStyle.PARAGRAPH)
                                        .setValue(game == null ? null : game.getDescription())
                                        .setRequired(false)
                                        .build()),
                        Label.of("Steam AppID",
                                TextInput.create(OPTION_STEAM_APPID, TextInputStyle.SHORT)
                                        .setValue(game == null || game.getSteamAppId() == null ? null : String.valueOf(game.getSteamAppId()))
                                        .setRequired(false)
                                        .build()),
                        TextDisplay.of("-# [Need help finding the Steam AppID?](https://steamdb.info)"));
    }
}
