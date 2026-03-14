package com.ampznetwork.herobrine.feature.accountlink.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MinecraftUsernameEnforcerConfig {
    @Id long guildId;
    boolean enforceNicknames;
}
