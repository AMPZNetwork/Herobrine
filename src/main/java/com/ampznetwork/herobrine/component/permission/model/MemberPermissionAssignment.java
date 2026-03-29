package com.ampznetwork.herobrine.component.permission.model;

import com.ampznetwork.herobrine.component.permission.abstr.PermissionAssignment;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(MemberPermissionAssignment.Key.class)
public class MemberPermissionAssignment implements PermissionAssignment {
    @Id long   guildId;
    @Id long   userId;
    @Id String key;
    boolean set;

    public record Key(long guildId, long userId, String key) {}
}
