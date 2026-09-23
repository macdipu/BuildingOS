package com.buildingos.building;

import com.buildingos.platform.web.config.PlatformWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PlatformWebConfiguration.class)
public class BuildingApplication {
    public static void main(String[] args) { SpringApplication.run(BuildingApplication.class, args); }
}
