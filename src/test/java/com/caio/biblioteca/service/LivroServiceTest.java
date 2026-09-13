package com.caio.biblioteca.service;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.dto.response.LivroResponse;
import com.caio.biblioteca.entity.Livro;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.exception.NegocioException;
import com.caio.biblioteca.mapper.LivroMapper;
import com.caio.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private LivroMapper livroMapper;

    @InjectMocks
    private LivroService livroService;

    private LivroRequest request;
    private Livro livro;
    private LivroResponse response;

    @BeforeEach
    void setUp() {
        request = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true
        );

        livro = Livro.builder()
                .id("123")
                .titulo("Clean Code")
                .autor("Robert C. Martin")
                .isbn("9780132350884")
                .anoPublicacao(2008)
                .genero(Genero.TECNOLOGIA)
                .disponivel(true)
                .build();

        response = new LivroResponse(
                "123",
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                2008,
                Genero.TECNOLOGIA,
                true,
                null,
                null
        );
    }

    @Test
    void deveCriarLivroComSucesso() {
        when(livroRepository.existsByIsbn(request.isbn()))
                .thenReturn(false);

        when(livroMapper.toEntity(request))
                .thenReturn(livro);

        when(livroRepository.save(livro))
                .thenReturn(livro);

        when(livroMapper.toResponse(livro))
                .thenReturn(response);

        LivroResponse resultado = livroService.criar(request);

        assertNotNull(resultado);
        assertEquals("123", resultado.id());
        assertEquals("Clean Code", resultado.titulo());

        verify(livroRepository).existsByIsbn(request.isbn());
        verify(livroRepository).save(livro);
    }

    @Test
    void deveLancarExcecaoQuandoIsbnJaExiste() {
        when(livroRepository.existsByIsbn(request.isbn()))
                .thenReturn(true);

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.criar(request)
        );

        assertEquals("ISBN_DUPLICADO", exception.getCodigo());

        verify(livroRepository).existsByIsbn(request.isbn());
        verify(livroRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoAnoPublicacaoForInvalido() {
        LivroRequest requestInvalido = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                1000,
                Genero.TECNOLOGIA,
                true
        );

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.criar(requestInvalido)
        );

        assertEquals("ANO_PUBLICACAO_INVALIDO", exception.getCodigo());

        verifyNoInteractions(livroRepository);
        verifyNoInteractions(livroMapper);
    }

    @Test
    void deveBuscarLivroPorIdComSucesso() {
        when(livroRepository.findById("123"))
                .thenReturn(Optional.of(livro));

        when(livroMapper.toResponse(livro))
                .thenReturn(response);

        LivroResponse resultado = livroService.buscarPorId("123");

        assertNotNull(resultado);
        assertEquals("123", resultado.id());
        assertEquals("Clean Code", resultado.titulo());

        verify(livroRepository).findById("123");
        verify(livroMapper).toResponse(livro);
    }

    @Test
    void deveLancarExcecaoQuandoLivroNaoForEncontrado() {
        when(livroRepository.findById("999"))
                .thenReturn(Optional.empty());

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.buscarPorId("999")
        );

        assertEquals("LIVRO_NAO_ENCONTRADO", exception.getCodigo());

        verify(livroRepository).findById("999");
        verify(livroMapper, never()).toResponse(any());
    }

    @Test
    void deveExcluirLivroComSucesso() {
        when(livroRepository.findById("123"))
                .thenReturn(Optional.of(livro));

        livroService.excluir("123");

        verify(livroRepository).findById("123");
        verify(livroRepository).delete(livro);
    }

    @Test
    void deveLancarExcecaoAoExcluirLivroInexistente() {
        when(livroRepository.findById("999"))
                .thenReturn(Optional.empty());

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.excluir("999")
        );

        assertEquals("LIVRO_NAO_ENCONTRADO", exception.getCodigo());

        verify(livroRepository).findById("999");
        verify(livroRepository, never()).delete(any());
    }
}