package com.caio.biblioteca.service;

import com.caio.biblioteca.dto.request.LivroRequest;
import com.caio.biblioteca.dto.response.LivroResponse;
import com.caio.biblioteca.entity.Livro;
import com.caio.biblioteca.enums.Genero;
import com.caio.biblioteca.exception.NegocioException;
import com.caio.biblioteca.mapper.LivroMapper;
import com.caio.biblioteca.repository.LivroRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class LivroService {

    private final LivroRepository livroRepository;
    private final LivroMapper livroMapper;

    public LivroService(
            LivroRepository livroRepository,
            LivroMapper livroMapper
    ) {
        this.livroRepository = livroRepository;
        this.livroMapper = livroMapper;
    }

    public LivroResponse criar(LivroRequest request) {
        validarAnoPublicacao(request.anoPublicacao());
        validarIsbnUnico(request.isbn());

        Livro livro = livroMapper.toEntity(request);

        Livro livroSalvo = livroRepository.save(livro);

        return livroMapper.toResponse(livroSalvo);
    }

    @Cacheable(cacheNames = "livro", key = "#id")
    public LivroResponse buscarPorId(String id) {
        Livro livro = buscarLivroOuLancar(id);

        return livroMapper.toResponse(livro);
    }

    public Page<LivroResponse> listar(Pageable pageable) {
        return livroRepository.findAll(pageable)
                .map(livroMapper::toResponse);
    }

    public Page<LivroResponse> listarPorGenero(
            Genero genero,
            Pageable pageable
    ) {
        return livroRepository.findByGenero(genero, pageable)
                .map(livroMapper::toResponse);
    }

    @CacheEvict(cacheNames = "livro", key = "#id")
    public LivroResponse atualizar(String id, LivroRequest request) {
        Livro livro = buscarLivroOuLancar(id);

        validarAnoPublicacao(request.anoPublicacao());
        validarIsbnUnicoNaAtualizacao(request.isbn(), id);

        livroMapper.updateEntity(request, livro);

        Livro livroAtualizado = livroRepository.save(livro);

        return livroMapper.toResponse(livroAtualizado);
    }

    @CacheEvict(cacheNames = "livro", key = "#id")
    public void excluir(String id) {
        Livro livro = buscarLivroOuLancar(id);

        livroRepository.delete(livro);
    }

    private Livro buscarLivroOuLancar(String id) {
        return livroRepository.findById(id)
                .orElseThrow(() -> livroNaoEncontrado(id));
    }

    private void validarIsbnUnico(String isbn) {
        if (livroRepository.existsByIsbn(isbn)) {
            throw isbnDuplicado(isbn);
        }
    }

    private void validarIsbnUnicoNaAtualizacao(String isbn, String id) {
        if (livroRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw isbnDuplicado(isbn);
        }
    }

    private void validarAnoPublicacao(Integer anoPublicacao) {
        int anoAtual = Year.now().getValue();

        if (anoPublicacao <= 1000 || anoPublicacao > anoAtual) {
            throw new NegocioException(
                    "ANO_PUBLICACAO_INVALIDO",
                    "O ano de publicação deve ser maior que 1000 e menor ou igual a "
                            + anoAtual + "."
            );
        }
    }

    private NegocioException livroNaoEncontrado(String id) {
        return new NegocioException(
                "LIVRO_NAO_ENCONTRADO",
                "Livro com id '" + id + "' não encontrado."
        );
    }

    private NegocioException isbnDuplicado(String isbn) {
        return new NegocioException(
                "ISBN_DUPLICADO",
                "Já existe um livro cadastrado com o ISBN '" + isbn + "'."
        );
    }
}
