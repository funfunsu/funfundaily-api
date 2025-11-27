package com.funfun.schedule.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MapStruct mappers
 * Since we've set defaultComponentModel=spring in pom.xml,
 * MapStruct will automatically generate implementations that are Spring components
 */
@Configuration
public class MapStructConfig {
    // No need to define explicit beans as MapStruct will generate Spring components automatically
}