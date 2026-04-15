package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.chatmod.api.model.Player;
import com.ampznetwork.herobrine.component.core.MaintenanceProvider;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.comroid.annotations.Description;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Log
@Service
@Interaction("linkadmin")
public class AccountLinkAdminService {
    @Autowired MaintenanceProvider     maintenance;
    @Autowired LinkedAccountRepository accounts;

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "268435456"))
    @Description("Show account links for a user")
    public CompletableFuture<MessageEmbed> lookup(@Parameter @Description("The user to look up") User user) {
        var account = accounts.findById(user.getIdLong()).orElseThrow(() -> Response.of("User %s has not linked any accounts yet".formatted(user)));

        return CompletableFuture.supplyAsync(() -> {
            var embed = new EmbedBuilder();

            embed.setTitle("Linked Accounts of User %s".formatted(user));
            if (account.getMinecraftId() instanceof UUID minecraftId) embed.addField("Minecraft",
                    "Username: `%s`\nUser ID: `%s`".formatted(Player.fetchUsername(minecraftId).join(), minecraftId),
                    false);

            return embed.build();
        });
    }

    @Interaction
    @Description("Delete a users entire account linkage data")
    public String drop(
            User executor, @Parameter @Description({
                    "The user whose data should be deleted", "If this user is yourself, you are automatically permitted"
            }) User user
    ) {
        if (!executor.equals(user)) maintenance.verifySuperadmin(executor);

        accounts.findById(user.getIdLong()).orElseThrow(() -> Response.of("User %s has not linked any accounts yet".formatted(user)));
        accounts.deleteById(user.getIdLong());

        return "All account linkage for user %s has been deleted";
    }
}
