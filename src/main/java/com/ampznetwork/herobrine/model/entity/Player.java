package com.ampznetwork.herobrine.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "libmod_players")
public class Player {
    @Id UUID uuid;
    String username;

    public com.ampznetwork.libmod.api.entity.Player upgrade() {
        return com.ampznetwork.libmod.api.entity.Player.builder().id(uuid).name(username).build();
    }
}
