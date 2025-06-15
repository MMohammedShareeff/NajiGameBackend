package com.naji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EntityScan(basePackages = {"com.naji"})
@SpringBootApplication
@EnableAsync
public class NajiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NajiApplication.class);
    }

}