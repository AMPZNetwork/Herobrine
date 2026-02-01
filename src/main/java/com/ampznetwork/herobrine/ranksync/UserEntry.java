package com.ampznetwork.herobrine.ranksync;

import lombok.Data;
import net.dv8tion.jda.api.entities.Role;
import org.comroid.api.net.luckperms.model.group.GroupData;

import java.util.UUID;

@Data
public final class UserEntry {
    private final UUID      minecraftId;
    private       long      discordId;
    private       GroupData group;
    private       Role      role;
}
