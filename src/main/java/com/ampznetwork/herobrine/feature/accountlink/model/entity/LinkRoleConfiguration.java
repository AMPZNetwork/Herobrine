package com.ampznetwork.herobrine.feature.accountlink.model.entity;

import com.ampznetwork.herobrine.feature.accountlink.model.LinkType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkRoleConfiguration {
    @Id      long guildId;
    @Default long minecraftRoleId = 0;
    @Default long hytaleRoleId    = 0;

    public @Nullable LinkType getTypeOfRole(long roleId) {
        if (minecraftRoleId == roleId) return LinkType.Minecraft;
        if (hytaleRoleId == roleId) return LinkType.Hytale;

        return null;
    }
}
