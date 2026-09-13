package com.caio.biblioteca.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String codigo,
        String mensagem,
        LocalDateTime timestamp
) {
}
