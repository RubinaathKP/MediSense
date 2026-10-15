package com.medisense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MediSenseApplication {

    private static final Logger log = LoggerFactory.getLogger(MediSenseApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MediSenseApplication.class, args);
        log.info("MediSense Health Literacy Application started successfully.");
    }
}
