package com.caio.biblioteca.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AnoPublicacaoValidator.class)
public @interface AnoPublicacaoValido {
    String message() default "Ano de publicação deve ser maior que 1000 e menor ou igual ao ano atual.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}