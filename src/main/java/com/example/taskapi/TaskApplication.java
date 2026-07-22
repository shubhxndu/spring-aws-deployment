package com.example.taskapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TaskApplication {

    private static final Logger log = LoggerFactory.getLogger(TaskApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }

    @Bean
    CommandLineRunner startupLogger() {
        return args -> log.info("AWS Cloud Task API started successfully");
    }
}
