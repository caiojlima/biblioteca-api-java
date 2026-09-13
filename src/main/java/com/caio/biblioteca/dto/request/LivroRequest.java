package com.caio.biblioteca.dto.request;

import com.caio.biblioteca.enums.Genero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LivroRequest(

        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
        String titulo,

        @NotBlank(message = "O autor é obrigatório.")
        @Size(max = 255, message = "O autor deve ter no máximo 255 caracteres.")
        String autor,

        @NotBlank(message = "O ISBN é obrigatório.")
        String isbn,

        @NotNull(message = "O ano de publicação é obrigatório.")
        Integer anoPublicacao,

        @NotNull(message = "O gênero é obrigatório.")
        Genero genero,

        @NotNull(message = "A disponibilidade é obrigatória.")
        Boolean disponivel
) {
}