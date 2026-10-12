package com.manush.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Minimal Mongo config — most is auto-configured via application.yml.
 * Extend here only for GridFS, custom converters, or index management.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.manush")
public class MongoConfig {
    // Spring Boot auto-config handles connection, codec registry, etc.
    // Add @Bean for GridFsTemplate if GridFS storage is enabled.
}
