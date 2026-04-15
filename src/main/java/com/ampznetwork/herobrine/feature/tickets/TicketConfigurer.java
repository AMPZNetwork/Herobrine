package com.ampznetwork.herobrine.feature.tickets;

import com.ampznetwork.herobrine.component.log.audit.model.AuditLogSender;
import com.ampznetwork.herobrine.component.log.error.model.ErrorLogSender;
import com.ampznetwork.herobrine.feature.tickets.model.TicketConfiguration;
import com.ampznetwork.herobrine.repo.TicketConfigurationRepository;
import com.ampznetwork.herobrine.util.EmbedTemplate;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.comroid.annotations.Description;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log
@Service
@Interaction("ticket-admin")
public class TicketConfigurer implements AuditLogSender, ErrorLogSender {
    @Autowired TicketConfigurationRepository configs;

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "16"))
    @Description("Configure the ticketing system")
    public EmbedBuilder configure(
            Guild guild, @Parameter(required = false) @Description("The base channel where ticket threads should be created") @Nullable TextChannel channel,
            @Parameter(required = false) @Description("The base role for all team members") @Nullable Role team
    ) {
        if (channel == null) {
            configs.deleteById(guild.getIdLong());
            return EmbedTemplate.success("Ticket system disabled");
        }

        var config = configs.findById(guild.getIdLong())
                .map(it -> it.setBaseChannelId(channel.getIdLong()).setTeamRoleId(team == null ? 0L : team.getIdLong()))
                .orElseGet(() -> new TicketConfiguration(guild.getIdLong(), channel.getIdLong(), team == null ? 0L : team.getIdLong()));
        configs.save(config);

        return EmbedTemplate.success("Ticket configuration updated")
                .addField("Ticket Channel", channel.getAsMention(), false)
                .addField("Team role", team == null ? "<none>" : team.getAsMention(), false);
    }
}
