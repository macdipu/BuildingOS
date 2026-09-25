package com.buildingos.backoffice;

import com.buildingos.platform.web.config.PlatformWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PlatformWebConfiguration.class)
public class BackOfficeApplication {
    public static void main(String[] args) { SpringApplication.run(BackOfficeApplication.class, args); }
}
