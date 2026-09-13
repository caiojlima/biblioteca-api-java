package com.caio.biblioteca.dto.response;

import com.caio.biblioteca.enums.Genero;

import java.time.LocalDateTime;

public record LivroResponse(
        String id,
        String titulo,
        String autor,
        String isbn,
        Integer anoPublicacao,
        Genero genero,
        Boolean disponivel,
        LocalDateTime dataInclusao,
        LocalDateTime dataAtualizacao
) {
}