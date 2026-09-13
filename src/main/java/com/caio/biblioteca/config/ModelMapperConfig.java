package com.caio.biblioteca.config;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.dto.response.LivroResponse;
import com.caio.biblioteca.entity.Livro;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<LivroRequest, Livro> livroRequestConverter =
                new Converter<>() {
                    @Override
                    public Livro convert(
                            MappingContext<LivroRequest, Livro> context
                    ) {
                        LivroRequest request = context.getSource();

                        return Livro.builder()
                                .titulo(request.titulo())
                                .autor(request.autor())
                                .isbn(request.isbn())
                                .anoPublicacao(request.anoPublicacao())
                                .genero(request.genero())
                                .disponivel(request.disponivel())
                                .build();
                    }
                };

        Converter<Livro, LivroResponse> livroResponseConverter =
                new Converter<>() {
                    @Override
                    public LivroResponse convert(
                            MappingContext<Livro, LivroResponse> context
                    ) {
                        Livro livro = context.getSource();

                        return new LivroResponse(
                                livro.getId(),
                                livro.getTitulo(),
                                livro.getAutor(),
                                livro.getIsbn(),
                                livro.getAnoPublicacao(),
                                livro.getGenero(),
                                livro.getDisponivel(),
                                livro.getDataInclusao(),
                                livro.getDataAtualizacao()
                        );
                    }
                };

        modelMapper.addConverter(livroRequestConverter);
        modelMapper.addConverter(livroResponseConverter);

        return modelMapper;
    }
}