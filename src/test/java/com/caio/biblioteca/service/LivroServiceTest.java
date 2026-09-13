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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    // ---------- CRIAR ----------

    @Test
    void deveCriarLivroComSucesso() {
        when(livroRepository.existsByIsbn(request.isbn())).thenReturn(false);
        when(livroMapper.toEntity(request)).thenReturn(livro);
        when(livroRepository.save(livro)).thenReturn(livro);
        when(livroMapper.toResponse(livro)).thenReturn(response);

        LivroResponse resultado = livroService.criar(request);

        assertNotNull(resultado);
        assertEquals("123", resultado.id());
        assertEquals("Clean Code", resultado.titulo());

        verify(livroRepository).existsByIsbn(request.isbn());
        verify(livroRepository).save(livro);
    }

    @Test
    void deveLancarExcecaoQuandoIsbnJaExiste() {
        when(livroRepository.existsByIsbn(request.isbn())).thenReturn(true);

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.criar(request)
        );

        assertEquals("ISBN_DUPLICADO", exception.getCodigo());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(livroRepository).existsByIsbn(request.isbn());
        verify(livroRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoAnoPublicacaoForMenorQue1001() {
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
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verifyNoInteractions(livroRepository);
        verifyNoInteractions(livroMapper);
    }

    @Test
    void deveLancarExcecaoQuandoAnoPublicacaoForNulo() {
        LivroRequest requestInvalido = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                null,
                Genero.TECNOLOGIA,
                true
        );

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.criar(requestInvalido)
        );

        assertEquals("ANO_PUBLICACAO_INVALIDO", exception.getCodigo());
        verifyNoInteractions(livroRepository);
    }

    // ---------- BUSCAR POR ID ----------

    @Test
    void deveBuscarLivroPorIdComSucesso() {
        when(livroRepository.findById("123")).thenReturn(Optional.of(livro));
        when(livroMapper.toResponse(livro)).thenReturn(response);

        LivroResponse resultado = livroService.buscarPorId("123");

        assertNotNull(resultado);
        assertEquals("123", resultado.id());
        assertEquals("Clean Code", resultado.titulo());

        verify(livroRepository).findById("123");
        verify(livroMapper).toResponse(livro);
    }

    @Test
    void deveLancarExcecaoQuandoLivroNaoForEncontrado() {
        when(livroRepository.findById("999")).thenReturn(Optional.empty());

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.buscarPorId("999")
        );

        assertEquals("LIVRO_NAO_ENCONTRADO", exception.getCodigo());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(livroRepository).findById("999");
        verify(livroMapper, never()).toResponse(any());
    }

    // ---------- LISTAR ----------

    @Test
    void deveListarTodosOsLivrosQuandoGeneroForNulo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Livro> pagina = new PageImpl<>(List.of(livro));

        when(livroRepository.findAll(pageable)).thenReturn(pagina);
        when(livroMapper.toResponse(livro)).thenReturn(response);

        Page<LivroResponse> resultado = livroService.listar(null, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Clean Code", resultado.getContent().get(0).titulo());

        verify(livroRepository).findAll(pageable);
        verify(livroRepository, never()).findByGenero(any(), any());
    }

    @Test
    void deveListarLivrosFiltrandoPorGenero() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Livro> pagina = new PageImpl<>(List.of(livro));

        when(livroRepository.findByGenero(Genero.TECNOLOGIA, pageable)).thenReturn(pagina);
        when(livroMapper.toResponse(livro)).thenReturn(response);

        Page<LivroResponse> resultado = livroService.listar(Genero.TECNOLOGIA, pageable);

        assertEquals(1, resultado.getTotalElements());

        verify(livroRepository).findByGenero(Genero.TECNOLOGIA, pageable);
        verify(livroRepository, never()).findAll(any(Pageable.class));
    }

    // ---------- ATUALIZAR ----------

    @Test
    void deveAtualizarLivroComSucesso() {
        when(livroRepository.findById("123")).thenReturn(Optional.of(livro));
        when(livroRepository.existsByIsbnAndIdNot(request.isbn(), "123")).thenReturn(false);
        when(livroRepository.save(livro)).thenReturn(livro);
        when(livroMapper.toResponse(livro)).thenReturn(response);

        LivroResponse resultado = livroService.atualizar("123", request);

        assertNotNull(resultado);
        assertEquals("123", resultado.id());

        verify(livroMapper).updateEntity(request, livro);
        verify(livroRepository).save(livro);
    }

    @Test
    void deveLancarExcecaoAoAtualizarLivroInexistente() {
        when(livroRepository.findById("999")).thenReturn(Optional.empty());

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.atualizar("999", request)
        );

        assertEquals("LIVRO_NAO_ENCONTRADO", exception.getCodigo());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(livroRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAtualizarComIsbnDeOutroLivro() {
        when(livroRepository.findById("123")).thenReturn(Optional.of(livro));
        when(livroRepository.existsByIsbnAndIdNot(request.isbn(), "123")).thenReturn(true);

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.atualizar("123", request)
        );

        assertEquals("ISBN_DUPLICADO", exception.getCodigo());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(livroRepository, never()).save(any());
    }

    // ---------- EXCLUIR ----------

    @Test
    void deveExcluirLivroComSucesso() {
        when(livroRepository.findById("123")).thenReturn(Optional.of(livro));

        livroService.excluir("123");

        verify(livroRepository).findById("123");
        verify(livroRepository).delete(livro);
    }

    @Test
    void deveLancarExcecaoAoExcluirLivroInexistente() {
        when(livroRepository.findById("999")).thenReturn(Optional.empty());

        NegocioException exception = assertThrows(
                NegocioException.class,
                () -> livroService.excluir("999")
        );

        assertEquals("LIVRO_NAO_ENCONTRADO", exception.getCodigo());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(livroRepository).findById("999");
        verify(livroRepository, never()).delete(any());
    }
}