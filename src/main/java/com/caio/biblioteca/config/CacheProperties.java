package com.caio.biblioteca.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Validated
@ConfigurationProperties(prefix = "biblioteca.cache")
public record CacheProperties(

        @NotBlank(message = "O prefixo do cache é obrigatório.")
        String prefixo,

        @NotNull(message = "O TTL do cache é obrigatório.")
        @DurationMin(seconds = 1, message = "O TTL deve ser de no mínimo 1 segundo.")
        Duration ttl
) {
}