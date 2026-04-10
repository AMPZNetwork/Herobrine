package com.ampznetwork.herobrine.component.user.permission.model;

import com.ampznetwork.herobrine.component.user.permission.abstr.PermissionAssignment;
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
@IdClass(RolePermissionAssignment.Key.class)
public class RolePermissionAssignment implements PermissionAssignment {
    @Id long   roleId;
    @Id String permissionKey;
    boolean state;

    public record Key(long roleId, String permissionKey) {}
}
