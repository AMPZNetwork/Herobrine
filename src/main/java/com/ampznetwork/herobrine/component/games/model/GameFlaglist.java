package com.ampznetwork.herobrine.component.games.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(GameFlaglist.Key.class)
public class GameFlaglist {
    @Id         long             guildId;
    @Id         Type             type;
    @ManyToMany Collection<Game> games;

    public enum Type {
        Blacklist, Whitelist
    }

    public record Key(long guildId, Type type) {}
}
