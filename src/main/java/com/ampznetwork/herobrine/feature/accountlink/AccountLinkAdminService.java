package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.herobrine.component.MaintenanceProvider;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import com.ampznetwork.libmod.api.entity.Player;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Log
@Service
@Command("linkadmin")
public class AccountLinkAdminService extends ListenerAdapter {
    @Autowired MaintenanceProvider     maintenance;
    @Autowired LinkedAccountRepository accounts;

    @Command(permission = "268435456")
    @Description("Show account links for a user")
    public CompletableFuture<MessageEmbed> lookup(@Command.Arg @Description("The user to look up") User user) {
        var account = accounts.findById(user.getIdLong())
                .orElseThrow(() -> new CommandError("User %s has not linked any accounts yet".formatted(user)));

        return CompletableFuture.supplyAsync(() -> {
            var embed = new EmbedBuilder();

            embed.setTitle("Linked Accounts of User %s".formatted(user));
            if (account.getMinecraftId() instanceof UUID minecraftId) embed.addField("Minecraft",
                    "Username: `%s`\nUser ID: `%s`".formatted(Player.fetchUsername(minecraftId).join(), minecraftId),
                    false);

            return embed.build();
        });
    }

    @Command
    @Description("Delete a users entire account linkage data")
    public String drop(
            User executor, @Command.Arg @Description({
                    "The user whose data should be deleted", "If this user is yourself, you are automatically permitted"
            }) User user
    ) {
        if (!executor.equals(user)) maintenance.verifySuperadmin(executor);

        accounts.findById(user.getIdLong())
                .orElseThrow(() -> new CommandError("User %s has not linked any accounts yet".formatted(user)));
        accounts.deleteById(user.getIdLong());

        return "All account linkage for user %s has been deleted";
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
