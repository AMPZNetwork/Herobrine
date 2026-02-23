package com.ampznetwork.herobrine.feature.timezone;

import lombok.Data;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class TimezoneConfiguration {
    private final Map<@NotNull Long, @NotNull String> userZoneIds = new ConcurrentHashMap<>();

    public void setZoneId(User user, ZoneId zoneId) {
        userZoneIds.put(user.getIdLong(), zoneId.getId());
    }

    public Optional<ZoneId> getZoneId(User user) {
        var userid = user.getIdLong();
        if (!userZoneIds.containsKey(userid)) return Optional.empty();
        var id = userZoneIds.get(userid);
        return Optional.of(ZoneId.of(id));
    }
}
