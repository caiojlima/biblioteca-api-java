package com.caio.biblioteca.mapper;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.dto.response.LivroResponse;
import com.caio.biblioteca.entity.Livro;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class LivroMapper {

    private final ModelMapper modelMapper;

    public LivroMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Livro toEntity(LivroRequest request) {
        return modelMapper.map(request, Livro.class);
    }

    public LivroResponse toResponse(Livro livro) {
        return modelMapper.map(livro, LivroResponse.class);
    }

    public void updateEntity(LivroRequest request, Livro livro) {
        Livro atualizado = modelMapper.map(request, Livro.class);

        livro.setTitulo(atualizado.getTitulo());
        livro.setAutor(atualizado.getAutor());
        livro.setIsbn(atualizado.getIsbn());
        livro.setAnoPublicacao(atualizado.getAnoPublicacao());
        livro.setGenero(atualizado.getGenero());
        livro.setDisponivel(atualizado.getDisponivel());
    }
}