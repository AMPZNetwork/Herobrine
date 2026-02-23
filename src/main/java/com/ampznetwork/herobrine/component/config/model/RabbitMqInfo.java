package com.ampznetwork.herobrine.component.config.model;

import lombok.Data;
import org.comroid.annotations.Default;
import org.comroid.annotations.Ignore;
import org.comroid.api.config.ConfigurationManager;

@Data
public class RabbitMqInfo {
    @Default("amqp://guest:guest@localhost:5672") @Ignore(ConfigurationManager.Presentation.class) String uri;
}
