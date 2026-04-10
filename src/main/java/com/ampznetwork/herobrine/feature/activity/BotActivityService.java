package com.ampznetwork.herobrine.feature.activity;

import com.ampznetwork.herobrine.component.MaintenanceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import org.comroid.annotations.Description;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Log
@Service
@Interaction("activity")
public class BotActivityService {
    public static final File                INFO = new File("./activity.json");
    @Autowired          JDA                 jda;
    @Autowired          MaintenanceProvider maintenance;
    @Autowired          ObjectMapper        mapper;

    @Interaction
    @Description("Update bot presence")
    public void set(
            User user, @Parameter OnlineStatus status, @Parameter Activity.ActivityType activity, @Parameter String name,
            @Parameter @Nullable String url
    ) throws IOException {
        maintenance.verifySuperadmin(user);

        var info = new ActivityInfo(status, activity, name, url);

        try {
            if (!INFO.exists() && !INFO.createNewFile()) throw new IOException("Could not create config file " + INFO.getAbsolutePath());

            mapper.writeValue(INFO, info);
        } finally {
            updatePresence(info);
        }
    }

    @Interaction
    @Description("Reload presence from config")
    public void reload(User user) throws IOException {
        if (user != null) maintenance.verifySuperadmin(user);
        if (!INFO.exists()) return;

        var info = mapper.readValue(INFO, ActivityInfo.class);

        updatePresence(info);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) throws IOException {
        event.getApplicationContext().getBean(InteractionCore.class).register(this);

        reload(null);

        log.info("Initialized");
    }

    private void updatePresence(ActivityInfo info) {
        jda.getPresence().setPresence(info.status, Activity.of(info.activity, info.name, info.url));
    }

    record ActivityInfo(
            OnlineStatus status, Activity.ActivityType activity, String name, @Nullable String url
    ) {}
}
