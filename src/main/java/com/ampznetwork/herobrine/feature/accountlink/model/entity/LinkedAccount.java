package com.ampznetwork.herobrine.feature.accountlink.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "minecraft_id" }), @UniqueConstraint(columnNames = { "hytale_id" })
})
public class LinkedAccount {
    @Id      long discordId;
    @Default UUID minecraftId = null;
    @Default UUID hytaleId    = null;
}
