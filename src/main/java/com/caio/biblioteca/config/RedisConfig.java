package com.caio.biblioteca.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper
    ) {
        RedisCacheConfiguration cacheConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .computePrefixWith(cacheName -> "biblioteca:livro:")
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new GenericJackson2JsonRedisSerializer(
                                                        redisObjectMapper
                                                )
                                        )
                        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}