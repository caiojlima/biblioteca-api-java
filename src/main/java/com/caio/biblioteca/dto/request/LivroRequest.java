package com.caio.biblioteca.dto.request;

import com.caio.biblioteca.dto.request.deserializer.IsbnDeserializer;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.validation.AnoPublicacaoValido;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LivroRequest(

        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
        String titulo,

        @NotBlank(message = "O autor é obrigatório.")
        @Size(max = 255, message = "O autor deve ter no máximo 255 caracteres.")
        String autor,

        @NotBlank(message = "O ISBN é obrigatório.")
        @JsonDeserialize(using = IsbnDeserializer.class)
        @Pattern(
                regexp = "^(?:\\d{9}[\\dX]|\\d{13})$",
                message = "O ISBN deve estar no formato ISBN-10 ou ISBN-13."
        )
        String isbn,

        @NotNull(message = "O ano de publicação é obrigatório.")
        @AnoPublicacaoValido
        Integer anoPublicacao,

        @NotNull(message = "O gênero é obrigatório.")
        Genero genero,

        @NotNull(message = "A disponibilidade é obrigatória.")
        Boolean disponivel
) {
}