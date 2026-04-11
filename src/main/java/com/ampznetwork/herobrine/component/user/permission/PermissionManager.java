package com.ampznetwork.herobrine.component.user.permission;

import com.ampznetwork.herobrine.component.MaintenanceProvider;
import com.ampznetwork.herobrine.component.user.permission.abstr.PermissionAssignment;
import com.ampznetwork.herobrine.component.user.permission.model.HerobrinePermission;
import com.ampznetwork.herobrine.component.user.permission.model.MemberPermissionAssignment;
import com.ampznetwork.herobrine.component.user.permission.model.RolePermissionAssignment;
import com.ampznetwork.herobrine.component.user.permission.model.UserPermissionAssignment;
import com.ampznetwork.herobrine.repo.MemberPermissionAssignmentRepository;
import com.ampznetwork.herobrine.repo.RolePermissionAssignmentRepository;
import com.ampznetwork.herobrine.repo.UserPermissionAssignmentRepository;
import lombok.Builder;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.kyori.adventure.util.TriState;
import org.comroid.annotations.Description;
import org.comroid.api.text.Markdown;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.comroid.interaction.annotation.ContextDefinition;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiFunction;

@Log
@Component
@Interaction("permission")
public class PermissionManager {
    @Autowired MaintenanceProvider                  maintenance;
    @Autowired UserPermissionAssignmentRepository   userAssignments;
    @Autowired RolePermissionAssignmentRepository   roleAssignments;
    @Autowired MemberPermissionAssignmentRepository memberAssignments;

    public void verify(ISnowflake target, CharSequence permission) {
        if (queryFor(target, permission).toBooleanOrElse(false)) return;
        throw Response.of("Insufficient permissions; missing " + Markdown.Code.apply(permission));
    }

    public TriState queryFor(ISnowflake target, CharSequence permission) {
        return queryFor(null, target, permission);
    }

    @Builder(builderClassName = "LookupAPI", builderMethodName = "lookup", buildMethodName = "query")
    public TriState queryFor(@Nullable Guild guild, ISnowflake target, CharSequence permission) {
        if (target instanceof Role role) guild = role.getGuild();
        else if (target instanceof Member member) guild = member.getGuild();

        var key = permission.toString();
        Optional<? extends PermissionAssignment> assignment = guild == null ? userAssignments.findById(new UserPermissionAssignment.Key(target.getIdLong(),
                key)) : switch (target) {
            case Role role -> roleAssignments.findById(new RolePermissionAssignment.Key(role.getIdLong(), key));
            case UserSnowflake user -> memberAssignments.findById(new MemberPermissionAssignment.Key(guild.getIdLong(), user.getIdLong(), key));
            default -> Optional.empty();
        };

        return assignment.map(PermissionAssignment::isState).map(TriState::byBoolean).orElse(TriState.NOT_SET);
    }

    @Interaction
    @Description("Define user-level permissions")
    public void user(
            User user, @Parameter @Description("Target to define permission for") User target,
            @Parameter @Description("Permission to set") HerobrinePermission permission,
            @Parameter(required = false) @Description("Permission state") @Nullable Boolean state
    ) {
        maintenance.verifySuperadmin(user);

        var key = new UserPermissionAssignment.Key(target.getIdLong(), permission.getPrimaryName());

        mutate(userAssignments, key, state, (k, b) -> new UserPermissionAssignment(k.userId(), k.permissionKey(), b));
    }

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "8"))
    @Description("Define member-level permissions")
    public void member(
            @Parameter @Description("Target to define permission for") Member target,
            @Parameter @Description("Permission to set") HerobrinePermission permission,
            @Parameter(required = false) @Description("Permission state") @Nullable Boolean state
    ) {
        var key = new MemberPermissionAssignment.Key(target.getGuild().getIdLong(), target.getIdLong(), permission.getPrimaryName());

        mutate(memberAssignments, key, state, (k, b) -> new MemberPermissionAssignment(k.guildId(), k.userId(), k.permissionKey(), b));
    }

    @Interaction(definitions = @ContextDefinition(key = JdaAdapter.KEY_PERMISSION, expr = "268435456"))
    @Description("Define role-level permissions")
    public void role(
            @Parameter @Description("Target to define permission for") Role target, @Parameter @Description("Permission to set") HerobrinePermission permission,
            @Parameter(required = false) @Description("Permission state") @Nullable Boolean state
    ) {
        var key = new RolePermissionAssignment.Key(target.getIdLong(), permission.getPrimaryName());

        mutate(roleAssignments, key, state, (k, b) -> new RolePermissionAssignment(k.roleId(), k.permissionKey(), b));
    }

    private <K, PA extends PermissionAssignment, REPO extends CrudRepository<PA, K>> void mutate(
            REPO repo, K key, @Nullable Boolean state,
            BiFunction<K, @NonNull Boolean, PA> constructor
    ) {
        if (state == null) repo.deleteById(key);
        else {
            var value = repo.findById(key).orElseGet(() -> constructor.apply(key, state));
            repo.save(value);
        }
    }
}
