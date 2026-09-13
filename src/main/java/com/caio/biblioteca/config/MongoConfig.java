package com.caio.biblioteca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing(modifyOnCreate = true)
public class MongoConfig {
}
