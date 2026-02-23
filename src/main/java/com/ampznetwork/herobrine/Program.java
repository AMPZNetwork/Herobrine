package com.ampznetwork.herobrine;

import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@Log
@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ampznetwork.herobrine.repo")
public class Program {
    public static void main(String[] args) {
        SpringApplication.run(Program.class, args);
    }

    @Bean
    public File botDir() {
        return new File("/srv/herobrine/");
    }
}
