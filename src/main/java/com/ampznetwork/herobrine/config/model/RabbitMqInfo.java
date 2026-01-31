package com.ampznetwork.herobrine.config.model;

import lombok.Data;
import org.comroid.annotations.Default;

@Data
public class RabbitMqInfo {
    @Default("amqp://guest:guest@localhost:5672") String uri;
}
