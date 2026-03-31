package com.ampznetwork.herobrine.component.permission;

import com.ampznetwork.herobrine.component.MaintenanceProvider;
import com.ampznetwork.herobrine.component.permission.abstr.PermissionAssignment;
import com.ampznetwork.herobrine.component.permission.model.HerobrinePermission;
import com.ampznetwork.herobrine.component.permission.model.MemberPermissionAssignment;
import com.ampznetwork.herobrine.component.permission.model.RolePermissionAssignment;
import com.ampznetwork.herobrine.component.permission.model.UserPermissionAssignment;
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
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiFunction;

@Log
@Component
@Command("permission")
public class PermissionManager {
    @Autowired MaintenanceProvider                  maintenance;
    @Autowired UserPermissionAssignmentRepository   userAssignments;
    @Autowired RolePermissionAssignmentRepository   roleAssignments;
    @Autowired MemberPermissionAssignmentRepository memberAssignments;

    public void verify(ISnowflake target, CharSequence permission) {
        if (queryFor(target, permission).toBooleanOrElse(false)) return;
        throw new CommandError("Insufficient permissions; missing " + Markdown.Code.apply(permission));
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

        return assignment.map(PermissionAssignment::isSet).map(TriState::byBoolean).orElse(TriState.NOT_SET);
    }

    @Command
    @Description("Define user-level permissions")
    public void user(
            User user, @Command.Arg @Description("Target to define permission for") User target,
            @Command.Arg(autoFillProvider = HerobrinePermission.AutoFill.class) @Description("Permission to set") String permission,
            @Command.Arg(required = false) @Description("Permission state") @Nullable Boolean state
    ) {
        maintenance.verifySuperadmin(user);

        var key = new UserPermissionAssignment.Key(target.getIdLong(), permission);

        mutate(userAssignments, key, state, (k, b) -> new UserPermissionAssignment(k.userId(), k.key(), b));
    }

    @Command(permission = "8")
    @Description("Define member-level permissions")
    public void member(
            @Command.Arg @Description("Target to define permission for") Member target,
            @Command.Arg(autoFillProvider = HerobrinePermission.AutoFill.class) @Description("Permission to set") String permission,
            @Command.Arg(required = false) @Description("Permission state") @Nullable Boolean state
    ) {
        var key = new MemberPermissionAssignment.Key(target.getGuild().getIdLong(), target.getIdLong(), permission);

        mutate(memberAssignments, key, state, (k, b) -> new MemberPermissionAssignment(k.guildId(), k.userId(), k.key(), b));
    }

    @Command(permission = "268435456")
    @Description("Define role-level permissions")
    public void role(
            @Command.Arg @Description("Target to define permission for") Role target,
            @Command.Arg(autoFillProvider = HerobrinePermission.AutoFill.class) @Description("Permission to set") String permission,
            @Command.Arg(required = false) @Description("Permission state") @Nullable Boolean state
    ) {
        var key = new RolePermissionAssignment.Key(target.getIdLong(), permission);

        mutate(roleAssignments, key, state, (k, b) -> new RolePermissionAssignment(k.roleId(), k.key(), b));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
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
