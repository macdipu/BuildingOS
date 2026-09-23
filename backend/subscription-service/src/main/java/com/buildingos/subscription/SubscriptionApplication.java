package com.buildingos.subscription;

import com.buildingos.platform.web.config.PlatformWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PlatformWebConfiguration.class)
public class SubscriptionApplication {
    public static void main(String[] args) { SpringApplication.run(SubscriptionApplication.class, args); }
}
