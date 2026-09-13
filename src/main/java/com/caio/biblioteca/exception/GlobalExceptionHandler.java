package com.caio.biblioteca.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> handleNegocioException(
            NegocioException exception
    ) {
        ErrorResponse error = new ErrorResponse(
                exception.getCodigo(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        HttpStatus status = definirStatus(exception.getCodigo());

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String mensagem = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dados inválidos.");

        ErrorResponse error = new ErrorResponse(
                "DADOS_INVALIDOS",
                mensagem,
                LocalDateTime.now()
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    private HttpStatus definirStatus(String codigo) {
        return switch (codigo) {
            case "LIVRO_NAO_ENCONTRADO" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
