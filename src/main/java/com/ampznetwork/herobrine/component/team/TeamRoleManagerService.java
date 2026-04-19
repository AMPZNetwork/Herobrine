package com.ampznetwork.herobrine.component.team;

import com.ampznetwork.herobrine.component.team.model.SupportLevel;
import com.ampznetwork.herobrine.component.team.model.TeamCategory;
import com.ampznetwork.herobrine.component.team.role.TeamRoleInfo;
import com.ampznetwork.herobrine.repo.TeamRoleInfoRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.comroid.annotations.Description;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log
@Service
@Interaction(value = "team-role", filter = { @ContextFilter(key = "guild") }, definitions = {
        @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "MANAGE_ROLES")
})
public class TeamRoleManagerService {
    public static final String INTERACTION_CREATE = "trms_create";
    public static final String INTERACTION_EDIT   = "trms_edit";

    public static final String OPTION_ROLE          = "option_role";
    public static final String OPTION_TEAM_CATEGORY = "option_team_category";
    public static final String OPTION_SUPPORT_LEVEL = "option_support_level";
    public static final String OPTION_INHERITS      = "option_inherits";

    @Autowired TeamRoleInfoRepository roles;

    @Interaction
    @Description("Create a new team role configuration")
    public Modal.Builder create(Guild guild) {
        return openEditor(null);
    }

    @Interaction
    @Description("Edit an existing team role configuration")
    public Optional<Modal.Builder> edit(@Parameter Role role) {
        return roles.findById(new TeamRoleInfo.Key(role)).map(this::openEditor);
    }

    @EventListener
    public void on(ModalInteractionEvent event) {
        if (!List.of(INTERACTION_CREATE, INTERACTION_EDIT).contains(event.getModalId())) return;

        var mapping = event.getValue(OPTION_ROLE);
        var role    = Objects.requireNonNull(mapping, "role option").getAsMentions().getRoles().getFirst();
        var key     = new TeamRoleInfo.Key(role);
        var builder = (event.getModalId().equals(INTERACTION_CREATE)
                       ? TeamRoleInfo.builder()
                       : roles.findById(key).map(TeamRoleInfo::toBuilder).orElseGet(TeamRoleInfo::builder)).guildId(role.getGuild().getIdLong())
                .roleId(role.getIdLong());

        mapping = event.getValue(OPTION_TEAM_CATEGORY);
        if (mapping != null) builder.teamCategory(TeamCategory.valueOf(mapping.getAsStringList().getFirst()));

        mapping = event.getValue(OPTION_SUPPORT_LEVEL);
        if (mapping != null) builder.supportLevel(SupportLevel.valueOf(mapping.getAsStringList().getFirst()));

        mapping = event.getValue(OPTION_INHERITS);
        if (mapping != null) builder.inherits(mapping.getAsMentions().getRoles().stream().map(ISnowflake::getIdLong).toList());

        var info = builder.build();
        if (roles.existsById(key)) roles.deleteById(key);
        roles.save(info);

        event.reply("Role configuration updated").setEphemeral(true).queue();
    }

    private Modal.Builder openEditor(@Nullable TeamRoleInfo info) {
        var create = info == null;
        return Modal.create(create ? INTERACTION_CREATE : INTERACTION_EDIT, "Modifying team Role")
                .addComponents(Label.of("Role",
                                EntitySelectMenu.create(OPTION_ROLE, EntitySelectMenu.SelectTarget.ROLE)
                                        .setDefaultValues(Optional.ofNullable(info)
                                                .map(TeamRoleInfo::getRoleId)
                                                .map(EntitySelectMenu.DefaultValue::role)
                                                .stream()
                                                .toList())
                                        .setDisabled(!create)
                                        .build()),
                        Label.of("Team Category",
                                StringSelectMenu.create(OPTION_TEAM_CATEGORY)
                                        .addOptions(Arrays.stream(TeamCategory.values()).map(TeamCategory::getSelectOption).toList())
                                        .setDefaultOptions(Optional.ofNullable(info)
                                                .map(TeamRoleInfo::getTeamCategory)
                                                .map(TeamCategory::getSelectOption)
                                                .stream()
                                                .toList())
                                        .setRequired(false)
                                        .build()),
                        Label.of("Support Level",
                                StringSelectMenu.create(OPTION_SUPPORT_LEVEL)
                                        .addOptions(Arrays.stream(SupportLevel.values()).map(SupportLevel::getSelectOption).toList())
                                        .setDefaultOptions(Optional.ofNullable(info)
                                                .map(TeamRoleInfo::getSupportLevel)
                                                .map(SupportLevel::getSelectOption)
                                                .stream()
                                                .toList())
                                        .setRequired(false)
                                        .build()),
                        Label.of("Inherits Roles",
                                EntitySelectMenu.create(OPTION_INHERITS, EntitySelectMenu.SelectTarget.ROLE)
                                        .setDefaultValues(Optional.ofNullable(info)
                                                .stream()
                                                .flatMap(it -> it.getInherits().stream())
                                                .map(EntitySelectMenu.DefaultValue::role)
                                                .toList())
                                        .setRequiredRange(0, 999)
                                        .build()));
    }
}
