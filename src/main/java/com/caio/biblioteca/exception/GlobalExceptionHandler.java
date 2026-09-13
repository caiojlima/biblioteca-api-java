package com.caio.biblioteca.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> handleNegocio(NegocioException ex) {
        log.warn("Erro de negócio [{}]: {}", ex.getCodigo(), ex.getMessage());
        ErrorResponse erro = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(ex.getStatus()).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Erro de validação: {}", mensagem);

        ErrorResponse erro = new ErrorResponse(
                "DADOS_INVALIDOS",
                mensagem,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String mensagem = "Corpo da requisição inválido ou malformado.";

        if (ex.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String campo = ife.getPath().isEmpty()
                        ? "desconhecido"
                        : ife.getPath().get(0).getFieldName();
                mensagem = "Valor inválido para o campo '" + campo + "': "
                        + ife.getValue() + ". Verifique os valores aceitos.";
            } else {
                String campo = ife.getPath().isEmpty()
                        ? "desconhecido"
                        : ife.getPath().get(0).getFieldName();
                mensagem = "Valor inválido para o campo '" + campo + "': " + ife.getValue() + ".";
            }
        }

        log.warn("Erro de desserialização: {}", mensagem);

        ErrorResponse erro = new ErrorResponse(
                "DADOS_INVALIDOS",
                mensagem,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensagem;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            String valoresAceitos = Arrays.stream(ex.getRequiredType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "': "
                    + ex.getValue() + ". Valores aceitos: " + valoresAceitos + ".";
        } else {
            mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue() + ".";
        }

        log.warn("Erro de tipo em parâmetro: {}", mensagem);

        ErrorResponse erro = new ErrorResponse("DADOS_INVALIDOS", mensagem, LocalDateTime.now());
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        String mensagem = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("Erro de validação em parâmetros: {}", mensagem);

        ErrorResponse erro = new ErrorResponse(
                "DADOS_INVALIDOS",
                mensagem,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        String mensagem = ex.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(MessageSourceResolvable::getDefaultMessage))
                .collect(Collectors.joining("; "));

        log.warn("Erro de validação de parâmetros: {}", mensagem);

        ErrorResponse erro = new ErrorResponse(
                "DADOS_INVALIDOS",
                mensagem,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex) {
        log.error("Erro inesperado não tratado", ex);

        ErrorResponse erro = new ErrorResponse(
                "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}