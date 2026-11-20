package com.petexpress.petexpress_site_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PetexpressSiteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetexpressSiteBackendApplication.class, args);
    }
}
