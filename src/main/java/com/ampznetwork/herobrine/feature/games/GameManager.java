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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
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
@Command("lobby-game")
public class GameManager {
    public static final String INTERACTION_CONFIRM = "games_confirm";
    public static final String INTERACTION_MODIFY  = "games_modify";
    public static final String OPTION_NAME         = "option_name";
    public static final String OPTION_DESCRIPTION  = "option_description";
    public static final String OPTION_STEAM_APPID  = "option_steam_appid";

    @Autowired PermissionManager      permissions;
    @Autowired GameRepository         games;
    @Autowired GameFlagListRepository lists;

    @Command
    @Description("Create a new game entry")
    public void create(SlashCommandInteractionEvent event, User user) {
        permissions.verify(user, HerobrinePermission.Gameadmin);

        event.replyModal(createEditorModal(null, Game.builder()).build()).queue();
    }

    @Command
    @Description("Edit a game entry")
    public void edit(SlashCommandInteractionEvent event, User user, @Command.Arg(autoFillProvider = Game.All.class) @Description("Game to edit") String game) {
        permissions.verify(user, HerobrinePermission.Gameadmin);

        var result = games.findById(game).orElseThrow(() -> new CommandError("Game with name `%s` not found".formatted(game)));

        event.replyModal(createEditorModal(result, result.toBuilder()).build()).queue();
    }

    @Command
    @Description("Create a new game entry")
    public MessageCreateBuilder remove(User user, @Command.Arg(autoFillProvider = Game.All.class) @Description("Game to remove") String game) {
        permissions.verify(user, HerobrinePermission.Gameadmin);

        var result = games.findById(game).orElseThrow(() -> new CommandError("Game with name `%s` not found".formatted(game)));

        return new MessageCreateBuilder().addEmbeds(EmbedTemplate.warning("Are you sure you want to delete this game?").addField(result.toField()).build())
                .addComponents(ActionRow.of(Button.danger(INTERACTION_CONFIRM, "Yes, delete!")));
    }

    @Command(permission = "8589934592")
    @Description("Edit game Blacklist for this discord server")
    public void blacklist(User user, Guild guild, @Command.Arg(autoFillProvider = Game.All.class) @Description("Game to blacklist") String game) {
        var result = games.findById(game).orElseThrow(() -> new CommandError("Game with name `%s` not found".formatted(game)));
        var list = lists.findById(new GameFlaglist.Key(guild.getIdLong(), GameFlaglist.Type.Blacklist))
                .orElseGet(() -> new GameFlaglist(guild.getIdLong(), GameFlaglist.Type.Blacklist, new ArrayList<>()));
        var col = list.getGames();

        if (col.contains(result)) col.remove(result);
        else col.add(result);

        lists.save(list);
    }

    @Command(permission = "8589934592")
    @Description("Edit game Whitelist for this discord server")
    public void whitelist(User user, Guild guild, @Command.Arg(autoFillProvider = Game.All.class) @Description("Game to whitelist") String game) {
        var result = games.findById(game).orElseThrow(() -> new CommandError("Game with name `%s` not found".formatted(game)));
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
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private Modal.Builder createEditorModal(@Nullable Game game, Game.Builder builder) {
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
