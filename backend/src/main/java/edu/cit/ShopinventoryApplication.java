package edu.cit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShopinventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopinventoryApplication.class, args);
    }
}