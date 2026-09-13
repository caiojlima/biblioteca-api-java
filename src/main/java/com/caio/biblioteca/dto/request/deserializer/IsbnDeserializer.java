package com.caio.biblioteca.dto.request.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class IsbnDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String valor = p.getText();
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("[\\s\\-]", "").toUpperCase();
    }
}