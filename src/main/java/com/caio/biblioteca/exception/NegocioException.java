package com.caio.biblioteca.exception;

public class NegocioException extends RuntimeException {

    private final String codigo;

    public NegocioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}