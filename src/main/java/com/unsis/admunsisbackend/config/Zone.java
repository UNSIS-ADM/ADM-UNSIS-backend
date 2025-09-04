package com.unsis.admunsisbackend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Zone {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

}
