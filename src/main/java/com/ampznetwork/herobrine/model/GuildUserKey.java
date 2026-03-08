package com.ampznetwork.herobrine.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;

public record GuildUserKey(Guild guild, UserSnowflake user) {}
