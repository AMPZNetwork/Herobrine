package com.ampznetwork.herobrine.model;

import com.ampznetwork.chatmod.api.model.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class CachedPlayer {
    @Id @Column(columnDefinition = "varchar(36)", updatable = false, nullable = false)
    protected UUID id = UUID.randomUUID();

    String name;

    public Player upgrade() {
        return new Player(id, name);
    }
}
