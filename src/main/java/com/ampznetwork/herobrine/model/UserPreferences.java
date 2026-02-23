package com.ampznetwork.herobrine.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    @Id       long   userId;
    @Nullable ZoneId timezone;
}
