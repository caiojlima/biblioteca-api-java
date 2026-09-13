package com.caio.biblioteca.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class AnoPublicacaoValidator implements ConstraintValidator<AnoPublicacaoValido, Integer> {
    @Override
    public boolean isValid(Integer ano, ConstraintValidatorContext context) {
        if (ano == null) return true; // deixa o @NotNull cuidar
        int anoAtual = Year.now().getValue();
        return ano > 1000 && ano <= anoAtual;
    }
}
