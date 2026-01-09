package com.ampznetwork.herobrine.web;

import com.ampznetwork.herobrine.Program;
import org.comroid.api.func.util.Debug;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class ServletInitializer extends SpringBootServletInitializer implements
        WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Program.class);
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        if (Debug.isDebug()) factory.setPort(8080);
        else {
            factory.setPort(25545);
            factory.setAddress(InetAddress.getLoopbackAddress());
        }
    }
}
