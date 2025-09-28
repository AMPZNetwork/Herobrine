package com.ampznetwork.herobrine.model.entity;

import com.ampznetwork.libmod.api.model.convert.UuidVarchar36Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Id;
import java.util.UUID;

@Data
@Entity
@Table(name = "libmod_players")
public class Player {
    @Id @Convert(converter = UuidVarchar36Converter.class)
    @Column(columnDefinition = "varchar(36)", updatable = false, nullable = false)
    protected UUID id = UUID.randomUUID();

    String name;

    public com.ampznetwork.libmod.api.entity.Player upgrade() {
        return com.ampznetwork.libmod.api.entity.Player.builder().id(id).name(name).build();
    }
}
