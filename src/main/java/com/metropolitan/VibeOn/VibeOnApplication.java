package com.metropolitan.VibeOn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VibeOnApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeOnApplication.class, args);
    }
}
