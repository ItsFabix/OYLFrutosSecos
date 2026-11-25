package com.oyl.frutossecos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OylApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(OylApiApplication.class, args);
    }
}
